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

import edu.byu.ece.rapidSmith.RSEnvironment;
import edu.byu.ece.rapidSmith.device.FamilyType;
import edu.byu.ece.rapidSmith.util.RunXilinxTools;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * ISE_XDLRCRetriever implementation for the Xilinx ISE tools.
 */
public class ISE_XDLRCRetriever implements XDLRCRetriever {
	// Set of files generated that should be deleted during cleanup
	private final Set<Path> generatedFiles = new HashSet<>();

	// Gets all of the parts in the family from partgen
	public List<String> getPartsInFamily(FamilyType family) {
		return RunXilinxTools.getPartNames(family.name(), false);

//      Code that has been useful for testing.  Returns only a single device to make it quick
//		List<String> parts = new ArrayList<>();
//		parts.add("xc6vlx75tff484");
//		return parts;
	 }

	// Gets the path to the XDLRC file located in the RapidSmith device location.
	// If the file already exists, mark it as such so it is not later deleted.
	public Path getXDLRCFileForPart(String part) throws DeviceCreationException {
		Path xdlrcFile = RSEnvironment.defaultEnv().getDevicePath().resolve(part + "_full.xdlrc");

		if (Files.isRegularFile(xdlrcFile))
			return xdlrcFile;
		if(!RunXilinxTools.generateFullXDLRCFile(part, xdlrcFile.toString())){
			throw new DeviceCreationException("Failed generating part " + part + ".");
		}
		generatedFiles.add(xdlrcFile);
		return xdlrcFile;
	}

	public void cleanupXDLRCFile(String part, Path xdlrcFile) throws IOException {
		// Only delete if the files we generated
		if (generatedFiles.contains(xdlrcFile)) {
			Files.deleteIfExists(xdlrcFile);
		}
	}
}
