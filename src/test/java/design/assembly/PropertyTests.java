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
package design.assembly;

import edu.byu.ece.rapidSmith.RSEnvironment;
import edu.byu.ece.rapidSmith.design.NetType;
import edu.byu.ece.rapidSmith.design.subsite.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.*;

/**
 * This class holds unit tests for {@link PropertyList} objects.
 */
class PropertyTests {

	/** CellLibrary object used for testing*/
	private static CellLibrary libCells;
	/** Path to the test resource directory*/
	private static final Path resourceDir = RSEnvironment.defaultEnv()
														.getEnvironmentPath()
														.resolve("src")
														.resolve("test")
														.resolve("resources");

	/**
	 * Initializes the PropertyList tests by loading a {@link CellLibrary}. The
	 * {@link CellLibrary} is used to create new {@link Cell} objects whose
	 * properties will be tested.  
	 */
	@BeforeAll
	static void initializeTest() {
		try {
			libCells = new CellLibrary(resourceDir.resolve("design/assembly/cellLibraryTest.xml"));
		} catch (IOException e) {
			fail("Cannot find cell library XML in test directory. Setup is incorrect.");
		}
	}
	
	/**
	 * Tests the {@link PropertyList#update} method.
	 */
	@Test
	@DisplayName("Update Property Test")
	void updatePropertyTest() {
		Cell cell = new Cell("testFF", libCells.get("LUT3"));
		
		// the cell should be created with one initial property
		assertEquals(1, cell.getProperties().size(), "The LUT3 cell has an incorrect number of default properties"); 
		
		// add a new property and make sure it is added correctly
		cell.getProperties().update("TEST_PROPERTY", PropertyType.USER, "TEST_VALUE");
		assertEquals(2, cell.getProperties().size(), "Cell property list size not updated.");
		Property property = cell.getProperties().get("TEST_PROPERTY");
		assertNotNull(property, "Test property not added to cell correctly.");
		assertEquals("TEST_VALUE", property.getStringValue(), "Test property value not stored correctly.");
		
		// update an existing property check that it was overwritten correctly
		assertEquals("0x8'h00", cell.getProperties().get("INIT").getStringValue(), "Property INIT should have inital value.");
		cell.getProperties().update("INIT", PropertyType.EDIF, "8'b01101001");
		assertEquals("8'b01101001", cell.getProperties().getStringValue("INIT"), "Property INIT not updated.");
		assertEquals(2, cell.getProperties().size(), "Updating a default property should not change property list size");
		
		// update the property with the other method signature and check it is overwritten correctly
		cell.getProperties().update(new Property("INIT", PropertyType.EDIF, "8'b00010111"));
		assertEquals("8'b00010111", cell.getProperties().getStringValue("INIT"), "Property INIT not updated.");
	}
	
	/**
	 * Tests the {@link PropertyList#remove} method.
	 */
	@Test
	@DisplayName("Remove Property Test")
	void removeDefaultPropertyTest() {
		Cell cell = new Cell("testFF", libCells.get("FDRE"));
				
		// try to remove each default cell property ... they cannot be removed
		assertAll(
			StreamSupport.stream(cell.getProperties().spliterator(), false)
				.map( prop -> (Executable) () -> assertThrows(
					UnsupportedOperationException.class,
					() -> cell.getProperties().remove(prop.getKey()))
				)
		);
	}

	/**
	 * Tests the {@link PropertyList#remove} method.
	 */
	@Test
	@DisplayName("Remove Property Test")
	void addAndRemovePropertyTest() {
		Cell cell = new Cell("testFF", libCells.get("FDRE"));

		// add a property, remove it, and verify that it was removed correctly
		int originalPropertyCount = cell.getProperties().size();
		cell.getProperties().update("TEST_PROP", PropertyType.EDIF, "TEST_VALUE");
		assertTrue(cell.getProperties().has("TEST_PROP"), "Test property not added to cell");
		cell.getProperties().remove("TEST_PROP");
		assertFalse(cell.getProperties().has("TEST_PROP"), "Test property not removed from cell");
		assertEquals(originalPropertyCount, cell.getProperties().size(), "Cell property list size has changed.");
	}


