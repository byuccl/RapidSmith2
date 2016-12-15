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
package edu.byu.ece.rapidSmith.design.xdl;

import java.io.Serializable;
import java.util.Objects;

import edu.byu.ece.rapidSmith.design.PinType;
import edu.byu.ece.rapidSmith.device.Tile;

/**
 *  This class represents the sources and sinks found in net declarations 
 *  (inpins and outpins)
 * @author Chris Lavin
 * Created on: Jun 22, 2010
 */
public class XdlPin implements Serializable, Cloneable {

	private static final long serialVersionUID = -6675131973998249758L;

	/** The type of pin (directionality), in/out/inout */
	private PinType pinType;
	/** The internal pin name on the instance this pin refers to */
	private String name;
	/** The instance where the pin is located */
	private XdlInstance instance;
	/** The Port that references this pin, if there is one */
	private XdlPort port;
	/** The net this pin is a member of */
	private XdlNet net;
	
	/**
	 * Constructor setting things to null and false.
	 */
	public XdlPin(){
		this.pinType = null;
		this.name = null;
		this.setInstance(null);
		this.port = null;
		this.setNet(null);
	}
	
	/**
	 * Creates a pin from parameters
	 * @param isOutputPin true if the new pin an outpin
	 * @param pinName the name of the pin on the instance (internal name)
	 * @param instance the instance where the pin resides
	 */
	public XdlPin(boolean isOutputPin, String pinName, XdlInstance instance){
		this.pinType = isOutputPin ? PinType.OUTPIN : PinType.INPIN;
		this.name = pinName;
		this.setInstance(instance);
		this.port = null;
	}

	/**
	 * Creates a pin from parameters
	 * @param pinType allows specification of an inout pin
	 * @param pinName the name of the pin on the instance (internal name)
	 * @param instance the instance where the pin resides
	 */
	public XdlPin(PinType pinType, String pinName, XdlInstance instance){
		this.pinType = pinType;
		this.name = pinName;
		this.setInstance(instance);
		this.port = null;
	}
	
	/**
	 * @return true if the pin is an outpin
	 */
	public boolean isOutPin(){
		return this.pinType == PinType.OUTPIN;
	}
	
	/**
	 * Returns the pin name of the pin.
	 * @return the pin name (internal instance pin name)
	 */
	public String getName(){
		return this.name;
	}
	
	/**
	 * Returns the instance where this pin resides.
	 * @return the instance where the pin resides
	 */
	public XdlInstance getInstance() {
		return this.instance;
	}
	
	/**
	 * Returns the name of the instance where this pin resides.
	 * @return the name of the instance where this pin resides
	 */
	public String getInstanceName(){
		return instance.getName();
	}
	
	/**
	 * Returns the module instance name corresponding to the
	 * instance of this pin. 
	 * @return the name of the module instance associated with this pin or null
	 * if there is not associated module instance
	 */
	public String getModuleInstanceName(){
		return instance.getModuleInstanceName();
	}
	
	/**
	 * Returns the tile where this pin resides. 
	 * @return the tile where this pin resides
	 */
	public Tile getTile(){
		return instance.getTile();
	}
	
	/**
	 * Sets the direction of the pin.
	 * @param dir the direction (true=outpin, false=inpin)
	 */
	public void setIsOutputPin(boolean dir){
		this.pinType = dir ? PinType.OUTPIN : PinType.INPIN;
	}
	
	/**
	 * Sets the name of the pin.
	 * @param name the new name of this pin
	 */
	public void setPinName(String name){
		this.name = name;
	}
	
	/**
	 * Sets the instance to which this pin belongs.
	 * @param instance the instance to which this pin belongs
	 */
	public void setInstance(XdlInstance instance){
		if(this.instance != null){
			this.instance.removePin(this);
			if(net != null && net.getPIPs().size() > 0){
				// TODO - Unroute only PIPs that are needed
				net.unroute();
			}
		}
		this.instance = instance;
		if(name != null && instance != null){
			instance.addPin(this);
		}
	}
	
	/**
	 * Removes any reference to the instance from this pin and
	 * removes the pin from the pin map in the instance.
	 */
	public void detachInstance(){
		if(instance != null){
			instance.removePin(this);
			this.instance = null;
		}
	}
	
	/**
	 * Sets the port that references this pin.
	 * @param port the port that references this pin
	 */
	public void setPort(XdlPort port){
		this.port = port;
	}
	
	/**
	 * Gets the port that references this pin.  Null if there is none
	 * @return the port that references this pin
	 */
	public XdlPort getPort(){
		return this.port;
	}
	
	/**
	 * Sets the net attached to this pin.
	 * @param net the net to set
	 */
	public void setNet(XdlNet net) {
		this.net = net;
	}

	/**
	 * Returns the net attached to this pin.
	 * @return the net
	 */
	public XdlNet getNet() {
		return net;
	}
	
	/**
	 * Get the concatenated primitiveSiteName.PinName (ex. SLICE_X1Y2.C1)
	 * name for the pin.  This pin name is unique throughout the device.
	 * @return the primitive site name  pin name
	 */
	public String getPrimitiveSitePinName(){
		return instance.getPrimitiveSiteName() + "." + name;
	}

	@Override
	public String toString(){
		return pinType.name().toLowerCase() +
			instance.getName() + "\" " + this.name;
	}

	/**
	 * Generates a hashCode based on the instance, direction and pinName.
	 */
	@Override
	public int hashCode(){
		final int prime = 31;
		int result = 1;
		result = prime * result + ((instance == null) ? 0 : instance.hashCode());
		result = prime * result + pinType.hashCode();
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	/**
	 * Checks if obj is a pin and if equal to this pin by comparing instance, direction and pinName.
	 */
	@Override
	public boolean equals(Object obj){
		if(this == obj)
			return true;
		if(obj == null)
			return false;
		if(getClass() != obj.getClass())
			return false;
		XdlPin other = (XdlPin) obj;
		return Objects.equals(getInstance(), other.getInstance()) &&
				Objects.equals(getPinType(), other.getPinType()) &&
				Objects.equals(getName(), other.getName());
	}

	/**
	 * Returns the type of this pin.
	 * @return the type of this pin
	 */
	public PinType getPinType(){
		return this.pinType;
	}

	/**
	 * Sets the type of this pin.
	 * @param pinType the type of this pin
	 */
	public void setPinType(PinType pinType){
		this.pinType = pinType;
	}
}
