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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import edu.byu.ece.rapidSmith.design.NetType;
import edu.byu.ece.rapidSmith.design.subsite.Cell;
import edu.byu.ece.rapidSmith.design.subsite.CellDesign;
import edu.byu.ece.rapidSmith.design.subsite.CellLibrary;
import edu.byu.ece.rapidSmith.design.subsite.CellNet;
import edu.byu.ece.rapidSmith.design.subsite.CellPin;
import edu.byu.ece.rapidSmith.design.subsite.Property;
import edu.byu.ece.rapidSmith.design.subsite.PropertyType;
import edu.byu.ece.rapidSmith.design.subsite.RouteTree;
import edu.byu.ece.rapidSmith.device.Bel;
import edu.byu.ece.rapidSmith.device.BelPin;

/**
 * This class is used to insert LUT buffers on BELs that are acting as routethroughs. <br>
 * It is not possible to manually set a BEL in Vivado as a routethrough, so a LUT1 cell
 * needs to be inserted on the bel to achieve the same functionality. 
 *  
 * TODO: add option to create a deep copy of the 
 */
public class LutRoutethroughInserter {
	
	private final CellDesign design;
	private final CellLibrary libCells; 
	private final Map<BelPin, CellPin> belPinToCellPinMap;
	private static final String ROUTETHROUGH_INIT_STRING = "2'h2";
	private static final String ROUTETHROUGH_NAME = "rapidSmithRoutethrough";
	private int routethroughID;
	private Collection<CellNet> netsToAdd = new ArrayList<>();
	
	/**
	 * Creates a new LutRoutethrough inserter object
	 * @param design {@link CellDesign}
	 * @param libCells {@link CellLibrary}
	 * @param pinMap A map from {@link BelPin} to the {@link CellPin} that is mapped onto it
	 */
	public LutRoutethroughInserter(CellDesign design, CellLibrary libCells, Map<BelPin, CellPin> pinMap) {
		this.design = design; 
		this.libCells = libCells;
		this.routethroughID = 0;
		this.netsToAdd = new ArrayList<>();
		this.belPinToCellPinMap = pinMap;
	}
	
	/**
	 * Creates a new LutRoutethrough inserter object
	 * @param design {@link CellDesign}
	 * @param libCells {@link CellLibrary}
	 */
	public LutRoutethroughInserter(CellDesign design, CellLibrary libCells) {
		this.design = design; 
		this.libCells = libCells;
		this.routethroughID = 0;
		this.netsToAdd = new ArrayList<>();
		this.belPinToCellPinMap = createBelPinToCellPinMap();
	}
		
	/**
	 * Creates and returns a map from BelPin to CellPin in the current design.
	 */
	private Map<BelPin, CellPin> createBelPinToCellPinMap() {
		
		// Get all cell pins in the netlist
		Iterator<CellPin> cellPinIt =  design.getUsedSites().stream()
										.flatMap(site -> design.getCellsAtSite(site).stream())
										.flatMap(cell -> cell.getPins().stream()).iterator();
		
		// create the map from BelPin to CellPin
		HashMap<BelPin, CellPin> tmpMap = new HashMap<>();
		while(cellPinIt.hasNext()) {
			CellPin cellPin = cellPinIt.next();
			
			for (BelPin belPin : cellPin.getMappedBelPins()) {
				tmpMap.put(belPin, cellPin);
			}	
		}
		
		return tmpMap;
	}
	
	/**
	 * Runs the routethrough inserter. Looks for all LUT BELs in the design that are being
	 * used as a routethrough, and inserts a LUT1 cell instead.
	 *  
	 * @param design CellDesign to insert routethroughs
	 * @param libCells CellLibrary of the current part 
	 */
	public CellDesign execute() {
		
		for (CellNet net: design.getNets()) {
								
			for (RouteTree routeTree : net.getSinkSitePinRouteTrees()) {
				
				List<CellPin> sinks = new ArrayList<>(4);
				BelPin rtSource = tryFindRoutethroughSourcePin(routeTree, sinks);
						
				if (rtSource != null) { // we found a routethrough
					assert(!sinks.isEmpty());
					insertRouteThroughBel(net, rtSource, sinks);
				}
			}
		}
		
		// add the newly created nets to the design
		addRoutethroughNetsToDesign();

		return design;
	}
	
	/**
	 * Looks for Lut routethroughs in the specified RouteTree.
	 *  
	 * @param route RouteTree to search for LUT routethroughs
	 * @param sinks List to add sink CellPins to
	 * @return The source BelPin of the routethrough. If no routethrough is found, null is returned
	 */
	private BelPin tryFindRoutethroughSourcePin(RouteTree route, List<CellPin> sinks) {
		
		Iterator<RouteTree> rtIterator = route.getFirstSource().iterator();
		BelPin rtSource = null;
		
		while (rtIterator.hasNext()) {
			RouteTree current = rtIterator.next();
			
			if(!current.isSourced()) {
				continue;
			}
			
			if (current.getConnection().isRouteThrough()) {
				rtSource = current.getSourceTree().getConnectingBelPin();
			}
			else if (current.isLeaf()) {
				BelPin bp = current.getConnectingBelPin();
				
				// add all sinks that are not connected to LUTs
				if (!bp.getBel().getName().endsWith("LUT")) {
					sinks.add(belPinToCellPinMap.get(bp));
				}
			}
		}
		
		// null indicates we didn't find a routethrough
		return rtSource;
	}
	
	/**
	 * Rips up the LUT routethrough, and replaces it with a LUT1 pass-through cell 
	 * that is placed on the routethrough BEL.
	 * 
	 * @param net Net containing the routethrough
	 * @param rtSource BelPin source of the routethrough
	 * @param sinks List of cell pin sinks in the original net
	 */
	private void insertRouteThroughBel(CellNet net, BelPin rtSource, List<CellPin> sinks) {
		// TODO: replace this code with a function
		//create a new lut1 cell with the appropriate init string
		Cell buffer = new Cell(ROUTETHROUGH_NAME + routethroughID, libCells.get("LUT1") );
		buffer.getProperties().update(new Property("INIT", PropertyType.EDIF, ROUTETHROUGH_INIT_STRING));
		design.addCell(buffer);
		
		// break the netlist 
		net.disconnectFromPins(sinks);
		net.connectToPin(buffer.getPin("I0"));
		
		// add new net .. TODO: randomize the naming scheme more
		CellNet routethroughNet = new CellNet(ROUTETHROUGH_NAME + "Net" + routethroughID++, NetType.WIRE);
		routethroughNet.connectToPin(buffer.getPin("O"));
		routethroughNet.connectToPins(sinks);
		routethroughNet.setIsIntrasite(true); // mark the second portion of the net as intrasite
		netsToAdd.add(routethroughNet);
		
		// place lut cell and map pins correctly
		Bel rtBel = rtSource.getBel();
		design.placeCell(buffer, rtBel);
		buffer.getPin("I0").mapToBelPin(rtSource);
		buffer.getPin("O").mapToBelPin(buffer.getPin("O").getPossibleBelPins().get(0));
	}
	
	/**
	 * Adds all newly created nets to the CellDesign
	 */
	private void addRoutethroughNetsToDesign() {
		netsToAdd.forEach(design::addNet);
	}
}
