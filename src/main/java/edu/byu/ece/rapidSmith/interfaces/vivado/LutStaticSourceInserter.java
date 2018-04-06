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

import edu.byu.ece.rapidSmith.design.NetType;
import edu.byu.ece.rapidSmith.design.subsite.*;
import edu.byu.ece.rapidSmith.device.Bel;
import edu.byu.ece.rapidSmith.device.BelPin;
import edu.byu.ece.rapidSmith.device.BelTemplate;

import java.util.*;

/**
 *
 */
public class LutStaticSourceInserter {

	private final CellDesign design;
	private final CellLibrary libCells;
	private final Map<BelPin, CellPin> belPinToCellPinMap;
	private static final String GND_INIT_STRING = "0";
    private static final String VCC_INIT_STRING = "1";
    private static final String STATIC_GND_NAME = "rapidSmithStaticGND";
    private static final String STATIC_VCC_NAME = "rapidSmithStaticVCC";

    private int staticSourceID;

	/**
	 * Creates a new LutRoutethrough inserter object
	 * @param design {@link CellDesign}
	 * @param libCells {@link CellLibrary}
	 * @param pinMap A map from {@link BelPin} to the {@link CellPin} that is mapped onto it
	 */
	public LutStaticSourceInserter(CellDesign design, CellLibrary libCells, Map<BelPin, CellPin> pinMap) {
		this.design = design;
		this.libCells = libCells;
		this.staticSourceID = 0;
		//this.netsToAdd = new ArrayList<>();
		this.belPinToCellPinMap = pinMap;
	}

	/**
	 * Creates a new LutRoutethrough inserter object
	 * @param design {@link CellDesign}
	 * @param libCells {@link CellLibrary}
	 */
	public LutStaticSourceInserter(CellDesign design, CellLibrary libCells) {
		this.design = design; 
		this.libCells = libCells;
		this.staticSourceID = 0;
		//this.netsToAdd = new ArrayList<>();
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
	 */
	public CellDesign execute() {
        //CellNet vccNet = design.getVccNet();
		System.out.println("Find LUT routethroughs");
        CellNet gndNet = design.getGndNet();

        for (RouteTree routeTree : gndNet.getBelPinRouteTrees().values()) {
			BelPin sinkBelPin = routeTree.getConnectedBelPin();
			CellPin sinkCellPin = belPinToCellPinMap.get(sinkBelPin);

			while (routeTree.getParent() != null) {
				routeTree = routeTree.getParent();
			}

            BelPin belPin = routeTree.getConnectedSourceBelPin();


			Bel bel = belPin.getBel();
            CellPin cellPin = belPinToCellPinMap.get(belPin);

			if ((bel.getType().equals("LUT6") && belPin.getName().equals("O6")) || (bel.getType().equals("LUT5") && belPin.getName().equals("O5"))){
				if (cellPin == null) {
					// No cell pin is connected to this bel pin, so this LUT needs to be a static source
					insertStaticSourceLut(gndNet, belPin, sinkCellPin);
				}
			}


        }

        return design;
    }



	
	/**
	 * Rips up the LUT routethrough, and replaces it with a LUT1 pass-through cell 
	 * that is placed on the routethrough BEL.
	 * 
	 * @param net Net containing the routethrough
	 * @param rtSource BelPin source of the routethrough
	 */
	private void insertStaticSourceLut(CellNet net, BelPin rtSource, CellPin sinkCellPin) {
		// TODO: replace this code with a function
		//create a new lut1 cell with the appropriate init string
		Cell buffer = new Cell(STATIC_GND_NAME + staticSourceID, libCells.get("LUT1") );
		buffer.getProperties().update(new Property("INIT", PropertyType.EDIF, GND_INIT_STRING));
		design.addCell(buffer);
		
		// break the netlist
		// TODO: Go through entire routetree (through children, etc.) and make sure all sink pins
		// are disconnected from global GND
		// We're assuming there will only be one sink for now.
		net.disconnectFromPin(sinkCellPin);

		// Make a new GND net for the static GND LUT
		CellNet staticGndNet = new CellNet(STATIC_GND_NAME + "Net" + staticSourceID++, NetType.WIRE);
		staticGndNet.connectToPin(buffer.getPin("O"));
		staticGndNet.connectToPin(sinkCellPin);
		staticGndNet.setIsIntrasite(true); // mark the net as intrasite
		design.addNet(staticGndNet);
		
		// place lut cell and map pins correctly
		Bel rtBel = rtSource.getBel();
		design.placeCell(buffer, rtBel);
		buffer.getPin("O").mapToBelPin(rtSource);
	}

}


