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

import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import edu.byu.ece.rapidSmith.device.families.FamilyInfo;
import edu.byu.ece.rapidSmith.device.families.FamilyInfos;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import edu.byu.ece.rapidSmith.RSEnvironment;
import edu.byu.ece.rapidSmith.design.subsite.BelRoutethrough;
import edu.byu.ece.rapidSmith.design.subsite.Cell;
import edu.byu.ece.rapidSmith.design.subsite.CellDesign;
import edu.byu.ece.rapidSmith.design.subsite.CellNet;
import edu.byu.ece.rapidSmith.design.subsite.CellPin;
import edu.byu.ece.rapidSmith.design.subsite.RouteTree;
import edu.byu.ece.rapidSmith.device.Bel;
import edu.byu.ece.rapidSmith.device.BelPin;
import edu.byu.ece.rapidSmith.device.Device;
import edu.byu.ece.rapidSmith.device.Site;
import edu.byu.ece.rapidSmith.device.SiteType;
import edu.byu.ece.rapidSmith.device.Wire;
import edu.byu.ece.rapidSmith.interfaces.vivado.TincrCheckpoint;
import edu.byu.ece.rapidSmith.interfaces.vivado.VivadoInterface;
import edu.byu.ece.rapidSmith.util.VivadoConsole;

/**
 * This class is used to test design import by verifying the following for a single TINCR Checkpoint (TCP): <br> 
 * <br>
 * (1) The checkpoint was loaded into RapidSmith without errors, and <br>
 * (2) The representation of the design in RapidSmith matches that of Vivado. <br> 
 * <br>
 * To create a new checkpoint test, extend this class and implement the {@link ImportTest#getCheckpointName()} method.<br>
 * (1) Extend this class and implement the {@link ImportTest#getCheckpointName()} method <br>
 * (2) Generate a TINCR Checkpoint (TCP) of the design in Vivado and add the .tcp to "RAPIDSMITH_PATH/ImportTest/TCP" <br>
 * (3) Generate a Vivado Checkpoint (DCP) of the design, and add the .dcp file to "RAPIDSMITH_PATH/ImportTest/DCP" <br>
 * 
 * @author Thomas Townsend
 */
public abstract class ImportTest {
	/** Set to true if you want to run the tests in debug mode, which prints additional information */
	private static final boolean DEBUG_MODE = true;
	/** Path to the import test directory */
	private static final Path testDirectory = RSEnvironment.defaultEnv().getEnvironmentPath()
																		.resolve("src")
																		.resolve("test")
																		.resolve("resources")
																		.resolve("ImportTests");
	// TODO: play around with the command count further...
	/** Number of commands to run before restarting Vivado*/
	private static final int MAX_COMMAND_COUNT = 4100;
	private static int commandCount = 0;
	
	/** Vivado process link */
	private static VivadoConsole console;
	/** Loaded TincrCheckpoint of the design under test*/
	private static TincrCheckpoint tcp;
	/** Flag used to determine if we need to load the TCP and DCP*/
	private static boolean initialized = false;
	/** Name of the current checkpoint design under test*/
	private static String testName ;
	
	/**
	 * Returns the checkpoint name of the import test to run. To add a new test,
	 * extend {@link ImportTest} and implement this method.
	 */
	public abstract String getCheckpointName();
	
	/**
	 * Initializes an import test.
	 */
	@BeforeAll
	public static void initializeClass() {
		initialized = false;

		// create temporary directory to store Vivado output
		new File(testDirectory.resolve("vivadoTmp").toString()).mkdir();
	}
	
	/**
	 * After all import test for a checkpoint have been run, this class
	 * closes the Vivado process used for the test, and deletes all
	 * temporary files created while the test was running. 
	 * 
	 * @throws IOException
	 */
	@AfterAll
	public static void cleanupClass() {
		
		resetTclDisplayLimit();		
		console.close(true);
		tcp = null;
		console = null;
		deleteTemporaryFiles();
	}
	
