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
import edu.byu.ece.rapidSmith.device.creation.ExtendedDeviceInfo;
import edu.byu.ece.rapidSmith.device.creation.PartialDeviceGenerator;

/**
 * Creates a RapidSmith2 device file from a rectangular region of an existing Xilinx device. See the
 * documentation of {@link PartialDeviceGenerator} for more information about partial
 * device files.
 * <p>
 * USAGE: PartialDeviceInstaller [originalDeviceName] [newDeviceName] [TopLeftTileName] [BottomRightTileName]
 * <p>
 * Example: PartialDeviceInstaller "xc7a100tcsg324-3" "xc7a_small" "INT_R_X39Y130" "INT_L_X40Y126"
 */
public class PartialDeviceInstaller {

	public static void main(String[] args) throws IOException {
		
		// print usage statement if arguments count is incorrect
		if (args.length != 4) {
			System.out.println("USAGE: PartialDeviceInstaller [originalDeviceName] [newDeviceName] [TopLeftTileName] [BottomRightTileName]");
		}
		
		System.out.println("Loading device: " + args[0]);
		Device originalDevice = RSEnvironment.defaultEnv().getDevice(args[0]);
		ExtendedDeviceInfo.loadExtendedInfo(originalDevice);
		
		System.out.println("Creating partial device file in region boundary: " + args[2] + " - " + args[3]);
		PartialDeviceGenerator generator = new PartialDeviceGenerator();
		Device partialDevice = generator.generatePartialDevice(args[1], originalDevice, args[2], args[3]);

		System.out.println("Writing partial device file to \"" + RSEnvironment.defaultEnv().getDevicePath()
				.resolve(partialDevice.getFamily().toString().toLowerCase())
				.resolve(partialDevice.getPartName() + "_.dat") + "\"");
		RSEnvironment.defaultEnv().writeDeviceFile(partialDevice);
		
		System.out.println("Done!");
		return;
	}
}
