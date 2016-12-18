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

/**
 *  Enumeration of the possible directions for a pin.
 */
public enum PinDirection {
	IN, OUT, INOUT;

	public static PinDirection convert(boolean isOutput) {
		if (isOutput)
			return OUT;
		else
			return IN;
	}
	
	public static PinDirection reverse(PinDirection dir) {
		
		switch (dir) {
			case IN: return PinDirection.OUT;
			case OUT: return PinDirection.IN;
			case INOUT: return PinDirection.INOUT;
			default : throw new AssertionError("Invalid Pin Direction");
		}
	}
	
	public static boolean isInput(PinDirection dir) {
		return dir == PinDirection.IN;
	}
	
	public static boolean isOutput(PinDirection dir) {
		return dir == PinDirection.OUT;
	}
	
	public static boolean isInout(PinDirection dir) {
		return dir == PinDirection.INOUT;
	}
}
