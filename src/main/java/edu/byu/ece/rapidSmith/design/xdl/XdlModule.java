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
import java.util.HashMap;

/**
 * This class represents the modules as found in XDL.  They are used to describe
 * hard macros and RPMs and instances of each.
 * @author Chris Lavin
 * Created on: Jun 22, 2010
 */
public class XdlModule implements Serializable{

	private static final long serialVersionUID = 7127893920489370872L;
	/** This is the key into externalPortMap for retrieving the constraints used to build the hard macro */
	public static final String moduleBuildConstraints = "MODULE_BUILD_CONSTRAINTS";
	/** Unique name of this module */
	private String name;
	/** All of the attributes in this module */
	private ArrayList<XdlAttribute> attributes;
	/** This is the anchor of the module */
	private XdlInstance anchor;
	/** Ports on the module */
	private final HashMap<String, XdlPort> portMap;
	/** Instances which are part of the module */
	private final HashMap<String,XdlInstance> instanceMap;
	/** Nets of the module */
	private final HashMap<String,XdlNet> netMap;
	/** Keeps track of the minimum clock period of this module */
	private float minClkPeriod = Float.MAX_VALUE;
	/** Provides a catch-all map to store information about hard macro */
	private HashMap<String, ArrayList<String>> metaDataMap;

	/**
	 * Empty constructor, strings are null, everything else is initialized
	 */
	public XdlModule(){
		name = null;
		anchor = null;
		attributes = new ArrayList<>();
		portMap = new HashMap<>();
		instanceMap = new HashMap<>();
		netMap = new HashMap<>();
	}
	
	/**
	 * Creates and returns a new hard macro design with the appropriate
	 * settings and adds this module to the module list.  
	 * @return A complete hard macro design with this module as the hard macro.
	 */
	public XdlDesign createDesignFromModule(String partName){
		XdlDesign design = new XdlDesign();
		design.setPartName(partName);
		design.setName(XdlDesign.hardMacroDesignName);
		design.setIsHardMacro(true);
		design.addModule(this);
		return design;
	}
	
	
	/**
	 * Sets the name of this module 
	 * @param name New name for this module
	 */
	public void setName(String name){
		this.name = name;
	}

	/**
	 * Gets and returns the current name of this module
	 * @return The current name of this module
	 */
	public String getName(){
		return name;
	}

	/**
	 * Gets and returns the current attributes of this module
	 * @return The current attributes of this module
	 */
	public ArrayList<XdlAttribute> getAttributes(){
		return attributes;
	}
	
	/**
	 * Adds the attribute with value to this module.
	 * @param physicalName Physical name of the attribute.
	 * @param value Value to set the new attribute to.
	 */
	public void addAttribute(String physicalName, String logicalName, String value){
		attributes.add(new XdlAttribute(physicalName, logicalName, value));
	}
	
	/**
	 * Add the attribute to this module.
	 * @param attribute The attribute to add.
	 */
	public void addAttribute(XdlAttribute attribute){
		attributes.add(attribute);
	}
	
