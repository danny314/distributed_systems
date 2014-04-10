package com.theater;
/*
 * Authors:
 * Puneet Bansal, Sadaf H Syed
 */
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

public class UDPServerThread extends Thread {
	
    private DatagramSocket socket = null;
    private DatagramPacket dataPacket;
    private String command =null;
    private TheaterServer server;
    private TheaterMap myTheaterMap;
    
	private String lastSpectator;
	private List<Integer> lastAssignedSeats;
	private String lastAction;
	
    public UDPServerThread(DatagramSocket socket, DatagramPacket dataPacket, TheaterServer server) {
		this.socket = socket;
		this.dataPacket = dataPacket;
		this.server = server;
		this.myTheaterMap = server.getMyTheaterMap();
  
		byte[] receiveBuffer = dataPacket.getData();
		this.command = new String(receiveBuffer,0,receiveBuffer.length);
    
  }

    @Override
    public synchronized void run() {
    	
    	server.requestCS();
    	
    	ServerMessage clientMessage = getServerMessageFromBytes(dataPacket);
    	this.command = clientMessage.getMessageType() + " " + clientMessage.getClientMessageData();
    	System.out.println(getThreadId() + "Got permission to enter CS. Processing command " + this.command);
		String response = executeCommand(command);
		
		//Send response back if there is something to send
		if (response != null) {
			byte[] returnBuffer = new byte[10000];
			
			returnBuffer = response.getBytes();
			
			DatagramPacket returnPacket = new DatagramPacket(returnBuffer,returnBuffer.length,dataPacket.getAddress(),dataPacket.getPort());
			try {
				socket.send(returnPacket);
				server.getDirectClock().tick();
			} catch (IOException e) {
				e.printStackTrace();
			}
			server.releaseCS(lastSpectator, lastAssignedSeats, lastAction);
		}
    }
	
    /*
	 * Parse the command sent by the client and delegate to appropriate command handler. 
	 */
    protected String executeCommand(String command) {
		String response =  null;
		StringTokenizer tokenizer = new StringTokenizer(command," ");
		String operation = tokenizer.nextToken().trim();
		String spectatorName;

		lastAction = operation;
		if ("reserve".equalsIgnoreCase(operation)) {
			spectatorName = tokenizer.nextToken().trim();
			Integer seatCount = null; 
			if (tokenizer.hasMoreTokens()) {
				String seatCountStr = tokenizer.nextToken().trim();
				seatCount = Integer.parseInt(seatCountStr);
			}
			response = reserveSeat(spectatorName,seatCount);
		} else if ("delete".equalsIgnoreCase(operation)) {
			spectatorName = tokenizer.nextToken().trim();
			response = deleteSeat(spectatorName);
		} else if ("search".equalsIgnoreCase(operation)) {
			spectatorName = tokenizer.nextToken().trim();
			response = searchSeat(spectatorName);
		} 
/*		synchronized (myTheaterMap.getSeatMap()) {
			System.out.println(getThreadId() + "Reservation chart = " + myTheaterMap.getSeatMap());	
		}
		synchronized (myTheaterMap.getAvailableSeats()) {
			System.out.println(getThreadId() + "Available seats = " + myTheaterMap.getAvailableSeats());	
		}
*/		
		return response;
	}

	/*
	 * Reserve a seat for the spectator
	 */
    private String reserveSeat(String spectatorName, int seatCount) {
    	//Default to sold out - No seat available.
		String response = "-1";
    	synchronized (myTheaterMap.getAvailableSeats()) {
			if (!myTheaterMap.getAvailableSeats().isEmpty() && myTheaterMap.getAvailableSeats().size() >= seatCount) {
				List<Integer> assignedSeats = myTheaterMap.getSeatMap().get(spectatorName);

				//Seats already booked against the name provided.
				if (assignedSeats != null && !assignedSeats.isEmpty()) {
					response = "-2";
					return response;
				} else {
					assignedSeats = new ArrayList<Integer>(seatCount);
				}
				
				for (int i=0; i < seatCount; i++) {
					assignedSeats.add(myTheaterMap.getAvailableSeats().get(i));
				}
				
				//add seat to the map and remove the seat number from availableSeats
				myTheaterMap.addSeat(spectatorName, assignedSeats);
				//We store the following attributes to send along with release message, so other servers can update their state
				lastSpectator = spectatorName;
				lastAssignedSeats = assignedSeats;
				
				response = assignedSeats.toString();
			}
    	}
		System.out.println(getThreadId() + response);
		return response;
	}
	
	/**
	 * Free an already reserved seat by a spectator. Put it back into the
	 * pool of available seats. 
	 */
    private String deleteSeat(String spectatorName) {
		List<Integer> freedSeats = null;
		//No reservation found 
		String response = "-1";
		synchronized (myTheaterMap.getAvailableSeats()) {
			freedSeats = myTheaterMap.getSeatMap().get(spectatorName);
			if (freedSeats != null) {
				myTheaterMap.removeSeat(spectatorName, freedSeats);

				//We store the following attributes to send along with release message, so other servers can update their state
				lastSpectator = spectatorName;
				lastAssignedSeats = freedSeats;
				
				response = freedSeats.toString(); 
			}
		}
		return response;
	}
	
	/**
	 * Find if a seat is reserved against a name.
	 */
    private String searchSeat(String spectatorName) {
		List<Integer> assignedSeats = null;
		//No reservation found 
		String response = "-1";
		assignedSeats = myTheaterMap.getSeatMap().get(spectatorName);
		if (assignedSeats != null) {
			response = assignedSeats.toString(); 
		}
		return response;
	}
    
    private String getThreadId() {
    	return "Server " + server.getServerInstance() + ":";
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
    
}