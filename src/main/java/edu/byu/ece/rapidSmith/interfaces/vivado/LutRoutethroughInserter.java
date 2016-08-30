package edu.byu.ece.rapidSmith.interfaces.vivado;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

import edu.byu.ece.rapidSmith.design.NetType;
import edu.byu.ece.rapidSmith.design.subsite.Cell;
import edu.byu.ece.rapidSmith.design.subsite.CellDesign;
import edu.byu.ece.rapidSmith.design.subsite.CellLibrary;
import edu.byu.ece.rapidSmith.design.subsite.CellNet;
import edu.byu.ece.rapidSmith.design.subsite.CellPin;
import edu.byu.ece.rapidSmith.design.subsite.Connection;
import edu.byu.ece.rapidSmith.design.subsite.Property;
import edu.byu.ece.rapidSmith.design.subsite.PropertyType;
import edu.byu.ece.rapidSmith.design.subsite.RouteTree;
import edu.byu.ece.rapidSmith.device.Bel;
import edu.byu.ece.rapidSmith.device.BelPin;
import edu.byu.ece.rapidSmith.device.Device;
import edu.byu.ece.rapidSmith.device.Site;
import edu.byu.ece.rapidSmith.device.SitePin;
import edu.byu.ece.rapidSmith.device.SiteType;
import edu.byu.ece.rapidSmith.device.WireEnumerator;

/**
 * This class is used to insert LUT buffers for LUT BELs that are acting <br>
 * as routethroughs. Vivado, during routing, can configure a LUT to be a <br> 
 * routethrough without placing a cell on it, but that is an internal operation <br>
 * this needs to be done to import routes correctly back into Vivado. 
 *  
 * TODO: This is focused on the series 7 slice architecture for now
 * @author Thomas Townsend
 *
 */
public class LutRoutethroughInserter {
	
	private static HashSet<BelPin> usedRouteThroughBelPins = new HashSet<BelPin>();
	private static int nextUniqueID = 0; 
	
	private CellDesign design;
	private WireEnumerator wireEnumerator;
	private CellLibrary libCells;
	
	private Collection<RoutethroughConfiguration> configsSliceL;
	private Collection<RoutethroughConfiguration> configsSliceM;
	
	private HashSet<SiteType> qualifiedSiteTypes;
	
	private static boolean isBelPinUsed(BelPin bp) {
		return usedRouteThroughBelPins.contains(bp);
	}
	
	private static void addUsedBelPin(BelPin bp) {
		usedRouteThroughBelPins.add(bp);
		nextUniqueID++;
	}
	
	/**
	 * 
	 * @param design
	 * @param device
	 * @param libCells
	 */
	public LutRoutethroughInserter(CellDesign design, Device device, CellLibrary libCells) {
		this.design = design;
		this.wireEnumerator = device.getWireEnumerator();
		this.libCells = libCells;
		this.configsSliceL = null;
		this.configsSliceM = null;
		
		// add to the qualifitedSiteTypes as needed
		this.qualifiedSiteTypes = new HashSet<SiteType>( 
			Arrays.asList(
				SiteType.SLICEL, 
				SiteType.SLICEM
			)
		);
	}
	
	/**
	 * 
	 */
	public void insertLutRoutethroughs() {
				
		for (Site site : design.getUsedSites()) {
			
			if (!isSiteQualified(site) ) {
				continue;
			}
			
			for (RoutethroughConfiguration config : getRoutethroughConfigurations(site)) {
				
				config.tryInsertLutRoutethroughBuffer(design, site, wireEnumerator, libCells);
			}
		}
	}
		
	public boolean isSiteQualified(Site site) {
		
		return qualifiedSiteTypes.contains(site.getType());
	}
	
	private Collection<RoutethroughConfiguration> getRoutethroughConfigurations(Site site) {
		return site.getType() == SiteType.SLICEL ? 
					getSliceLRoutethroughConfigurations() :
					getSliceMRoutethroughConfigurations();
	}
	
