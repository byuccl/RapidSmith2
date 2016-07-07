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
package edu.byu.ece.rapidSmith.bitstreamTools.examples;

import edu.byu.ece.rapidSmith.bitstreamTools.configurationSpecification.DeviceLookup;
import edu.byu.ece.rapidSmith.bitstreamTools.configurationSpecification.XilinxConfigurationSpecification;

/**
 * Provides a main method that prints out information about a particular device.
 */
public class DeviceInformation {

	static public void main(String args[]) {

		if (args.length < 1) {
			System.err.println("usage: <executable> <part name>\n");
			DeviceLookup.printAvailableParts(System.err);
			System.exit(1);
		}

		String partname = args[0];

		XilinxConfigurationSpecification spec = DeviceLookup.lookupPartV4V5V6(partname);
		if (spec == null) {
			DeviceLookup.printAvailableParts(System.err);
			System.exit(1);
		}

		System.out.println(spec.toString());

	}

}
