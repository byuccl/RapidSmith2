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
package device;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import edu.byu.ece.rapidSmith.RSEnvironment;
import edu.byu.ece.rapidSmith.device.Connection;
import edu.byu.ece.rapidSmith.device.Device;
import edu.byu.ece.rapidSmith.device.FamilyType;
import edu.byu.ece.rapidSmith.device.Tile;
import edu.byu.ece.rapidSmith.device.TileType;
import edu.byu.ece.rapidSmith.device.Wire;
import edu.byu.ece.rapidSmith.device.families.Artix7;
import edu.byu.ece.rapidSmith.util.FileTools;
import edu.byu.ece.rapidSmith.device.creation.PartialDeviceGenerator;

/**
 * This class contains unit tests for {@link PartialDeviceGenerator}.
 */
public class PartialDeviceTest {

	// class members
	private static Device device;
	private static Tile oocWireTile;
	
	/**
	 * Creates a partial device file in the specified region of an Artix7 FPGA part to be 
	 * used for the rest of the unit tests in this file.
	 */
	@BeforeAll
	public static void createPartialDevice() {
		Device largeDevice = RSEnvironment.defaultEnv().getDevice("xc7a100tcsg324-3");
		PartialDeviceGenerator generator = new PartialDeviceGenerator();
		device = generator.generatePartialDevice("xc7a_small", largeDevice, "INT_R_X39Y130", "INT_L_X40Y126");
		oocWireTile = device.getTile(0, device.getColumns() - 1);
	}
	
	/**
	 * This test verifies some basic properties of the partial device
	 * data structure.
	 */
	@Test
	@DisplayName("Top-level Device Test")
	public void topLevelDeviceTest() {
		int EXPECTED_ROW_COUNT = 5;
		int EXPECTED_COLUMN_COUNT = 6;
		
		TileType[] expectedTileTypes = 
			{Artix7.TileTypes.INT_R, Artix7.TileTypes.CLBLM_R, Artix7.TileTypes.VBRK, Artix7.TileTypes.CLBLL_L, Artix7.TileTypes.INT_L, TileType.valueOf(device.getFamily(), "OOC_WIRE"),
				Artix7.TileTypes.INT_R, Artix7.TileTypes.CLBLM_R, Artix7.TileTypes.VBRK, Artix7.TileTypes.CLBLL_L, Artix7.TileTypes.INT_L, Artix7.TileTypes.NULL, 
				Artix7.TileTypes.INT_R, Artix7.TileTypes.CLBLM_R, Artix7.TileTypes.VBRK, Artix7.TileTypes.CLBLL_L, Artix7.TileTypes.INT_L, Artix7.TileTypes.NULL,
				Artix7.TileTypes.INT_R, Artix7.TileTypes.CLBLM_R, Artix7.TileTypes.VBRK, Artix7.TileTypes.CLBLL_L, Artix7.TileTypes.INT_L, Artix7.TileTypes.NULL,
				Artix7.TileTypes.INT_R, Artix7.TileTypes.CLBLM_R, Artix7.TileTypes.VBRK, Artix7.TileTypes.CLBLL_L, Artix7.TileTypes.INT_L, Artix7.TileTypes.NULL};
 		
		assertEquals(EXPECTED_ROW_COUNT, device.getRows(), "Incorrect number of device rows");
		assertEquals(EXPECTED_COLUMN_COUNT, device.getColumns(), "Incorrect number of device columns");
		assertEquals(FamilyType.valueOf("artix7"), device.getFamily(), "Incorrect device family");
		assertEquals("xc7a_small", device.getPartName(), "Incorrect part name");
		
		for (int i = 0; i < device.getRows(); i++) {
			for (int j = 0; j < device.getColumns(); j++) {
				assertEquals(expectedTileTypes[i*EXPECTED_COLUMN_COUNT + j], device.getTile(i,j).getType(), i + " " + j);
			}
		}
		
		// Test the tile and site map are populated correctly
		assertEquals(30, device.getTiles().size(), "Tile Map data structure in device not populated correctly");
		assertNotNull(device.getTile("INT_R_X39Y130"), "Tile Map data structure in device not populated correctly");
		assertEquals(30, device.getSites().size(), "Site Map data structure in device not populated correctly");
		assertNotNull(device.getSite("SLICE_X63Y130"), "Site Map data structure in device not populated correctly");
	}
	
	/**
	 * This test verifies some basic properties of the OOC_WIRE tile in the
	 * partial device.
	 */
	@Test
	@DisplayName("OOC_WIRE Tile Test")
	public void oocWireTileTest() {
		int EXPECTED_WIRE_COUNT = 853;
		
		assertEquals("OOC_WIRE_X0Y0", oocWireTile.getName(), "Incorrect ooc wire tile name");
		assertEquals(EXPECTED_WIRE_COUNT, oocWireTile.getWires().size(), "Incorrect number of wires");
		assertEquals(TileType.valueOf(device.getFamily(), "OOC_WIRE"), oocWireTile.getType(),
			"Incorrect tile type.");
	}
	