	// TODO: add SLICEM connections as well
	private Collection<RoutethroughConfiguration> getSliceLRoutethroughConfigurations() {
		
		// lazy initialization
		if (configsSliceL != null) {
			return configsSliceL;
		}
		
		// create a new configuration for SLICEL
		configsSliceL = new ArrayList<RoutethroughConfiguration>();
		
		// Flip Flop routethrough configs
		configsSliceL.add(new RoutethroughConfiguration("AFF", "D", "intrasite:SLICEL/AFFMUX.O6", "A6LUT"));
		configsSliceL.add(new RoutethroughConfiguration("AFF", "D", "intrasite:SLICEL/AFFMUX.O5", "A5LUT"));
		configsSliceL.add(new RoutethroughConfiguration("A5FF", "D", "intrasite:SLICEL/A5FFMUX.IN_A", "A5LUT"));
		configsSliceL.add(new RoutethroughConfiguration("BFF", "D", "intrasite:SLICEL/BFFMUX.O6", "B6LUT"));
		configsSliceL.add(new RoutethroughConfiguration("BFF", "D", "intrasite:SLICEL/BFFMUX.O5", "B5LUT"));
		configsSliceL.add(new RoutethroughConfiguration("B5FF", "D", "intrasite:SLICEL/B5FFMUX.IN_A", "B5LUT"));
		configsSliceL.add(new RoutethroughConfiguration("CFF", "D", "intrasite:SLICEL/CFFMUX.O6", "C6LUT"));
		configsSliceL.add(new RoutethroughConfiguration("CFF", "D", "intrasite:SLICEL/CFFMUX.O5", "C5LUT"));
		configsSliceL.add(new RoutethroughConfiguration("C5FF", "D", "intrasite:SLICEL/C5FFMUX.IN_A", "C5LUT"));
		configsSliceL.add(new RoutethroughConfiguration("DFF", "D", "intrasite:SLICEL/DFFMUX.O6", "D6LUT"));
		configsSliceL.add(new RoutethroughConfiguration("DFF", "D", "intrasite:SLICEL/DFFMUX.O5", "D5LUT"));
		configsSliceL.add(new RoutethroughConfiguration("D5FF", "D", "intrasite:SLICEL/D5FFMUX.IN_A", "D5LUT"));
		
		// Carry routethrough configs
		configsSliceL.add(new RoutethroughConfiguration("CARRY4", "S[0]", null, "A6LUT"));
		configsSliceL.add(new RoutethroughConfiguration("CARRY4", "S[1]", null, "B6LUT"));
		configsSliceL.add(new RoutethroughConfiguration("CARRY4", "S[2]", null, "C6LUT"));
		configsSliceL.add(new RoutethroughConfiguration("CARRY4", "S[3]", null, "D6LUT"));
		configsSliceL.add(new RoutethroughConfiguration("CARRY4", "DI[0]", "intrasite:SLICEL/ACY0.O5", "A5LUT"));
		configsSliceL.add(new RoutethroughConfiguration("CARRY4", "DI[1]", "intrasite:SLICEL/BCY0.O5", "B5LUT"));
		configsSliceL.add(new RoutethroughConfiguration("CARRY4", "DI[2]", "intrasite:SLICEL/CCY0.O5", "C5LUT"));
		configsSliceL.add(new RoutethroughConfiguration("CARRY4", "DI[3]", "intrasite:SLICEL/DCY0.O5", "D5LUT"));
		
		return configsSliceL;
	}
	
