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
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;
import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import edu.byu.ece.rapidSmith.RSEnvironment;
import edu.byu.ece.rapidSmith.device.Bel;
import edu.byu.ece.rapidSmith.device.BelPin;
import edu.byu.ece.rapidSmith.device.BondedType;
import edu.byu.ece.rapidSmith.device.Connection;
import edu.byu.ece.rapidSmith.device.Device;
import edu.byu.ece.rapidSmith.device.FamilyType;
import edu.byu.ece.rapidSmith.device.PinDirection;
import edu.byu.ece.rapidSmith.device.Site;
import edu.byu.ece.rapidSmith.device.SitePin;
import edu.byu.ece.rapidSmith.device.SiteType;
import edu.byu.ece.rapidSmith.device.Tile;
import edu.byu.ece.rapidSmith.device.TileType;
import edu.byu.ece.rapidSmith.device.Wire;
import edu.byu.ece.rapidSmith.device.creation.ExtendedDeviceInfo;
import edu.byu.ece.rapidSmith.device.creation.PartialDeviceGenerator;
import edu.byu.ece.rapidSmith.device.families.Artix7;
import edu.byu.ece.rapidSmith.util.FileTools;

/**
 * This class contains unit tests for {@link PartialDeviceGenerator}.
 */
@RunWith(JUnitPlatform.class)
public class PartialDeviceTest {

	// class members
	private static Device device;
	private static Tile portTile;
	
