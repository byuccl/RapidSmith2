package edu.byu.ece.rapidSmith.design.subsite;

/**
 * A property containing a key/value pair.  A property type can be specified
 * to indicate the source and use of the property.
 */
public class Property {
	private Object key;
	private PropertyType type;
	private Object value;

	public Property(Object key, PropertyType type, Object value) {
		this.key = key;
		this.type = type;
		this.value = value;

	}

	public Object getKey() {
		return key;
	}

	/**
	 * Convenience method to get the key casted as a String.
	 * @throws ClassCastException if the key is not a String
	 */
	public String getStringKey() {
		return (String) key;
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
