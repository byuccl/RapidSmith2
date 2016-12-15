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
package edu.byu.ece.rapidSmith.bitstreamTools.examples.support;

/**
 * Maintains a revision number for all executables. This file should be updated and
 * committed each time a release is given to distinguish releases for the users
 * of the executables.
 * 
 */
public class ExecutableRevision {

	/** A revision tag maintained by SVN. Do not edit manually. **/
	public static String SVN_VERSION = "$Revision: 1877 $";
	
	/** A date tag maintained by SVN. Do not edit manually. **/
	public static String SVN_DATE = "$Date: 2010-03-19 09:01:50 -0600 (Fri, 19 Mar 2010) $";
	
	public static String REVISION = "0.4.1";
	
	public static String BUILD_DATE = "3-19-2010";
	
	/**
	 * Parses the SVN_VERSION String and returns the value without the extra characters.
	 */
	public static String getSVNVersion() {
		String svn = SVN_VERSION;
		
		int beginIndex = "$Revision:".length() + 1;
		int endIndex = SVN_VERSION.length()-2;
		return svn.substring(beginIndex, endIndex);
	}
	
	public static String getSVNDate() {
		String svn = SVN_DATE;
		
		int beginIndex = "$Date:".length() + 1;
		int endIndex = SVN_DATE.length()-2;
		return svn.substring(beginIndex, endIndex);		
	}
	
	public static String getRevisionString() {
		return "Version " + REVISION + " (svn:" + getSVNVersion() + ") " + getSVNDate();
	}
	
}
