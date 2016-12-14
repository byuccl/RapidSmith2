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
package edu.byu.ece.rapidSmith.device.creation;

import edu.byu.ece.rapidSmith.RSEnvironment;
import edu.byu.ece.rapidSmith.device.Device;
import edu.byu.ece.rapidSmith.util.MessageGenerator;

import java.io.IOException;
import java.nio.file.Path;

/**
 * This class provides the base class for generating a new device and its
 * associated family files.  To generate the files, an extending class should
 * implement the protected methods to provide the needed information about the
 * device and its family.
 *
 * @author Chris Lavin
 * Created on: May 20, 2010
 *
 * Updated June 22, 2014 Travis Haroldsen
 * Made more generic to allow non-ISE XDLRC files.
 */
public class DeviceFilesCreator {
	private final XDLRCRetriever xdlrcRetriever;
	private final RSEnvironment env;

	public DeviceFilesCreator(XDLRCRetriever xdlrcRetriever, RSEnvironment env) {
		this.xdlrcRetriever = xdlrcRetriever;
		this.env = env;
	}

	/**
	 * Creates the specified device.
	 * This method obtains the needed input files, parses them, writes the created
	 * device file, then cleans up any created input files.
	 * @param part the part to create the device for
	 */
	public void createDevice(String part) {
		// Create XDLRC File if it already hasn't been created
		System.out.println("Retrieving XDLRC file");
		Path xdlrcFilePath = xdlrcRetriever.getXDLRCFileForPart(part);

		// Initialize Parser
		DeviceGenerator generator = new DeviceGenerator();
		Device device = generator.generate(xdlrcFilePath, env);

		// Write the Device to File
		System.out.println("Writing device to compact file");
		try {
			env.writeDeviceFile(device);
		} catch (IOException e) {
			System.err.println("Failed writing device to disk.");
			e.printStackTrace();
		}

		// Delete XDLRC file
		MessageGenerator.briefMessage("Cleaning up XDLRC file.");
		xdlrcRetriever.cleanupXDLRCFile(part, xdlrcFilePath);

		// Building extended device info
		MessageGenerator.briefMessage("Building extended info.");
		new ExtendedDeviceInfo().buildExtendedInfo(device);
		MessageGenerator.briefMessage("Finished writing extended info.");
	}
}
