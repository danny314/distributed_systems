package com.theater;
/*
 * Authors:
 * Puneet Bansal, Sadaf H Syed
 */
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.StringTokenizer;

public class TheaterClient {
	
	PropertyReader properties; 
	
	private static Random random = new Random();
	
	public static void main(String args[]) {
		
		System.out.println("\nFollowing Commands accepted...\n");
		System.out.println("reserve <name> <seatcount>");
		System.out.println("delete <name>");
		System.out.println("search <name>");
		System.out.println("exit\n");
		
		String response = null;
				
		TheaterClient client = new TheaterClient();
		client.setProperties(new PropertyReader(args[0]));
		
		while (true) {
		
			String command = client.getUserInput();
		
			if ("exit".equalsIgnoreCase(command)) {
				System.exit(0);
			}
			
			//Select a random server instance to send command to
			int serverInstance = new Random().nextInt(client.getProperties().getServerCount()-1) + 1 ;
			
			
			while (true) {
				
				System.out.println("Sending command to server instance " + serverInstance);
				
				InetAddress host = null;
				try {
					host = InetAddress.getByName(client.getProperties().getServerHost(serverInstance));
				} catch (UnknownHostException e1) {
					e1.printStackTrace();
				}
				
				
				Integer port = client.getProperties().getServerPort(serverInstance);
				
				ServerMessage serverMessage = client.getCommandAsObject(command);
				
				byte[] buffer = client.getMessageAsBytes(serverMessage);
				DatagramPacket commandPacket = new DatagramPacket(buffer,buffer.length, host,port);
				
				try {
					DatagramSocket udpSocket = client.getUdpServerSocket();
					//send command to the server
					udpSocket.send(commandPacket);
					byte[] returnBuffer = new byte[10000];
					DatagramPacket receivePacket = new DatagramPacket(returnBuffer, returnBuffer.length);
					udpSocket.setSoTimeout(client.getProperties().getTimeout().intValue()*1000);
					
					try {
						//recieves response from the server
						udpSocket.receive(receivePacket);
					} catch (SocketTimeoutException e){
						System.out.println("Request to server " + serverInstance + " timed out, Trying next server...");
						int nextServerInstance;
						do {
							nextServerInstance = random.nextInt(client.getProperties().getServerCount()-1) + 1 ;
						} while (serverInstance == nextServerInstance);
						
						serverInstance = nextServerInstance;
						continue;
					}
					
					response = new String(returnBuffer,0,returnBuffer.length);
					System.out.println(response);
					break;
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	//Gets bytes out of the command
	private byte[] getMessageAsBytes(ServerMessage message) {
		//System.out.println("Client:Trying to serialize server message " + message);
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
	
	/***************************************************************************
	 * Prompts user to input command  
	****************************************************************************/
	private String getUserInput(){
		
		String command = null;
		
		do {
			System.out.println("\nEnter command...\n");
			BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
			try {
		         command = br.readLine();
		      } catch (IOException ioe) {
		         System.out.println("IO error trying to read command!");
		         System.exit(1);
		      }
		} while (!isValidCommand(command));
		return command;
	}
	
	//Converts the command entered from command line into an object to be sent to the server
	private ServerMessage getCommandAsObject(String commandLine) {
		
		StringTokenizer tokenizer = new StringTokenizer(commandLine," ");
		String command = tokenizer.nextToken();
		ServerMessage serverMessage = new ServerMessage();
		if ("reserve".equalsIgnoreCase(command)) {
			serverMessage.setMessageType(MessageType.RESERVE);
			String commandArgs =  tokenizer.nextToken() + " " + tokenizer.nextToken();
			serverMessage.setClientMessageData(commandArgs);
		} else if ("delete".equalsIgnoreCase(command)) {
			serverMessage.setMessageType(MessageType.DELETE);
			serverMessage.setClientMessageData(tokenizer.nextToken());
		} else if ("search".equalsIgnoreCase(command)) {
			serverMessage.setMessageType(MessageType.SEARCH);
			serverMessage.setClientMessageData(tokenizer.nextToken());
		} else {
			System.out.println("ERROR:Invalid command " +  command);
		}
		return serverMessage;
	}
	
	/***************************************************************************
	 * Validates that command entered by the user is valid 
	****************************************************************************/
	private boolean isValidCommand(String command){
		
		boolean isValidCommand = false;
		
		if (command != null) {
			StringTokenizer tokenizer = new StringTokenizer(command," ");
			
			List<String> tokens = new ArrayList<String>();
			
			while (tokenizer.hasMoreTokens()) {
				tokens.add(tokenizer.nextToken());
			}
			
			if (tokens.size() == 1) {
				isValidCommand = "exit".equalsIgnoreCase(tokens.get(0)); 
			} else if (tokens.size() ==  2) {
				String operation = tokens.get(0);
				
				isValidCommand =  ("delete".equalsIgnoreCase(operation)) || ("search".equalsIgnoreCase(operation));
			} else if (tokens.size() == 3) {
				String operation = tokens.get(0);
				Integer seatNumber = null;
				try {
					seatNumber = Integer.parseInt(tokens.get(2));
				} catch (NumberFormatException nfe) {
				}
				isValidCommand = "reserve".equalsIgnoreCase(operation) && seatNumber != null;
			} else {
				isValidCommand = false;
			}
			
		}
			
			if (!isValidCommand) {
				System.out.println("\nFollowing Commands accepted...\n");
				System.out.println("reserve <name> <seatcount>");
				System.out.println("delete <name>");
				System.out.println("search <name>");
				System.out.println("exit\n");
			}
			return isValidCommand;
	}
	
	private DatagramSocket getUdpServerSocket() {
		DatagramSocket datagramSocket = null;
		try {
			datagramSocket = new DatagramSocket();
		} catch (SocketException e) {
			e.printStackTrace();
		}
		return datagramSocket;
	}

	public PropertyReader getProperties() {
		return properties;
	}

	public void setProperties(PropertyReader properties) {
		this.properties = properties;
	}
	
}
