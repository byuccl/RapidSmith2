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
package edu.byu.ece.rapidSmith.interfaces.vivado;

/**
 *	This class is used in the {@link EdifInterface} class to 
 *	consolidate port information together. For bus ports,
 *	it keeps track of the minimum and maximum bus index for the port.
 */
class PortInformation {

	private String name;
	private int width;
	private int max;
	private int min;
	private int direction;
	private final boolean isBus;
	private final int firstIndex;
	
	/**
	 * Constructor
	 * 
	 * @param name Relative name of the port. (i.e. port for port[0]) 
	 * @param direction Edif port direction
	 * @param isSingleBit {@code true} if the port has a width of 1 (i.e. it is not a bus)
	 * @param firstIndex First index of the port 
	 */
	public PortInformation(String name, int direction, boolean isSingleBit, int firstIndex) {
		this.name = name;
		this.direction = direction;
		max = 0;
		
		if (isSingleBit) {
			min = 0;
			width = 1;
		}
		else {
			min = Integer.MAX_VALUE;
			width = 0;
		}
		
		isBus = !isSingleBit;
		this.firstIndex = firstIndex;
	}
	
	/**
	 * Updates the width, min bus index, and max bus index of the port 
	 * with the specified bus number
	 * 
	 * @param busMember bus index for the port
	 */
	public void addPort(int busMember) {
		this.width++;
		
		if (busMember > max) {
			max = busMember;
		}
		if (busMember < min) {
			min = busMember;
		}		
	}
	
	/**
	 * Returns the relative name of the port
	 */
	public String getName() {
		return this.name;
	}
	/**
	 * Returns the width of the port
	 */
	public int getWidth() {
		return this.width;
	}
	/**
	 * Returns the max index of the port
	 */
	public int getMax() {
		return max;
	}
	/**
	 * Returns the min index of the port
	 */
	public int getMin() {
		return min;
	}
	/**
	 * Returns the EDIF direction of the port
	 */
	public int getDirection() {
		return this.direction;
	}
	/**
	 * Returns true if the width of the port is equal to 1,
	 * false otherwise.
	 */
	public boolean isSingleBitPort() {
		return this.width == 1;
	}
	/**
	 * Returns true if the port was initially created as a bus
	 */
	public boolean createdAsBus() {
		return isBus;
	}
	/**
	 * Returns the first bus index used when the port was initally created
	 */
	public int getFirstIndex() {
		return firstIndex;
	}
}