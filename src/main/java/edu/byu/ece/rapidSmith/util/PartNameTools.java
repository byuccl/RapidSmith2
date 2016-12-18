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

/**
 * Provides a method to remove the speed grade.
 * @author Chris Lavin
 * Created on: Jan 27, 2011
 */
public class PartNameTools {
	/**
	 * This method removes the speed grade (ex: -10) from a conventional Xilinx part name.
	 * @param partName The name of the part to remove the speed grade from.
	 * @return The base part name with speed grade removed.  If no speed grade is present, returns
	 * the original string.
	 */
	public static String removeSpeedGrade(String partName) {
		if(partName != null && partName.contains("-")){
			return partName.substring(0, partName.indexOf("-"));
		}
		else{
			return partName;
		}
	}
}
