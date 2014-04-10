package com.theater;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/*
 * Authors:
 * Puneet Bansal, Sadaf H Syed
 */
public class TheaterServer extends Thread {
	
	//This server's id
	private Integer serverInstance;
	
	//Properties from config file
	PropertyReader properties;
	
	//Total number of servers in the cluster (configured in config file)
	Integer serverCount;
	
	//Request queue
	private RequestQueue myQ;
	
	//Object containing reservation chart and available seats
	private TheaterMap myTheaterMap;
	
	private DirectClock directClock;
	
    private DatagramSocket dataSocket;
    
	//Queue to hold requests that arrived out of order due to non FIFO nature of UDP
    private Map<Integer, DatagramPacket> deferredQ;
    
    //Seq number to assign to requests to ensure FIFO
	private Integer seqNo;
	
	//Holds physical time when the last ping was received from all servers. This is used to determine if a server is dead or alive.
	private Long[] lastPingRecieved;
	
	//Keeps track of if my join to the cluster has been acknowledged by at least one server.
	private Boolean joinAckReceived = Boolean.FALSE;

	//Initializez all data structures
	public TheaterServer(int serverInstance, String configFilePath) {
    	this.serverInstance = serverInstance;
    	this.properties = new PropertyReader(configFilePath);
    	this.serverCount = properties.getServerCount();
    	
    	System.out.println(getServerIdString() + "Initializing theater");
    	
    	directClock = new DirectClock(serverCount,serverInstance);
    	
    	myQ = new RequestQueue(serverCount);
    	myTheaterMap = new TheaterMap(properties.getTotalSeats());
    	
    	deferredQ = new TreeMap<Integer, DatagramPacket>();  	
    	this.seqNo = 0;
    	
    	lastPingRecieved = new Long[serverCount];
    	for (int i=0; i < serverCount; i++) {
    		lastPingRecieved[i] = 0L;
    	}
    }
	
	public PropertyReader getProperties() {
		return properties;
	}
	
