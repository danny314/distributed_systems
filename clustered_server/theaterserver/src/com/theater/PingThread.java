package com.theater;
/*
 * Authors:
 * Puneet Bansal, Sadaf H Syed
 */

/**
 * Thread to periodically broadcast ping messages to other servers 
 */
public class PingThread extends Thread {
	
	private TheaterServer server;
	private Long timeout;
	
	public PingThread (TheaterServer server, Long timeout){
		this.server = server;
		this.timeout = timeout;
	}
	
	@Override
    public synchronized void run() {
		
		while(true){
			ServerMessage pingMsg = ServerMessage.getNewPingMsg(server.getServerInstance());
			
			server.broadcastMessage(pingMsg);
			
			try {
				Thread.sleep((timeout-2)*1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

}
