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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class PrimitiveElement implements Serializable {
	private static final long serialVersionUID = -8482957591597345980L;
	private String name;
	private boolean isBel;
	private boolean isPin;
	private boolean isMux;
	private boolean isConfiguration;
	private boolean isRouteThrough;
	private List<PrimitiveDefPin> pins;
	private List<String> cfgOptions;
	private List<PrimitiveConnection> connections;

	public PrimitiveElement(){
		name = null;
		isBel = false;
		isPin = false;
		isMux = false;
		isConfiguration = false;
		pins = new ArrayList<>();
		cfgOptions = null;
		connections = new ArrayList<>();
	}
	
	public void addPin(PrimitiveDefPin p){
		pins.add(p);
	}
	public void addConnection(PrimitiveConnection c){
		connections.add(c);
	}
	public void addCfgOption(String option){
		if(cfgOptions == null){
			cfgOptions = new ArrayList<>();
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
		return isBel;
	}
	public void setBel(boolean bel) {
		this.isBel = bel;
	}
	public boolean isPin() {
		return isPin;
	}
	public void setPin(boolean isPin) {
		this.isPin = isPin;
	}
	public boolean isMux() {
		return isMux;
	}
	public void setMux(boolean isMuxElement) {
		this.isMux = isMuxElement;
	}
	public boolean isConfiguration() {
		return isConfiguration;
	}
	public void setConfiguration(boolean isConfiguration) {
		this.isConfiguration = isConfiguration;
	}
	public boolean isRouteThrough() {
		return isRouteThrough;
	}
	public void setRouteThrough(boolean isRouteThrough) {
		this.isRouteThrough = isRouteThrough;
	}

	public List<PrimitiveDefPin> getPins() {
		return pins;
	}
	public void setPins(List<PrimitiveDefPin> pins) {
		this.pins = pins;
	}
	public List<String> getCfgOptions() {
		return cfgOptions;
	}
	public void setCfgOptions(List<String> cfgOptions) {
		this.cfgOptions = cfgOptions;
	}
	public List<PrimitiveConnection> getConnections() {
		return connections;
	}
	public void setConnections(List<PrimitiveConnection> connections) {
		this.connections = connections;
	}
	
	@Override
	public String toString(){
		StringBuilder s = new StringBuilder();
		String nl = System.getProperty("line.separator");
		s.append("(element ").append(name).append(" ").append(pins.size()).append(isBel ? " # BEL" : "").append(nl);
		for(PrimitiveDefPin p : pins){
			s.append("\t\t\t").append(p.toString()).append(nl);
		}
		if(cfgOptions != null){
			s.append("\t\t\t(cfg");
			for(String option : cfgOptions){
				s.append(" ").append(option);
			}
			s.append(")").append(nl);
		}
		for(PrimitiveConnection c : connections){
			s.append("\t\t\t").append(c.toString()).append(nl);
		}
		s.append("\t\t)");
		return s.toString();
	}
}