	private Collection<RoutethroughConfiguration> getSliceMRoutethroughConfigurations() {
		
		// lazy initialization
		if (configsSliceL != null) {
			return configsSliceM;
		}
		
		// create a new configuration for SLICEL
		configsSliceM = new ArrayList<RoutethroughConfiguration>();
		
		// Flip Flop routethrough configs
		configsSliceM.add(new RoutethroughConfiguration("AFF", "D", "intrasite:SLICEM/AFFMUX.O6", "A6LUT"));
		configsSliceM.add(new RoutethroughConfiguration("AFF", "D", "intrasite:SLICEM/AFFMUX.O5", "A5LUT"));
		configsSliceM.add(new RoutethroughConfiguration("A5FF", "D", "intrasite:SLICEM/A5FFMUX.IN_A", "A5LUT"));
		configsSliceM.add(new RoutethroughConfiguration("BFF", "D", "intrasite:SLICEM/BFFMUX.O6", "B6LUT"));
		configsSliceM.add(new RoutethroughConfiguration("BFF", "D", "intrasite:SLICEM/BFFMUX.O5", "B5LUT"));
		configsSliceM.add(new RoutethroughConfiguration("B5FF", "D", "intrasite:SLICEM/B5FFMUX.IN_A", "B5LUT"));
		configsSliceM.add(new RoutethroughConfiguration("CFF", "D", "intrasite:SLICEM/CFFMUX.O6", "C6LUT"));
		configsSliceM.add(new RoutethroughConfiguration("CFF", "D", "intrasite:SLICEM/CFFMUX.O5", "C5LUT"));
		configsSliceM.add(new RoutethroughConfiguration("C5FF", "D", "intrasite:SLICEM/C5FFMUX.IN_A", "C5LUT"));
		configsSliceM.add(new RoutethroughConfiguration("DFF", "D", "intrasite:SLICEM/DFFMUX.O6", "D6LUT"));
		configsSliceM.add(new RoutethroughConfiguration("DFF", "D", "intrasite:SLICEM/DFFMUX.O5", "D5LUT"));
		configsSliceM.add(new RoutethroughConfiguration("D5FF", "D", "intrasite:SLICEM/D5FFMUX.IN_A", "D5LUT"));
		
		// Carry routethrough configs
		configsSliceM.add(new RoutethroughConfiguration("CARRY4", "S[0]", null, "A6LUT"));
		configsSliceM.add(new RoutethroughConfiguration("CARRY4", "S[1]", null, "B6LUT"));
		configsSliceM.add(new RoutethroughConfiguration("CARRY4", "S[2]", null, "C6LUT"));
		configsSliceM.add(new RoutethroughConfiguration("CARRY4", "S[3]", null, "D6LUT"));
		configsSliceM.add(new RoutethroughConfiguration("CARRY4", "DI[0]", "intrasite:SLICEM/ACY0.O5", "A5LUT"));
		configsSliceM.add(new RoutethroughConfiguration("CARRY4", "DI[1]", "intrasite:SLICEM/BCY0.O5", "B5LUT"));
		configsSliceM.add(new RoutethroughConfiguration("CARRY4", "DI[2]", "intrasite:SLICEM/CCY0.O5", "C5LUT"));
		configsSliceM.add(new RoutethroughConfiguration("CARRY4", "DI[3]", "intrasite:SLICEM/DCY0.O5", "D5LUT"));
		
		return configsSliceM;
	}
		
	// TODO: put this in its own class file so that I can have a static WireEnumerator that I can set once
	private class RoutethroughConfiguration {
		
		private static final String ROUTETHROUGH_INIT_STRING = "2'h2";
		
		// Strings that define the routethrough configuration
		private final String sinkBelName;
		private final String sinkCellPinName;
		private final String connectingSitePipName;
		private final String candidateLutName;
		private final int lutInputCount;
		
		// actual physical elements for a specific configuration... used to create buffer
		private Bel lutBel;
		private BelPin lutBelPin;
		private CellPin sinkPin;
		private CellNet net;
		
		public RoutethroughConfiguration(String sinkBelName, String sinkCellPinName, String connectingSitePipName, String candidateLutName) {
			this.sinkBelName = sinkBelName;
			this.sinkCellPinName = sinkCellPinName;
			this.connectingSitePipName = connectingSitePipName;
			this.candidateLutName = candidateLutName;
			this.lutInputCount = candidateLutName.contains("5") ? 5 : 6 ;
		}
		
		/*
		 * Tests to check if a routethrough buffer needs to be added to the design for Vivado import. <br>
		 * If it does, then a routethrough buffer is inserted.
		 */
		public boolean tryInsertLutRoutethroughBuffer(CellDesign design, Site site, WireEnumerator we, CellLibrary library) {
			
			if (!testRoutethroughConfiguration(design, site, we)) {
				return false;
			}
			
			insertRoutethroughBuffer(design, library);
			return true;
		}
		
		/*
		 * Returns true if a routethrough LUT needs to be added to the design.
		 */
		private boolean testRoutethroughConfiguration(CellDesign design, Site site, WireEnumerator we) {
			
			return isConnectingSitePipUsed(design, site, we) &&
					!isCandidateLutUsed(design, site) &&
					isSinkPinConnectedToNet(design, site) &&
					isNetConnectedToCandidateLut(site);
		}
		