	/**
	 * Deletes all Vivado produced files from the temporary
	 * test directory where Vivado was run.
	 */
	private static void deleteTemporaryFiles() {
		try {
			// delete directory with Vivado output
			Files.walkFileTree(testDirectory.resolve("vivadoTmp"), new SimpleFileVisitor<Path>() {
			   @Override
			   public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
			       Files.delete(file);
			       return FileVisitResult.CONTINUE;
			   }
	
			   @Override
			   public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
			       Files.delete(dir);
			       return FileVisitResult.CONTINUE;
			   }
			});
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Initializes the test by loading the TCP into RapidSmith
	 * and loading the DCP into Vivado when the test is first run. 
	 */
	@BeforeEach
	public void initializeTest() {
		
		if (initialized == false) {
			debugPrint("Loading TCP...");
			String checkpointName = getCheckpointName();
			testName = checkpointName;
			tcp = loadTCP(checkpointName + ".tcp");
			
			debugPrint("Loading DCP...");
			console = loadDCP(checkpointName + ".dcp");
			initialized = true;			
			setTclDisplayLimit();
		}
	}
	
	/**
	 * Loads a Tincr Checkpoint into RapidSmith for testing
	 * 
	 * @param tcpCheckpointFile checkpoint file name
	 * @return Newly created {@link TincrCheckpoint} object
	 */
	private static TincrCheckpoint loadTCP(String tcpCheckpointFile) {
		TincrCheckpoint tcp = null;
		try {
			tcp = VivadoInterface.loadTCP(testDirectory.resolve("TCP").resolve(tcpCheckpointFile).toString(), true);			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return tcp;
	}
	
	/**
	 * Creates a new Vivado process, and loads the specified Vivado checkpoint
	 * 
	 * @param dcpCheckpointFile checkpoint name
	 * @return a {@link VivadoConsole} that can be used to communicate with the Vivado process
	 */
	private static VivadoConsole loadDCP(String dcpCheckpointFile) {
		VivadoConsole console = new VivadoConsole(testDirectory.resolve("vivadoTmp").toString());
		console.runCommand("open_checkpoint " +  testDirectory.resolve("DCP")
															.resolve(dcpCheckpointFile)
															.toString().replaceAll("\\\\", "/"));
		return console;
	}
	
	/**
	 * When accessing Vivado's TCL interface the time it takes to return the result
	 * of a sent command continually increases (not sure why this is). Therefore, it 
	 * is beneficial to occasionally restart Vivado for the tests to run faster. This
	 * function closes the current running Vivado process, and creates a new one.
	 */
	private static void restartVivado() {
		debugPrint("Starting new Vivado instance...");
		console.close(false); 
		console = loadDCP(testName + ".dcp");
		setTclDisplayLimit();
	}
	
	/**
	 * 
	 */
	private static void setTclDisplayLimit() {
		String command = "tincr::set_tcl_display_limit 100000";
		console.runCommand(command);
	}
	
	/**
	 * 
	 */
	private static void resetTclDisplayLimit() {
		String command = "tincr::reset_tcl_display_limit";
		console.runCommand(command);
	}
	
	/**
	 * Test to verify that the static nets (VCC and GND)
	 * were imported correctly into RapidSmith
	 */
	@Test
	@DisplayName("VCC and GND Net Test")
	public void staticNetTest() {
		
		CellNet vccNet = tcp.getDesign().getVccNet();
		
		String command = "tincr::report_vcc_routing_info ";
		List<String> result = sendVivadoCommand(command);
		
		assert (result.size() == 4);
		testNetWires(vccNet, result.get(0));
		testStaticNetCellPins(vccNet, result.get(2), Integer.parseInt(result.get(1)));
		testNetBelPins(vccNet, result.get(3));
		
		CellNet gndNet = tcp.getDesign().getGndNet();
		
		command = "tincr::report_gnd_routing_info ";
		result = sendVivadoCommand(command);
	
		assert (result.size() == 4);
		
		testNetWires(gndNet, result.get(0));
		testStaticNetCellPins(gndNet, result.get(2), Integer.parseInt(result.get(1)));
		testNetBelPins(gndNet, result.get(3));
	}
	
	/**
	 * Verifies that each net in the design was imported correctly into RapidSmith.
	 * Specifically, this test checks that: <br>
	 * (1) All imported wires are represented in the Vivado design <br>
	 * (2) The RS net is connected to all of the same cell pins as the Vivado design <br>
	 * (3) The RS net is connected to all of the same bel pins as the Vivado design <br> 
	 */
	@Test
	@DisplayName("Net connection test")
	public void netTest() {
		CellDesign design = tcp.getDesign();
		int netCount = design.getNets().size();
		int count = 1;
		
		for (CellNet net : design.getNets()) {
			debugPrint("Processing net: " + net.getName() + (net.isIntrasite() ? " INTRASITE " : " REGULAR ") + count++ + "/" + netCount);
			
			if (net.isGNDNet() || net.isVCCNet()) {
				continue;
			}
			
			String command = "tincr::report_physical_net_info " + net.getName();
			List<String> result = sendVivadoCommand(command);
			
			assert (result.size() == 3);
			
			testNetWires(net, result.get(0));
			testNetCellPins(net, result.get(1));
			testNetBelPins(net, result.get(2));
		}
	}
	
	/**
	 * Verifies that the specified RapidSmith {@link CellNet} uses the same wires as 
	 * the corresponding net in Vivado.
	 *  
	 * @param net {@link CellNet} object to test
	 * @param wireString A string of all wires used in the corresponding Vivado net in the format: <br>
	 * 						"tilename1/wirename1 tilename2/wirename2 ... "
	 */
	private void testNetWires(CellNet net, String wireString) {
		
		// If the net is an intrasite net, it should have no wires used here
		if (!net.hasIntersiteRouting()) {
			assertTrue(wireString.isEmpty());
			return;
		}
		
		// create a new set of wire names from wireString.
		// TODO: initialize the capacity of the set since we know its final size: result.size()*4 ?
		Set<String> wiresInNet = new HashSet<String>();
		
		for (String wireName : wireString.split(" ")) {
			wiresInNet.add(wireName);
		}
		
		// iterator through the RouteTree data structure and make sure each of the wires are in the set
		Iterator<RouteTree> rtIterator = net.getIntersiteRouteTree().iterator();
		while (rtIterator.hasNext()) {
			RouteTree next = rtIterator.next();
			Wire nextWire = next.getWire();
			
			String fullWireName = nextWire.getTile().getName() + "/" + nextWire.getWireName();
			
			assertTrue(wiresInNet.contains(fullWireName), 
					String.format("Routing import failure: \nWire \"%s\" does not exist in net \"%s\"\n", fullWireName, net.getName()));
		}
	}
	
	/**
	 * Verifies that the specified RapidSmith {@link CellNet} connects to the same
	 * cell pins as the corresponding net in Vivado.
	 * 
	 * @param net {#link cellNet} object
	 * @param cellPinString A string of all cell pins attached to the net in Vivado in the format: <br>
	 * 						"cellnamme1/pinname1 cellname2/pinname2 ... "
	 */
	private void testNetCellPins(CellNet net, String cellPinString) {
		
		CellDesign design = tcp.getDesign();
		String[] tclCellPins = cellPinString.split(" ");
		
		// Test the number of cell pins attached to the net matches Vivado
		
		assertTrue(tclCellPins.length == net.getPins().size() - getPortPinCount(net) - net.getPseudoPinCount(), 
			String.format("Number of CellPins for net %s does not match!\n"
						+ "Expected: %d Actual: %d", net.getName(), net.getPins().size() - getPortPinCount(net) - net.getPseudoPinCount(), tclCellPins.length));
		
		// Test that each TCL pin is included in the RapidSmith net
		for (String tclPin: tclCellPins) {
			
			int last = tclPin.lastIndexOf("/");
			String cellName = tclPin.substring(0, last);
			String cellPinName = tclPin.substring(last + 1);
			
			// TODO: should this be part of the check?
			Cell cell = design.getCell(cellName);
			CellPin pin = cell.getPin(cellPinName);
			
			assertTrue(net.isConnectedToPin(pin), String.format("Net %s should be connected to cell pin %s, but is not!", net.getName(), tclPin));
		}
	}
	
	/**
	 * Returns the number of pins that are connected to port cells in the specified net.
	 * 
	 * @param net {#link CellNet}
	 * @return The number of pins connected to a port in the net
	 */
	private int getPortPinCount(CellNet net) {
		int portPinCount = 0;
		
		for (CellPin pin : net.getPins()) {
			if (pin.getCell().isPort()) {
				portPinCount++;
			}
		}
		return portPinCount; 
	}
	
	/**
	 * For VCC and GND nets in RapidSmith, verifies that all VCC and GND nets in Vivado connect to the same
	 * cell pins. See {@link ImportTest#testNetCellPins(CellNet, String)} for more details. 
	 */
	private void testStaticNetCellPins(CellNet net, String cellPinString, int cellCount) {

		CellDesign design = tcp.getDesign();
		String[] tclCellPins = cellPinString.split(" ");
		
		// Test the number of cell pins attached to the net matches Vivado
		int expectedCellPinCount = net.getPins().size() - net.getPseudoPinCount();
		int actualCellPinCount = tclCellPins.length - cellCount + 1;
		assertTrue(actualCellPinCount == expectedCellPinCount, 
			String.format("Number of CellPins for net %s does not match!\n"
						+ "Expected: %d Actual: %d", net.getName(), expectedCellPinCount, actualCellPinCount));
		
		// Test that each TCL pin is included in the RapidSmith static net
		for (String tclPin: tclCellPins) {
		
			// ignore cell pins connected to VCC or GND bels in Vivado since they don't exist in RapidSmith
			if (tclPin.endsWith("/P") || tclPin.endsWith("/G")) {
				continue;
			}
			
			int last = tclPin.lastIndexOf("/");
			String cellName = tclPin.substring(0, last);
			String cellPinName = tclPin.substring(last + 1);
			
			Cell cell = design.getCell(cellName);
			CellPin pin = cell.getPin(cellPinName);
			
			assertTrue(net.isConnectedToPin(pin), String.format("Net %s should be connected to cell pin %s, but is not!", net.getName(), tclPin));
		}
	}
	
	/**
	 * Verifies that the specified RapidSmith {@link CellNet} connects to the same
	 * bel pins as the corresponding net in Vivado.
	 * 
	 * @param net {@link CellNet}
	 * @param belPinString A string of all bel pins attached to the net in Vivado in the format: <br>
	 * 						"sitename1/belname1/pinname1 sitename2/belname2/pinname2 ... " 
	 */
	private void testNetBelPins(CellNet net, String belPinString) {
		
		Device device = tcp.getDevice();
		Map<BelPin, CellPin> pinMap = tcp.getBelPinToCellPinMap();
		Set<Bel> routethroughSet = tcp.getBelRoutethroughs();
		
		Set<BelPin> connectedBelPins = net.getBelPins();
				
		// Check that each bel pin in the Vivado design, is represented in RapidSmith
		int tclIgnoreCount = 0;
		int rsPinCount = 0;
		String[] tclBelPinNames = belPinString.split("\\s+");
		for (String tclBelPinName : tclBelPinNames) {
			String[] belPinToks = tclBelPinName.split("/");
			Site site = device.getPrimitiveSite(belPinToks[0]);
			Bel bel = site.getBel(belPinToks[1]);
			BelPin belPin = bel.getBelPin(belPinToks[2]);
			
			if (!connectedBelPins.contains(belPin)) {
				
				if (routethroughSet.contains(bel)) {
					tclIgnoreCount++;
				}
				else {
					CellPin cellPin = pinMap.get(belPin);
					assertTrue(cellPin != null && cellPin.isPseudoPin(), 
							String.format("\nCell net %s should connect to bel pin %s, but it doesn't!", net.getName(), tclBelPinName));
				}
			} 
			else {
				rsPinCount++;
			}
		}
		
		// Test that the net connects to the same number of bel pins as the Vivado design
		assertTrue( rsPinCount == tclBelPinNames.length - tclIgnoreCount, 
				String.format("\nCell net %s does not connect to the same number of bel pins as the vivado design!\n"
						+ "Expected %d, Actual: %d", net.getName(), rsPinCount, tclBelPinNames.length - tclIgnoreCount));
	}
	
	/**
	 * Verifies that a cell in RapidSmith was imported correctly by comparing the following
	 * against the corresponding cell in the Vivado design. <br> 
	 * (1) Where the cell is placed (site and bel) <br>
	 * (2) The cell's pin mappings
	 */
	@Test
	@DisplayName("Cell Placement Test")
	public void testCells() {
		CellDesign design = tcp.getDesign();
		int max = design.getCells().size();
		int count = 1;
		
		for (Cell cell : design.getCells()) {
			debugPrint("Cell " + cell.getName() + " " + count++ + "/" + max);
			
			// skip port, vcc, and gnd since they are not placed in Vivado
			if (cell.isPort() || cell.isVccSource() || cell.isGndSource()) {
				continue;
			}
						
			String command = "tincr::report_cell_placement_info " + cell.getName();
			List<String> result = sendVivadoCommand(command);
			
			assert(result.size() == cell.getPins().size() - cell.getPseudoPinCount() + 1);
			
			testCellPlacement(cell, result.get(0));
			testCellPinMapping(cell, result);
		}
	}
		
	/**
	 * Verifies that specified RapidSmith {@link Cell} is placed in the same location
	 * as the corresponding Vivado cell.
	 * 
	 * @param cell {@link Cell}
	 * @param actualBelPlacement Vivado bel placement in the form "sitename/belname"
	 */
	private void testCellPlacement(Cell cell, String actualBelPlacement) {
		
		if (cell.isPlaced()) {
			assertTrue(cell.getAnchor().getFullName().equals(actualBelPlacement) , 
					String.format("Cell Placement for cell %s is incorrect!\n"
					+ "Expected: %s Actual: %s\n", cell.getName(), cell.getAnchor().getFullName(), actualBelPlacement));
		} 
		else {
			assertTrue(actualBelPlacement.isEmpty(), 
					"Cell " + cell.getName() + " is unplaced in RapidSmith, but has a placement in Vivado!");
		}
	}
	
	/**
	 * Verifies that the specified {@link Cell} in RapidSmith has the same pin mappings
	 * as the corresponding cell in Vivado. 
	 * 
	 * @param cell {@link Cell}
	 * @param pinMapList List of pin mappings for each cell pin in the form. Each element in the list has the form: <br>
	 * 				"cellpinname belpinmap1 belpinmap2 ... "
	 */
	private void testCellPinMapping(Cell cell, List<String> pinMapList) {
		
		Bel parentBel = cell.getAnchor();
		
		for (int i = 1; i < pinMapList.size(); i++) {
			String[] pinMapToks = pinMapList.get(i).split(" ");
							
			CellPin cellPin = cell.getPin(pinMapToks[0]);
			
			int actualMapCount = 0;
			for (int j = 1; j < pinMapToks.length; j++) {
				// format of bel pin names in tcl are "site/bel/pin"
				String[] belPinToks = pinMapToks[j].split("/");
				
				// Skip bel pins from TCL that aren't actually attached to the parent bel
				// This can happen with Routethough pins... TODO: document this better.
				if (!belPinToks[1].equals(parentBel.getName())) {
					continue;
				}
				
				// Check that the cell pin is mapped to all expected BelPins 
				assertTrue(cellPin.isMappedTo(parentBel.getBelPin(belPinToks[2])),
						String.format("Cell pin %s is missing bel pin mapping to %s\n!", cellPin.getFullName(), pinMapToks[j]));
				actualMapCount++;
			}
			
			// Check to make sure the cell pin is mapped to the correct number of bel pins
			assertTrue(actualMapCount == cellPin.getMappedBelPinCount(), 
					String.format("Cell pin %s maps to an incorrect number of bel pins!\n"
					+ "Expected: %d Actual: %d", cellPin.getFullName(), cellPin.getMappedBelPinCount(), pinMapToks.length - 1));
		}
	}
	
	/**
	 * Verifies that each routethrough Bel in RapidSmith is also a routethrough Bel in Vivado.
	 */
	@Test
	@DisplayName("Routethrough test")
	public void routethroughTest() {
		
		for (BelRoutethrough rt : tcp.getRoutethroughObjects()) {
			String command = String.format("tincr::test_routethrough %s %s %s", 
											rt.getBel().getFullName(), rt.getInputPin().getName(), rt.getOutputPin().getName());
			
			List<String> result = sendVivadoCommand(command);
			
			assert(result.size() == 1);
			
			if (!result.get(0).equals("1")) {
				String failMessage = "";
				if (result.size() == 1) {
					failMessage = "Bel " + rt.getBel().getFullName() + " is not a routethrough!";
				}
				else {
					failMessage = String.format("Routethrough error:\n"
							+ "Expected Input Pin: %s Actual Input Pin: %s\n"
							+ "Expected Output Pin: %s Actual Output Pin: %s \n", 
							result.get(1), result.get(2), result.get(3), result.get(4));
				}
				
				fail(failMessage);
			}
		}
	}
	
	/**
	 * Verifies that each static source Bel in RapidSmith is also used as a static source in Vivado
	 */
	@Test
	@DisplayName("Static Bel Test")
	public void staticSourceTest() {
		
		for (Bel bel : tcp.getStaticSourceBels()) {
			String command = String.format("tincr::test_static_sources %s", bel.getFullName());
			List<String> result = sendVivadoCommand(command);
			
			if (!result.get(0).equals("1")) {
				fail("Bel " + bel.getFullName() + " is not a static source!");
			}
		}
	}
	
	/**
	 * Verifies that the cell properties in RapidSmith match the cell properties in Vivado
	 */
	@Test
	@DisplayName("Cell Property Test")
	public void cellPropertyTest() {
		
		//TODO: Mark this function as don't count instructions for (i.e. don't create an new instance of Vivado for)?
		int max = tcp.getDesign().getCells().size();
		int count = 1;
		
		for (Cell cell : tcp.getDesign().getCells()) {

			debugPrint("Cell " + cell.getName() + " " + count++ + "/" + max);
			
			// TODO: do we need to handle this?
			if (cell.isPort() || cell.isVccSource() || cell.isGndSource()) {
				continue;
			}
			
			// For cells with no properties, check that all properties in Vivado are also the default
			if (cell.getProperties().size() == 0) {
				String command = "tincr::test_default_cell " + cell.getName();
				List<String> result = sendVivadoCommand(command);
				
				if (result.get(0).equals("0")) {
					fail(String.format("\nCell property %s is a default value in RapidSmith, but not Vivado!\n"
							+ "Expected Value(RapidSmith): %s Actual Value (Vivado): %s", result.get(1), result.get(2), result.get(3)));
				}
				continue;
			}
			
			// For cells with non-default properties, make sure the value in RapidSmith, matches the value in Vivado
			// TODO: filter out non-EDIF properties?
			String propertyList = "";
			for (Object propertyName : cell.getPropertyNames()) {
				propertyList += propertyName + " ";
			}
			
			String command = String.format("tincr::report_property_values %s {%s}", cell.getName(), propertyList);
			List<String> result = sendVivadoCommand(command);
			
			assert(result.size() == cell.getProperties().size());
			
			for (String tclProperty : result) {
				String[] tclPropertyToks = tclProperty.split("!");
				String rsPropertyValue = cell.getProperty(tclPropertyToks[0]).getValue().toString();
				String tclPropertyValue = tclPropertyToks[1];
				
				boolean propertiesMatch = true;
				switch(rsPropertyValue) {
					case "true":
					case "1":
						propertiesMatch = tclPropertyValue.equals("1") || tclPropertyValue.equals("true");
						break;
					case "false":
					case "0":
						propertiesMatch = tclPropertyValue.equals("0") || tclPropertyValue.equals("false");
						break;
					default:
						propertiesMatch = rsPropertyValue.equals(tclPropertyValue);
				}
				
				assertTrue(propertiesMatch, String.format("\nIncorrect property value on cell %s for property key %s!\n"
						+ "Expected (Vivado): %s Actual (RapidSmith): %s", cell.getName(), tclPropertyToks[0], tclPropertyValue, rsPropertyValue));
			}
		}
	}
	
	/**
	 * Verifies that the used site pips for each site in RapidSmith match the used site pips
	 * in Vivado.
	 */
	@Test 
	@DisplayName("Site Pip Test")
	public void sitePipTest() {
		
		CellDesign design = tcp.getDesign();
		Device device = tcp.getDevice();

		// Test to make sure the number of used sites is identical
		String command = "tincr::report_used_site_count";
		List<String> results = sendVivadoCommand(command);
		
		assert (results.size() == 1);
		
		int tclUsedSiteCount = Integer.parseInt(results.get(0));
		
		int max = design.getUsedSites().size();
		int count = 1;
		
		int rsUsedSiteCount = 0;
		String expectedUsedSiteResult = "0"; 
		for (Site site : design.getUsedSites()) {
			
			debugPrint(site.getName() + " " + count++ + "/" + max);
			
			// skip sites 
			if (!isSinglePortSite(site, design)) {
				rsUsedSiteCount++;
				expectedUsedSiteResult = "1";
			} else {
				expectedUsedSiteResult = "0";
			}
			
			command = "tincr::report_used_site_pips " + site.getName();
			results = sendVivadoCommand(command);
			
			assert (results.size() == 2);
			
			assertTrue(results.get(0).equals(expectedUsedSiteResult), 
					String.format("\nUsed Site mismatch for site %s!\n"
							+ "Expected (RapidSmith): %s Actual (Vivado): %s ", site.getName(), expectedUsedSiteResult, results.get(0)));
			
			// 
			Set<String> usedSitePips = design.getUsedSitePipsAtSite(site).stream()
					.map(intEnum -> device.getWireEnumerator().getWireName(intEnum).split("/")[1])
					.collect(Collectors.toSet());
				
			if (usedSitePips.isEmpty()) {
				assertTrue(results.get(1).isEmpty());
				continue ;
			}
			
			String[] sitePipToks = results.get(1).split("\\s+");
			
			assertTrue(usedSitePips.size() == sitePipToks.length * 2);
			
			for(String tclSitePip : sitePipToks) {
				String modifiedName = tclSitePip.split("/")[1].replaceAll(":", ".");
				
				assertTrue(usedSitePips.contains(modifiedName), 
						String.format("\nSite Pip: %s not imported correctly!\n"
								+ "Modified Name: %s", tclSitePip, modifiedName));
			}
		}
		
		// Test that the number of used sites in Vivado and RapidSmith match
		assertTrue(tclUsedSiteCount == rsUsedSiteCount, 
				String.format("\nThe number of used sites in RapidSmith and Vivado mismatch!\n"
						+ "Expected: %s Actual %s", rsUsedSiteCount, tclUsedSiteCount));
	}
		
	/**
	 * Returns <code>true</code> if the site has a type of IOB*, and only a single
	 * port is placed on the site. <code>false</code> otherwise. This is needed
	 * due to a bug in Vivado that marks these types of sites as unused even though they are. 
	 * 
	 * @param site {@code Site}
	 * @param design {@code CellDesign}
	 */
	private boolean isSinglePortSite(Site site, CellDesign design) {
		
		if (!isIobPad(design.getDevice(), site)) {
			return false;
		}
		
		Collection<Cell> cellsAtSite = design.getCellsAtSite(site);
		return cellsAtSite.size() == 1 && cellsAtSite.iterator().next().isPort();
	}
	
	/**
	 * Returns <code>true</code> if the specified site is an IOB type.
	 * @param device
	 * @param site {@code Site}
	 */
	private boolean isIobPad(Device device, Site site) {
		SiteType siteType = site.getType();

		FamilyInfo familyInfo = FamilyInfos.get(device.getFamily());
		return familyInfo.ioSites().contains(siteType);
	}

	/**
	 * Sends a command to the Vivado console. If the number of commands exceeds
	 * the maximum command count, then a new instance of Vivado is created before
	 * sending the command. 
	 * 
	 * @param command Vivado command
	 * @return the results of the command returned from Vivado
	 */
	private static List<String> sendVivadoCommand(String command) {
		
		// Every so often, restart the Vivado console to speed up I/O communication
		if (commandCount++ == MAX_COMMAND_COUNT) {
			restartVivado();
			commandCount = 0;
		}
		
		return console.runCommand(command);
	}
	
	/**
	 * function used to only print messages to the console if DEBUG_MODE for
	 * test class is set to <code>true</code>
	 * @param msg
	 */
	private static void debugPrint(String msg) {
		if (DEBUG_MODE) {
			System.out.println(msg);
		}
	}
}
