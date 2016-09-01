/*
 * Copyright (c) 2010-2011 Brigham Young University
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
package edu.byu.ece.rapidSmith.router;

import java.util.HashSet;

import edu.byu.ece.rapidSmith.device.Device;

public class V5PinSorter extends PinSorter{
	static {
		needHARD1 = new HashSet<String>();
		needHARD1.add("CLK_B0");
		needHARD1.add("CLK_B1");
		needHARD1.add("FAN_B0");
		needHARD1.add("FAN_B1");
		needHARD1.add("FAN_B2");
		needHARD1.add("FAN_B3");
		needHARD1.add("FAN_B4");
		needHARD1.add("FAN_B5");
		needHARD1.add("FAN_B6");
		needHARD1.add("FAN_B7");
		
		needSLICESource = new HashSet<String>();
	}


	public V5PinSorter(Device dev) {
		this.we = dev.getWireEnumerator();
	}
}