	public static void main(String args[]) {
		TheaterServer server = new TheaterServer(Integer.parseInt(args[0]),args[1]);
		int serverPort = server.getProperties().getServerPort(server.getServerInstance());
    	System.out.println("Server " + server.getServerInstance() + ": Listening for UDP requests on port " + serverPort);
		
    	//Start a ping thread, that will periodically broadcast ping message to tell other serviers that this instance is alive.
    	PingThread pingThread = new PingThread(server, server.getProperties().getTimeout());
    	pingThread.start();
    	
    	//On instance start, broadcast a join message, so that other servers can respond with the latest reservation chart.
    	ServerMessage joinMessage = ServerMessage.getNewJoinMsg(server.getServerInstance());
    	server.broadcastMessage(joinMessage);
    	
    	try {
			server.dataSocket = new DatagramSocket(serverPort);
			while (true) {
				byte[] buffer = new byte[102400];
				DatagramPacket dataPacket = new DatagramPacket(buffer,buffer.length);
				try {
					server.dataSocket.receive(dataPacket);
					server.directClock.tick();
					ServerMessage message = server.getServerMessageFromBytes(dataPacket);

					if (server.isClientMessage(message)) {
						
						//check if request queue is empty
						if(server.getMyQ().getMyReq(server.getServerInstance()) == RequestQueue.NO_REQUEST) {
							// start a separate thread to handle client's request
							UDPServerThread reservationThread = new UDPServerThread(server.dataSocket,dataPacket, server);
							reservationThread.start();
						}
						else{
							//defer the request
							server.deferredQ.put(server.seqNo++, dataPacket);
						}
					} else {
						server.handleServerMessage(dataPacket);
					}
					
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		} catch (SocketException e) {
			e.printStackTrace();
		}
	}

	public synchronized void addReq(){
		//add request time to the request queue
		getMyQ().addReqToMyQ(getServerInstance(), getDirectClock().getValue(getServerInstance()));
	}

	public synchronized void requestCS() {
	    	System.out.println(getServerIdString() + "Requesting CS.");
	    	
	    	addReq();
	    	
	    	//create a request with request timestamp to broadcast to other servers
	    	ServerMessage requestMessage = ServerMessage.getNewRequestCSMsg(getMyQ().getMyReq(getServerInstance()),getServerInstance());
	    	
	    	//broadcast request
	    	broadcastMessage(requestMessage);
	    	
	    	//Wait until request to enter CS is granted
	    	while (!okayCS()) {
	    		waiting();
	    	}
	    }
	    
	    public synchronized void releaseCS(String lastSpectator, List<Integer> lastAssignedSeats, String lastAction) {
	    	System.out.println(getServerIdString() + "Releasing CS.");

			//remove completed request from the queue
			getMyQ().removeCompletedRequest(getServerInstance());
			
			//create a release serverMessage
			ServerMessage releaseMessage = ServerMessage.getNewReleaseCSMsg(getDirectClock().getValue(getServerInstance()),
																			getServerInstance(),
																			lastSpectator,
																			lastAssignedSeats,
																			lastAction);
			
			//broadcast release to all servers 
			broadcastMessage(releaseMessage);
	    }
	    
	    //Evaluates if this instance is allowed to enter CS
	    public Boolean okayCS() {
	    	for (int j=1; j< properties.getServerCount(); j++){
	    		if(isAlive(j)) {
		    		if (isGreater(getMyQ().getMyReq(getServerInstance()), getServerInstance(), getMyQ().getMyReq(j), j))
		    			return false;
		    		if(isGreater(getMyQ().getMyReq(getServerInstance()), getServerInstance(), getDirectClock().getValue(j), j))
		    			return false;
	    		}
	    	}
	    	return true;
	    }
	    
	    //Checks if a server is alive by comparing current time with the time the last ping was received
		private boolean isAlive(int j) {
			Long myCurrentTime = System.currentTimeMillis();
			//If it has been more than the timeout since I last received ping from server j, assume server j is dead.
			if(myCurrentTime - lastPingRecieved[j] > this.properties.getTimeout()*1000){
				return false;
			}
			
			return true;
		}

		//Compare timestamps
		private boolean isGreater(Long server1Timestamp, Integer server1Id, Long server2Timestamp ,int server2Id) {
			if(server2Timestamp == RequestQueue.NO_REQUEST)
				return false;
			return (server1Timestamp > server2Timestamp) || ((server1Timestamp == server2Timestamp) && (server1Id > server2Id));
		}

		public synchronized void waiting() {
			System.out.println(getServerIdString() + "Waiting...");
			synchronized (this) {
				try {
					this.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		
	//Handles different types of server messages when received
	private synchronized void handleServerMessage(DatagramPacket dataPacket) {
		
		ServerMessage serverMessage = getServerMessageFromBytes(dataPacket);
		MessageType receivedMsgType = serverMessage.getMessageType(); 
		
		if(receivedMsgType==MessageType.REQUEST_CS || receivedMsgType == MessageType.ACK_REQ || receivedMsgType == MessageType.RELEASE_CS ){
			directClock.receiveAction(serverMessage.getServerInstance(), serverMessage.getTimestamp());
		}
		
		switch (serverMessage.getMessageType()) {
			case REQUEST_CS:
				
				//add received request to your queue
				myQ.addReqToMyQ(serverMessage.getServerInstance(), serverMessage.getTimestamp());
				
				//Send acknowledgement of received request for CS
				ServerMessage ackServerMessage = ServerMessage.getNewRequestAckMsg(directClock.getValue(serverInstance), serverInstance);
				sendMessage(ackServerMessage, dataPacket, serverMessage.getServerInstance());
				break;
				
			case RELEASE_CS:
				//Synchonize state only if reservation chart changed
				if (serverMessage.getLastSpectator() != null && (serverMessage.getLastAssignedSeats() != null && !serverMessage.getLastAssignedSeats().isEmpty() )) { 
					updateMyTheaterMap(serverMessage);
				}
				
				myQ.removeCompletedRequest(serverMessage.getServerInstance());
				break;
			case PING:
				lastPingRecieved[serverMessage.getServerInstance()] = System.currentTimeMillis();
				break;
			case JOIN:
				ServerMessage joinAckMsg = ServerMessage.getNewJoinAckMsg(getServerInstance());
				joinAckMsg.setMyTheaterMap(myTheaterMap);
				joinAckMsg.setMyQ(getMyQ());
				sendMessage(joinAckMsg, dataPacket, serverMessage.getServerInstance());
				break;
			case JOIN_ACK:
				//If I have not already received a join ack, update my seat map and send join completed message so that other servers can resume normal operation
				if (!joinAckReceived) {
					myTheaterMap.setAvailableSeats(serverMessage.getMyTheaterMap().getAvailableSeats());
					myTheaterMap.setMaxSeats(serverMessage.getMyTheaterMap().getMaxSeats());
					myTheaterMap.setSeatMap(serverMessage.getMyTheaterMap().getSeatMap());
					System.out.println(getServerIdString() + "Synchronized state");
					
					joinAckReceived = true;
				}
				break;
			case ACK_REQ:
				//Request acknowledgement - don't need to do anything, timestamp has already been recorded by receiveAction
				break;
			default:
				System.out.println(getServerIdString() + "WARN: Into default case!  " + serverMessage);
				
		}
		notify();
	}

	private void updateMyTheaterMap(ServerMessage lastUpdate) {
		if("delete".equalsIgnoreCase(lastUpdate.getLastAction())){
			myTheaterMap.removeSeat(lastUpdate.getLastSpectator(), lastUpdate.getLastAssignedSeats());
		}
		else if ("reserve".equalsIgnoreCase(lastUpdate.getLastAction())) {
			myTheaterMap.addSeat(lastUpdate.getLastSpectator(), lastUpdate.getLastAssignedSeats());
		}
	}

	private Boolean isClientMessage(ServerMessage message) {
		if (message == null) {
			System.out.println(getServerIdString() + "ERROR: Message is null");
		}
		MessageType messageType = message.getMessageType();
		
		boolean isClientMsg = messageType == MessageType.RESERVE || messageType == MessageType.DELETE || messageType == MessageType.SEARCH;
		if (isClientMsg) {
			System.out.println(getServerIdString() + "RECEIVED client message " + message.getMessageType() + " " + message.getClientMessageData());
		}
		return isClientMsg;
	}
	
	//Broadcast a message to all other servers
	public void broadcastMessage(ServerMessage messageToSend) {
		for (int i = 1; i < serverCount; i++) {
			if (i != getServerInstance()) {
				InetAddress host = null;
				try {
					host = InetAddress.getByName(properties.getServerHost(i));
				} catch (UnknownHostException e1) {
					e1.printStackTrace();
				}
				
				
				Integer port = properties.getServerPort(i);
				
				byte[] buffer = getMessageAsBytes(messageToSend);
				DatagramSocket udpSocket = getNewSocket();
				DatagramPacket commandPacket = new DatagramPacket(buffer,buffer.length, host,port);
				
				try {
					udpSocket.send(commandPacket);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			else{
				//if  I am releasing the CS and my request queue is empty, request again
				if((messageToSend.getMessageType()==MessageType.RELEASE_CS) && (getMyQ().getMyReq(serverInstance)==RequestQueue.NO_REQUEST)){
					//check if there's a deferred request
					if(!deferredQ.isEmpty()){	
						//start another UDPServeThread to handle client's deferred request
						for(Map.Entry<Integer, DatagramPacket> entry : deferredQ.entrySet()) {
							ServerMessage message = getServerMessageFromBytes(entry.getValue());
							
							System.out.println(getServerIdString() + "Adding deferred client's request to the queue now: " + message.getMessageType() + " " + message.getClientMessageData());
							addReq();

							UDPServerThread reservationThread = new UDPServerThread(dataSocket, entry.getValue(), this);
							reservationThread.start();
							
							//delete the deferred request
							deferredQ.remove(entry.getKey());
							break;
					    } 
					}
				}
			}
		}
	}
	
	private void sendMessage(ServerMessage message, DatagramPacket destinationAddress, Integer destinationServerInstance) {
		byte[] buffer = getMessageAsBytes(message);
		DatagramSocket udpSocket = getNewSocket();
		
		InetAddress destinationIP = null;
		try {
			destinationIP = InetAddress.getByName(getProperties().getServerHost(destinationServerInstance));
		} catch (UnknownHostException e1) {
			e1.printStackTrace();
		}
		Integer destinationPort = getProperties().getServerPort(destinationServerInstance);
		DatagramPacket commandPacket = new DatagramPacket(buffer,buffer.length, destinationIP,destinationPort);
		
		try {
			udpSocket.send(commandPacket);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private byte[] getMessageAsBytes(ServerMessage message) {
		ByteArrayOutputStream bStream = new ByteArrayOutputStream();
		ObjectOutput oo;
		try {
			oo = new ObjectOutputStream(bStream);
			oo.writeObject(message);
			oo.close();
		} catch (IOException e) {
			e.printStackTrace();
		} 

		byte[] serializedMessage = bStream.toByteArray();
		return serializedMessage;
		
	}
	
	private ServerMessage getServerMessageFromBytes(DatagramPacket dataPacket) {
		byte[] receiveBuffer = dataPacket.getData();
		ServerMessage message = null;
		try {
			ObjectInputStream iStream = new ObjectInputStream(new ByteArrayInputStream(receiveBuffer));
			message = (ServerMessage) iStream.readObject();
			iStream.close();		
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return message;
	}
	
	private DatagramSocket getNewSocket() {
		DatagramSocket datagramSocket = null;
		try {
			datagramSocket = new DatagramSocket();
		} catch (SocketException e) {
			e.printStackTrace();
		}
		return datagramSocket;
	}
	

	public Integer getServerInstance() {
		return serverInstance;
	}
	
	public String getServerIdString() {
		return "Server "  + serverInstance + ":";
	}

	public DirectClock getDirectClock() {
		return directClock;
	}

	public void setDirectClock(DirectClock directClock) {
		this.directClock = directClock;
	}
	
	public RequestQueue getMyQ() {
		return myQ;
	}

	public void setMyQ(RequestQueue myQ) {
		this.myQ = myQ;
	}

	public TheaterMap getMyTheaterMap() {
		return myTheaterMap;
	}

	public void setMyTheaterMap(TheaterMap myTheaterMap) {
		this.myTheaterMap = myTheaterMap;
	}
}
