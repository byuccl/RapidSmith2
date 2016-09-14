/*
 * Copyright (c) 2010 Brigham Young University
 * 
 * This file is part of the BYU RapidSmith Tools.
 * 
 * BYU RapidSmith Tools is free software: you may redistribute it 
 * and/or modify it under the terms of the GNU General Public License 
 * as published by the Free Software Foundation, either version 2 of 
 * the License, or (at your option) any later version.
 * 
 * BYU RapidSmith Tools is distributed in the hope that it will be 
 * useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU 
 * General Public License for more details.
 * 
 * A copy of the GNU General Public License is included with the BYU 
 * RapidSmith Tools. It can be found at doc/gpl2.txt. You may also 
 * get a copy of the license at <http://www.gnu.org/licenses/>.
 * 
 */
package edu.byu.ece.rapidSmith.timing;


import edu.byu.ece.rapidSmith.design.xdl.Net;

public class PathDelay extends Path {
	/** Delay or offset in nanoseconds (data path - clock path skew + uncertainty) */
	private float delay;
	/** Skew estimated for the clock path */
	private float clockPathSkew;
	/** The net driving the clock on the source register */
	private Net sourceClock;
	
	
	/**
	 * @return the delay
	 */
	public float getDelay() {
		return delay;
	}
	/**
	 * @param delay the delay to set
	 */
	public void setDelay(float delay) {
		this.delay = delay;
	}

	/**
	 * @return the clockPathSkew
	 */
	public float getClockPathSkew() {
		return clockPathSkew;
	}
	/**
	 * @param clockPathSkew the clockPathSkew to set
	 */
	public void setClockPathSkew(float clockPathSkew) {
		this.clockPathSkew = clockPathSkew;
	}
	/**
	 * @return the sourceClock
	 */
	public Net getSourceClock() {
		return sourceClock;
	}
	/**
	 * @param sourceClock the sourceClock to set
	 */
	public void setSourceClock(Net sourceClock) {
		this.sourceClock = sourceClock;
	}
}
