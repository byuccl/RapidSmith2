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
import java.util.*;

import edu.byu.ece.rapidSmith.device.Site;
import edu.byu.ece.rapidSmith.device.SiteType;
import edu.byu.ece.rapidSmith.device.Tile;

/**
 * This class represents the inst object in XDL design files.  It can determine
 * if an instance is placed/unplaced, its name, attributes, module instance and
 * also maintains a net list of which nets connect to the instance pins.
 * @author Chris Lavin
 * Created on: Jun 22, 2010
 */
public class Instance implements Serializable{

	private static final long serialVersionUID = -7993281723620318536L;

	/** Unique name of this instance */
	private String name;
	/** Type of the instance (e.g. "SLICEM" or "SLICEL") */
	private SiteType type;
	/** When an instance is unplaced, it might be bonded (true) or unbonded (false) */
	private Boolean bonded;
	/** The XDL Design this instance belongs to,
	 * it is null if this instance is part of a module definition */
	private transient Design design;
	/** All of the attributes in this instance */
	private Map<String, Attribute> attributes;
	/** This is a site of where the primitive will reside */
	private Site site;
	/** A list of nets to which this instance is connected */
	private HashSet<Net> netList;
	/** A list of all pins on this instance which are connected in nets */
	private Map<String, Pin> pinMap;
	/** Name of the module instance which this instance belongs to */
	private ModuleInstance moduleInstance;
	/** The module template (or definition) this instance is a member of */
	private Module moduleTemplate;
	/** The instance in the module template corresponding to this instance */
	private Instance moduleTemplateInstance;
	
	/**
	 * Creates a new nameless, typeless Instance.
	 * The attributes, nets and pins are initialized with empty structures.
	 */
	public Instance(){
		name = null;
		design = null;
		attributes = new HashMap<>();
		type = null;
		bonded = null;
		site = null;
		netList = new HashSet<>();
		pinMap = new HashMap<>();
		
		// Null unless this instance is of a module
		this.moduleInstance = null;
		this.moduleTemplate = null;
		this.moduleTemplateInstance = null;
	}
	
	/**
	 * Creates a new Instance with specified name and type.
	 * The attributes, nets and pins are initialized with empty structures.
	 * @param name name of the new Instance
	 * @param type the type of the new instance
	 */
	public Instance(String name, SiteType type){
		this.name = name;
		this.type = type;
		
		design = null;
		attributes = new HashMap<>();
		bonded = null;
		site = null;
		netList = new HashSet<>();
		pinMap = new HashMap<>();
		
		// Null unless this instance is of a module
		this.moduleInstance = null;
		this.moduleTemplate = null;
		this.moduleTemplateInstance = null;
	}

	/**
	 * Returns the name of this instance.
	 * @return the name of this instance
	 */
	public String getName(){
		return this.name;
	}

	/**
	 * Sets the name of this instance.
	 * @param name the name for this instance
	 */
	public void setName(String name){
		this.name = name;
	}

	/**
	 * Returns the type of this instance (such as "SLICEL" or "SLICEM")
	 * @return the PrimitiveType of this instance
	 */
	public SiteType getType(){
		return type;
	}

	/**
	 * Sets the type of this instance.
	 * @param type the PrimitiveType for this instance
	 */
	public void setType(SiteType type){
		this.type = type;
	}

	/**
	 * When an instance is unplaced, it could be bonded (true) or unbonded (false).
	 * @return true if bonded, false if unbonded, null otherwise
	 */
	public Boolean getBonded(){
		return bonded;
	}

	/**
	 * Sets the bonded parameter for this instance.
	 * @param bonded true if bonded, false if unbonded, or null if not applicable
	 */
	public void setBonded(Boolean bonded){
		this.bonded = bonded;
	}

	/**
	 * Returns the design which this instance is a part of.
	 * Members of module definitions are not part of designs.
	 * @return the design this instance is a part of or null if this instance is
	 *  not part of a design
	 */
	public Design getDesign(){
		return design;
	}

	/**
	 * Sets the design which this instance is a part of.
	 * The design should be null if it is a module definition.
	 * @param design the design for this instance
	 */
	public void setDesign(Design design){
		this.design = design;
	}

