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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.nio.file.Path;

import edu.byu.ece.rapidSmith.RSEnvironment;
import edu.byu.ece.rapidSmith.device.Bel;
import edu.byu.ece.rapidSmith.device.Device;
import edu.byu.ece.rapidSmith.device.FamilyType;
import edu.byu.ece.rapidSmith.device.PackagePin;
import edu.byu.ece.rapidSmith.device.creation.DeviceGenerator;

/**
 * Tests a variety of package pin methods for a device
 */
public class PackagePinTests {

	private static final Path resourceDir = RSEnvironment.defaultEnv()
			.getEnvironmentPath()
			.resolve("src")
			.resolve("test")
			.resolve("resources");
	
	/**
	 * This test verifies that package pin information found in the device_info.xml file
	 * can parsed and loaded into a device without error.
	 */
	@Test
	@DisplayName("Load Package Pins from XML")
	public void loadPackagePinFromXmlTest() {
		Device device = new Device();
		device.setFamily(FamilyType.valueOf("kintexu"));
		device.setPartName("xcku025ffva1156");
		
		DeviceGenerator.parseDeviceInfo(device);
		
		assertEquals(374, device.getPackagePins().size(), "Incorrect number of package pins loaded from device info XML");
		assertEquals(48, device.getClockPads().count(), "Incorrect number of clock pads loaded from device info XML");	
	}
	
	/**
	 * This test verifies that the package pin information is correct for the device "xcku025-ffva1156-1-c"
	 */
	@Test
	@DisplayName("Package Pin Device Test")
	public void packagePinDeviceTest() {
		Device device = RSEnvironment.defaultEnv().getDevice("xcku025-ffva1156-1-c");
		
		assertEquals(374, device.getPackagePins().size(), "Incorrect number of package pins for the device xcku025-ffva1156-1-c ");
		assertEquals(48, device.getClockPads().count(), "Incorrect number of clock pads for the device xcku025-ffva1156-1-c");
		
		String goldenFile = resourceDir.resolve("packagePin.txt").toString();
		
		// Read the golden package pins from the resource directory, and verify that each PAD bel in the
		// device corresponds to the correct package pin
		try (LineNumberReader br = new LineNumberReader(new BufferedReader(new FileReader(goldenFile)))) {
			String line;
			while ((line = br.readLine()) != null) {
				String[] toks = line.split("\\s+");
				Bel bel = device.getSite(toks[0]).getBel(toks[1]);
				PackagePin packagePin = device.getPackagePin(bel);
				assertNotNull(packagePin, "Missing package pin for PAD bel: " + bel.getFullName());
				assertEquals(toks[2], packagePin.getName(), "Incorrect package pin name mapped to bel: " + bel.getFullName());
				if (packagePin.isClockPad()) {
					assertTrue(Boolean.parseBoolean(toks[3]), "Package pin " + packagePin.getName() + " incorrectly marked as clock pad");
				} else {
					assertFalse(Boolean.parseBoolean(toks[3]), "Package pin " + packagePin.getName() + " should be marked as clock pad");
				}
				
				assertEquals(bel, device.getPackagePinBel(packagePin), "Package pin maps to wrong bel");
			}
		} catch (IOException e) {
			fail("Cannot find packagePin.txt golden file in test resource directory. Setup is incorrect.");
		}
		
		// Now, try a non-pad BEL and verify that null is returned when we try to get its package pin
		Bel nonPad = device.getSite("SLICE_X0Y179").getBel("A6LUT");
		assertNull(device.getPackagePin(nonPad), "Bel " + nonPad.getFullName() + " should not be mapped to a package pin");
		
		PackagePin pp = new PackagePin("a", "b/c", false);
		assertNull(device.getPackagePinBel(pp));
		
		pp = new PackagePin("a", "IOB_X1Y79/c", false);
		assertNull(device.getPackagePinBel(pp));
	}
	
	/**
	 * This test verifies that for devices that do not have package pin information,
	 * null pointer exceptions are NOT thrown when package pin methods are called.
	 */
	@Test
	@DisplayName("Empty Package Pin Test")
	public void emptyPackagePinTests() {
		Device device = new Device();
		
		assertNotNull(device.getPackagePins(), "Device.getPackagePins() should not return null.");
		assertEquals(0, device.getPackagePins().size(), "Device with not package pins should return an empty list");
		
		assertNotNull(device.getClockPads(), "Device.getClockPads() should not return null");
		assertEquals(0, device.getClockPads().count(), "Device with no clock pads should return an empty stream");
	}
}
