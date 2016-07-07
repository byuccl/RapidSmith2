/*
 * Copyright (c) 2010-2011 Brigham Young University
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

import edu.byu.ece.rapidSmith.util.FamilyType;

import java.nio.file.Path;
import java.util.List;

/**
 * Interface providing methods for retrieving, creating if necessary,
 * the requested XDLRC file and cleaning it up after the utility is
 * finished using the file.
 */
public interface XDLRCRetriever {
	/**
	 * Returns all of the parts in the family for this device.
	 *
	 * @param family the family to get the part for
	 * @return list containing the names of all parts in the specified family
	 */
	List<String> getPartsInFamily(FamilyType family);

	/**
	 * Returns the path to the XDLRC file for the specified part.  Create it if
	 * it does not exist.
	 *
	 * @param part the part to get the XDLRC file for
	 * @return the path to the created XDLRC
	 */
	Path getXDLRCFileForPart(String part);

	/**
	 * The XDLRC file for the part is no longer needed and may be cleaned up.
	 *
	 * @param part name of the part to clean up
	 * @param filePath path to the create XDLRC file
	 */
	void cleanupXDLRCFile(String part, Path filePath);
}
