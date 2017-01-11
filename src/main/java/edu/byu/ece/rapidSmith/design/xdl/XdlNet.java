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
import java.util.ArrayList;
import java.util.Collection;

import edu.byu.ece.rapidSmith.design.NetType;
import edu.byu.ece.rapidSmith.device.PIP;
import edu.byu.ece.rapidSmith.device.Tile;

/**
 * This class represents the nets in XDL.  It keeps track of the type, 
 * source and sinks as well as routing resources (PIPs).  It also has 
 * references to keep track of the module it may be a part of.
 * @author Chris Lavin
 * Created on: Jun 25, 2010
 */
public class XdlNet implements Serializable {
	private static final long serialVersionUID = 6252168375875946963L;

	/** Unique name of this net */
	private String name;
	/** Type of this net (VCC, GND, WIRE, ...) */
	private NetType type;
	/** Attributes of this net, often are not used */
	private ArrayList<XdlAttribute> attributes;
	/** Source and sink pins of this net */
	private ArrayList<XdlPin> pins;
	/** Routing resources or Programmable-Interconnect-Points */ 
	private ArrayList<PIP> pips;
	/** The source pin for this net */
	private XdlPin source;
	/** The number of sinks this net contains */
	private int fanOut;
	/** The module instance this net is a member of */
	private XdlModuleInstance moduleInstance;
	/** The module template (or definition) this net is a member of */
	private XdlModule moduleTemplate;
	/** The net in the module template corresponding to this net */
	private XdlNet moduleTemplateNet;
	
	/**
	 * Constructs a nameless net.
	 * Initializes the pins and pips with empty structures.
	 */
	public XdlNet(){
		this.name = null;
		this.type = NetType.WIRE;
		this.pins = new ArrayList<>();
		this.pips = new ArrayList<>();
		this.source = null;
		this.fanOut = 0;
		moduleInstance = null;
		moduleTemplate = null;
		moduleTemplateNet = null;
	}
	
	/**
	 * Constructs a net with the specified name and type.
	 * Initializes the pins and pips with empty structures.
	 *
	 * @param name name for the net
	 * @param type type for the net
	 */
	public XdlNet(String name, NetType type){
		this.name = name;
		this.type = type;
		this.pins = new ArrayList<>();
		this.pips = new ArrayList<>();
		this.source = null;
		this.fanOut = 0;
		moduleInstance = null;
		moduleTemplate = null;
		moduleTemplateNet = null;
	}
	
	/**
	 * Returns the name of this net.
	 * @return the name of this net
	 */
	public String getName(){
		return name;
	}

	/**
	 * Sets the name of this net. User is responsible to make sure the
	 * net name is unique to all other net names in the design.
	 * @param name New name of this net.
	 */
	public void setName(String name){
		this.name = name;
	}

	/**
	 * Returns the type of this net.
	 * @return the type of this net
	 */
	public NetType getType(){
		return type;
	}

	/**
	 * Sets the type of this net.
	 * @param type type of this net
	 */
	public void setType(NetType type){
		this.type = type;
	}

	/**
	 * Returns the pins (source and sinks) of this net.
	 * @return the pins of this net
	 */
	public ArrayList<XdlPin> getPins(){
		return pins;
	}

	/**
	 * Sets the pins (source and sinks) of this net.
	 * For each pin in the list, updates the pin to be apart of this net and adds
	 * this net to the pin's instance.
	 * @param list list containing the new pins
	 */
	public boolean setPins(ArrayList<XdlPin> list){
		XdlPin src = null;
		this.fanOut = 0;
		for(XdlPin p : list){
			if(p.isOutPin()){
				if(src != null){
					return false;
				}
				src = p;
			}
			else{
				this.fanOut++;
			}
			p.setNet(this);
			p.getInstance().addToNetList(this);
		}
		this.pins = list;
		this.source = src;
		return true;
	}