	/**
	 * Returns a collection containing the attributes of this instance.
	 * The collection supports removal but not insertion.
	 * @return the Collection containing the attributes of this instance
	 */
	public Collection<Attribute> getAttributes(){
		return attributes.values();
	}

	/**
	 * Returns the attribute from this instance with the specified physical name.
	 * @param physicalName name of the attribute to get
	 * @return the attribute with the physical name specified, or null if
	 *   no such attribute exists.
	 */
	public Attribute getAttribute(String physicalName){
		return attributes.get(physicalName);
	}

	/**
	 * Returns the value of the attribute with the specified physical name.
	 * @param physicalName physical name of the attribute to get the value of
	 * @return the value of the attribute or null if the attribute does not exist
	 */
	public String getAttributeValue(String physicalName){
		Attribute attr = getAttribute(physicalName);
		return attr==null ? null : attr.getValue();
	}

	/**
	 * Adds the attribute with value to this instance.
	 * @param physicalName physical name of the attribute
	 * @param logicalName the logical name, usually the name of the BEL
	 * @param value value to set the new attribute to
	 */
	public void addAttribute(String physicalName, String logicalName, String value){
		if(physicalName.getBytes()[0] == '_'){
			Attribute attr = attributes.get(physicalName);
			if(attr != null){
				attr.setLogicalName(attr.getLogicalName() + Attribute.multiValueSeparator + logicalName);
				attr.setValue(attr.getValue() + Attribute.multiValueSeparator + value);
				return;
			}
		}
		attributes.put(physicalName, new Attribute(physicalName, logicalName, value));
	}
	
	/**
	 * Adds the attribute to this instance.
	 * @param attribute the attribute to add
	 */
	public void addAttribute(Attribute attribute){
		addAttribute(attribute.getPhysicalName(), attribute.getLogicalName(), attribute.getValue());
	}

	/**
	 * Sets the map of attributes for this instance.
	 * @param attributes the map of attributes to associate with this instance
	 */
	public void setAttributes(HashMap<String, Attribute> attributes){
		this.attributes = attributes;
	}

	/**
	 * Checks if the design has an attribute with the specified physical name.
	 * @param physicalName the physical name of the attribute to check for
	 * @return true if this instance contains an attribute with the specified
	 *   physical name
	 */
	public boolean hasAttribute(String physicalName){
		return getAttribute(physicalName) != null;
	}
	
	/**
	 * Removes the attribute with the specified physical name.
	 * Note that if this is a multi-valued attribute, the caller will have
	 * to separate the attribute into its separate parts using
	 * Attribute.multiValueSeparator.
	 * @param physicalName name of the attribute to remove
	 * @return the removed attribute, null if the attribute does not exist
	 */
	public Attribute removeAttribute(String physicalName){
		return attributes.remove(physicalName);
	}
	
	/**
	 * Tests if the attribute with the specified physical name given has the
	 * specified value.
	 * @param physicalName name of the attribute to test
	 * @param value the value to test for
	 * @return true if the value of the attribute matches
	 */
	public boolean testAttributeValue(String physicalName, String value){
		Attribute attr = getAttribute(physicalName); 
		return attr != null && attr.getValue().equals(value);
	}

	/**
	 * Returns the nets that connect to this instance and its pins.
	 * @return the set of nets connecting to this instance
	 */
	public HashSet<Net> getNetList() {
		return netList;
	}

	/**
	 * Adds the net to the netlist for this instance
	 * @param net the net to be added
	 */
	public void addToNetList(Net net){
		this.netList.add(net);
	}

	/**
	 * Sets the netlist which contain nets connecting to this instance.
	 * This method does not add this instance to the netlist.
	 * @param netList the netlist for this instance
	 */
	public void setNetList(HashSet<Net> netList) {
		this.netList = netList;
	}

	/**
	 * Returns the pin on this instance with the specified name.
	 * @param pinName name of the pin on this instance to get
	 * @return the pin on this instance with the specified name or null if no pin
	 *   exists with the name
	 */
	public Pin getPin(String pinName){
		return pinMap.get(pinName);
	}

