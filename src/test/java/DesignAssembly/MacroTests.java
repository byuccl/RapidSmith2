package DesignAssembly;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;
import static org.junit.jupiter.api.Assertions.*;

import edu.byu.ece.rapidSmith.RSEnvironment;
import edu.byu.ece.rapidSmith.design.NetType;
import edu.byu.ece.rapidSmith.design.subsite.Cell;
import edu.byu.ece.rapidSmith.design.subsite.CellDesign;
import edu.byu.ece.rapidSmith.design.subsite.CellLibrary;
import edu.byu.ece.rapidSmith.design.subsite.CellNet;
import edu.byu.ece.rapidSmith.design.subsite.CellPin;
import edu.byu.ece.rapidSmith.design.subsite.CellPinType;
import edu.byu.ece.rapidSmith.util.Exceptions;

/**
 *	Tests macro cell functionality in RapidSmith
 */
@RunWith(JUnitPlatform.class)
public class MacroTests {
	
	private static CellLibrary libCells;
	private static Cell testMacro;
	private static final Path resourceDir = RSEnvironment.defaultEnv()
														.getEnvironmentPath()
														.resolve("src")
														.resolve("test")
														.resolve("resources");
	/**
	 * Initializes the macro tests by loading a CellLibrary, and creating a 
	 * new macro cell of type "RAM128X1D." This is the cell in the macro
	 * test below. 
	 */
	@BeforeAll
	public static void initializeTest() {
		try {
			libCells = new CellLibrary(resourceDir.resolve("cellLibraryTest.xml"));
			testMacro = new Cell("ram", libCells.get("RAM128X1D"));
		} catch (IOException e) {
			fail("Cannot find cell library XML in test directory. Setup is incorrect.");
		}
	}
	
	/**
	 * Augments an existing cell library, with macros from an external source. This test
	 * verifies that no errors occur during this process and the macro cell is added correctly.
	 */
	@Test
	@DisplayName("Adding Macros to CellLibrary Test")
	public void externalAdditionTest() throws IOException {
		
		int cellCountBefore = libCells.size();
		
		libCells.loadMacroXML(resourceDir.resolve("macrosTest.xml"));
		
		int cellCountAfter = libCells.size();
		
		assertTrue(cellCountAfter == cellCountBefore + 1, "Macro cells in \"macrosTest.xml\" not correctly loaded into the CellLibrary!");
		assertTrue(libCells.get("IOBUF") != null, "Macro cell \"IOBUF\" is expected to be in the CellLibrary, but was not found.");
	}
	
	/**
	 * Tests that the pins on a macro cell of type "RAM128X1D" are
	 * created correctly when the macro cell is instanced.
	 */
	@Test
	@DisplayName("Macro Pin Test")
	public void macroPinTest() {
		Set<String> pinsGolden = new HashSet<String>();
		pinsGolden.add("DPO");
		pinsGolden.add("SPO");
		pinsGolden.add("A[6]");
		pinsGolden.add("A[5]");
		pinsGolden.add("A[4]");
		pinsGolden.add("A[3]");
		pinsGolden.add("A[2]");
		pinsGolden.add("A[1]");
		pinsGolden.add("A[0]");
		pinsGolden.add("D");
		pinsGolden.add("DPRA[6]");
		pinsGolden.add("DPRA[5]");
		pinsGolden.add("DPRA[4]");
		pinsGolden.add("DPRA[3]");
		pinsGolden.add("DPRA[2]");
		pinsGolden.add("DPRA[1]");
		pinsGolden.add("DPRA[0]");
		pinsGolden.add("WCLK");
		pinsGolden.add("WE");
		
		int goldenCount = pinsGolden.size();
		int actualCount = testMacro.getPins().size();
		assertTrue(goldenCount == actualCount, 
				String.format("Mismatching pin count for macro cell RAM128X1D.\n"
						+ "Expected: %d, Actual: %d", goldenCount, actualCount));
		
		for (CellPin pin : testMacro.getPins()) {
			assertTrue(pinsGolden.contains(pin.getName()), 
					String.format("Unexpected pin \"%s\" attached to macro cell. This pin should not be attached or is named incorrectly", pin.getName()));
			
			assertTrue(pin.getType().equals(CellPinType.MACRO), 
					String.format("Incorrect pin type for pin \"%s\".\n"
							+ "Expected: %s, Actual: %s", pin.getName(), "MACRO", pin.getType()));
		}
	}
	
