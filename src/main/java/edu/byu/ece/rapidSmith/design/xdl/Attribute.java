/*
 * Copyright (c) 2010 Brigham Young University
 * 
 * This file is part of the BYU RapidSmith Tools.
 * 
 * BYU RapidSmith Tools is free software: you may redistribute it 
 * and/or modify it under the terms of the GNU General Public License 
 * as published by the Free Software Foundation, either version 2 of 
 * the License, or (at your option) any later version.
 * 
 * BYU RapidSmith Tools is distributed in the hope that it will be 
 * useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU 
 * General Public License for more details.
 * 
 * A copy of the GNU General Public License is included with the BYU 
 * RapidSmith Tools. It can be found at doc/gpl2.txt. You may also 
 * get a copy of the license at <http://www.gnu.org/licenses/>.
 * 
 */
package edu.byu.ece.rapidSmith.design.xdl;

import java.io.Serializable;

/**
 * The Attribute object in XDL is used in several places, Design, Instance, Net 
 * and Module.  Each are generally a list of attributes.  An attribute in XDL consists
 * of a triplet of Strings separated by colons: "Physical Name":"Logical Name":"Value".
 * This class captures these elements of an attribute. 
 * 
 * In XDL, the physical name of an attribute can have multiple logical names and/or values
 * for a single "physical name". To represent multiple logical names and values 
 * in a standard Map, the multiple logical and value strings are stored in a single
 * string and are separated by the final multiValueSeparator character. A variety
 * of methods are available for determining whether the attribute is multi-valued
 * and accesing the multi-value fields.
 * 
 * @author Chris Lavin
 * Created on: Jun 22, 2010
 */
public class Attribute implements Serializable {

	private static final long serialVersionUID = -7266101885597264094L;

	/** Physical name of the attribute (_::) */
	private String physicalName;
	/** Logical or user name of the attribute (:_:) */
	private String logicalName;
	/** Value of the attribute (::_) */
	private String value;
	/** 
	 * This is used to separate multiple value and multiple logical names 
	 * found in attributes where multiple entries have the same physical name.
	 */
	public static final String multiValueSeparator = "`";
	
	/**
	 * @param physicalName Physical name of the attribute (_::)
	 * @param logicalName Logical or user name of the attribute (:_:)
	 * @param value Value of the attribute (::_)
	 */
	public Attribute(String physicalName, String logicalName, String value){
		this.physicalName = physicalName;
		this.logicalName = logicalName;
		this.value = value;
	}

	/**
	 * Creates a new attribute by copying the class members from attr.
	 * @param attr The attribute to model the new attribute after.
	 */
	public Attribute(Attribute attr){
		this.physicalName = attr.physicalName;
		this.logicalName = attr.logicalName;
		this.value = attr.value;
	}
	
	/**
	 * Gets the physical name of the attribute (_::)
	 * @return Physical name of the attribute (_::)
	 */
	public String getPhysicalName(){
		return physicalName;
	}

	/**
	 * Sets the physical name of the attribute (_::)
	 * @param physicalName physical name of the attribute (_::)
	 */
	public void setPhysicalName(String physicalName){
		this.physicalName = physicalName;
	}

	/**
	 * Gets the logical or user name of the attribute (:_:)
	 * @return Logical or user name of the attribute (:_:)
	 */
	public String getLogicalName(){
		return logicalName;
	}

	/**
	 * Sets the logical or user name of the attribute (:_:)
	 * @param logicalName Logical or user name of the attribute (:_:)
	 */
	public void setLogicalName(String logicalName){
		this.logicalName = logicalName;
	}

	/**
	 * Gets the value of the attribute (::_)
	 * @return Value of the attribute (::_)
	 */
	public String getValue(){
		return value;
	}

	/**
	 * Sets the value of the attribute (::_)
	 * @param value Value of the attribute (::_)
	 */
	public void setValue(String value){
		this.value = value;
	}
	
	/**
	 * Some physical name attributes have multiple values. This method indicates whether
	 * this attribute is a multiple value attribute or not. If this attribute is
	 * a multi-value attribute, additional methods are available to access the
	 * multiple logical and value strings.
	 */
	public boolean isMultiValueAttribute() {
		return (getLogicalName().contains(multiValueSeparator));
	}
	
	/**
	 * If this attribute is a multiple value attribute, this method returns
	 * a set of Strings that correspond to the "values" of the attribute.
	 * If this attribute is a single value attribute, it returns a set of one
	 * String which is the same as this.value.
	 */
	public String[] getMultiValueValues() {		
		return value.split(multiValueSeparator, -1);
	}
	
	/**
	 * If this attribute is a multiple value attribute, this method returns
	 * a set of Strings that correspond to the "logical names" of the attribute.
	 * If this attribute is a single value attribute, it returns a set of one
	 * String which is the same as this.logicalName.
	 */
	public String[] getMultiValueLogicalNames() {		
		return logicalName.split(multiValueSeparator, -1);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		if(logicalName.contains(multiValueSeparator)){
			String[] logicalNames = getMultiValueLogicalNames();
			String[] values = getMultiValueValues();
			for (int i = 0; i < values.length; i++) {
				sb.append(physicalName);
				sb.append(":");
				sb.append(logicalNames[i]);
				sb.append(":");
				sb.append(values[i]);
				if(i < values.length-1) sb.append(" ");
			}
			return sb.toString();
		}
		sb.append(physicalName);
		sb.append(":");
		sb.append(logicalName);
		sb.append(":");
		sb.append(value);
		return sb.toString();
	}

	/**
	 * Creates a string representation of the attribute that follows
	 * how it would appear in and XDL file.
	 */
	public String toXdlString() {
		StringBuilder sb = new StringBuilder();
		if(logicalName.contains(multiValueSeparator)){
			String[] logicalNames = getMultiValueLogicalNames();
			String[] values = getMultiValueValues();
			for (int i = 0; i < values.length; i++) {
				sb.append(physicalName);
				sb.append(":");
				sb.append(logicalNames[i]);
				sb.append(":");
				sb.append(values[i]);
				if(i < values.length-1) sb.append(" ");
			}
			return sb.toString();
		}
		sb.append(physicalName);
		sb.append(":");
		sb.append(logicalName);
		sb.append(":");
		sb.append(value);
		return sb.toString();
	}
}
