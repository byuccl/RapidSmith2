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

package design.subsite;
import java.io.IOException;
import java.util.Collection;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.junit.platform.runner.JUnitPlatform;
import static org.junit.jupiter.api.Assertions.*;
import edu.byu.ece.rapidSmith.RSEnvironment;
import edu.byu.ece.rapidSmith.device.Device;
import edu.byu.ece.rapidSmith.device.PortDirection;
import edu.byu.ece.rapidSmith.device.PinDirection;
import edu.byu.ece.rapidSmith.device.BelId;
import edu.byu.ece.rapidSmith.device.Site;
import edu.byu.ece.rapidSmith.device.Bel;
import edu.byu.ece.rapidSmith.design.subsite.CellDesign;
import edu.byu.ece.rapidSmith.design.subsite.CellLibrary;
import edu.byu.ece.rapidSmith.design.subsite.Cell;
import edu.byu.ece.rapidSmith.design.subsite.CellPin;
import edu.byu.ece.rapidSmith.design.subsite.CellNet;
import edu.byu.ece.rapidSmith.design.NetType;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * unit test for the CellNet class in RapidSmith2
 * @author Mark Crossen
 */
class CellNetTests {

    // The cell library is needed to create different types of pins. These pins are found by creating various Cells.
    private static CellLibrary cell_library;

    /**
     * Initializes the CellLibrary.
     */
    @BeforeAll
    public static void initializeTest() {
        // Get a CellLibrary to use
        try {
            cell_library = new CellLibrary(RSEnvironment.defaultEnv()
                    .getPartFolderPath("xc7a100tcsg324")
                    .resolve("cellLibrary.xml"));
        } catch (IOException e) {
            fail("Cannot find cell library XML in test directory. Setup is incorrect.");
        }
    }

    @Test
    @DisplayName("test CellNet 'isConnectedToPin' method")
    void testIsConnectedToPin() {
        CellNet net = new CellNet("test_net", NetType.WIRE);
        Cell cell = new Cell("test_cell", cell_library.get("LUT3"));
        List<CellPin> pins = new ArrayList<CellPin>(cell.getPins());
        for (int index = 0; index < pins.size(); index++) {
            net.connectToPin(pins.get(index));
            assertTrue(net.isConnectedToPin(pins.get(index)), "CellNet not connected to pin after adding it");
            assertEquals(index+1, net.getPins().size(), "CellNet has improper pin count after adding a new pin.");
        }
        for (int index = 0; index < pins.size(); index++) {
            net.disconnectFromPin(pins.get(index));
            assertFalse(net.isConnectedToPin(pins.get(index)), "CellNet still connected to pin after being removed.");
            assertEquals(pins.size()-index-1, net.getPins().size(), "CellNet has improper pin count after removing a pin.");
        }
    }

    @Test
    @DisplayName("test CellNet source pins")
    void testSourcePins() {
        Cell lut3_cell = new Cell("test_lut", cell_library.get("LUT3"));
        Cell ioport_cell = new Cell("test_inout", cell_library.get("IOPORT"));
        CellNet net = new CellNet("test_net", NetType.WIRE);
        net.connectToPins(lut3_cell.getPins());
        net.connectToPins(ioport_cell.getPins());
        assertNotNull(net.getSourcePin(), "Net has no source pin.");
        assertEquals(lut3_cell.getPin("O"), net.getSourcePin(), "Net has improper source pin.");
        assertEquals(lut3_cell.getOutputPins().size() + ioport_cell.getPins().size(), net.getAllSourcePins().size(), "Net has improper source pin count");
        verify_collection(lut3_cell.getOutputPins(), net.getAllSourcePins(), "CellNet", "source pin", true);
        verify_collection(ioport_cell.getPins(), net.getAllSourcePins(), "CellNet", "source pin", true);
        assertEquals(lut3_cell.getInputPins().size() + ioport_cell.getPins().size(), net.getSinkPins().size(), "Net has improper sink pin count");
        verify_collection(lut3_cell.getInputPins(), net.getSinkPins(), "CellNet", "sink pin", true);
        verify_collection(ioport_cell.getPins(), net.getSinkPins(), "CellNet", "sink pin", true);
        net.disconnectFromPin(lut3_cell.getPin("O"));
        assertEquals(ioport_cell.getPin("PAD"), net.getSourcePin(), "Net has improper source pin after removing original source pin");
        verify_collection(ioport_cell.getPins(), net.getAllSourcePins(), "CellNet", "source pin", false);
    }

    @Test
    @DisplayName("test CellNet 'detachNet' method")
    void testDetachNet() {
        CellNet net = new CellNet("test_net", NetType.WIRE);
        Cell cell = new Cell("test_cell", cell_library.get("LUT3"));
        net.connectToPins(cell.getPins());
        assertEquals(cell.getPins().size(), net.getPins().size(), "Net didn't connect to pins correctly");
        net.detachNet();
        assertEquals(0, net.getPins().size(), "Net didn't detach from pins correctly");
    }

    /**
     * helper function to assert that two Collections are equal to each other
     * expected the Collection to check against
     * actual the Coollection to test
     * collection_name collection name to be used in debug message
     * element_name element name to be used in debug message
     * ignore_different_size set this to true if the expected Collection contains more elements than the actual Collection.
     */
    private <type> void verify_collection(Collection<type> expected, Collection<type> actual, String collection_name, String element_name, Boolean ignore_different_size) {
        if (!ignore_different_size) {
            assertEquals(expected.size(), actual.size(), collection_name + " " + element_name + " size mismatch");
        }
        for (type element : expected) {
            assertTrue(actual.contains(element), collection_name + " is missing a " + element_name);
        }
    }
}