	/**
	 *	Tests that the internal cells for a macro of type "RAM128X1D" 
	 *	are created correctly when the macro is instanced.
	 */
	@Test
	@DisplayName("Internal Cell Test")
	public void internalCellTest() {
				
		// create expected cell map
		Map<String, String> internalCellsGolden = new HashMap<>();
		internalCellsGolden.put("ram/DP.HIGH", "RAMD64E");
		internalCellsGolden.put("ram/DP.LOW", "RAMD64E");
		internalCellsGolden.put("ram/F7.DP", "MUXF7");
		internalCellsGolden.put("ram/F7.SP", "MUXF7");
		internalCellsGolden.put("ram/SP.HIGH", "RAMD64E");
		internalCellsGolden.put("ram/SP.LOW", "RAMD64E");
				
		//test to see if the number of internal cells matches what's expected
		int internalCountGolden = internalCellsGolden.size();
		int internalCountActual = testMacro.getInternalCells().size(); 
		assertTrue( internalCountGolden == internalCountActual , 
				String.format("Incorrect number of internal cells.\n"
						+ "Expected: %d Actual: %d", internalCountGolden, internalCountActual));
		
		// test that all internal cells have the correct name and are of the correct type
		for (Cell internal : testMacro.getInternalCells()) {
			String type = internalCellsGolden.get(internal.getName());
			
			assertTrue(type != null, String.format("Internal cell \"%s\" is missing from macro.", internal.getName()));
			assertTrue(type.equals(internal.getType()), 
						String.format("Internal cell \"%s\" has the wrong type\nExpected: %s Actual %s ", internal.getName(), type, internal.getType()));
		}
	}
	
	/**
	 * Tests that the internal nets of macros of type "RAM128X1D"
	 * are created correctly when the macro is instanced. 
	 */
	@Test
	@DisplayName("Internal Net Test")
	public void internalNetTest() {
		Map<String, Set<String>> internalNetsGolden = new HashMap<>(); 
		
		internalNetsGolden.put("ram/DPO0", new HashSet<String>(Arrays.asList("ram/F7.DP/I0", "ram/DP.LOW/O")));
		internalNetsGolden.put("ram/DPO1", new HashSet<String>(Arrays.asList("ram/F7.DP/I1", "ram/DP.HIGH/O")));
		internalNetsGolden.put("ram/SPO0", new HashSet<String>(Arrays.asList("ram/F7.SP/I0", "ram/SP.LOW/O")));
		internalNetsGolden.put("ram/SPO1", new HashSet<String>(Arrays.asList("ram/F7.SP/I1", "ram/SP.HIGH/O")));
		
		// test the number of internal nets of the macro is correct
		int goldenCount = internalNetsGolden.size();
		int actualCount = testMacro.getInternalNets().size();
		
		assertTrue( goldenCount == actualCount, 
				String.format("Incorrect number of internal nets.\n"
						+ "Expected: %d Actual: %d", goldenCount, actualCount));
		
		// test the structure of each internal net
		for(CellNet inet : testMacro.getInternalNets()) {
			// check that each internal net of the macro is in the golden map
			Set<String> pinNames = internalNetsGolden.get(inet.getName());
			assertTrue(pinNames != null, String.format("Internal net \"%s\" is not found in the macro.", inet.getName()));
			
			// check the number of pins connected to the internal net is correct
			int goldenPinCount = pinNames.size();
			int actualPinCount = inet.getPins().size();
			
			assertTrue( goldenPinCount == actualPinCount, 
					String.format("Incorrect number of pins connected to internal net \"%s\".\n"
							+ "Expected: %d Actual: %d", inet.getName(), goldenPinCount, actualPinCount));
			
			// check that the internal net is connected to the correct internal cell pins
			for(CellPin ipin : inet.getPins() ) {
				assertTrue(pinNames.contains(ipin.getFullName()), String.format("Unexpected cell pin \"%s\" connected to net \"%s\"", ipin.getFullName() ,inet.getName()));
			}
		}
	}
	