	/**
	 * Verifies that when a new object with properties is created,
	 * its default properties are set correctly. Objects in RapidSmith
	 * that have properties include:
	 * <p/>
	 * Creates a new cell of type FDRE, and verifies that its
	 * default properties match the expected properties in
	 * the cellLibrary.xml file.
	 */
	@Test
	@DisplayName("Default Cell Property Test")
	void testCellDefaultProperties() {
		Map<String, String> defaultPropertiesGolden = new HashMap<>();
		defaultPropertiesGolden.put("INIT", "1'b0");
		defaultPropertiesGolden.put("IS_C_INVERTED", "1'b0");
		defaultPropertiesGolden.put("IS_D_INVERTED", "1'b0");
		defaultPropertiesGolden.put("IS_R_INVERTED", "1'b0");
		// defaultPropertiesGolden.put("PRIMITIVE_GROUP", "1'b0");
		
		Cell cell = new Cell("testFF", libCells.get("FDRE"));
		
		// check the default property count
		assertEquals(4, cell.getProperties().size(), "The FDRE cell has an incorrect number of default properties"); 
	
		// Iterate through the property list and verify that the actual values match the default values
		for (Property property : cell.getProperties() ) {
			String key = property.getKey();
			String actualValue = property.getStringValue();
			String expectedValue = defaultPropertiesGolden.get(key);
			
			assertNotNull(expectedValue, "Property " + key + " not found in FDRE cell");
			assertEquals(expectedValue, actualValue, "Default value for property " + key + " is not correct");
		}

		// test the "has" functionality of the PropertyList for default properties
		assertTrue(cell.getProperties().has("INIT"), "FDRE cell should have an INIT property");
		assertTrue(cell.getProperties().has("IS_C_INVERTED"), "FDRE cell should have an IS_C_INVERTED property");
		assertTrue(cell.getProperties().has("IS_D_INVERTED"), "FDRE cell should have an IS_D_INVERTED property");
		assertTrue(cell.getProperties().has("IS_R_INVERTED"), "FDRE cell should have an IS_R_INVERTED property");
		
		// test the "get" functionality of the PropertyList for default properties
		assertNotNull(cell.getProperties().get("INIT"), "INIT property on FDRE cell no correctly returned");
		assertNotNull(cell.getProperties().get("IS_C_INVERTED"), "IS_C_INVERTED property on FDRE cell no correctly returned");
		assertNotNull(cell.getProperties().get("IS_D_INVERTED"), "IS_D_INVERTED property on FDRE cell no correctly returned");
		assertNotNull(cell.getProperties().get("IS_R_INVERTED"), "IS_R_INVERTED property on FDRE cell no correctly returned");
	}
	
	/**
	 * Verifies that when a new object with properties is created,
	 * its default properties are set correctly. Objects in RapidSmith
	 * that have properties include:
	 * <p/>
	 * Creates a new CellNet object, and verifies that no default properties exist
	 */
	@Test
	@DisplayName("Default Net Property Test")
	void testCellNetDefaultProperties(){
		// Verify that 
		CellNet net = new CellNet("TestNet", NetType.WIRE);
		assertEquals(0, net.getProperties().size(), "Newly constructed CellNet objects should NOT have default properties");
	}
	
	/**
	 * Verifies that when a new object with properties is created,
	 * its default properties are set correctly. Objects in RapidSmith
	 * that have properties include:
	 * <p/>
	 * Creates a new CellDesign object, and verifies that no default properties exist
	 */
	@Test
	@DisplayName("Default Design Property Test")
	void testCellDesignDefaultProperties(){
		// Verify that 
		CellDesign design = new CellDesign();
		assertEquals(0, design.getProperties().size(), "Newly constructed CellDesign objects should NOT have default properties");
	}
	
	/**
	 * Tests the configuration properties on a {@link LibraryCell} are added correctly
	 * from the cellLibrary.xml file. Specifically, the methods to gain access to the configuration
	 * properties are tested.
	 */
	@Test
	@DisplayName("Library Cell Property Test")
	void libraryCellPropertyTest() {
		
		Map<String, Set<String>> propertyMapGolden = new HashMap<>();
		
		propertyMapGolden.put("FARSRC", new HashSet<>(Arrays.asList("EFAR", "FAR")));
		propertyMapGolden.put("FRAME_RBT_IN_FILENAME", Collections.emptySet());
		
		LibraryCell libCell = libCells.get("FRAME_ECCE2");
		
		assertEquals(2, libCell.getConfigurableProperties().size(), "The FRAME_ECCE2 library cell has the wrong number of configurable properties");
		
		for (String propertyName : libCell.getConfigurableProperties() ) {
			assertTrue(propertyMapGolden.containsKey(propertyName), "Unexpected property:  " + propertyName);
			
			Set<String> possibleValuesGolden = propertyMapGolden.get(propertyName); 
			String[] possibleValuesActual = libCell.getPossibleValues(propertyName);

			assertEquals(possibleValuesGolden.size(), possibleValuesActual.length, 
					"Possible value mismatch for property " + propertyName + "on lib cell " + libCell.getName());
			
			for (String value : possibleValuesActual) {
				assertTrue(possibleValuesGolden.contains(value), "Unexpected value " + value + " found in lib cell " + libCell.getName());
			}
		}
	}
}