		private boolean isConnectingSitePipUsed(CellDesign design, Site site, WireEnumerator we) {
			
			HashSet<Integer> usedSitePips = design.getUsedSitePipsAtSite(site);
			return connectingSitePipName == null || usedSitePips.contains(we.getWireEnum(connectingSitePipName)); 
		}
		
		private boolean isCandidateLutUsed(CellDesign design, Site site) {
			
			// check if the lut bel is used
			lutBel = site.getBel(candidateLutName);
			return design.getCellAtBel(lutBel) != null; 
		}
		
		private boolean isSinkPinConnectedToNet(CellDesign design, Site site) {
			
			// check if there is cell placed at the bel
			Bel sinkBel = site.getBel(sinkBelName);
			Cell sinkCell = design.getCellAtBel(sinkBel);
			
			if (sinkCell == null) {
				return false;
			}
			
			// check if there is a net connected to the sink pin
			sinkPin = sinkCell.getPin(sinkCellPinName);
			net = sinkPin.getNet(); 
		
			return sinkPin.isConnectedToNet(); 
		}
		
		private boolean isNetConnectedToCandidateLut(Site site) {
								
			// assuming the first RouteTree object in the net is the actual route
			RouteTree route = net.getRouteTrees().iterator().next(); 
			
			Iterator<RouteTree> rtIterator = route.getFirstSource().iterator(); 
			
			// search through the entire route tree
			while ( rtIterator.hasNext() ) {
				
				RouteTree currentRouteTree = rtIterator.next(); 
				
				Collection<Connection> pinConnections = currentRouteTree.getWire().getPinConnections();
				
				if (pinConnections.size() == 0) {
					continue;
				}
				
				SitePin sitePin = pinConnections.iterator().next().getSitePin(); 
				
				if (isSitePinConnectedToCandidateLut(sitePin, site)) {
					return true;
				}
			}
			
			// partially routed net...do not create a routethrough
			return false;
		}
		
		private boolean isSitePinConnectedToCandidateLut(SitePin sitePin, Site site) {
			
			if (sitePin.getSite().equals(site)) { // && sitePin.getDirection() == PinDirection.IN) {
				String pinName = sitePin.getName();
				
				int input = Character.getNumericValue(pinName.charAt(pinName.length() - 1));
				char pinStart = pinName.charAt(0);
				char lutStart = this.candidateLutName.charAt(0);
				this.lutBelPin = this.lutBel.getBelPin("A" + input);
				
				return pinStart == lutStart &&
						input > 0 && input <= lutInputCount &&
						!isBelPinUsed(lutBelPin);
			}
			
			return false;
		}
				
		/*
		 * Inserts a RT buffer to the design
		 * TODO: create a better way to create unique names for the inserted cell and net
		 */
		private void insertRoutethroughBuffer(CellDesign design, CellLibrary libCells) {
			addUsedBelPin(lutBelPin);
			
			//create a new lut1 cell with the appropriate init string
			Cell buffer = new Cell("lutRtBuffer" + LutRoutethroughInserter.nextUniqueID, libCells.get("LUT1") );
			buffer.updateProperty(new Property("INIT", PropertyType.EDIF, ROUTETHROUGH_INIT_STRING));
			design.addCell(buffer);
			
			// break the netlist 
			net.disconnectFromPin(sinkPin);
			net.connectToPin(buffer.getPin("I0"));
			
			// add new net
			CellNet tmpNet = new CellNet("lutRtBufferNet" + LutRoutethroughInserter.nextUniqueID, NetType.WIRE);
			tmpNet.connectToPin(buffer.getPin("O"));
			tmpNet.connectToPin(sinkPin);
			design.addNet(tmpNet);
			
			// place lut cell and map pins correctly
			design.placeCell(buffer, lutBel);
			buffer.getPin("I0").setBelPin(lutBelPin);
			buffer.getPin("O").setBelPin("O" + this.lutInputCount);
		}
		
		private void addUsedBelPin(BelPin bp) {
			LutRoutethroughInserter.addUsedBelPin(bp);
		}
		
		private boolean isBelPinUsed(BelPin bp) {
			return LutRoutethroughInserter.isBelPinUsed(bp);
		}
	}
}
