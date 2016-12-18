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

import java.util.ArrayList;

/**
 * There is no direct representation of a module instance in XDL. Each member of
 * a module instance is referenced in a particular way back to the module
 * instance. This class attempts to collect all the module instance information
 * into a single class.
 * 
 * @author Chris Lavin Created on: Jun 22, 2010
 */
public class XdlModuleInstance {

	/** Name of the module instance */
	private String name;
	/** The design which contains this module instance */
	private transient XdlDesign design;
	/** The module of which this object is an instance of */
	private XdlModule module;
	/** The anchor instance of the module instance */
	private XdlInstance anchor;
	/** A list of all primitive instances which make up this module instance */
	private ArrayList<XdlInstance> instances;
	/** A list of all nets internal to this module instance */
	private ArrayList<XdlNet> nets;
	
	/**
	 * Constructor initializing instance module name
	 * @param name Name of the module instance
	 */
	public XdlModuleInstance(String name, XdlDesign design){
		this.name = name;
		this.setDesign(design);
		this.module = null;
		this.setAnchor(null);
		instances = new ArrayList<>();
		nets = new ArrayList<>();
	}

	/**
	 * This will initialize this module instance to the same attributes
	 * as the module instance passed in.  This is primarily used for classes
	 * which extend ModuleInstance.
	 * @param moduleInstance The module instance to mimic.
	 */
	public XdlModuleInstance(XdlModuleInstance moduleInstance){
		this.name = moduleInstance.name;
		this.setDesign(moduleInstance.design);
		this.module = moduleInstance.module;
		this.setAnchor(moduleInstance.anchor);
		instances =  moduleInstance.instances;
		nets = moduleInstance.nets;	
	}
	
	/**
	 * Adds the instance inst to the instances list that are members of the
	 * module instance.
	 * @param inst The instance to add.
	 */
	public void addInstance(XdlInstance inst){
		instances.add(inst);
	}

	/**
	 * Adds the net to the net list that are members of the module instance.
	 * @param net The net to add.
	 */
	public void addNet(XdlNet net){
		nets.add(net);
	}

	/**
	 * @return the name of this module instance
	 */
	public String getName(){
		return name;
	}

	/**
	 * @param name the name to set
	 */
	public void setName(String name){
		this.name = name;
	}

	/**
	 * @param design the design to set
	 */
	public void setDesign(XdlDesign design){
		this.design = design;
	}

	/**
	 * @return the design
	 */
	public XdlDesign getDesign(){
		return design;
	}

	/**
	 * @return the moduleType
	 */
	public XdlModule getModule(){
		return module;
	}

	/**
	 * @param module the module to set.
	 */
	public void setModule(XdlModule module){
		this.module = module;
	}

	/**
	 * @return the instances
	 */
	public ArrayList<XdlInstance> getInstances(){
		return instances;
	}

	/**
	 * @param instances the instances to set
	 */
	public void setInstances(ArrayList<XdlInstance> instances){
		this.instances = instances;
	}

	/**
	 * @return the nets
	 */
	public ArrayList<XdlNet> getNets(){
		return nets;
	}

	/**
	 * @param nets the nets to set
	 */
	public void setNets(ArrayList<XdlNet> nets){
		this.nets = nets;
	}

	/**
	 * Sets the anchor instance for this module instance.
	 * @param anchor The new anchor instance for this module instance.
	 */
	public void setAnchor(XdlInstance anchor){
		this.anchor = anchor;
	}

	/**
	 * Gets and returns the anchor instance for this module instance.
	 * @return The anchor instance for this module instance.
	 */
	public XdlInstance getAnchor(){
		return anchor;
	}
	
	public boolean isPlaced(){
		return anchor.isPlaced();
	}

	/**
	 * Removes all placement information and unroutes all nets of the module instance.
	 */
	public void unplace(){
		//unplace instances
		for(XdlInstance inst : instances){
			inst.unPlace();
		}
		//unplace nets (remove pips)
		for(XdlNet net : nets){
			net.getPIPs().clear();
		}
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode(){
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj){
		if(this == obj)
			return true;
		if(obj == null)
			return false;
		if(getClass() != obj.getClass())
			return false;
		XdlModuleInstance other = (XdlModuleInstance) obj;
		if(name == null){
			if(other.name != null)
				return false;
		}
		else if(!name.equals(other.name))
			return false;
		return true;
	}
	
	
}
