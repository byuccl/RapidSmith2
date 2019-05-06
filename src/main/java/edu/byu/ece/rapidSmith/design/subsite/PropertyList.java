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
import java.util.*;

/**
 * This class represents an objects that can have properties.
 */
public final class PropertyList implements Iterable<Property>, Serializable {
	private static final long serialVersionUID = 969996324308298671L;
	/** Properties in the property list */
	private Map<String, Property> properties;

	PropertyList() {
		properties = null;
	}
	
	PropertyList(Map<String, Property> defaultProperties) {
		properties = null;
		if (defaultProperties != null && !defaultProperties.isEmpty()) {
			// Default load factor is .75, ie size * 1.33
			properties = new HashMap<>((int) (defaultProperties.size() * 1.34));

			for (Property p : defaultProperties.values()) {
				if (p.isReadOnly()) {
					properties.put(p.getKey(), p);
				} else {
					Property local = new Property(
						p.getKey(), p.getType(), p.getValue(), p.isReadOnly(), true);
					properties.put(p.getKey(), local);
				}
			}
		}
	}
	
	private void initPropertiesMap() {
		if (properties == null)
			properties = new HashMap<>(4);
	}
	
	public int size() {
		return properties == null ? 0 : properties.size();
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
		return (properties == null) ? null : properties.get(propertyKey) ;
	}

	/**
	 * Adds the specified property to the list.  If a property with the same key,
	 * already exists, this method will throw an error.  This method can be used to
	 * add read only properties to the list.
	 * @param property the {@link Property} to add
	 */
	public void add(Property property) {
		Objects.requireNonNull(property);
		String key = property.getKey();
		if (has(key))
			throw new IllegalArgumentException("Property " + key + " already in list");
		initPropertiesMap();
		properties.put(key, property);
	}

	/**
	 * Updates or adds the properties in the provided collection to the properties
	 * of this cell.
	 *
	 * @param properties the properties to add or update
	 * @throws UnsupportedOperationException if any of the properties affect read only
	 *   properties with the same key. This method will exit upon the first detected error
	 *   leaving no guarantee as to which properties in the collection were successfully
	 *   added.
	 */
	public void updateAll(Collection<Property> properties) {
		Objects.requireNonNull(properties);

		properties.forEach(this::update);
	}

	/**
	 * Updates or adds the property to this cell.
	 *
	 * @param property the property to add or update
	 * @throws UnsupportedOperationException if the current property with the same key is read only
	 */
	public void update(Property property) {
		Objects.requireNonNull(property);

		String key = property.getKey();
		Property old = get(key);
		if (old != null && old.isReadOnly())
			throw new UnsupportedOperationException("Cannot update read only property");

		initPropertiesMap();
		if (!property.isReadOnly())
			property = property.copy();
		this.properties.put(key, property);
	}

	/**
	 * Updates the value of the property <i>propertyKey</i> in this cell or creates and
	 * adds the property if it is not already present.
	 * <p/> As with {@link #update(Property)}, this method will throw an exception if the
	 * user tries to update a read only property.
	 *
	 * @param propertyKey the name of the property
	 * @param type the new type of the property
	 * @param value the value to set the property to
	 * @throws UnsupportedOperationException if the current property with the key is read only
	 */
	public void update(String propertyKey, PropertyType type, Object value) {
		Objects.requireNonNull(propertyKey);
		Objects.requireNonNull(type);
		Objects.requireNonNull(value);

		update(new Property(propertyKey, type, value, false, false));
	}

	/**
	 * Removes the property <i>propertyKey</i> and returns the removed property.
	 * <p/> Removing default properties is not allowed with this method and will through
	 * an exception.  However, this method can be used to remove read only properties and
	 * will be needed if the user wishes to update a read only property.
	 *
	 * @param propertyKey the name of the property to remove
	 * @return the removed property. null if the property doesn't exist
	 * @throws UnsupportedOperationException if the current property with the key is read only
	 */
	public Property remove(String propertyKey) {
		Objects.requireNonNull(propertyKey);

		Property property = get(propertyKey);
		if (property == null)
			return null;

		if (property.isDefaultProperty())
			throw new UnsupportedOperationException("Cannot remove default properties");

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
		if (properties == null)
			return Collections.emptyIterator();
		return properties.values().iterator();
	}
}