	/**
	 * Checks if the design attribute has an attribute with a physical
	 * name called phyisicalName.  
	 * @param physicalName The physical name of the attribute to check for.
	 * @return True if this module contains an attribute with the 
	 * 		   physical name physicalName, false otherwise.
	 */
	public boolean hasAttribute(String physicalName){
		for(XdlAttribute attr : attributes){
			if(attr.getPhysicalName().equals(physicalName)){
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Sets the list of attributes for this module.
	 * @param attributes The new list of attributes to associate with this
	 * module.
	 */
	public void setAttributes(ArrayList<XdlAttribute> attributes){
		this.attributes = attributes;
	}

	
	/**
	 * This gets and returns the instance anchor of the module.
	 * @return Instance which is the anchor for this module.
	 */
	public XdlInstance getAnchor(){
		return anchor;
	}
	
	/**
	 * Gets and returns the instance in the module called name.
	 * @param name Name of the instance in the module to get.
	 * @return The instance name or null if it does not exist.
	 */
	public XdlInstance getInstance(String name){
		return instanceMap.get(name);
	}
	
	/**
	 * Gets and returns all of the instances part of this module.
	 * @return The instances that are part of this module.
	 */
	public Collection<XdlInstance> getInstances(){
		return instanceMap.values();
	}
	
	/**
	 * Gets and returns the net in the module called name.
	 * @param name Name of the net in the module to get.
	 * @return The net name or null if it does not exist.
	 */
	public XdlNet getNet(String name){
		return netMap.get(name);
	}
	
	/**
	 * Gets and returns all the nets that are part of the module.
	 * @return The nets that are part of this module.
	 */
	public Collection<XdlNet> getNets(){
		return netMap.values();
	}
	
	/**
	 * Removes a net from the design
	 * @param name The name of the net to remove.
	 */
	public void removeNet(String name){
		XdlNet n = getNet(name);
		if(n != null) removeNet(n);
	}
	
	/**
	 * Removes a net from the design
	 * @param net The net to remove from the design.
	 */
	public void removeNet(XdlNet net){
		for(XdlPin p : net.getPins()){
			p.getInstance().getNetList().remove(net);
			if(p.getNet().equals(net)){
				p.setNet(null);
			}
		}
		netMap.remove(net.getName());
	}
	
	
	/**
	 * This method carefully removes an instance in a design with its
	 * pins and possibly nets.  Nets are only removed if they are empty
	 * after removal of the instance's pins. This method CANNOT remove 
	 * instances that are part of a ModuleInstance.
	 * @param name The instance name in the module to remove.
	 * @return True if the operation was successful, false otherwise.
	 */
	public boolean removeInstance(String name){
		return removeInstance(getInstance(name));
	}
	
	/**
	 * This method carefully removes an instance in a design with its
	 * pins and possibly nets.  Nets are only removed if they are empty
	 * after removal of the instance's pins. This method CANNOT remove 
	 * instances that are part of a ModuleInstance.
	 * @param instance The instance in the design to remove.
	 * @return True if the operation was successful, false otherwise.
	 */
	public boolean removeInstance(XdlInstance instance){
		if(instance.getModuleInstance() != null){
			return false;
		}
		for(XdlPin p : instance.getPins()){
			p.getNet().unroute(); 
			if(p.getNet().getPins().size() == 1){
				netMap.remove(p.getNet().getName());
			}
			else{
				p.getNet().removePin(p);				
			}
		}
		instanceMap.remove(instance.getName());
		instance.setDesign(null);
		instance.setNetList(null);
		instance.setModuleTemplate(null);
		return true;
	}
	
	
	
	/**
	 * Sets the anchor instance for this module.
	 * @param anchor New anchor instance for this module.
	 */
	public void setAnchor(XdlInstance anchor){
		this.anchor = anchor;
	}
	
	/**
	 * Gets and returns the port list for this module.
	 * @return The port list for this module.
	 */
	public Collection<XdlPort> getPorts(){
		return portMap.values();
	}

	/**
	 * Sets the port list for this module.
	 * @param portList The new port list to be set for this module.
	 */
	public void setPorts(ArrayList<XdlPort> portList){
		portMap.clear();
		for(XdlPort p: portList){
			addPort(p);
		}
	}
	
	/**
	 * Adds a port to this module.
	 * @param port The new port to add.
	 */
	public void addPort(XdlPort port){
		this.portMap.put(port.getName(), port);
	}
	
	/**
	 * Returns the port with the given name.
	 * @param name the port's name
	 * @return the port
	 */
	public XdlPort getPort(String name){
		return this.portMap.get(name);
	}
	
	/**
	 * Adds a net to this module.
	 * @param net The net to add to the module.
	 */
	public void addNet(XdlNet net){
		this.netMap.put(net.getName(), net);
	}
	
	/**
	 * Adds an instance to this module.
	 * @param inst The instance to add to the module.
	 */
	public void addInstance(XdlInstance inst){
		this.instanceMap.put(inst.getName(), inst);
	}
	
	/**
	 * @return the metaDataMap
	 */
	public HashMap<String, ArrayList<String>> getMetaDataMap() {
		return metaDataMap;
	}

	/**
	 * @param metaDataMap the metaDataMap to set
	 */
	public void setMetaDataMap(HashMap<String, ArrayList<String>> metaDataMap) {
		this.metaDataMap = metaDataMap;
	}

	/**
	 * @param minClkPeriod the minClkPeriod to set
	 */
	public void setMinClkPeriod(float minClkPeriod) {
		this.minClkPeriod = minClkPeriod;
	}

	/**
	 * @return the minClkPeriod
	 */
	public float getMinClkPeriod() {
		return minClkPeriod;
	}

	/**
	 * Sets the design in all the module's instances to null
	 */
	public void disconnectDesign(){
		for(XdlInstance i: this.getInstances()){
			i.setDesign(null);
		}
	}

	/**
	 * Generates the hashCode strictly on the module name.
	 */
	@Override
	public int hashCode(){
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	/**
	 *  Checks if two modules are equal based on the name of the module.
	 */
	@Override
	public boolean equals(Object obj){
		if(this == obj)
			return true;
		if(obj == null)
			return false;
		if(getClass() != obj.getClass())
			return false;
		XdlModule other = (XdlModule) obj;
		if(name == null){
			if(other.name != null)
				return false;
		}
		else if(!name.equals(other.name))
			return false;
		return true;
	}
	
	public String toString(){
		return name;
	}
}
