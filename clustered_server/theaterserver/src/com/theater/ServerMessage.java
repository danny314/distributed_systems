package com.theater;
/*
 * Authors:
 * Puneet Bansal, Sadaf H Syed
 */

import java.io.Serializable;
import java.util.List;

//Object representing different types of messages exchanged between severs
public class ServerMessage implements Serializable {

	private static final long serialVersionUID = 85011192580811313L;

	private Long timestamp;
	private Integer serverInstance;
	private MessageType messageType;
	
	private String clientMessageData;
	
	private String lastAction;
	private String lastSpectator;
	private List<Integer> lastAssignedSeats;
	
	private RequestQueue myQ;
	
	private TheaterMap myTheaterMap;
	
	private Integer ackServerId;
	
	public static ServerMessage getNewRequestCSMsg(Long requestTimestamp, Integer serverInstance){
		ServerMessage message = new ServerMessage();
		message.setMessageType(MessageType.REQUEST_CS);
		message.setTimestamp(requestTimestamp);
		message.setServerInstance(serverInstance);
		return message;
	}

	public static ServerMessage getNewReleaseCSMsg(Long releaseTimestamp, Integer serverInstance, 
													String lastSpectator, List<Integer> lastAssignedSeats,
													String lastAction){
		ServerMessage message = new ServerMessage();
		message.setMessageType(MessageType.RELEASE_CS);
		message.setTimestamp(releaseTimestamp);
		message.setServerInstance(serverInstance);
		message.setLastSpectator(lastSpectator);
		message.setLastAssignedSeats(lastAssignedSeats);
		message.setLastAction(lastAction);
		return message;
	}

	public static ServerMessage getNewRequestAckMsg(Long ackTimestamp, Integer ackServerId) {
		ServerMessage message = new ServerMessage();
		message.setMessageType(MessageType.ACK_REQ);
		message.setTimestamp(ackTimestamp);
		message.setServerInstance(ackServerId);
		return message;
	}
	
	public static ServerMessage getNewPingMsg(Integer serverInstance) {
		ServerMessage message = new ServerMessage();
		message.setMessageType(MessageType.PING);
		message.setServerInstance(serverInstance);
		return message;
	}

	public static ServerMessage getNewJoinMsg(Integer serverInstance){
		ServerMessage message = new ServerMessage();
		message.setMessageType(MessageType.JOIN);
		message.setServerInstance(serverInstance);
		return message;
	}

	public static ServerMessage getNewJoinAckMsg(Integer serverInstance){
		ServerMessage message = new ServerMessage();
		message.setMessageType(MessageType.JOIN_ACK);
		message.setServerInstance(serverInstance);
		return message;
	}

	public Long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Long timestamp) {
		this.timestamp = timestamp;
	}
	
	public Integer getServerInstance() {
		return serverInstance;
	}

	public void setServerInstance(Integer serverInstance) {
		this.serverInstance = serverInstance;
	}

	public MessageType getMessageType() {
		return messageType;
	}

	public void setMessageType(MessageType messageType) {
		this.messageType = messageType;
	}

	@Override
	public String toString() {
		return messageType + ":TS=" + timestamp + ";clientData=" + clientMessageData + ":LAct=" + lastAction + ":LUsr=" + lastSpectator;
	}

	public String getClientMessageData() {
		return clientMessageData;
	}

	public void setClientMessageData(String clientMessageData) {
		this.clientMessageData = clientMessageData;
	}
	
	public Integer getAckServerId() {
		return ackServerId;
	}

	public void setAckServerId(Integer ackServerId) {
		this.ackServerId = ackServerId;
	}
	
	public String getLastSpectator() {
		return lastSpectator;
	}

	public void setLastSpectator(String lastSpectator) {
		this.lastSpectator = lastSpectator;
	}

	public List<Integer> getLastAssignedSeats() {
		return lastAssignedSeats;
	}

	public void setLastAssignedSeats(List<Integer> lastAssignedSeats) {
		this.lastAssignedSeats = lastAssignedSeats;
	}

	public String getLastAction() {
		return lastAction;
	}

	public void setLastAction(String lastAction) {
		this.lastAction = lastAction;
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
