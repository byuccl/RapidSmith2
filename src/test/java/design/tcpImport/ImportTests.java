package design.tcpImport;
import java.io.IOException;
import java.nio.file.Path;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;
import static org.junit.jupiter.api.Assertions.*;

import edu.byu.ece.edif.util.parse.ParseException;
import edu.byu.ece.rapidSmith.RSEnvironment;
import edu.byu.ece.rapidSmith.design.subsite.Cell;
import edu.byu.ece.rapidSmith.design.subsite.CellDesign;
import edu.byu.ece.rapidSmith.design.subsite.CellLibrary;
import edu.byu.ece.rapidSmith.interfaces.vivado.EdifInterface;
import edu.byu.ece.rapidSmith.interfaces.vivado.TincrCheckpoint;
import edu.byu.ece.rapidSmith.interfaces.vivado.VivadoInterface;
import edu.byu.ece.rapidSmith.util.Exceptions;

/**
 * This test class verifies that the test benchmarks are imported into RapidSmith without error.
 * These tests should pass before a Pull Request is issued against the RapidSmith
 * repository. Add a new test to this directory for each test benchmark that is added
 * to the src/test/resources folder. 
 */
@RunWith(JUnitPlatform.class)
public class ImportTests {
	private static final Path testDirectory = RSEnvironment.defaultEnv().getEnvironmentPath()
			.resolve("src")
			.resolve("test")
			.resolve("resources")
			.resolve("ImportTests");
	
	@Test
	@DisplayName("Count16 Import Test")
	public void count16Test() throws IOException {
		VivadoInterface.loadTCP(testDirectory.resolve("TCP").resolve("count16.tcp").toString());
	}
	
	@Test
	@DisplayName("BramDSP Import Test")
	public void bramdspTest() throws IOException {
		VivadoInterface.loadTCP(testDirectory.resolve("TCP").resolve("bramdsp.tcp").toString());
	}
	
	@Test
	@DisplayName("Cordic Import Test")
	public void cordicTest() throws IOException {
		VivadoInterface.loadTCP(testDirectory.resolve("TCP").resolve("cordic.tcp").toString());
	}
	
	@Test
	@DisplayName("SuperCounter Import Test")
	public void superCounterTest() throws IOException {
		VivadoInterface.loadTCP(testDirectory.resolve("TCP").resolve("superCounter.tcp").toString());
	}
	
	@Test
	@DisplayName("Leon3 Import Test")
	public void leon3Test() throws IOException {
		VivadoInterface.loadTCP(testDirectory.resolve("TCP").resolve("leon3.tcp").toString());
	}
	
	/**
	 * This test verifies that internal properties for Vivado macro primitives
	 * are correctly loaded into RapidSmith from a RSCP.
	 */
	@Test
	@DisplayName("Simon Import Test")
	public void simonTest() throws IOException {
		TincrCheckpoint tcp = VivadoInterface.loadTCP(testDirectory.resolve("TCP").resolve("simon.tcp").toString());
		CellDesign design = tcp.getDesign();
		
		// test that internal properties were imported correctly
		Cell test1 = design.getCell("seq_ram_reg_0_127_0_0/DP.HIGH");
		assertEquals("64'h0000000000000000", test1.getProperties().getStringValue("INIT"), "Internal property INIT is not applied correctly");
		assertEquals("2'b10", test1.getProperties().getStringValue("RAM_ADDRESS_MASK"), "Internal property RAM_ADDRESS_MASK is not applied correctly");
		assertEquals("2'b11", test1.getProperties().getStringValue("RAM_ADDRESS_SPACE"), "Internal property RAM_ADDRESS_SPACE is not applied correctly");
	}
	
