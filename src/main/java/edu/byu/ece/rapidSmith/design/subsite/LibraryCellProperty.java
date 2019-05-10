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
package edu.byu.ece.rapidSmith.design.subsite;

import java.io.Serializable;

/**
 * Class that encapsulates all of the important information of a
 * library cell property. It is package private, and should not
 * be used by regular users. 
 * 
 * TODO: How to handle read-only properties?
 */
final class LibraryCellProperty implements Serializable {
	private static final long serialVersionUID = 1726711718270864375L;
	private final String name;
	private final String type;
	private final String[] possibleValues;
	private final boolean isReadonly;
	
	/**
	 * Constructor
	 * 
	 * @param name Name of the property
	 * @param type Type of the property
	 * @param possibleValues A list of possible property values
	 * @param isReadonly {@code true} if the property is a read-only property
	 */
	LibraryCellProperty(String name, String type, String[] possibleValues, boolean isReadonly) {
		this.name = name;
		this.type = type;
		this.possibleValues = possibleValues;
		this.isReadonly = isReadonly;
	}
	
	/**
	 * Returns the name of the property
	 */
	String getName() {
		return name;
	}
	
	/**
	 * Returns the type of the property
	 */
	String getType() {
		return type;
	}
	
	/**
	 * Returns the possible values of the property
	 */
	String[] getPossibleValues() {
		return possibleValues;
	}
	
	/**
	 * Returns {@code true} if the property is read-only, {@code false} otherwise.
	 */
	boolean isReadonly() {
		return isReadonly;
	}
}
