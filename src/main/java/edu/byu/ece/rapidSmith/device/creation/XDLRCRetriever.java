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

import java.io.IOException;
import java.nio.file.Path;

/**
 * Interface providing methods for retrieving, creating if necessary,
 * the requested XDLRC file and cleaning it up after the utility is
 * finished using the file.
 */
public interface XDLRCRetriever {
	/**
	 * Returns the path to the XDLRC file for the specified part.  Create it if
	 * it does not exist.
	 *
	 * @return the path to the created XDLRC
	 */
	XDLRCSource getXDLRCSource() throws DeviceCreationException;

	/**
	 * The XDLRC file for the part is no longer needed and may be cleaned up.
	 */
	void cleanup() throws IOException;
}