	/**
	 * Returns the set of pins on this instance.
	 * The collection supports removal but not insertion.
	 * @return the set of pins on this instance
	 */
	public Collection<Pin> getPins(){
		return pinMap.values();
	}

	/**
	 * Returns the pin map for this instance.
	 * The pin map consist of pin names to pins.
	 * @return returns the pin map for this instance
	 */
	public Map<String, Pin> getPinMap(){
		return pinMap;
	}

	/**
	 * Returns all the pin names that are on this instance.
	 * The set supports removal but not insertion.
	 * @return the set of pin names on this instance
	 */
	public Set<String> getPinNames(){
		return pinMap.keySet();
	}

	/**
	 * Adds a pin to the pin list of this instance.
	 * @param pin the pin to add
	 */
	public void addPin(Pin pin){
		if(pin.getName() != null) this.pinMap.put(pin.getName(), pin);
	}

	/**
	 * Removes the pin from this instance.
	 * @param pin the pin to remove
	 */
	public Pin removePin(Pin pin){
		return this.pinMap.remove(pin.getName());
	}

	/**
	 * Returns the PrimitiveSite for this instance.
	 * @return the PrimitiveSite or null if it is not placed.
	 */
	public Site getPrimitiveSite(){
		return site;
	}

	/**
	 * Returns the name of the location of this instance (ex. SLICE_X0Y0).
	 * @return the name of the site this instance is placed at or null if this
	 *   instance is unplaced
	 */
	public String getPrimitiveSiteName(){
		if(site == null){
			return null;
		}
		return site.getName();
	}

	/**
	 * Returns the tile this instance resides on the chip.
	 * @return the tile this instance resides
	 */
	public Tile getTile(){
		return site == null ? null : site.getTile();
	}

	/**
	 * Determines if this instance is placed or unplaced
	 * @return true if the instance is placed
	 */
	public boolean isPlaced(){
		return getPrimitiveSite() != null;
	}

	/**
	 * Places the primitive at the site specified.
	 * If this instance is already placed, it will first be unplaced.  This method
	 * will update the design if the design is set.  The bonded status of this
	 * instance is also cleared.
	 * @param site the site where the instance will reside
	 */
	public void place(Site site){
		if(this.site != null && design != null){
			design.releasePrimitiveSite(this.site);
		}
		setPrimitiveSite(site);
		setBonded(null);
		if(design != null) design.setPrimitiveSiteUsed(site, this);
	}

	/**
	 * This method is used for Module creation only.  DO NOT use.
	 * @see #place(Site)
	 */
	public void setSite(Site site){
		this.site = site;
	}

	/**
	 * Removes all placement information for the instance.  If the design is set,
	 * this method will update the design to reflect this operation.
	 */
	public void unPlace(){
		if(design != null) design.releasePrimitiveSite(site);
		setPrimitiveSite(null);
	}

	// sets the site and updates the design if set
	private void setPrimitiveSite(Site site){
		if(site != null){
			this.site = site;
			if(design != null) design.setPrimitiveSiteUsed(site, this);
		}

		this.site = site;
	}

	/**
	 * Returns the integer X value of the instance location
	 * (ex: SLICE_X5Y10, it will return 5).
	 * @return the X integer value of the site name or -1 if this instance is
	 *   not placed or does not have X/Y coordinates in the site name
	 */
	public int getInstanceX(){
		return site == null ? -1 : site.getInstanceX();
	}

	/**
	 * Returns the integer Y value of the instance location
	 * (ex: SLICE_X5Y10, it will return 10).
	 * @return the Y integer value of the site name or -1 if this instance is
	 *   not placed or does not have X/Y coordinates in the site name
	 */
	public int getInstanceY(){
		return site == null ? -1 : site.getInstanceY();
	}

	/**
	 * Returns the module instance this instance is a member of.
	 * @return the module instance this instance is a member of or null if it not
	 *   a member of any module instance
	 */
	public ModuleInstance getModuleInstance(){
		return moduleInstance;
	}

	/**
	 * Returns the name of the module instance this instance is a member of.
	 * @return the name of the module instance this instance is a member of or
	 *   null if it not a member of any module instance
	 */
	public String getModuleInstanceName(){
		return moduleInstance != null ? moduleInstance.getName() : null;
	}
	
