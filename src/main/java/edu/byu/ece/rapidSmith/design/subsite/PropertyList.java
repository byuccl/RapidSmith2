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

import java.util.*;
import java.util.stream.Stream;

/**
 * This class represents an objects that can have properties.
 */
public final class PropertyList implements Iterable<Property> {
	/** Properties in the property list */
	private Map<String, Property> properties;
	/** Map of default properties */
	private final Map<String, Property> defaultProperties;

	PropertyList() {
		properties = null;
		defaultProperties = null; 
	}
	
	PropertyList(Map<String, Property> defaultProperties) {
		this.defaultProperties = defaultProperties;
	}
	
	private void initPropertiesMap() {
		properties = new HashMap<>(4);
	}
	
	public int size() {
		
		if (properties == null) {
			return (defaultProperties==null) ? 0 : defaultProperties.size();
		}
		
		if (defaultProperties == null) {
			return properties.size();
		}
		
		int propCount = properties.size(); 
		propCount += defaultProperties.keySet().stream().filter(key -> !properties.containsKey(key)).count();
		
		return propCount;
	}

	/**
	 * Returns true if this cell contains a property with the specified name.
	 *
	 * @param propertyKey the name of the property to check for
	 * @return true if this cell contains a property with the specified name
	 */
	public final boolean has(String propertyKey) {
		Objects.requireNonNull(propertyKey);

		return get(propertyKey) != null;
	}

	/**
	 * Returns the property from this cell with the specified name.  If this collection
	 * has no property with the key {@code propertyKey}, then this method returns
	 * {@code null}.
	 *
	 * @param propertyKey name of the property to get
	 * @return the property with name <i>propertyKey</i> or {@code null} if the property
	 * is not in the cell
	 */
	public Property get(String propertyKey) {
		Objects.requireNonNull(propertyKey);
		Property userProp = getUserProperty(propertyKey);
		return userProp != null ? userProp : getDefaultProperty(propertyKey);	
	}
	
	private Property getUserProperty(String propertyKey) {
		return (properties == null) ? null : properties.get(propertyKey) ;
	}
	
	private Property getDefaultProperty(String propertyKey) {
		return (defaultProperties == null) ? null : defaultProperties.get(propertyKey) ;
	}

	/**
	 * Updates or adds the properties in the provided collection to the properties
	 * of this cell.
	 *
	 * @param properties the properties to add or update
	 */
	public void updateAll(Collection<Property> properties) {
		Objects.requireNonNull(properties);

		properties.forEach(this::update);
	}

	/**
	 * Updates or adds the property to this cell.
	 *
	 * @param property the property to add or update
	 */
	public void update(Property property) {
		Objects.requireNonNull(property);

		if (this.properties == null)
			initPropertiesMap();
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
	public void update(String propertyKey, PropertyType type, Object value) {
		Objects.requireNonNull(propertyKey);
		Objects.requireNonNull(type);
		Objects.requireNonNull(value);

		update(new Property(propertyKey, type, value));
	}

	/**
	 * Removes the property <i>propertyKey</i>.  Returns the removed property.
	 *
	 * @param propertyKey the name of the property to remove
	 * @return the removed property. null if the property doesn't exist
	 */
	public Property remove(String propertyKey) {
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
	public boolean testValue(String propertyKey, Object value) {
		Objects.requireNonNull(propertyKey);

		return Objects.equals(getValue(propertyKey), value);
	}

	/**
	 * Returns the value of the property with the associated name.  If this collection
	 * has no property with the key {@code propertyKey}, then this method returns
	 * {@code null}.
	 *
	 * @param propertyKey the name of the property
	 * @return the value of the specified property or {@code null} if it does not exist
	 */
	public Object getValue(String propertyKey) {
		Objects.requireNonNull(propertyKey);

		Property property = get(propertyKey);
		return property == null ? null : property.getValue();
	}

	/**
	 * Return the given specified property as an integer. Only use this function
	 * if you are sure the property value can be safely cast to an int.  If this
	 * collection has no property with the key {@code propertyKey}, then this method
	 * returns {@code null}.
	 * @param propertyKey the name of the property 
	 * @return the value as an Integer or {@code null} if it does not exist
	 */
	public Integer getIntegerValue(String propertyKey) {
		
		Property property = get(propertyKey);
		return property == null ? null : (int) property.getValue();
	}
	
	/**
	 * Return the given specified property as a string. Only use this function if
	 * you are sure the property value can be safely cast to a string.  If this
	 * collection has no property with the key {@code propertyKey}, then this method
	 * returns {@code null}.
	 * @param propertyKey the name of the property 
	 * @return the value as a String or {@code null} if it does not exist
	 */
	public String getStringValue(String propertyKey) {
			
		Property property = get(propertyKey);
		return property == null ? null : (String) property.getValue();
	}
	
	/**
	 * Return the given specified property as a boolean. Only use this function if
	 * you are sure the property value can be safely cast to a boolean.  If this
	 * collection has no property with the key {@code propertyKey}, then this method
	 * returns {@code null}.
	 * @param propertyKey the name of the property 
	 * @return the value as a Boolean or {@code null} if it does not exist
	 */
	public Boolean getBooleanValue(String propertyKey) {
		
		Property property = get(propertyKey);
		return property == null ? null : (boolean) property.getValue();
	}
	
	/**
	 * Return the given specified property as a double. Only use this function if
	 * you are sure the property value can be safely cast to a double.  If this
	 * collection has no property with the key {@code propertyKey}, then this method
	 * returns {@code null}.
	 * @param propertyKey the name of the property 
	 * @return the value as a Double or {@code null} if it does not exist
	 */
	public double getDoubleValue(String propertyKey) {
		
		Property property = get(propertyKey);
		return property == null ? null : (double) property.getValue();
	}
		
	@Override
	public Iterator<Property> iterator() {
		
		// If no properties have been set by the user, return the default property list
		if (properties == null) {
			return defaultProperties == null ? Collections.emptyIterator() : defaultProperties.values().iterator(); 
		}
		
		// If the default properties are null, then just return the user property list
		if (defaultProperties == null) {
			return properties.values().iterator();
		}
		
		// otherwise, create an iterator that includes the user property list
		// concatenated with default properties that are NOT defined in the user list
		Stream<Property> propStream = properties.values().stream();
		Stream<Property> defaultStream = defaultProperties.values().stream().filter(p -> !properties.containsKey(p.getKey()));
		
		return Stream.concat(propStream, defaultStream).iterator();
	}
}
