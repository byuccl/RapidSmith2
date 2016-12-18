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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * This class represents an objects that can have properties. Any class that needs <br> 
 * to have properties, should inherit from this class. 
 * 
 * @author Thomas Townsend
 */
public class PropertyList {
	
	/** Properties of the cell */
	private Map<Object, Property> properties = null;
		
	/**
	 * Returns true if this cell contains a property with the specified name.
	 *
	 * @param propertyKey the name of the property to check for
	 * @return true if this cell contains a property with the specified name
	 */
	public final boolean hasProperty(Object propertyKey) {
		Objects.requireNonNull(propertyKey);

		return getProperty(propertyKey) != null;
	}

	/**
	 * Returns the property from this cell with the specified name.
	 *
	 * @param propertyKey name of the property to get
	 * @return the property with name <i>propertyKey</i> or null if the property
	 * is not in the cell
	 */
	public Property getProperty(Object propertyKey) {
		Objects.requireNonNull(propertyKey);

		if (properties == null)
			return null;
		return properties.get(propertyKey);
	}

	/**
	 * Returns the properties of this cell.  The returned collection should not be
	 * modified by the user.
	 *
	 * @return the properties of this cell
	 */
	public Collection<Property> getProperties() {
		if (properties == null) {
			properties = new HashMap<>();
		}
		return properties.values();
	}

	/**
	 * Updates or adds the properties in the provided collection to the properties
	 * of this cell.
	 *
	 * @param properties the properties to add or update
	 */
	public void updateProperties(Collection<Property> properties) {
		Objects.requireNonNull(properties);

		properties.forEach(this::updateProperty);
	}

	/**
	 * Updates or adds the property to this cell.
	 *
	 * @param property the property to add or update
	 */
	public void updateProperty(Property property) {
		Objects.requireNonNull(property);

		if (this.properties == null)
			this.properties = new HashMap<>();
		this.properties.put(property.getKey(), property);
	}

	/**
	 * Updates the value of the property <i>propertyKey</i> in this cell or creates and
	 * adds the property if it is not already present.
	 *
	 * @param propertyKey the name of the property
	 * @param type the new type of the property
	 * @param value the value to set the property to
	 */
	public void updateProperty(Object propertyKey, PropertyType type, Object value) {
		Objects.requireNonNull(propertyKey);
		Objects.requireNonNull(type);
		Objects.requireNonNull(value);

		updateProperty(new Property(propertyKey, type, value));
	}

	/**
	 * Removes the property <i>propertyKey</i>.  Returns the removed property.
	 *
	 * @param propertyKey the name of the property to remove
	 * @return the removed property. null if the property doesn't exist
	 */
	public Property removeProperty(Object propertyKey) {
		Objects.requireNonNull(propertyKey);

		if (properties == null)
			return null;
		return properties.remove(propertyKey);
	}

	/**
	 * Tests if the property with the specified name has the specified value.
	 *
	 * @param propertyKey the name of the property to test
	 * @param value the value to compare the property's to test
	 * @return true if the value matches the property's value
	 */
	public boolean testPropertyValue(Object propertyKey, Object value) {
		Objects.requireNonNull(propertyKey);

		return Objects.equals(getPropertyValue(propertyKey), value);
	}

	/**
	 * Returns the value of the property with the associated name.
	 *
	 * @param propertyKey the name of the property
	 * @return the value of the specified property
	 */
	public Object getPropertyValue(Object propertyKey) {
		Objects.requireNonNull(propertyKey);

		Property property = getProperty(propertyKey);
		return property == null ? null : property.getValue();
	}
	
	/**
	 * Return the given specified property as an integer. Only use this function <br>
	 * if you are sure the property value can be safely cast to an int.
	 * @param propertyKey the name of the property 
	 * @return int
	 */
	public int getIntegerPropertyValue(Object propertyKey) {
		
		Property property = getProperty(propertyKey);
		return property == null ? null : (int) property.getValue();
	}
	
	/**
	 * Return the given specified property as a string. Only use this function <br>
	 * if you are sure the property value can be safely cast to a string.
	 * @param propertyKey the name of the property 
	 * @return string
	 */
	public String getStringPropertyValue(Object propertyKey) {
			
		Property property = getProperty(propertyKey);
		return property == null ? null : (String) property.getValue();
	}
	
	/**
	 * Return the given specified property as a boolean. Only use this function <br>
	 * if you are sure the property value can be safely cast to a boolean.
	 * @param propertyKey the name of the property 
	 * @return boolean
	 */
	public boolean getBooleanPropertyValue(Object propertyKey) {
		
		Property property = getProperty(propertyKey);
		return property == null ? null : (boolean) property.getValue();
	}
	
	/**
	 * Return the given specified property as a double. Only use this function <br>
	 * if you are sure the property value can be safely cast to a double.
	 * @param propertyKey the name of the property 
	 * @return double 
	 */
	public double getDoublePropertyValue(Object propertyKey) {
		
		Property property = getProperty(propertyKey);
		return property == null ? null : (double) property.getValue();
	}
}
