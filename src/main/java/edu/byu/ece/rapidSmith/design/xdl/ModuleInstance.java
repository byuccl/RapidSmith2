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

import java.util.ArrayList;
import java.util.HashMap;

import edu.byu.ece.rapidSmith.design.PIP;
import edu.byu.ece.rapidSmith.device.*;
import edu.byu.ece.rapidSmith.util.MessageGenerator;

/**
 * There is no direct representation of a module instance in XDL. Each member of
 * a module instance is referenced in a particular way back to the module
 * instance. This class attempts to collect all the module instance information
 * into a single class.
 * 
 * @author Chris Lavin Created on: Jun 22, 2010
 */
public class ModuleInstance{

	/** Name of the module instance */
	private String name;
	/** The design which contains this module instance */
	private transient Design design;
	/** The module of which this object is an instance of */
	private Module module;
	/** The anchor instance of the module instance */
	private Instance anchor;
	/** A list of all primitive instances which make up this module instance */
	private ArrayList<Instance> instances;
	/** A list of all nets internal to this module instance */
	private ArrayList<Net> nets;
	
	/**
	 * Constructor initializing instance module name
	 * @param name Name of the module instance
	 */
	public ModuleInstance(String name, Design design){
		this.name = name;
		this.setDesign(design);
		this.module = null;
		this.setAnchor(null);
		instances = new ArrayList<Instance>();
		nets = new ArrayList<Net>();
	}

