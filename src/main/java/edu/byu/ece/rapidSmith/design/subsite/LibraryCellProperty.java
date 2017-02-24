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

/**
 * 
 * If the property is readonly, then the default value will hold the actual value?
 */
class LibraryCellProperty {

	private final String name;
	private final String type;
	private final Object[] possibleValues;
	private final boolean isReadonly;
	
	LibraryCellProperty(String name, String type, Object[] possibleValues, boolean isReadonly) {
		this.name = name;
		this.type = type;
		this.possibleValues = possibleValues;
		this.isReadonly = isReadonly;
	}
	
	String getName() {
		return name;
	}
	
	String getType() {
		return type;
	}
		
	Object[] getPossibleValues() {
		return possibleValues;
	}
	
	boolean isReadonly() {
		return isReadonly;
	}
}
