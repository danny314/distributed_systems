package com.theater;
/*
 * Authors:
 * Puneet Bansal, Sadaf H Syed
 */

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TheaterMap implements Serializable {
	
	private static final long serialVersionUID = 6509608585648578955L;

	/**
	 * Represents the total number of seats in the theater. Read from 'c' parameter of the config file.
	 */
	protected int maxSeats;
	
	/**
	 * Map containing the seats that have been reserved. The key is the spectator name and value is the list of reserved seat numbers.
	 */
	protected Map<String,List<Integer>> seatMap;

	/*
	 * A list keeping track of available seats. Any seats that are reserved by a spectator are removed from ths list.
	 * Any seat that is freed by calling 'delete' command is put back in this list.
	 */
	protected List<Integer> availableSeats;
	
	public TheaterMap(int totalSeats) {
		this.maxSeats = totalSeats;
    	this.seatMap = new HashMap<String,List<Integer>>();
    	this.availableSeats = new ArrayList<Integer>();
    	
    	for (int i=1; i <= maxSeats; i++) {
			availableSeats.add(i);
		}	
	}
	
	public int getMaxSeats() {
		return maxSeats;
	}

	public void setMaxSeats(int maxSeats) {
		this.maxSeats = maxSeats;
	}

	public Map<String, List<Integer>> getSeatMap() {
		return seatMap;
	}

	public void setSeatMap(Map<String, List<Integer>> seatMap) {
		this.seatMap = seatMap;
	}

	public List<Integer> getAvailableSeats() {
		return availableSeats;
	}

	public void setAvailableSeats(List<Integer> availableSeats) {
		this.availableSeats = availableSeats;
	}

	public void addSeat(String spectatorName, List<Integer> assignedSeats) {
		seatMap.put(spectatorName, assignedSeats);
		
		for (Integer assignedSeat : assignedSeats) {
			availableSeats.remove(assignedSeat);	
		}
	}

	public void removeSeat(String spectatorName, List<Integer> freedSeats) {
		availableSeats.addAll(freedSeats);
		seatMap.remove(spectatorName);
	}
	
	@Override
	public synchronized String toString() {
		return "Reservation chart = " + getSeatMap();	
	}
}