	/**
	 * Adds a macro to a CellDesign, and verifies that the macro is added correctly (the 
	 * internal cells and nets are added). Also removes a macro from a CellDesign, 
	 * and verifies that it is removed correctly.
	 */
	@Test
	@DisplayName("Cell Design Test")
	public void macroCellDesignTest() {
		CellDesign design = new CellDesign();
				
		// First, add the macro cell to the design
		design.addCell(testMacro);	
		assertTrue(design.getCells().size() == 1, "Macro cell not added to design");
		assertTrue(design.getNets().size() == 4, "Internal nets of macro not added to design");
		assertTrue(design.getLeafCells().count() == 6, "Internal cells of macro not added to design");
		
		for (Cell iCell : testMacro.getInternalCells()) {
			// check the internal cell is in the design
			assertTrue(iCell.getDesign() == design, String.format("Internal cell \"%s\" not added to design", iCell.getName()));
			
			//check that an exception is thrown when you try to remove the internal cell
			assertThrows(Exceptions.DesignAssemblyException.class, () -> design.removeCell(iCell));
		}
		for (CellNet iNet : testMacro.getInternalNets()) {
			// check that the internal net is the design
			assertTrue(iNet.getDesign() == design, String.format("Internal net \"%s\" not added to design", iNet.getName()));
			
			//check that an exception is thrown when you try to remove the internal net
			assertThrows(Exceptions.DesignAssemblyException.class, () -> design.removeNet(iNet));
		}
				
		// Now, remove the cell from the design
		design.removeCell(testMacro);
		assertTrue(design.getCells().isEmpty(), "No more cells should exist in the design");
		assertTrue(design.getNets().isEmpty(), "No more nets should exist in the design");
		assertTrue(design.getLeafCells().count() == 0, "No more internal cells should exist in the design");
		
		for (Cell iCell : testMacro.getInternalCells()) {
			assertTrue(iCell.getDesign() == null, String.format("Internal cell \"%s\" not removed to design", iCell.getName()));
		}
		for (CellNet iNet : testMacro.getInternalNets()) {
			assertTrue(iNet.getDesign() == null, String.format("Internal net \"%s\" not removed to design", iNet.getName()));
			assertTrue(iNet.getPins().size() == 2, "Internal nets disconnected after removing macro cell from design.");
		}
	}
	
