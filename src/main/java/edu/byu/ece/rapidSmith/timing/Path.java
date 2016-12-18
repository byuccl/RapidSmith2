/*
 * Copyright (c) 2016 Brigham Young University
 *
 * This file is part of the BYU RapidSmith Tools.
 *
 * BYU RapidSmith Tools is free software: you may redistribute it
 * and/or modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 *
 * BYU RapidSmith Tools is distributed in the hope that it will be
 * useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * A copy of the GNU General Public License is included with the BYU
 * RapidSmith Tools. It can be found at doc/LICENSE.GPL3.TXT. You may
 * also get a copy of the license at <http://www.gnu.org/licenses/>.
 */
package edu.byu.ece.rapidSmith.timing;

import java.util.ArrayList;

import edu.byu.ece.rapidSmith.design.xdl.XdlNet;

public abstract class Path {
	/** The source of this connection delay */
	private String source;
	/** The destination of this connection delay */
	private String destination;
	/** Data path delay in nanoseconds */
	private float dataPathDelay;
	/** Number of logic levels the data path traverses */
	private int levelsOfLogic;
	/** The net driving the clock on the destination register */
	private XdlNet destinationClock;
	/** The uncertainty in the clock as determined by Xilinx trce */
	private float clockUncertainty;
	/** List of physical / logical resources */
	private ArrayList<PathElement> maxDataPath = new ArrayList<>();
	
	/**
	 * @return the source
	 */
	public String getSource() {
		return source;
	}
	/**
	 * @param source the source to set
	 */
	public void setSource(String source) {
		this.source = source;
	}
	/**
	 * @return the destination
	 */
	public String getDestination() {
		return destination;
	}
	/**
	 * @param destination the destination to set
	 */
	public void setDestination(String destination) {
		this.destination = destination;
	}
	/**
	 * @return the dataPathDelay
	 */
	public float getDataPathDelay() {
		return dataPathDelay;
	}
	/**
	 * @param dataPathDelay the dataPathDelay to set
	 */
	public void setDataPathDelay(float dataPathDelay) {
		this.dataPathDelay = dataPathDelay;
	}
	/**
	 * @return the levelsOfLogic
	 */
	public int getLevelsOfLogic() {
		return levelsOfLogic;
	}
	/**
	 * @param levelsOfLogic the levelsOfLogic to set
	 */
	public void setLevelsOfLogic(int levelsOfLogic) {
		this.levelsOfLogic = levelsOfLogic;
	}
	/**
	 * @return the destinationClock
	 */
	public XdlNet getDestinationClock() {
		return destinationClock;
	}
	/**
	 * @param destinationClock the destinationClock to set
	 */
	public void setDestinationClock(XdlNet destinationClock) {
		this.destinationClock = destinationClock;
	}
	/**
	 * @return the clockUncertainty
	 */
	public float getClockUncertainty() {
		return clockUncertainty;
	}
	/**
	 * @param clockUncertainty the clockUncertainty to set
	 */
	public void setClockUncertainty(float clockUncertainty) {
		this.clockUncertainty = clockUncertainty;
	}
	/**
	 * @param maxDataPath the maxDataPath to set
	 */
	public void setMaxDataPath(ArrayList<PathElement> maxDataPath) {
		this.maxDataPath = maxDataPath;
	}
	/**
	 * @return the maxDataPath
	 */
	public ArrayList<PathElement> getMaxDataPath() {
		return maxDataPath;
	}

}
