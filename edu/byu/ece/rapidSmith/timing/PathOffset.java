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

import java.util.ArrayList;


public class PathOffset extends Path{
	/** Offset in nanoseconds (data path - clock path skew + uncertainty) */
	private float offset;
	/** Number of logic levels the connection traverses in the clock */
	private int clockLevelsOfLogic;
	/** Delay estimated for the clock path */
	private float clockPathDelay;
	/** List of physical / logical resources */
	private ArrayList<PathElement> minDataPath = new ArrayList<PathElement>();	
	
	/**
	 * @return the offset
	 */
	public float getOffset() {
		return offset;
	}
	/**
	 * @param offset the offset to set
	 */
	public void setOffset(float offset) {
		this.offset = offset;
	}
	/**
	 * @return the clockLevelsOfLogic
	 */
	public int getClockLevelsOfLogic() {
		return clockLevelsOfLogic;
	}
	/**
	 * @param clockLevelsOfLogic the clockLevelsOfLogic to set
	 */
	public void setClockLevelsOfLogic(int clockLevelsOfLogic) {
		this.clockLevelsOfLogic = clockLevelsOfLogic;
	}
	/**
	 * @return the clockPathDelay
	 */
	public float getClockPathDelay() {
		return clockPathDelay;
	}
	/**
	 * @param clockPathDelay the clockPathDelay to set
	 */
	public void setClockPathDelay(float clockPathDelay) {
		this.clockPathDelay = clockPathDelay;
	}
	/**
	 * @param minDataPath the minDataPath to set
	 */
	public void setMinDataPath(ArrayList<PathElement> minDataPath) {
		this.minDataPath = minDataPath;
	}
	/**
	 * @return the minDataPath
	 */
	public ArrayList<PathElement> getMinDataPath() {
		return minDataPath;
	}
}
