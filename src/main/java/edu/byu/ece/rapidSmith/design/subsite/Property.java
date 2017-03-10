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
 * A property containing a key/value pair.  A property type can be specified
 * to indicate the source and use of the property.
 */
public class Property {
	private final String key;
	private PropertyType type;
	private Object value;

	public Property(String key, PropertyType type, Object value) {
		this.key = key;
		this.type = type;
		this.value = value;
	}

	public String getKey() {
		return key;
	}

	/**
	 * Returns the property type.  The type indicates the source of the property
	 * and its use.
	 */
	public PropertyType getType() {
		return type;
	}

	/**
	 * Sets the property type.  The stype indicates the source of the property
	 * and its use.
	 */
	public void setType(PropertyType type) {
		this.type = type;
	}

	/**
	 * Returns the value of the property.
	 */
	public Object getValue() {
		return value;
	}

	/**
	 * Convenience method to get the value of the property casted as a String.
	 */
	public String getStringValue() {
		return (String) value;
	}
	
	/**
	 * Convenience method to get the value of the property casted as a int.
	 */	
	public int getIntValue() {
		return (int) value;
	}

	/**
	 * Convenience method to get the value of the property casted as a boolean.
	 */
	public boolean getBooleanValue() {
		return (boolean) value;
	}

	/**
	 * Convenience method to get the value of the property casted as a double.
	 */
	public double getDoubleValue() {
		return (double) value;
	}

	/**
	 * Sets the value of the property
	 */
	public void setValue(Object value) {
		this.value = value;
	}

	/**
	 * Returns a new copy of this property with the same key/type/value.
	 */
	public Property deepCopy() {
		return new Property(key, type, value);
	}

	@Override
	public String toString() {
		return "{ " + key + " -> " + value + " }";
	}
}
