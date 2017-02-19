package design.tcpExport;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

import edu.byu.ece.rapidSmith.RSEnvironment;
import edu.byu.ece.rapidSmith.design.NetType;
import edu.byu.ece.rapidSmith.design.subsite.Cell;
import edu.byu.ece.rapidSmith.design.subsite.CellDesign;
import edu.byu.ece.rapidSmith.design.subsite.CellLibrary;
import edu.byu.ece.rapidSmith.design.subsite.CellNet;
import edu.byu.ece.rapidSmith.design.subsite.RouteTree;
import edu.byu.ece.rapidSmith.device.BelPin;
import edu.byu.ece.rapidSmith.device.Connection;
import edu.byu.ece.rapidSmith.device.Device;
import edu.byu.ece.rapidSmith.device.Site;
import edu.byu.ece.rapidSmith.device.Wire;
import edu.byu.ece.rapidSmith.interfaces.vivado.LutRoutethroughInserter;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Tests the {@link LutRoutethroughInserter} to verify that LUT routethroughs
 * are correctly created on design export. 
 */
@RunWith(JUnitPlatform.class)
public class RoutethroughInserterTest {
	
	// Objects needed for the test 
	private static Device device;
	private static CellLibrary libCells;
	
	/**
	 * Initializes the test by loading a {@link Device} and 
	 * {@link CellLibrary} into memory.
	 */
	@BeforeAll
	public static void initializeTest() {
		try {
			device = RSEnvironment.defaultEnv().getDevice("xc7a100tcsg324");
			libCells = new CellLibrary(RSEnvironment.defaultEnv()
													.getPartFolderPath("xc7a100tcsg324")
													.resolve("cellLibrary.xml"));
		} catch (IOException e) {
			fail("Cannot find cell library XML in test directory. Setup is incorrect.");
		}
	}
	
	/**
	 * Creates a simple netlist, and wires ones of the nets using a routethrough {@link Connection}
	 * in the device. The {@link LutRoutethroughInserter} is run to verify that the routethrough
	 * {@link Connection} is replaced with a passthrough LUT1.
	 */
	@Test
	@DisplayName("LUT Routethrough Insert Test")
	public void routeThroughInsertTest() throws IOException {
		// Create the test design
		CellDesign testDesign = createTestDesign();
		assertNull(testDesign.getCell("rapidSmithRoutethrough0"), 
				"Routethrough should not be added to design until after inserter is run");
		assertNull(testDesign.getNet("rapidSmithRoutethroughNet0"), 
				"Routethrough should not be added to design until after inserter is run");
		
		// Run the inserter 
		LutRoutethroughInserter inserter = new LutRoutethroughInserter(testDesign, libCells);
		inserter.execute();
				
		// Check that a RT LUT is added to the design and initialized correctly
		Cell rtCell = testDesign.getCell("rapidSmithRoutethrough0");
		assertNotNull(rtCell, "Routethrough cell not added to design.");
		assertEquals(libCells.get("LUT1"), rtCell.getLibCell(), "Routethrough cell is not the correct type.");
		assertEquals("2'h2", rtCell.getProperties().get("INIT").getStringValue(), "Routethrough INIT equation is incorrect.");
		
		// Check that a RT net is added to the design and connects the output of the RT LUT to the sink cell pin
		CellNet rtNet = testDesign.getNet("rapidSmithRoutethroughNet0");
		assertNotNull(rtNet, "Routethrough net not added to design.");
		assertEquals(2, rtNet.getPins().size(), "Routethrough net connects to the incorrect number of pins");
		assertTrue(rtNet.isConnectedToPin(rtCell.getPin("O")), "Routethrough net should connect to the output pin of the routhrough LUT");
		assertTrue(rtNet.isConnectedToPin(testDesign.getCell("carry").getPin("DI[0]")), 
				"RoutethroughInserterTest net should connect to carry4/DI[0]");
			
		// Test that net that used the routethrough connection was disconnected 
		// from the sink cell pin and reconnected to the RT LUT input.
		CellNet in1 = testDesign.getNet("in1");
		assertEquals(2, in1.getPins().size(), "Net \"in1\" should still connect to two pins after the inserter is run");
		assertTrue(in1.isConnectedToPin(rtCell.getPin("I0")), "Net \"in1\" should be connected to pin I0 of the routethrough LUT");
		assertFalse(in1.isConnectedToPin(testDesign.getCell("carry").getPin("DI[0]")), 
				"Net \"in1\" should no longer be connected to pin DI[0] of the routethrough LUT");
	}
	
	/*
	 * Creates a small design that can be used to test the RoutethroughInserter 
	 */
	private CellDesign createTestDesign() {
		CellDesign design = new CellDesign("TestDesign", device.getPartName());

		// create partial netlist
		Cell lut1 = design.addCell(new Cell("lut1", libCells.get("LUT1")));
		Cell carry = design.addCell(new Cell("carry", libCells.get("CARRY4"))); 
		Cell iport1 = design.addCell(new Cell("iport1", libCells.get("IPORT")));
		Cell iport2 = design.addCell(new Cell("iport2", libCells.get("IPORT")));
		
		CellNet in1 = design.addNet(new CellNet("in1", NetType.WIRE));
		in1.connectToPins(Arrays.asList(iport1.getPin("PAD"), carry.getPin("DI[0]")));
		
		CellNet in2 = design.addNet(new CellNet("in2", NetType.WIRE));
		in2.connectToPins(Arrays.asList(iport2.getPin("PAD"), lut1.getPin("I0")));
		
		CellNet lutout = design.addNet(new CellNet("lutout", NetType.WIRE));
		lutout.connectToPins(Arrays.asList(lut1.getPin("O"), carry.getPin("S[0]")));
		
		// place LUT and carry cells on the same site
		Site site = device.getSite("SLICE_X4Y82");
		
		design.placeCell(lut1, site.getBel("A6LUT"));
		lut1.getPin("I0").mapToBelPin(site.getBel("A6LUT").getBelPin("A3"));
		
		design.placeCell(carry, site.getBel("CARRY4"));
		carry.getPin("DI[0]").mapToBelPin(site.getBel("CARRY4").getBelPin("DI0"));
		carry.getPin("S[0]").mapToBelPin(site.getBel("CARRY4").getBelPin("S0"));
		
		// create a RouteTree object that uses a routethrough
		RouteTree route = routeInternalNet(site.getSinkPin("A3").getInternalWire(), site.getBel("CARRY4").getBelPin("DI0"));
		in1.addSinkRouteTree(site.getSinkPin("A3"), route);
		
		return design;
	}
	
	/*
	 * Creates a RouteTree data structure that should use a routethrough connection
	 * (this is easier than hand creating the RouteTree).
	 */
	private RouteTree routeInternalNet(Wire startWire, BelPin sink) {
		
		RouteTree start = new RouteTree(startWire);
		Queue<RouteTree> rtQueue = new LinkedList<RouteTree>();
		
		rtQueue.add(start);
		
		while (!rtQueue.isEmpty()) {
			RouteTree tree = rtQueue.poll();
			
			BelPin terminal = tree.getConnectingBelPin();
			if (terminal != null && sink.equals(terminal)) {
				start.prune(tree);
				break;
			}
			
			// add all of the connections to the queue
			for (Connection conn : tree.getWire().getWireConnections()) {
				rtQueue.add(tree.addConnection(conn));
			}
		}
		
		return start;
	}
}
