package edu.byu.ece.rapidSmith.design.subsite;

import java.util.HashMap;
import java.util.Map;

/**
 * Type of a property specifying its source and type.
 */
public final class PropertyType {
	private static final Map<String, PropertyType> propertyTypes = new HashMap<>();

	/** A property stemming from XDL */
	public static final PropertyType DESIGN = registerType("DESIGN");
	/** Properties specified by the user for personal storage */
	public static final PropertyType USER = registerType("USER");
	/** XDL BELPROP properties */
	public static final PropertyType BELPROP = registerType("BELPROP");
	/** EDIF Properties */
	public static final PropertyType EDIF = registerType("EDIF");

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
