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
import edu.byu.ece.rapidSmith.device.xdlrc.XDLRCSource;
import edu.byu.ece.rapidSmith.util.RunXilinxTools;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * ISE_XDLRCRetriever implementation for the Xilinx ISE tools.
 */
public class ISE_XDLRCRetriever implements XDLRCRetriever {
	private final String part;
	private final Path xdlrcFile;
	private boolean removeWhenDone;

	public ISE_XDLRCRetriever(String part) {
		Objects.requireNonNull(part);
		this.part = part;
		this.xdlrcFile = RSEnvironment.defaultEnv()
				.getDevicePath().resolve(part + "_full.xdlrc");
	}

	// Gets the path to the XDLRC file located in the RapidSmith device location.
	// If the file already exists, mark it as such so it is not later deleted.
	public XDLRCSource getXDLRCSource() throws DeviceCreationException {
		if (Files.isRegularFile(xdlrcFile))
			return new XDLRCSource.XDLRCFileSource(xdlrcFile);
		if(!RunXilinxTools.generateFullXDLRCFile(part, xdlrcFile.toString())){
			throw new DeviceCreationException("Failed generating part " + part + ".");
		}
		removeWhenDone = true;
		return new XDLRCSource.XDLRCFileSource(xdlrcFile);
	}

	public void cleanup() throws IOException {
		// Only delete if the files we generated
		if (removeWhenDone) {
			Files.deleteIfExists(xdlrcFile);
		}
	}
}
