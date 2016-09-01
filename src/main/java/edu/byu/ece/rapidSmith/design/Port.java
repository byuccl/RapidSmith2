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
package edu.byu.ece.rapidSmith.design;

import java.io.Serializable;
import java.util.Objects;

/**
 * This class represents the ports used to define the interfaces of modules
 * in XDL.  They consist of a unique name, the instance to which they are
 * connected and a pin name on the instance.
 * @author Chris Lavin
 * Created on: Jun 22, 2010
 */
public class Port implements Serializable, Cloneable{

	private static final long serialVersionUID = -8961782654770650827L;
	/** Name of the Port of the current module, this is the port of an instance in the module. */
	private String name;
	/** This is the pin that the port references. */
	private Pin pin;

	/**
	 * Default constructor, everything is null.
	 */
	public Port(){
		name = null;
		setPin(null);
	}
	

	/**
	 * Constructs a new port.
	 * @param name Name of the port
	 * @param pin Pin which the port references
	 */
	public Port(String name, Pin pin){
		this.name = name;
		this.setPin(pin);
	}
	

	/**
	 * Returns the name of the port.
	 * @return the name of the port
	 */
	public String getName(){
		return name;
	}
	
	/**
	 * Sets the name of the port.
	 * @param name the new name of the port
	 */
	public void setName(String name){
		this.name = name;
	}
	
	/**
	 * Returns the instance name.
	 * @return the name of the instance where this port resides
	 */
	public String getInstanceName(){
		return pin.getInstanceName();
	}
	
	/**
	 * Returns the pin name of the instance where the port resides.
	 * @return the pin name of the port
	 */
	public String getPinName(){
		return pin.getName();
	}
	

	/**
	 * Sets the pin this port exists on.
	 * @param pin the pin to set
	 */
	public void setPin(Pin pin) {
		this.pin = pin;
	}

	/**
	 * Returns the pin this port resides on.
	 * @return the pin this port resides on
	 */
	public Pin getPin() {
		return pin;
	}

	/**
	 * Returns the instance this port resides on.
	 * @return the instance this port resides on
	 */
	public Instance getInstance() {
		return pin.getInstance();
	}

	/**
	 * Checks if this pin is an output port.
	 * @return true if this port is an output
	 */
	public boolean isOutPort(){
		return pin.isOutPin();
	}
	
	/**
	 * Generates hashCode for this port based on instance name, port name, and pin name.
	 */
	@Override
	public int hashCode(){
		final int prime = 31;
		int result = 1;
		result = prime * result + ((getInstanceName() == null) ? 0 : getInstanceName().hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((getPinName() == null) ? 0 : getPinName().hashCode());
		return result;
	}

	/**
	 * Checks if this and obj are equal ports by comparing port name,
	 * instance name and pin name.
	 */
	@Override
	public boolean equals(Object obj){
		if(this == obj)
			return true;
		if(obj == null)
			return false;
		if(getClass() != obj.getClass())
			return false;
		Port other = (Port) obj;
		return Objects.equals(getInstance(), other.getInstance()) &&
				Objects.equals(getName(), other.getName()) &&
				Objects.equals(getPin(), other.getPin());
	}
	
	@Override
	public String toString() {
		return "port \"" + name + "\" \"" + pin.getInstanceName() + "\" \"" + pin.getName() +"\"";
	}
}
