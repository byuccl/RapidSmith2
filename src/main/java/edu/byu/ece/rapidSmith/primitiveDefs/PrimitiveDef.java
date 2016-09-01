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
package edu.byu.ece.rapidSmith.primitiveDefs;

import edu.byu.ece.rapidSmith.device.SiteType;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class PrimitiveDef implements Serializable {
	private static final long serialVersionUID = 6507494629635884224L;
	private SiteType type;
	private List<PrimitiveDefPin> pins;
	private List<PrimitiveElement> elements;

	public PrimitiveDef(){
		setType(null);
		pins = new ArrayList<PrimitiveDefPin>();
		elements = new ArrayList<PrimitiveElement>();
	}
	
	public void addPin(PrimitiveDefPin p){
		pins.add(p);
	}
	public void addElement(PrimitiveElement e){
		elements.add(e);
	}
	
	// Setters and Getters
	public void setType(SiteType type) {
		this.type = type;
	}
	public SiteType getType() {
		return type;
	}
	public List<PrimitiveDefPin> getPins() {
		return pins;
	}
	public void setPins(ArrayList<PrimitiveDefPin> pins) {
		this.pins = pins;
	}
	public PrimitiveDefPin getPin(String name) {
		for (PrimitiveDefPin pin : pins) {
			if (pin.getInternalName().equals(name))
				return pin;
		}
		return null;
	}
	public List<PrimitiveElement> getElements() {
		return elements;
	}
	public PrimitiveElement getElement(String name) {
		for (PrimitiveElement el : getElements()) {
			if (el.getName().equals(name))
				return el;
		}
		return null;
	}
	public void setElements(ArrayList<PrimitiveElement> elements) {
		this.elements = elements;
	}
	
	@Override
	public String toString(){
		StringBuilder s = new StringBuilder();
		String nl = System.getProperty("line.separator");
		s.append("(primitive_def " + type.toString() +" "+ pins.size() + " " + elements.size() + nl);
		for(PrimitiveDefPin p : pins){
			s.append("\t\t"+p.toString()+nl);
		}
		for(PrimitiveElement e : elements){
			s.append("\t\t"+e.toString()+nl);
		}
		s.append("\t)");
		return s.toString();
	}
}
