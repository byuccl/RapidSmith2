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

import edu.byu.ece.rapidSmith.device.xdlrc.XDLRCSource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 *	TODO: In the future it would be nice if this could be integrated with TINCR and Vivado,
 *			and call the TINCR code to create an XDLRC, but for now just hand in the path
 */
public class UserProvidedCXDLRCRetriever implements XDLRCRetriever {
	private final Path cxdlrcFile;

	public UserProvidedCXDLRCRetriever(Path cxdlrcFile) {
		Objects.requireNonNull(cxdlrcFile);
		this.cxdlrcFile = cxdlrcFile;
	}

	@Override
	public XDLRCSource getXDLRCSource() throws DeviceCreationException {
		//if the file doesn't exist, then throw an error and
		if (!Files.isRegularFile(cxdlrcFile)) {
			throw new DeviceCreationException("XDLRC file " + cxdlrcFile + " does not exist.");
		}
		return new XDLRCSource.CompressedXDLRCSource(cxdlrcFile);
	}

	@Override
	public void cleanup() {
		// User provided file.  Leave it alone.
	}
}
