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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class holds design constraints found in the constraints.xdc file.
 * TODO: Make these constraints easier to work with
 * TODO: Provide an example XDC constraints file.
 *
 */
public final class XdcConstraint {

	private final String command;
	private final String options;
	private final String comment;
	private XdcConstraintPackagePin constraintPackagePin;
	private static final Pattern patternPackagePin = Pattern.compile("\\s*set_property\\s+.*PACKAGE_PIN\\s+(\\w+)\\s+.*\\[\\s*get_ports*\\s+\\{?\\s*([^{}\\s]+)\\s*}?\\s*].*$");

	public XdcConstraint(String command, String options, String comment){
		this.command = command;
		this.options = options;
		this.comment = comment;

		// Set the package pin if this constraint includes one.
		String constraint = command + " " + options;
		Matcher matcher = patternPackagePin.matcher(constraint);
		if (matcher.find()) {
			constraintPackagePin = new XdcConstraintPackagePin(matcher.group(1), matcher.group(2));
		}
	}
	
	/**
	 * @return The name of the XDC constraint command
	 */
	public String getCommandName() {
		return command;
	}
	
	/**
	 * @return The parameters of the command (separated by whitespace)
	 */
	public String getOptions() {
		return options;
	}

	/**
	 * @return the comment of the XDC constraint. null if there is no comment.
	 */
	public String getComment() { return comment; }
	
	/**
	 * Formats the XDC constraint and returns it as a string.
	 */
	@Override
	public String toString(){
		return (comment != null) ? command + " " + options + " " + comment : command + " " + options;
	}
	
	/**
	 * @return The XDC pin package constraint instance
	 */
	public XdcConstraintPackagePin getPackagePinConstraint() {
		return constraintPackagePin;
	}
	
	public class XdcConstraintPackagePin {
		
		private String pinName;
		private String portName;

		XdcConstraintPackagePin(String pinName, String portName) {
			this.pinName = pinName;
			this.portName = portName;
		}
		
		/**
		 * @return The name of the pin that the net is constrained to (eg. D7)
		 */
		public String getPinName() {
			return pinName;
		}
		
		/**
		 * @return The name of the port that is constrained to a pin.
		 */
		public String getPortName() {
			return portName;
		}

	}
}