	/**
	 * This will initialize this module instance to the same attributes
	 * as the module instance passed in.  This is primarily used for classes
	 * which extend ModuleInstance.
	 * @param moduleInstance The module instance to mimic.
	 */
	public ModuleInstance(ModuleInstance moduleInstance){
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
	public void addInstance(Instance inst){
		instances.add(inst);
	}

	/**
	 * Adds the net to the net list that are members of the module instance.
	 * @param net The net to add.
	 */
	public void addNet(Net net){
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
	public void setDesign(Design design){
		this.design = design;
	}

	/**
	 * @return the design
	 */
	public Design getDesign(){
		return design;
	}

	/**
	 * @return the moduleType
	 */
	public Module getModule(){
		return module;
	}

	/**
	 * @param module the module to set.
	 */
	public void setModule(Module module){
		this.module = module;
	}

	/**
	 * @return the instances
	 */
	public ArrayList<Instance> getInstances(){
		return instances;
	}

	/**
	 * @param instances the instances to set
	 */
	public void setInstances(ArrayList<Instance> instances){
		this.instances = instances;
	}

	/**
	 * @return the nets
	 */
	public ArrayList<Net> getNets(){
		return nets;
	}

	/**
	 * @param nets the nets to set
	 */
	public void setNets(ArrayList<Net> nets){
		this.nets = nets;
	}

	/**
	 * Sets the anchor instance for this module instance.
	 * @param anchor The new anchor instance for this module instance.
	 */
	public void setAnchor(Instance anchor){
		this.anchor = anchor;
	}

	/**
	 * Gets and returns the anchor instance for this module instance.
	 * @return The anchor instance for this module instance.
	 */
	public Instance getAnchor(){
		return anchor;
	}
	
	public boolean isPlaced(){
		return anchor.isPlaced();
	}
	
	/**
	 * Does a brute force search to find all valid locations of where this module
	 * instance can be placed.  It returns the module instance to its original
	 * location.
	 * @return A list of valid anchor sites for the module instance to be placed.
	 */
	public ArrayList<Site> getAllValidPlacements(){
		ArrayList<Site> validSites = new ArrayList<Site>();
		Site originalSite = getAnchor().getPrimitiveSite();
		Design design = getDesign();
		Site[] sites = design.getDevice().getAllCompatibleSites(getAnchor().getType());
		for(Site newAnchorSite : sites){
			if(place(newAnchorSite, design.getDevice())){
				validSites.add(newAnchorSite);
				unplace();
			}
		}
		
		// Put hard macro back
		if(originalSite != null) place(originalSite, design.getDevice());
		
		return validSites;
	}

	
	/**
	 * Places the module instance anchor at the newAnchorSite as well as all other 
	 * instances and nets within the module instance at their relative offsets of the new site.
	 * @param newAnchorSite The new site for the anchor of the module instance.
	 * @param dev The device on which the module instance is being placed.
	 * @return True if placement was successful, false otherwise.
	 */
	public boolean place(Site newAnchorSite, Device dev){
		// Check if parameters are null
		if(newAnchorSite == null || dev == null){
			return false;
		}
		
		// Do some error checking on the newAnchorSite
		Site p = module.getAnchor().getPrimitiveSite();
		Tile t = newAnchorSite.getTile();
		Site newValidSite = Device.getCorrespondingPrimitiveSite(p, t);
		if(!newAnchorSite.equals(newValidSite)){
			//MessageGenerator.briefError("New anchor site (" + newAnchorSite.getName() +
			//		") is incorrect.  Should be " + newValidSite.getName());
			//this.unplace();
			return false;
		}
		
		// save original placement in case new placement is invalid
		HashMap<Instance, Site> originalSites;
		originalSites = isPlaced() ? new HashMap<Instance, Site>() : null;

		//=======================================================//
		/* Place instances at new location                       */
		//=======================================================//
		for(Instance inst : instances){
			Site templateSite = inst.getModuleTemplateInstance().getPrimitiveSite();
			Tile newTile = module.getCorrespondingTile(templateSite.getTile(), newAnchorSite.getTile(), dev);
			Site newSite = Device.getCorrespondingPrimitiveSite(templateSite, newTile);

			if(newSite == null){
				//MessageGenerator.briefError("ERROR: No matching primitive site found." +
				//	" (Template Primitive:"	+ templateSite.getName() + 
				//	", Template Tile:" + templateSite.getTile() +
				//	" => New Primitive:" + newSite + ", New Tile:" + newTile+")");
				
				// revert placement to original placement before method call
				if(originalSites == null){
					unplace();
					return false;
				}
				for(Instance i : originalSites.keySet()){
					design.getInstance(i.getName()).place(originalSites.get(i));
				}
				return false;
			}
			
			if(originalSites != null){ 
				originalSites.put(inst, inst.getPrimitiveSite());
			}
			inst.place(newSite);
		}
		
		//=======================================================//
		/* Place net at new location                             */
		//=======================================================//
		WireEnumerator we = design.getDevice().getWireEnumerator();
		int mCout = we.getWireEnum("M_COUT");
		int llCout = we.getWireEnum("LL_COUT");
		int wl5beg_s0 = we.getWireEnum("WL5BEG_S0");
		for(Net net : nets){
			net.getPIPs().clear();
			Net templateNet = net.getModuleTemplateNet();
			for(PIP pip : templateNet.getPIPs()){
				Tile templatePipTile = pip.getTile();
				Tile newPipTile = module.getCorrespondingTile(templatePipTile, newAnchorSite.getTile(), dev);
				if(newPipTile == null){
					unplace();
					MessageGenerator.briefError("Warning: Unable to return module instance "+ name +" back to original placement.");
					return false;
				}
				PIP newPip = new PIP(newPipTile, pip.getStartWire(), pip.getEndWire());
				//if(!newPipTile.hasPIP(newPip)){
				//	return false;
				//}
				net.addPIP(newPip);
				// Special cases for Virtex 5
				if(newPip.getStartWire() == mCout && newPipTile.getType().equals(TileType.CLBLL)){
					newPip.setStartWire(llCout);
				}
				else if(newPip.getStartWire() == llCout && newPipTile.getType().equals(TileType.CLBLM)){
					newPip.setStartWire(mCout);
				}
				else if(newPip.getEndWire() == wl5beg_s0){
					TileType check = dev.getTile(newPipTile.getRow(), newPipTile.getColumn()-1).getType();
					TileType check2 = dev.getTile(newPipTile.getRow(), newPipTile.getColumn()-2).getType();
					if(check.equals(TileType.INT_BUFS_R) || check2.equals(TileType.INT_BUFS_R)){
						int currWire = wl5beg_s0;
						WireConnection[] wcs = newPipTile.getWireConnections(currWire);
						Tile currTile = newPipTile;
						while(wcs.length == 1){
							if(wcs[0].isPIP()){
								net.addPIP(new PIP(currTile, currWire, wcs[0].getWire()));
							}
							currTile = wcs[0].getTile(currTile);
							currWire = wcs[0].getWire();
							wcs = currTile.getWireConnections(currWire);
						}
					}
				}
				
			}
		}
		return true;
	}
	
	/**
	 * Removes all placement information and unroutes all nets of the module instance.
	 */
	public void unplace(){
		//unplace instances
		for(Instance inst : instances){
			inst.unPlace();
		}
		//unplace nets (remove pips)
		for(Net net : nets){
			net.getPIPs().clear();
		}
	}

	/**
	 * This method will calculate and return the corresponding tile of a module instance.
	 * for a new anchor location.
	 * @param templateTile The tile in the module which acts as a template.
	 * @param newAnchorTile This is the tile of the new anchor instance of the module instance.
	 * @param dev The device which corresponds to this module instance.
	 * @return The new tile of the module instance which corresponds to the templateTile, or null
	 * if none exists.
	 */
	public Tile getCorrespondingTile(Tile templateTile, Tile newAnchorTile, Device dev){
		return module.getCorrespondingTile(templateTile, newAnchorTile, dev);
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
		ModuleInstance other = (ModuleInstance) obj;
		if(name == null){
			if(other.name != null)
				return false;
		}
		else if(!name.equals(other.name))
			return false;
		return true;
	}
	
	
}
