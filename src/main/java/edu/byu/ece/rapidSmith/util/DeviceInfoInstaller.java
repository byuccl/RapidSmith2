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
package edu.byu.ece.rapidSmith.util;

import java.io.IOException;

import edu.byu.ece.rapidSmith.RSEnvironment;
import edu.byu.ece.rapidSmith.device.Device;
import edu.byu.ece.rapidSmith.device.creation.DeviceGenerator;

/**
 * The DeviceInfoInstaller can be used to add additional information
 * from a  deviceInfo_*.xml file to an existing device in RapidSmith.
 * This prevents users from having to generate an entire device file from
 * scratch if they need to update the device. 
 */
public class DeviceInfoInstaller {
	
	/**
	 * Runs the DeviceInfoInstaller
	 */
	public static void main(String[] args) throws IOException {
		
		if (args.length != 1) {
			System.err.println("USAGE: edu.byu.ece.rapidSmith.util.DeviceInfoInstaller <Full Xilinx Partname>");
			return;
		}
		
		String partname = args[0];
		
		System.out.println("Warning: Make sure to have a backup device file for part " + partname + " before running this program.");
		MessageGenerator.promptToContinue();
		
		System.out.println("Loading Device...");
		Device device = RSEnvironment.defaultEnv().getDevice(partname);
		
		System.out.println("Parsing Device Info...");
		Boolean success = DeviceGenerator.parseDeviceInfo(device);
		
		if (!success) {
			System.out.println("Device info for part " + partname + " cannot be found. Generate the device info and re-run.");
		}
		
		System.out.println("Writing Updated Device File...");
		RSEnvironment.defaultEnv().writeDeviceFile(device);
		
		System.out.println("Done!");
	}
}
