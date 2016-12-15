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

package edu.byu.ece.rapidSmith.interfaces.vivado;

/**
 * This class holds design constraints found in the constraints.xdc file.
 * TODO: Make these constraints easier to work with
 * TODO: Provide an example XDC constraints file.
 *
 */
public final class XdcConstraint {

	private final String command;
	private final String options;
	
	public XdcConstraint(String command, String options){
		this.command = command;
		this.options = options;
	}
	
	/**
	 * Formats the XDC constraint and returns it as a string.
	 */
	@Override
	public String toString(){
		return command + " " + options;
	}
}
