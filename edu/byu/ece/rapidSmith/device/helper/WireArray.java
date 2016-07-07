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
package edu.byu.ece.rapidSmith.device.helper;

import edu.byu.ece.rapidSmith.device.WireConnection;

import java.util.Arrays;

/**
 * A helper class to help remove duplicate objects and reduce memory usage and file
 * size of the Device class.
 *
 * @author Chris Lavin
 */
public class WireArray {
	/**
	 * An array of wires
	 */
	public WireConnection[] array;
	private Integer hash = null;

	/**
	 * Constructor
	 *
	 * @param array The Array of wires that correspond to this object.
	 */
	public WireArray(WireConnection[] array) {
		if (array == null)
			array = new WireConnection[0];
		this.array = array;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		if (hash == null) {
			hash = Arrays.deepHashCode(array);
		}
		return hash;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;

		WireArray other = (WireArray) obj;
		return Arrays.deepEquals(array, other.array);
	}
}
