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

package edu.byu.ece.rapidSmith.device;

import edu.byu.ece.rapidSmith.design.subsite.Cell;

/**
 * Enumeration of the possible directions for a top-level port.
 * @author Thomas Townsend
 *
 */
public enum PortDirection {
	IN, OUT, INOUT;
	
	public static PortDirection getPortDirectionForImport(Cell portCell) {
		checkIsValidPort(portCell);
		
		PinDirection dir = portCell.getPin("PAD").getDirection();
	
		switch (dir) {
			case IN: return PortDirection.OUT;
			case OUT: return PortDirection.IN;
			case INOUT: return PortDirection.INOUT;
			default : throw new AssertionError("Undefined PinDirection");
		}
	}
	
	public static PortDirection getPortDirection(Cell portCell) {
		
		checkIsValidPort(portCell);
		return (PortDirection)portCell.getProperty("Dir").getValue();
	}
	
	public static boolean isInputPort(Cell portCell) {
		
		checkIsValidPort(portCell);
		return portCell.getProperty("Dir").getValue() == PortDirection.IN;
	}
	
	public static boolean isOutputPort(Cell portCell) {

		checkIsValidPort(portCell);
		return portCell.getProperty("Dir").getValue() == PortDirection.OUT;
	}
	
	public static boolean isInoutPort(Cell portCell) {

		checkIsValidPort(portCell);
		return portCell.getProperty("Dir").getValue() == PortDirection.INOUT;
	}
	
	private static void checkIsValidPort(Cell portCell) {
		
		if (!portCell.isPort()) {
			throw new AssertionError("Attempting to get a Port direction for a cell that is not a port!");
		}
	}
}
