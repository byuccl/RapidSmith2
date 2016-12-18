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

package edu.byu.ece.rapidSmith.interfaces.vivado;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import edu.byu.ece.rapidSmith.design.subsite.BelRoutethrough;
import edu.byu.ece.rapidSmith.design.subsite.CellDesign;
import edu.byu.ece.rapidSmith.design.subsite.CellLibrary;
import edu.byu.ece.rapidSmith.design.subsite.CellPin;
import edu.byu.ece.rapidSmith.device.Bel;
import edu.byu.ece.rapidSmith.device.BelPin;
import edu.byu.ece.rapidSmith.device.Device;

/**
 * This class packages a TINCR checkpoint so that it can be returned to the user.
 * 
 * @author Thomas Townsend
 *
 */
public final class TincrCheckpoint {
	
	private final CellLibrary libCells;
	private final Device device;
	private final CellDesign design;
	private final String partName;
	private Set<Bel> routethroughBels;
	private Collection<BelRoutethrough> routethroughObjects;
	private Collection<Bel> staticSourceBels;
	private Map<BelPin, CellPin> belPinToCellPinMap;

	public TincrCheckpoint(String partName, CellDesign design, Device device, CellLibrary libCells) {
		this.partName = partName;
		this.design = design;
		this.device = device;
		this.libCells = libCells;
	}
	
	/**
	 * Returns the FPGA device associated with the TCP
	 */
	public Device getDevice() {
		return device;
	}
	
	/**
	 * Returns the cell library associated with the TCP
	 */
	public CellLibrary getLibCells() {
		return libCells;
	}
	
	/**
	 * Returns the FPGA design associated with the TCP
	 */
	public CellDesign getDesign() {
		return design;
	}
	
	/**
	 * Returns the part name of the device associated with the TCP
	 * 
	 * TODO: Do we need this? The device should already have this...
	 */
	public String getPartName() {
		return partName;
	}
	
	public void setRoutethroughBels(Map<Bel, BelRoutethrough> rtBels) {
		this.routethroughBels = rtBels.keySet();
		this.routethroughObjects = rtBels.values();
	}
	
	public Collection<BelRoutethrough> getRoutethroughObjects() {
		return routethroughObjects;
	}
	
	public Set<Bel> getBelRoutethroughs() {
		return routethroughBels;
	}
	
	public void setStaticSourceBels(Collection<Bel> bels) {
		this.staticSourceBels = bels;
	}
	
	public Collection<Bel> getStaticSourceBels() {
		return this.staticSourceBels;
	}
	
	public void setBelPinToCellPinMap(Map<BelPin, CellPin> pinMap) {
		this.belPinToCellPinMap = pinMap;
	}
	
	public Map<BelPin, CellPin> getBelPinToCellPinMap() {
		return this.belPinToCellPinMap;
	}
}