	/**
	 * Adds a new pin to this net.
	 * Also checks if this is a new sink and updates the fan-out accordingly.
	 * This method will unroute this net.
	 * @param pin the new pin to add
	 * @return true if the operation completed successfully
	 */
	public boolean addPin(XdlPin pin){
		if(pin.isOutPin()){
			if(source != null){
				return false;
			}
			this.source = pin;
		}
		else{
			fanOut++;
		}
		pins.add(pin);
		pin.setNet(this);
		return true;
	}

	/**
	 * Adds each pin in the provided collection to this net.
	 * @param pinsToAdd the list of new pins to add
	 * @return true if the operation completed successfully
	 */
	public boolean addPins(Collection<XdlPin> pinsToAdd) {
		boolean success = true;
		for(XdlPin pin : pinsToAdd){
			if(!this.addPin(pin))
				success = false;
		}
		return success;
	}

	/**
	 * Removes a pin from this net.
	 * Updates the fan-out accordingly and clears the source if the removed pin was
	 * the source.
	 * @param pin the pin to remove
	 * @return true if the operation completed successfully
	 */
	public boolean removePin(XdlPin pin){
		if(pin.isOutPin() && pin.equals(source)){
			this.source = null;
		}
		else{
			fanOut--;
		}
		pin.setNet(null);
		if(pips.size() > 0){
			// TODO - Be smarter about unrouting only the resources
			// connected to the pin, such as unroute(Pin);
			unroute();
		}
		return pins.remove(pin);
	}

	/**
	 * Returns the fan-out (number of sinks) of this net.
	 * @return the fan-out of this net
	 */
	public int getFanOut(){
		return fanOut;
	}

	/**
	 * Returns the PIPs (routing resources) used by this net.
	 * @return the PIPs used by this net
	 */
	public ArrayList<PIP> getPIPs(){
		return pips;
	}

	/**
	 * Sets the PIPs of this net.
	 * @param list the new list of PIPs
	 */
	public void setPIPs(ArrayList<PIP> list){
		this.pips = list;
	}

	/**
	 * Adds a PIP to this net.
	 * @param pip the PIP to add
	 */
	public void addPIP(PIP pip){
		pips.add(pip);
	}

	/**
	 * Removes a PIP from this net.
	 * @param pip the PIP to remove
	 * @return true if the operation completed successfully
	 */
	public boolean removePIP(PIP pip){
		return pips.remove(pip);
	}

	/**
	 * Checks if this net has any PIPs.
	 * @return true if this net contains 1 or more PIPs
	 */
	public boolean hasPIPs(){
		return pips.size() > 0;
	}

	/**
	 * Returns the attributes for this net.
	 * @return the attributes for this net
	 */
	public ArrayList<XdlAttribute> getAttributes(){
		return attributes;
	}

	/**
	 * Sets the list of attributes for this net.
	 * @param attributes the new list of attributes
	 */
	public void setAttributes(ArrayList<XdlAttribute> attributes){
		this.attributes = attributes;
	}

	/**
	 * Adds an attribute to this net.
	 * @param physicalName the physical name portion of the attribute
	 * @param logicalName the logical name portion of the attribute
	 * @param value the value of the attribute
	 */
	public void addAttribute(String physicalName, String logicalName, String value){
		addAttribute(new XdlAttribute(physicalName, logicalName, value));
	}

	/**
	 * Adds an attribute with no logical name to this net.
	 * @param physicalName the physical name portion of the attribute
	 * @param value the value of the attribute
	 */
	public void addAttribute(String physicalName, String value){
		addAttribute(new XdlAttribute(physicalName, null, value));
	}

	/**
	 * Add the attribute to this net.
	 * @param attribute the attribute to add
	 */
	public void addAttribute(XdlAttribute attribute){
		if(attributes == null){
			attributes = new ArrayList<>();
		}
		attributes.add(attribute);
	}

	/**
	 * Checks if this net has any attributes.
	 * @return true if this net has one or more attributes
	 */
	public boolean hasAttributes(){
		return getAttributes() != null;
	}

	/**
	 * Checks if this net is a static net (source is VCC/GND).
	 * @return true if net is source'd by VCC or GND
	 */
	public boolean isStaticNet(){
		return (this.type.equals(NetType.VCC)) || (this.type.equals(NetType.GND));
	}

