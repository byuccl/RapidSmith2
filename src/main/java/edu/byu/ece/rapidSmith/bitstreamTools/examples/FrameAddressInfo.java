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
package edu.byu.ece.rapidSmith.bitstreamTools.examples;

import edu.byu.ece.rapidSmith.bitstreamTools.configuration.FrameAddressRegister;
import edu.byu.ece.rapidSmith.bitstreamTools.configurationSpecification.DeviceLookup;
import edu.byu.ece.rapidSmith.bitstreamTools.configurationSpecification.XilinxConfigurationSpecification;

/**
 * Provides information about a frame address.
 *
 */
public class FrameAddressInfo {

	/**
	 * Simple main that parses a FAR address and returns information about it.
	 */
	public static void main(String[] args) {
		if (args.length < 2) {
			System.err.println("usage: <executable> <part name> <far address - hex format>\n");
			System.exit(1);
		}
		String partname = args[0];
		String farStringAddress = args[1];
		int farAddress = 0;
		try {
			farAddress = Integer.parseInt(farStringAddress,16);
		} catch (NumberFormatException e) {
			System.err.println("Illegal number format: "+farStringAddress);
			System.exit(1);
		}
		
		XilinxConfigurationSpecification spec = DeviceLookup.lookupPartV4V5V6(partname);
		
		System.out.println(spec.toString());
		
		System.out.println(FrameAddressRegister.toString(spec,farAddress));
	}


}
