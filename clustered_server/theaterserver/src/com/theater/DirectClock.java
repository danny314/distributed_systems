package com.theater;

/*
 * Authors:
 * Puneet Bansal, Sadaf H Syed
 */

public class DirectClock {

	private Long[] timestamp;
	
	private Integer myServerInstance;
	
	public DirectClock(Integer totalServerCount, Integer myServerInstance) {
		timestamp = new Long[totalServerCount]; 
		for (int i=0; i < totalServerCount; i++) {
			timestamp[i] = 0L;
		}
		timestamp[myServerInstance] = 1L;
		this.myServerInstance = myServerInstance;
	}

	public Long getValue(Integer serverInstance) {
		return timestamp[serverInstance];
	}
	
	public Long[] getTimestamp() {
		return timestamp;
	}

	public void sendAction() {
		tick();
	}
	
	public void receiveAction(Integer sender, Long sentValue) {
		timestamp[sender] = timestamp[sender] > sentValue  ? timestamp[sender] : sentValue;
		timestamp[myServerInstance] = (timestamp[myServerInstance] > sentValue  ? timestamp[myServerInstance] : sentValue) + 1;
	}
	
	public void tick() {
		timestamp[myServerInstance]++;
	}
	
	public Integer getMyServerInstance() {
		return myServerInstance;
	}

	public void setMyServerInstance(Integer myServerInstance) {
		this.myServerInstance = myServerInstance;
	}

	public String toString() {
		return "TS-" + this.timestamp + ";" + "ServerID-" + this.getMyServerInstance();
	}
	
}
