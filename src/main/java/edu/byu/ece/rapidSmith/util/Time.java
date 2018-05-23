package edu.byu.ece.rapidSmith.util;

/**
 * 
 * @author Matthew Cannon
 * Keeps track of Start and End time for a process (user has to manually call the the functions to track).
 */
public class Time {
	private long startTime, endTime;
	
	public Time() {
		startTime = 0;
		endTime = 0;
	}
	
	public void setStartTime(){
		startTime = System.nanoTime();
	}
	
	public void setEndTime(){
		endTime = System.nanoTime();
	}
	
	/**
	 * Get Total Time in seconds
	 * @return
	 */
	public double getTotalTime(){
		return getTotalTime(-9);
	}
	
	/**
	 * 
	 * @param pow - Power of the result (default pow=0 returns time in nanoseconds
	 * @return The total time it took to run in nanoseconds
	 */
	public double getTotalTime(int pow){
		return (endTime - startTime) * Math.pow(10, pow);
	}
}
