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
import java.util.HashMap;
import java.util.Map;

/**
 * Type of a property specifying its source and type.
 */
public final class PropertyType implements Serializable {
	private static final Map<String, PropertyType> propertyTypes = new HashMap<>();

	/** A property stemming from XDL */
	public static final PropertyType DESIGN = registerType("DESIGN");
	/** Properties specified by the user for personal storage */
	public static final PropertyType USER = registerType("USER");
	/** XDL BELPROP properties */
	public static final PropertyType BELPROP = registerType("BELPROP");
	/** EDIF Properties */
	public static final PropertyType EDIF = registerType("EDIF");
	private static final long serialVersionUID = 4035028819392316516L;

	private final String name;

	private PropertyType(String propertyName) {
		this.name = propertyName;
	}

	/**
	 * Returns the property with the given name if the property is already
	 * registered, else creates and registers a new property type.
	 * @param propertyName the name of the property type
	 * @return the property with this name, newly created if necessary
	 */
	public static PropertyType registerType(String propertyName) {
		if (propertyTypes.containsKey(propertyName))
			return propertyTypes.get(propertyName);
		PropertyType propertyType = new PropertyType(propertyName);
		propertyTypes.put(propertyName, propertyType);
		return propertyType;
	}

	/**
	 * Returns the property with the given name or null if the property is not
	 * registered.
	 */
	public static PropertyType get(String propertyName) {
		return propertyTypes.get(propertyName);
	}

	@java.lang.Override
	public java.lang.String toString() {
		return "PropertyType{" +
				"name='" + name + '\'' +
				'}';
	}
}
