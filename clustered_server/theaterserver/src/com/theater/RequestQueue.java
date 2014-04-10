package com.theater;
/*
 * Authors:
 * Puneet Bansal, Sadaf H Syed
 */

import java.io.Serializable;

//Objec representing the request queue
public class RequestQueue implements Serializable {

	private static final long serialVersionUID = -9051464614733099813L;
	public static final Long NO_REQUEST = -1L; 
	public static final Long DEAD = -2L;
	
	private Long[] reqQ;

	public RequestQueue(Integer serverCount) {
    	reqQ = new Long[serverCount];
    	for (int i=0; i < serverCount; i++) {
    		reqQ[i] = NO_REQUEST;
    	}
	}

	public Long[] getReQ() {
		return reqQ;
	}

	public void setReqQ(Long[] reqQ) {
		this.reqQ = reqQ;
	}
	
	public void setDeadReq(Integer serverInstance){
		this.reqQ[serverInstance] = DEAD;
	}
	
	public Long getFirstReq(){
		return reqQ[0];
	}	
	
	public Long getMyReq(Integer myServerInstance){
		return reqQ[myServerInstance];
	}
	
	public void addReqToMyQ(Integer myServerInstance, Long reqTimestamp){
		reqQ[myServerInstance] = reqTimestamp;
	}
	
	public void removeCompletedRequest(Integer serverInstance) {
		reqQ[serverInstance] = NO_REQUEST;
	}
	
	public String printMyQ() {
		for(int i=0; i<reqQ.length; i++){
			System.out.print("myQ[" + i + "] = " + reqQ[i] + ", ");
		}
		System.out.println();
		return null;
	}
}
