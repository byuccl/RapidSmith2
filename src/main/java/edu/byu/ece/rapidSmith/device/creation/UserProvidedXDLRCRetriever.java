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
import java.util.Objects;

import edu.byu.ece.rapidSmith.RSEnvironment;
import edu.byu.ece.rapidSmith.device.FamilyType;

/**
 *	TODO: In the future it would be nice if this could be integrated with TINCR and Vivado,
 *			and call the TINCR code to create an XDLRC, but for now just hardcode the artix value. 
 */
public class UserProvidedXDLRCRetriever implements XDLRCRetriever {
	private final Path xdlrcFile;

	public UserProvidedXDLRCRetriever(Path xdlrcFile) {
		Objects.requireNonNull(xdlrcFile);
		this.xdlrcFile = xdlrcFile;
	}

	@Override
	public Path getXDLRCFile() throws DeviceCreationException {
		//if the file doesn't exist, then throw an error and
		if (!Files.isRegularFile(xdlrcFile)) {
			throw new DeviceCreationException("XDLRC file " + xdlrcFile + " does not exist.");
		}
		return xdlrcFile;
	}

	@Override
	public void cleanupXDLRCFile() {
		// User provided file.  Leave it alone.
	}
}