	/**
	 * When a macro pin is connected to a net, the internal cell pins
	 * should be connected to the net instead of the external macro pin.
	 * This function verifies that that the correct pins are connected to a net
	 * for a macro of type "RAM128X1D".
	 */
	@Test
	@DisplayName("Macro Net Test")
	public void macroNetTest() {
		
		// build golden map of external cell pin, to internal cell pins
		Map<String, Set<String>> connectedPinMapGolden = new HashMap<>(); 
		connectedPinMapGolden.put("DPO", new HashSet<String>(Arrays.asList("ram/F7.DP/O")));
		connectedPinMapGolden.put("SPO", new HashSet<String>(Arrays.asList("ram/F7.SP/O")));
		connectedPinMapGolden.put("A[6]", new HashSet<String>(Arrays.asList("ram/F7.SP/S", "ram/DP.HIGH/WADR6", "ram/DP.LOW/WADR6", "ram/SP.HIGH/WADR6", "ram/SP.LOW/WADR6")));
		connectedPinMapGolden.put("A[5]", new HashSet<String>(Arrays.asList("ram/DP.HIGH/WADR5", "ram/DP.LOW/WADR5", "ram/SP.HIGH/WADR5", "ram/SP.LOW/WADR5", "ram/SP.HIGH/RADR5", "ram/SP.LOW/RADR5")));
		connectedPinMapGolden.put("A[4]", new HashSet<String>(Arrays.asList("ram/DP.HIGH/WADR4", "ram/DP.LOW/WADR4", "ram/SP.HIGH/WADR4", "ram/SP.LOW/WADR4", "ram/SP.HIGH/RADR4", "ram/SP.LOW/RADR4")));
		connectedPinMapGolden.put("A[3]", new HashSet<String>(Arrays.asList("ram/DP.HIGH/WADR3", "ram/DP.LOW/WADR3", "ram/SP.HIGH/WADR3", "ram/SP.LOW/WADR3", "ram/SP.HIGH/RADR3", "ram/SP.LOW/RADR3")));
		connectedPinMapGolden.put("A[2]", new HashSet<String>(Arrays.asList("ram/DP.HIGH/WADR2", "ram/DP.LOW/WADR2", "ram/SP.HIGH/WADR2", "ram/SP.LOW/WADR2", "ram/SP.HIGH/RADR2", "ram/SP.LOW/RADR2")));
		connectedPinMapGolden.put("A[1]", new HashSet<String>(Arrays.asList("ram/DP.HIGH/WADR1", "ram/DP.LOW/WADR1", "ram/SP.HIGH/WADR1", "ram/SP.LOW/WADR1", "ram/SP.HIGH/RADR1", "ram/SP.LOW/RADR1")));
		connectedPinMapGolden.put("A[0]", new HashSet<String>(Arrays.asList("ram/DP.HIGH/WADR0", "ram/DP.LOW/WADR0", "ram/SP.HIGH/WADR0", "ram/SP.LOW/WADR0", "ram/SP.HIGH/RADR0", "ram/SP.LOW/RADR0")));
		connectedPinMapGolden.put("D", new HashSet<String>(Arrays.asList("ram/DP.HIGH/I", "ram/DP.LOW/I", "ram/SP.HIGH/I", "ram/SP.LOW/I")));
		connectedPinMapGolden.put("DPRA[6]", new HashSet<String>(Arrays.asList("ram/F7.DP/S")));
		connectedPinMapGolden.put("DPRA[5]", new HashSet<String>(Arrays.asList("ram/DP.HIGH/RADR5", "ram/DP.LOW/RADR5")));
		connectedPinMapGolden.put("DPRA[4]", new HashSet<String>(Arrays.asList("ram/DP.HIGH/RADR4", "ram/DP.LOW/RADR4")));
		connectedPinMapGolden.put("DPRA[3]", new HashSet<String>(Arrays.asList("ram/DP.HIGH/RADR3", "ram/DP.LOW/RADR3")));
		connectedPinMapGolden.put("DPRA[2]", new HashSet<String>(Arrays.asList("ram/DP.HIGH/RADR2", "ram/DP.LOW/RADR2")));
		connectedPinMapGolden.put("DPRA[1]", new HashSet<String>(Arrays.asList("ram/DP.HIGH/RADR1", "ram/DP.LOW/RADR1")));
		connectedPinMapGolden.put("DPRA[0]", new HashSet<String>(Arrays.asList("ram/DP.HIGH/RADR0", "ram/DP.LOW/RADR0")));
		connectedPinMapGolden.put("WCLK", new HashSet<String>(Arrays.asList("ram/DP.HIGH/CLK", "ram/DP.LOW/CLK", "ram/SP.HIGH/CLK", "ram/SP.LOW/CLK")));
		connectedPinMapGolden.put("WE", new HashSet<String>(Arrays.asList("ram/DP.HIGH/WE", "ram/DP.LOW/WE", "ram/SP.HIGH/WE", "ram/SP.LOW/WE")));
		
		// create a temporary net 
		CellNet tmp = new CellNet("testNet", NetType.WIRE);
		
		for (CellPin pin : testMacro.getPins()) {
			// connect each external macro pin to the temporary net. This should internally connect only internal cell pins
			tmp.connectToPin(pin);
			
			// check that the net of the macro pin is set to (but not connected to) the tmp net
			assertTrue(pin.getNet() == tmp, "External macro pin should be connected to net.");
			
			Set<String> goldenPins = connectedPinMapGolden.get(pin.getName());
			assertTrue(goldenPins != null, String.format("Unexpected pin found in macro cell \"%s\"", pin.getName()));
			
			// check that the number of internal pins connected to the net matches what is expected
			int goldenPinCount = goldenPins.size();
			int actualPinCount = tmp.getPins().size();
			assertTrue(goldenPinCount == actualPinCount, 
					String.format("Incorrect number of internal pins mappings for pin \"%s\".\n"
								+ "Expected: %d, Actual: %d", pin.getName(), goldenPinCount, actualPinCount));
			
			// check that each internal pin is correct
			Collection<CellPin> internalPins = tmp.getPins();
			for (CellPin netPin : tmp.getPins()) {
				assertTrue(goldenPins.contains(netPin.getFullName()), 
						String.format("Pin map %s -> %s does not exist in the macro", pin.getName(), netPin.getFullName()));
				
				assertTrue(netPin.getNet() == tmp, "Internal cell pin " + netPin.getFullName() + " is not attached to temp net");
			}
			
			// disconnect the macro pin... this should disconnect the internal cell pins 
			tmp.disconnectFromPin(pin);
			assertTrue(pin.getNet() == null, "External pin net should be set to null");
			assertTrue(tmp.getPins().isEmpty(), "Internal pins not removed from net for external pin: " + pin.getName());
			
			for (CellPin iPin : internalPins) {
				assertTrue(iPin.getNet() == null, "Internal pin " + iPin.getFullName() + " did not disconnect correctly");
			}
		}
	}
}
