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
package edu.byu.ece.rapidSmith.device.vsrt.primitiveDefs;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class Element implements Serializable{

	private static final long serialVersionUID = -4173250951419860912L;

	private String name;
	private boolean bel;
	private boolean isTest;
	private boolean pin;
	private ArrayList<PrimitiveDefPin> pins;
	private ArrayList<String> cfgOptions;
	private ArrayList<String> cfgElements;
	private ArrayList<Connection> connections;
	
	//testing attributes
	private HashMap<String, PrimitiveDefPin> pinMap;
	private HashSet<String> connSet;
	
	public Element(){
		name = null;
		bel = false;
		isTest = false;
		pins = new ArrayList<PrimitiveDefPin>();
		cfgOptions = null;
		cfgElements = null;
		connections = new ArrayList<Connection>();
		
		pinMap = new HashMap<String, PrimitiveDefPin>();
		connSet = new HashSet<String>();
		
	}
	
	public void addPin(PrimitiveDefPin p){
		pins.add(p);
		pinMap.put(p.getInternalName(), p);
	}
	public void addConnection(Connection c){
		connections.add(c);
		connSet.add( c.toString() );
	}
	public boolean hasConnections(Connection c) {
		return connSet.contains( c.toString() ); 
	}
	public void addCfgOption(String option){
		if(cfgOptions == null){
			cfgOptions = new ArrayList<String>();
		}
		cfgOptions.add(option);
	}
	
	// Getters and Setters
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public boolean isBel() {
		return bel;
	}
	public void setBel(boolean bel) {
		this.bel = bel;
	}
	public boolean isTest() {
		return isTest;
	}
	public void setIsTest(boolean isTest) {
		this.isTest = isTest;
	}
	public boolean isPin() {
		return pin;
	}
	public void setPin(boolean pin){
		this.pin = pin;
	}
	public ArrayList<PrimitiveDefPin> getPins() {
		return pins;
	}
	public void setPins(ArrayList<PrimitiveDefPin> pins) {
		this.pins = pins;
		
		for (PrimitiveDefPin pin : pins) 
			this.pinMap.put(pin.getInternalName(), pin);
	}
	public PrimitiveDefPin getPin(PrimitiveDefPin pin){
		return this.pinMap.get(pin.getInternalName()); 
	}
	public void removePin(PrimitiveDefPin pin){
		this.pins.remove(pin);
		if(!this.bel && !this.pin && cfgOptions != null){
			for (int i = cfgOptions.size()-1; i >=0; i--) {
				if(cfgOptions.get(i).equals(pin.getInternalName())) {
					cfgOptions.remove(i);
					break;
				}
			}
		}
	}
	public int pinCount() {
		return pins.size();
	}
	public ArrayList<String> getCfgOptions() {
		return cfgOptions;
	}
	public void setCfgOptions(ArrayList<String> cfgOptions) {
		this.cfgOptions = cfgOptions;
	}
	public ArrayList<Connection> getConnections() {
		return connections;
	}
	public void setConnections(ArrayList<Connection> connections) {
		this.connections = connections;
		
		for (Connection connection : connections) {
			this.connSet.add( connection.toString() );
		}
	}
	/**
	 * Removes all of the current connections for the Element
	 */
	public void clearConnections() {
		this.connections.clear();
	}
	public void addCfgElement(String cfgElement){
		if(cfgElements == null){
			cfgElements = new ArrayList<String>();
		}
		this.cfgElements.add(cfgElement);
	}
	public void removeCfgElement(int index){
		this.cfgElements.remove(index);
	}
	public ArrayList<String> getCfgElements(){
		return this.cfgElements;
	}
	public void clearCfgElements(){
		if ( cfgElements != null )
			this.cfgElements.clear();
	}
	public void removeCfgElement(String name){
		if (cfgElements != null) {
			for (int i = 0; i < this.cfgElements.size(); i++){
				if( cfgElements.get(i).startsWith(name) ) {
					cfgElements.remove(i);
					i--;
					return;
				}
			}
		}
	}
		
	//testing elements
	
	
	//Don't use the default toString function when using this class, use this function!
	public String toString(boolean printName){
		StringBuilder s = new StringBuilder();
		String nl = System.getProperty("line.separator");
		
		s.append("(element " + name +" "+ pins.size() +(bel ? " # BEL" : "") + (isTest ? " TEST" : "") + nl);
		for(PrimitiveDefPin p : pins){
			s.append("\t\t\t"+p.toString(printName) + nl);
		}
		if(cfgOptions != null){
			s.append("\t\t\t(cfg");
			for(String option : cfgOptions){
				s.append(" " + option);
			}
			s.append(")"+nl);
		}
		//if (!printName || VSRTool.singleBelMode) { //only print the connections if we are writing the final primitive def and not! the original 
			for(Connection c : connections){
				s.append("\t\t\t"+c.toString() + nl);
			}
		//}
		
		if(cfgElements != null && cfgElements.size() > 0) {
			s.append("\t\t)" + nl); 
			for(int k = 0;k <  cfgElements.size(); k++) {
				String[] tokens = cfgElements.get(k).trim().split("\\s+");
				s.append("\t\t(element " + tokens[0].replace(":", "") + " 0" + (printName ? " #" + this.name + nl : nl)); 
				s.append("\t\t\t(cfg");
				for(int i = 1; i < tokens.length; i++)
				{	s.append(" " + tokens[i]); 	}
				s.append(")" + nl);
				s.append("\t\t)" + ((k != cfgElements.size() - 1) ? nl : "")); 
			}
		}
		else {
			s.append("\t\t)");
		}
		
		return s.toString();
	}
}