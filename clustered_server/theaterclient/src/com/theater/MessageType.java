package com.theater;
/*
 * Authors:
 * Puneet Bansal, Sadaf H Syed
 */
public enum MessageType {
	//Different types of messages in the system
	REQUEST_CS, //Request CS
	ACK_REQ,    //Acknowledge receipt of request fo rCS
	RELEASE_CS, //Release CS
	JOIN,       //Server instance join
	JOIN_ACK,   //Acknowledgement for Server instance join
	RESERVE,    //Reserve seats
	DELETE,     //Delete seats
	SEARCH,     //Search reservation chart
	PING        //Heartbeat message for fault tolerance 
	
}