	/**
	 * This test verifies that wire connections <b>within</b> the 
	 * partial device file are generated correctly
	 */
	@Test
	@DisplayName("Wire Connection Test")
	public void wireConnectionTest() {
		
		int EXPECTED_DOWNHILL_COUNT = 32;
		String[] expectedDownhillNodes = 
			{"IMUX4", "IMUX36", "IMUX44", "FAN_ALT7", "BYP_ALT2", 
			 "IMUX28", "IMUX12", "IMUX20", "SS2BEG2", "SS6BEG2", 
			 "SW2BEG2", "WR1BEG3", "SW6BEG2", "WL1BEG1", "NL1BEG1", 
			 "EE2BEG2", "SE2BEG2", "WW4BEG2", "NE6BEG2", "NR1BEG2", 
			 "NN6BEG2", "NW2BEG2", "NN2BEG2", "NW6BEG2", "NE2BEG2", 
			 "SL1BEG2", "ER1BEG3", "SR1BEG3", "EE4BEG2", "WW2BEG2", 
			 "SE6BEG2", "EL1BEG1"};
		
		Set<String> downhillSet = new HashSet<>();
		for (int i = 0; i < expectedDownhillNodes.length; i++) {
			downhillSet.add(expectedDownhillNodes[i]);
		}
		
		Tile tile = device.getTile(0, 0);
		Wire wire = tile.getWire("LOGIC_OUTS14");
		
		assertEquals(EXPECTED_DOWNHILL_COUNT, wire.getWireConnections().size(), "Incorrect downhill connection count");
		
		for (Connection conn : wire.getWireConnections()) {
			Wire sinkWire = conn.getSinkWire(); 
			assertTrue(downhillSet.contains(sinkWire.getName()), "Unexpected downhill wire from wire " + wire.getFullName() + ": " + sinkWire.getName());
			
			boolean reverseConnectionsExists = false;
			for (Connection reverseConn : sinkWire.getReverseWireConnections()) {
				if (reverseConn.getSinkWire().equals(wire)) {
					reverseConnectionsExists = true;
					break;
				}
			}
			
			assertTrue(reverseConnectionsExists, "Missing reverse connection from downhill wire " + sinkWire.getName());
		}
	}
	
	/**
	 * This test verifies that each IWIRE within the "OOC_WIRE" tile has a connection <b>to</b> a wire in the partial
	 * device. Similarly, it also tests that a reverse wire connection exists from the downhill partial device wire to
	 * the IWIRE.
	 */
	@Test
	@DisplayName("IWIRE Test")
	public void iwireTest() {
		// Testing that verifies all IWIRES are connected to a small device wire
		for (Wire w : oocWireTile.getWires()) {
			assertTrue(1 <= w.getWireConnections().size(), "Incorrect number of wire connections for IWIRE " + w.getFullName());
			Wire sinkWire = w.getWireConnections().iterator().next().getSinkWire();
			
			boolean reverseConnectionExists = false;
			
			for (Connection conn : sinkWire.getReverseWireConnections()) {
				if (conn.getSinkWire().equals(w)) {
					reverseConnectionExists = true;
					break;
				}
			}
			
			assertTrue(reverseConnectionExists, "Missing reverse wire connection " + sinkWire.getFullName() + "->" + w.getFullName()); 
		}
	}
	
	/**
	 * This test verifies that each OWIRE within the "OOC_WIRE" tile has a connection <b>from</b> a wire in the partial
	 * device. Similarly, it also tests that a reverse wire connection exists from the OWIRE to the downhill partial
	 * device wire.
	 */
	@Test
	@DisplayName("OWIRE Test")
	public void owireTest() {
		
		for (Wire w: oocWireTile.getSinks()) {
			assertEquals(1, w.getReverseWireConnections().size(), "Incorrect number of reverse wire connections for OWIRE " + w.getFullName());
			Wire sourceWire = w.getReverseWireConnections().iterator().next().getSinkWire();
			
			boolean forwardConnectionExists = false;
			
			for (Connection conn : sourceWire.getWireConnections()) {
				if (conn.getSinkWire().equals(w)) {
					forwardConnectionExists = true;
					break;
				}
			}
			
			assertTrue(forwardConnectionExists, "Missing wire connection: " + sourceWire.getFullName() + "->" + w.getFullName());
		}
	}
	
	/**
	 * This test verifies that a device created from {@link edu.byu.ece.rapidSmith.util.PartialDeviceInstaller}
	 * can be loaded without error.
	 */
	@Test
	@DisplayName("LoadingTest")
	public void loadPartialDeviceTest() {
		Path deviceFilePath = RSEnvironment.defaultEnv().getEnvironmentPath()
				.resolve("src")
				.resolve("test")
				.resolve("resources")
				.resolve("xc7a_small_db.dat");

		assertNotNull(deviceFilePath, "Missing test device file " +  RSEnvironment.defaultEnv().getEnvironmentPath() + "src/test/resources/xc7a_small_db.dat");
		
		Device test = FileTools.loadDevice(deviceFilePath);
		assertNotNull(test, "Error loading device file");
		assertEquals(5, device.getRows());
		assertEquals(6, device.getColumns());
	}
}