	/**
	 * Creates a partial device file in the specified region of an Artix7 FPGA part to be 
	 * used for the rest of the unit tests in this file.
	 */
	@BeforeAll
	public static void createPartialDevice() {
		Device largeDevice = RSEnvironment.defaultEnv().getDevice("xc7a100tcsg324-3");
		ExtendedDeviceInfo.loadExtendedInfo(largeDevice);
		PartialDeviceGenerator generator = new PartialDeviceGenerator();
		device = generator.generatePartialDevice("xc7a_small", largeDevice, "INT_R_X39Y130", "INT_L_X40Y126");
		portTile = device.getTile(0, device.getColumns() - 1);
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
			{Artix7.TileTypes.INT_R, Artix7.TileTypes.CLBLM_R, Artix7.TileTypes.VBRK, Artix7.TileTypes.CLBLL_L, Artix7.TileTypes.INT_L, TileType.valueOf(device.getFamily(), "HIERARCHICAL_PORT"),  
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
		assertEquals(1932, device.getSites().size(), "Site Map data structure in device not populated correctly");
		assertNotNull(device.getSite("SLICE_X63Y130"), "Site Map data structure in device not populated correctly");
	}
	
	/**
	 * This test verifies some basic properties of the PORT tile in the 
	 * partial device.
	 */
	@Test
	@DisplayName("Port Tile Test")
	public void portTileTest() {
		int EXPECTED_PORT_COUNT = 1902;
		int EXPECTED_IPORT_COUNT = 981;
		int EXPECTED_OPORT_COUNT = 921;
		
		assertEquals("HEIR_PORT_X0Y0", portTile.getName(), "Incorrect port tile name");
		assertEquals(EXPECTED_OPORT_COUNT, portTile.getSinks().size(), "Incorrect number of sink wires");
		assertEquals(EXPECTED_IPORT_COUNT, portTile.getSources().size(), "Incorrect number of source wires");
		assertEquals(EXPECTED_PORT_COUNT, portTile.getSites().length, "Incorrect number of sites");
		assertEquals(TileType.valueOf(device.getFamily(), "HIERARCHICAL_PORT"), portTile.getType(), 
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
			assertTrue(downhillSet.contains(sinkWire.getWireName()), "Unexpected downhill wire from wire " + wire.getFullWireName() + ": " + sinkWire.getWireName());
			
			boolean reverseConnectionsExists = false;
			for (Connection reverseConn : sinkWire.getReverseWireConnections()) {
				if (reverseConn.getSinkWire().equals(wire)) {
					reverseConnectionsExists = true;
					break;
				}
			}
			
			assertTrue(reverseConnectionsExists, "Missing reverse connection from downhill wire " + sinkWire.getWireName());
		}
	}
	
	/**
	 * This test verifies that each wire connected to an IPORT site within the "HIERARCHICAL_PORT"
	 * tile has a connection <b>to</b> a wire in the partial device. Similarly, it also tests that a reverse
	 * wire connection exists from the downhill partial device wire to the IPORT wire. 
	 */
	@Test
	@DisplayName("IPORT Wire Test")
	public void iportWireTest() {
		// Testing that verifies all IPORT wires are connected to a small device wire
		for (Wire w : portTile.getWires()) {
			assertEquals(1, w.getWireConnections().size(), "Incorrect number of wire connections for IPORT wire " + w.getFullWireName());
			Wire sinkWire = w.getWireConnections().iterator().next().getSinkWire();
			
			boolean reverseConnectionExists = false;
			
			for (Connection conn : sinkWire.getReverseWireConnections()) {
				if (conn.getSinkWire().equals(w)) {
					reverseConnectionExists = true;
					break;
				}
			}
			
			assertTrue(reverseConnectionExists, "Missing reverse wire connection " + sinkWire.getFullWireName() + "->" + w.getFullWireName()); 
		}
	}
	
	/**
	 * This test verifies that each wire connected to an OPORT site within the "HIERARCHICAL_PORT"
	 * tile has a connection <b>from</b> a wire in the partial device. Similarly, it also tests that a reverse
	 * wire connection exists from the OPORT wire to the downhill partial device wire. 
	 */
	@Test
	@DisplayName("OPORT Wire Test")
	public void oportWireTest() {
		
		for (Wire w: portTile.getSinks()) {
			assertEquals(1, w.getReverseWireConnections().size(), "Incorrect number of reverse wire connections for OPORT wire " + w.getFullWireName());
			Wire sourceWire = w.getReverseWireConnections().iterator().next().getSinkWire();
			
			boolean forwardConnectionExists = false;
			
			for (Connection conn : sourceWire.getWireConnections()) {
				if (conn.getSinkWire().equals(w)) {
					forwardConnectionExists = true;
					break;
				}
			}
			
			assertTrue(forwardConnectionExists, "Missing wire connection: " + sourceWire.getFullWireName() + "->" + w.getFullWireName());
		}
	}
	
	/**
	 * This test verifies that the correct number of IPORT and OPORT sites are 
	 * added to the port tile, and that they are of the correct type and connect
	 * to the correct tile wire. 
	 */
	@Test
	@DisplayName("Port Site Test")
	public void portSiteTest() {
		
		int EXPECTED_PORT_COUNT = 1902;
		int EXPECTED_IPORT_COUNT = 981;
		SiteType iportType = SiteType.valueOf(device.getFamily(), "IPORT");
		SiteType oportType = SiteType.valueOf(device.getFamily(), "OPORT");
		
		Site[] portSites = portTile.getSites();
		assertEquals(EXPECTED_PORT_COUNT, portSites.length, "Incorrect number of PORT sites in the partial device");
		
		// Check that there are a correct number of IPORT sites in the port tile and they are initialized
		for (int i = 0; i < EXPECTED_IPORT_COUNT; i++) {
			Site site = portSites[i];
			assertEquals(iportType, site.getType(), "Incorrect type for IPORT site");
			SitePin pin = site.getSitePin("O");
			assertEquals("iport_wire" + i, pin.getExternalWire().getWireName(), "Incorrect wire name connected to IPORT site: " + site.getName());
		}

		// Check that there are a correct number of OPORT sites in the port tile and they are initialized
		int offset = EXPECTED_IPORT_COUNT;
		for (int i = offset; i < portSites.length; i++) {
			Site site = portSites[i];
			assertEquals(oportType, site.getType(), "Incorrect type for OPORT site");
			SitePin pin = site.getSitePin("I");
			assertEquals("oport_wire" + (i-offset), pin.getExternalWire().getWireName(), "Incorrect wire name connected to OPORT site: " + site.getName());
		}
	}
	
	/**
	 * This test verifies that the IPORT site template was constructed
	 * correctly. IPORTs should have a single site pin with a single
	 * IPAD BEL (which in turn has a single BEL pin)
	 */
	@Test
	@DisplayName("IPORT Structural Verification")
	public void iportStructureTest() {
		int IPORT_INDEX = 0;
		SiteType iportType = SiteType.valueOf(device.getFamily(), "IPORT");
		
		// Site verification
		Site site = portTile.getSite(IPORT_INDEX);
		assertEquals(iportType, site.getType(), "Incorrect type");
		assertEquals(portTile, site.getTile(), "Parent tile not set for IPORT site");
		assertEquals(BondedType.INTERNAL, site.getBondedType(), "Bonded type incorrect");
		assertEquals("IPORT_0", site.getName(), "Incorrect site name");
		assertEquals(1, site.getSourcePins().size(), "Incorrect source pin count for IPORT site type");
		assertEquals(0, site.getSinkPins().size(), "Incorrect sink pin count for IPORT site type");
		
		// SitePin verification
		SitePin sitePin = site.getSitePin("O"); 
		assertNotNull(sitePin);
		assertEquals(PinDirection.OUT, sitePin.getDirection(), "Incorrect SitePin direction");
		
		// Intrasite routing connection verification
		Wire internalWire = sitePin.getInternalWire();
		assertNotNull(internalWire);
		assertEquals("intrasite:IPORT/O.O", internalWire.getWireName(), "Incorrect intrasite wire name connecting to site pin " + site.getName() + "/" + sitePin.getName());
		
		assertNull(internalWire.getTerminal(), "Wire " + internalWire.getWireName() + " should not connect to a BelPin");
		assertEquals(0, internalWire.getWireConnections().size(), "Incorrect number of wire connections for wire " + internalWire.getWireName());
		assertNotNull(internalWire.getConnectedPin(), "Wire " + internalWire.getWireName() + " should connect to a site pin");
		assertEquals(sitePin, internalWire.getConnectedPin(), "Wire " + internalWire.getWireName() + " connects to incorrect site pin");
		
		assertEquals(1, internalWire.getReverseWireConnections().size(), "Incorrect number of reverse connections for wire " + internalWire.getWireName());
		Wire sourceWire = internalWire.getReverseWireConnections().iterator().next().getSinkWire();
		assertEquals("intrasite:IPORT/IPAD.PAD", sourceWire.getWireName(), "Incorrect reverse wire connection for wire " + internalWire.getWireName());
		
		assertNotNull(sourceWire.getSource(), "Wire " + sourceWire.getFullWireName() + " should be sourced by a BelPin");
		assertEquals("PAD", sourceWire.getSource().getName(), "Wire " + sourceWire.getFullWireName() + " is sourced by wrong BelPin");
		assertNull(sourceWire.getTerminal(), "Wire " + sourceWire.getFullWireName() + " should not drive a BelPin");
		assertNull(sourceWire.getConnectedPin(), "Wire " + sourceWire.getFullWireName() + " should not drive a SitePin");
		assertEquals(1, sourceWire.getWireConnections().size(), "Incorrect number of wire connections for wire " + sourceWire.getFullWireName());
		assertEquals(internalWire, sourceWire.getWireConnections().iterator().next().getSinkWire(), "Incorrect down hill wire from " + sourceWire.getFullWireName());
		
		// BEL verification
		assertEquals(1, site.getBels().size());
		Bel bel = site.getBel("IPAD");
		assertNotNull(bel, "Missing BEL: IPAD from IPORT site.");
		assertEquals(1, bel.getBelPins().count(), "Incorrect number of BEL pins on \"IPAD\" BEL");
		assertEquals(1, bel.getSources().size(), "Incorrect number of output BEL pins on \"IPAD\" BEL");
		assertEquals(0, bel.getSinks().size(), "Incorrect number of input BEL pins on \"IPAD\" BEL");
		BelPin belPin = bel.getBelPin("PAD");
		assertNotNull(belPin, "Missing pin \"PAD\" from BEL \"IPAD\"");
		assertEquals(PinDirection.OUT, belPin.getDirection(), "Incorrect BelPin direction");
		assertEquals(1, belPin.getSitePins().size(), "Bel pin \"PAD\" should connect to site pin");
		assertEquals(sitePin, belPin.getSitePins().iterator().next(), "Bel pin \"PAD\" should connect to site pin");
		Wire belPinWire = belPin.getWire();
		assertEquals("intrasite:IPORT/IPAD.PAD", belPinWire.getWireName(), "Bel pin \"PAD\" connects to incorrect wire");
	}
	
	/**
	 * This test verifies that the OPORT site template was constructed
	 * correctly. OPORTs should have a single site pin with a single
	 * OPAD BEL (which in turn has a single BEL pin)
	 */
	@Test
	@DisplayName("OPORT Structural Verification")
	public void oportStructureTest() {
		int OPORT_INDEX = portTile.getSites().length - 1;
		SiteType oportType = SiteType.valueOf(device.getFamily(), "OPORT");
		
		// Site verification
		Site site = portTile.getSite(OPORT_INDEX);
		assertEquals(oportType, site.getType(), "Incorrect type");
		assertEquals(portTile, site.getTile(), "Parent tile not set for OPORT site");
		assertEquals(BondedType.INTERNAL, site.getBondedType(), "Bonded type incorrect");
		assertEquals("OPORT_920", site.getName(), "Incorrect site name");
		assertEquals(0, site.getSourcePins().size(), "Incorrect source pin count for OPORT site type");
		assertEquals(1, site.getSinkPins().size(), "Incorrect sink pin count for OPORT site type");
		
		// SitePin verification
		SitePin sitePin = site.getSitePin("I"); 
		assertNotNull(sitePin);
		assertEquals(PinDirection.IN, sitePin.getDirection(), "Incorrect SitePin direction");
		
		// Intrasite routing connection verification
		Wire internalWire = sitePin.getInternalWire();
		assertNotNull(internalWire);
		assertEquals("intrasite:OPORT/I.I", internalWire.getWireName(), "Incorrect intrasite wire name connecting to site pin " + site.getName() + "/" + sitePin.getName());
		
		assertNull(internalWire.getTerminal(), "Wire " + internalWire.getWireName() + " should not connect to a BelPin");
		assertNull(internalWire.getSource(), "Wire " + internalWire.getWireName() + " should not be driven by a BelPin");
		assertNull(internalWire.getConnectedPin(), "Wire " + internalWire.getWireName() + " should not drive a SitePin");		
		assertNotNull(internalWire.getReverseConnectedPin(), "Wire " + internalWire.getWireName() + " should be driven by a SitePin");		
		assertEquals(sitePin, internalWire.getReverseConnectedPin(), "Wire " + internalWire.getWireName() + " driven by incorrect SitePin");
		assertEquals(0, internalWire.getReverseWireConnections().size(), "Incorrect number of reverse wire connections for wire " + internalWire.getWireName());
		assertEquals(1, internalWire.getWireConnections().size(), "Incorrect number of wire connections for wire " + internalWire.getWireName());
		Wire sinkWire = internalWire.getWireConnections().iterator().next().getSinkWire();
		assertEquals("intrasite:OPORT/OPAD.PAD", sinkWire.getWireName(), "Incorrect downhill wire connection for wire " + internalWire.getWireName());
		
		assertNull(sinkWire.getSource(), "Wire " + sinkWire.getFullWireName() + " should not be sourced by a BelPin");
		assertNull(sinkWire.getConnectedPin(), "Wire " + sinkWire.getWireName() + " should not drive a SitePin");
		assertNull(sinkWire.getReverseConnectedPin(), "Wire " + sinkWire.getWireName() + " should not be driven by a SitePin");
		assertEquals(0, sinkWire.getWireConnections().size(), "Incorrect number of forward wire connections for wire " + sinkWire.getWireName());
		assertEquals("PAD", sinkWire.getTerminal().getName(), "Wire " + sinkWire.getFullWireName() + " should drive a BEL pin");
		assertEquals(1, sinkWire.getReverseWireConnections().size(), "Incorrect number of reverse wire connections for wire " + sinkWire.getWireName());
		assertEquals(internalWire, sinkWire.getReverseWireConnections().iterator().next().getSinkWire(), "Incorrect down hill wire from " + sinkWire.getFullWireName());
	
		// BEL verification
		assertEquals(1, site.getBels().size());
		Bel bel = site.getBel("OPAD");
		assertNotNull(bel, "Missing BEL: OPAD from OPORT site.");
		assertEquals(1, bel.getBelPins().count(), "Incorrect number of BEL pins on \"OPAD\" BEL");
		assertEquals(0, bel.getSources().size(), "Incorrect number of output BEL pins on \"OPAD\" BEL");
		assertEquals(1, bel.getSinks().size(), "Incorrect number of input BEL pins on \"OPAD\" BEL");
		BelPin belPin = bel.getBelPin("PAD");
		assertNotNull(belPin, "Missing pin \"PAD\" from BEL \"OPAD\"");
		assertEquals(PinDirection.IN, belPin.getDirection(), "Incorrect BelPin direction");
		assertEquals(1, belPin.getSitePins().size(), "Bel pin \"PAD\" should connect to site pin");
		assertEquals(sitePin, belPin.getSitePins().iterator().next(), "Bel pin \"PAD\" should connect to site pin");
		Wire belPinWire = belPin.getWire();
		assertEquals("intrasite:OPORT/OPAD.PAD", belPinWire.getWireName(), "Bel pin \"PAD\" connects to incorrect wire");
	}
	
	/**
	 * This test verifies that a device created from {@link PartialDeviceInstaller}
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