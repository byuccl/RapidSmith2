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

package edu.byu.ece.rapidSmith.device.creation;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import edu.byu.ece.rapidSmith.RSEnvironment;
import edu.byu.ece.rapidSmith.device.FamilyType;
import edu.byu.ece.rapidSmith.util.MessageGenerator;

/**
 *	TODO: In the future it would be nice if this could be integrated with TINCR and Vivado,
 *			and call the TINCR code to create an XDLRC, but for now just hardcode the artix value. 
 */
public class Vivado_XDLRCRetriever implements XDLRCRetriever {

	@Override
	public List<String> getPartsInFamily(FamilyType family) {
		// TODO Auto-generated method stub
		
		List<String> parts = new ArrayList<>();
		parts.add("xc7a100tcsg324");
		return parts;
	}

	@Override
	public Path getXDLRCFileForPart(String part) {
		Path xdlrcFile = RSEnvironment.defaultEnv().getDevicePath().resolve(part + "_full.xdlrc");
		
		//if the file doesn't exist, then throw an error and 
		if (!Files.isRegularFile(xdlrcFile)) {
			MessageGenerator.briefErrorAndExit("ERROR: XDLRC file " + xdlrcFile + " does not exist. Generate this file before continuing.");
		}
		return xdlrcFile;
	}

	@Override
	public void cleanupXDLRCFile(String part, Path filePath) {
		// TODO create an option in the installer / DeviceFilesCreator to optionally delete the XDLRC after a device file has been generated
		
	}

}
