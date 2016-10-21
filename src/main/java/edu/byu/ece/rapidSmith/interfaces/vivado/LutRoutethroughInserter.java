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
 * 
 * @author Thomas Townsend
 *
 */
public class LutRoutethroughInserter {
	
	private final CellDesign design;
	private final CellLibrary libCells; 
	private final Map<BelPin, CellPin> belPinToCellPinMap;
	private static final String ROUTETHROUGH_INIT_STRING = "2'h2";
	private static final String ROUTETHROUGH_NAME = "rapidSmithRoutethrough";
	private int routethroughID;
	private Collection<CellNet> netsToAdd = new ArrayList<CellNet>();
	private List<Cell> cellsToAdd = new ArrayList<Cell>();
	
	public LutRoutethroughInserter(CellDesign design, CellLibrary libCells, Map<BelPin, CellPin> pinMap) {
		this.design = design; 
		this.libCells = libCells;
		this.routethroughID = 0;
		this.netsToAdd = new ArrayList<CellNet>();
		this.belPinToCellPinMap = pinMap;
	}
	
	public LutRoutethroughInserter(CellDesign design, CellLibrary libCells) {
		this.design = design; 
		this.libCells = libCells;
		this.routethroughID = 0;
		this.netsToAdd = new ArrayList<CellNet>();
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
		HashMap<BelPin, CellPin> tmpMap = new HashMap<BelPin, CellPin>();
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
				
				List<CellPin> sinks = new ArrayList<CellPin>(4);
				BelPin rtSource = tryFindRoutethroughSourcePin(routeTree, sinks);
						
				if (rtSource != null) { // we found a routethrough
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
	private BelPin tryFindRoutethroughSourcePin(RouteTree route, List<CellPin> sinks ) {
		
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
				sinks.add(belPinToCellPinMap.get(bp));
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
		buffer.updateProperty(new Property("INIT", PropertyType.EDIF, ROUTETHROUGH_INIT_STRING));
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
		
		cellsToAdd.add(buffer);
	}
	
	/**
	 * Adds all newly created nets to the CellDesign
	 */
	private void addRoutethroughNetsToDesign() {
		netsToAdd.forEach(net -> design.addNet(net));
	}
}