	/**
	 * Reports if this instance corresponds to the anchor instance in a module
	 * instance, or is the anchor for a module.
	 * @return true if this instance is the anchor in a module or module
	 * instance
	 */
	public boolean isAnchor(){
		// If this is an instance part of a module instance
		if(moduleTemplateInstance != null){
			if(moduleTemplateInstance.equals(moduleTemplate.getAnchor())){
				return true;
			}
		}
		// else if this is an instance part of a module (module template)
		else if(moduleTemplate != null){
			if(moduleTemplate.getAnchor().equals(this)){
				return true;
			}
		}
		return false;
	}

	/**
	 * Sets the module instance of this instance (the instance of a
	 * module to which this instance belongs).  
	 * @param instanceModule the module instance for this instance
	 */
	public void setModuleInstance(ModuleInstance instanceModule){
		this.moduleInstance = instanceModule;
	}
	
	/**
	 * Checks if the provided instance and this instance are members of
	 * the same module instance.
	 * @param inst the instance to check
	 * @return true if both instances are members of the same module instance
	 */
	public boolean isMemberOfSameModuleInstance(Instance inst){
		if(this.moduleInstance==null || inst.moduleInstance == null)
			return false;
		return this.moduleInstance.equals(inst.moduleInstance);
	}
	
	/**
	 * Returns the module template this instance is a member of.
	 * @return the module template this instance is a member of
	 */
	public Module getModuleTemplate(){
		return moduleTemplate;
	}

	/**
	 * Sets the module this instance implements.
	 * @param instanceModuleTemplate the module which this instance implements
	 */
	public void setModuleTemplate(Module instanceModuleTemplate){
		this.moduleTemplate = instanceModuleTemplate;
	}

	/**
	 * Returns the instance in the module which this instance implements.
	 * @return the instance in the module which this instance implements
	 */
	public Instance getModuleTemplateInstance(){
		return moduleTemplateInstance;
	}

	/**
	 * Sets the corresponding instance inside a module template of this instance.
	 * @param moduleTemplateInstance the instance in the module to which this
	 * instance corresponds
	 */
	public void setModuleTemplateInstance(Instance moduleTemplateInstance){
		this.moduleTemplateInstance = moduleTemplateInstance;
	}

	/**
	 * This method will detach and remove all references of this instance from its module
	 * or module instance.
	 */
	public void detachFromModule(){
		this.moduleInstance = null;
		this.moduleTemplate = null;
		this.moduleTemplateInstance = null;
	}

	@Override
	public String toString(){
		StringBuilder sb = new StringBuilder();
		String nl = System.getProperty("line.separator");
		sb.append("inst \"").append(name).append("\" \"").append(type).append("\",");
		if(isPlaced()){
			sb.append("placed ").append(getTile()).append(" ").append(site);
		}
		else{
			sb.append("unplaced");
		}
		if(moduleInstance != null){
			sb.append("module \"").append(getModuleInstanceName()).append("\" \"")
					.append(getModuleTemplate().getName()).append("\" \"")
					.append(getModuleTemplateInstance().getName()).append("\" ,");
		}
		sb.append(nl).append("  cfg \"");
		for(Attribute attr : attributes.values()){
			sb.append(" ").append(attr.toString());
		}
		sb.append(" \"").append(nl).append("  ;").append(nl);
		
		return sb.toString();
	}
	
	/**
	 * Checks the instance type to see if it is a slice.
	 * @return true if this instance is a slice
	 */
	public boolean isSLICE(){
		return Design.sliceTypes.contains(getType());
	}
	
	/**
	 * Checks the instance type to see if it is a DSP.
	 * @return true if this instance is a DSP
	 */
	public boolean isDSP(){
		return Design.dspTypes.contains(getType());
	}
	
	/**
	 * Checks the instance type to see if it is a BRAM.
	 * @return true if this instance is a BRAM
	 */
	public boolean isBRAM(){
		return Design.bramTypes.contains(getType());
	}
	
	/**
	 * Checks the instance type to see if it is a IOB.
	 * @return true if this instance is a IOB
	 */
	public boolean isIOB(){
		return Design.iobTypes.contains(getType());
	}
}