	/**
	 * Checks if a net is a clk net and should use the clock routing 
	 * resources.
	 * Nets are considered clk nets if most of the sink pins have "CLK" in the
	 * internal pin name
	 * @return true if this net is a clock net
	 */
	public boolean isClkNet(){
		// This is kind of difficult to quantify, but we'll use the following
		// checks to see if a net is a clk net:
		// Most of the sink pins have "CLK" in the internal pin name
		int count = 0;
		for(XdlPin p : pins){
			if(p.getName().contains("CLK")){
				count++;
			}
		}
		return count >= (pins.size() / 2);
	}

	/**
	 * Returns the source of this net.
	 * @return the current source of this net, or null if it does not exist
	 */
	public XdlPin getSource(){
		return source;
	}

	/**
	 * Sets the source pin of this net.
	 * The user is responsible to ensure the pin is already a part of this net.
	 * Usually this method is not needed as <code>addPin</code> will set the pin as
	 * the source if the pin is an outpin.
	 * @param source the new source pin for this net
	 */
	public void setSource(XdlPin source){
		this.source = source;
	}

	/**
	 * Replaces the current source with the new source and
	 * adds it to the pin list in this net.
	 * @param newSource the new source of this net
	 */
	public boolean replaceSource(XdlPin newSource){
		if(!newSource.isOutPin()){
			return false;
		}
		if(this.source != null){
			removePin(this.source);
		}
		this.source = newSource;
		return this.pins.add(newSource);
	}

	/**
	 * Removes the source of this net.
	 * Does not remove the pin.
	 */
	public void removeSource(){
		this.source = null;
	}
	
	/**
	 * Returns the tile where the source pin resides.
	 * @return the tile where the source pin resides, or null if there is
	 * no source for this net. 
	 */
	public Tile getSourceTile(){
		return source != null ? source.getTile() : null;
	}
	
	/**
	 * This removes all PIPs from this net, causing it to be in an unrouted state.
	 */
	public void unroute(){
		this.pips.clear();
	}
	
	/**
	 * Returns the total number of pins plus the total number
	 * of PIPs.
	 * @return the sum of pins and PIPs in this net
	 */
	public int getPinAndPIPCount(){
		return pins.size() + pips.size();
	}
	
	/**
	 * Returns the module template this net is a member of.
	 * @return the module template this net is a member of
	 */
	public XdlModule getModuleTemplate(){
		return moduleTemplate;
	}

	/**
	 * Sets the module class this net implements.
	 * @param module the module which this net implements
	 */
	public void setModuleTemplate(XdlModule module){
		this.moduleTemplate = module;
	}

	/**
	 * Returns this nets current module instance it belongs to.
	 * @return the module instance of this net, or null if none exists
	 */
	public XdlModuleInstance getModuleInstance(){
		return moduleInstance;
	}

	/**
	 * Sets the module instance which this net belongs to.
	 * @param moduleInstance this nets new moduleInstance
	 */
	public void setModuleInstance(XdlModuleInstance moduleInstance){
		this.moduleInstance = moduleInstance;
	}

	/**
	 * Returns the net found in the module which this net implements.
	 * @return the net found in the module which this net implements
	 */
	public XdlNet getModuleTemplateNet(){
		return moduleTemplateNet;
	}

	/**
	 * Sets the reference to the template net from a module template corresponding to this net.
	 * @param moduleTemplateNet the template net in the module to which this
	 * net corresponds.
	 */
	public void setModuleTemplateNet(XdlNet moduleTemplateNet){
		this.moduleTemplateNet = moduleTemplateNet;
	}

	/**
	 * This method will detach and remove all reference of this net to a module
	 * or module instance.
	 */
	public void detachFromModule(){
		this.moduleInstance = null;
		this.moduleTemplate = null;
		this.moduleTemplateNet = null;
		this.setAttributes(null);
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("net \"" + getName() + "\" ");
		if (!getType().equals(NetType.WIRE))
			sb.append(getType().name().toLowerCase());
		return sb.toString();
	}
}