	/**
	 * This test verifies that single-level macros (a single level of hierarchy) can be parsed from EDIF 
	 * and loaded into RapidSmith2 macro cells. The macro cell is inspected to ensure
	 * that the internal cells, internal nets, and properties are initialized correctly. 
	 */
	@Test
	@DisplayName("Macro Import Test1")
	public void macroTest1() throws IOException {
		TincrCheckpoint tcp = VivadoInterface.loadTCP(testDirectory.resolve("TCP").resolve("macro.tcp").toString());
		CellDesign design = tcp.getDesign();
		
		// Test that macro in the EDIF file was built properly
		Cell macroCell = design.getCell("counter0");
		assertNotNull(macroCell, "Macro cell \"counter0\" not extracted correctly from EDIF netlist.");
		assertTrue(macroCell.isMacro(), "Cell \"counter0\" should be a macro cell, but is marked as leaf.");
		assertEquals(4, macroCell.getPins().size(), "Incorrect number of internal cells");
		assertEquals(42, macroCell.getInternalCells().size(), "Incorrect number of internal cells");
		assertEquals(72, macroCell.getInternalNets().size(), "Incorrect number of internal cells");
		assertEquals(32, macroCell.getProperties().get("width").getValue(), "Macro cell properties not loaded from EDIF.");
		// Test that LUT internal cell has its INIT property set to "4'h6"
		Cell internalLut = design.getCell("counter0/count[3]_i_5");
		assertNotNull(internalLut, "Internal cell \"count[3]_i_5\" not found in the design.");
		assertEquals("4'h6", internalLut.getProperties().get("INIT").getValue(), "INIT string for internal cell \"counter0/count[3]_i_5\" is incorrect.");
		
		//Test that each FDRE internal cell has an INIT property set to "1'b0"
		for (Cell internalCell : macroCell.getInternalCells()) {
			if (internalCell.getLibCell().getName().equals("FDRE")) {
				assertEquals("1'b0", internalCell.getProperties().get("INIT").getValue(), 
						"INIT string for internal cell \"counter0" + internalCell.getName() + "\" is incorrect.");
			}
		}
	}
	
	/**
	 * This test verifies that the correct exception is thrown if multiple levels of hierarchy
	 * are detected in the EDIF file of a RSCP. This is important so the user knows that they
	 * have to regenerate the checkpoint from Vivado.
	 */
	@Test
	@DisplayName("Macro Import Test2")
	public void macroTest2() throws IOException {
		CellLibrary libCells = loadArtix7CellLibrary();
		Throwable exception = expectThrows(Exceptions.ParseException.class, () -> EdifInterface.parseEdif(testDirectory.resolve("hierarchy.edf").toString(), libCells));
		assertTrue(exception.getMessage().startsWith("Multiple levels of hierarchy detected with cell "), "Wrong exception message thrown! " + exception.getMessage());
	}
	
	/**
	 * This test verifies that two macros of the same type can be parsed from EDIF
	 * and that the cells are identical besides their placement locations.
	 */
	@Test
	@DisplayName("Macro Import Test3")
	public void macroTest3() throws IOException, ParseException {

		CellLibrary libCells = loadArtix7CellLibrary();
		CellDesign design = EdifInterface.parseEdif(testDirectory.resolve("hierarchy2.edf").toString(), libCells); 
		
		assertTrue(design.hasCell("counter0"), "Missing macro cell \"counter0\"");
		assertTrue(design.hasCell("counter1"), "Missing macro cell \"counter1\"");
		
		Cell counter0 = design.getCell("counter0");
		Cell counter1 = design.getCell("counter1");
		
		assertEquals(counter0.getLibCell(), counter1.getLibCell());
		assertEquals(counter0.getInternalCells().size(), counter1.getInternalCells().size());
		assertEquals(counter0.getInternalNets().size(), counter1.getInternalNets().size());
		assertEquals(counter0.getPins().size(), counter1.getPins().size());
	}
	
	/**
	 * Loads and returns an artix7 cell library for testing
	 */
	private CellLibrary loadArtix7CellLibrary() throws IOException {
		// load the cell library
		CellLibrary libCells = new CellLibrary(RSEnvironment.defaultEnv()
				.getPartFolderPath("xc7a100tcsg324-3")
				.resolve("cellLibrary.xml"));
		
		// Add a missing cell for the cell library that is needed to parse the EDIF
		libCells.loadMacroXML(RSEnvironment.defaultEnv().getEnvironmentPath()
								.resolve("src")
								.resolve("test")
								.resolve("resources")
								.resolve("macrosTest.xml"));
		return libCells;
	}
}
