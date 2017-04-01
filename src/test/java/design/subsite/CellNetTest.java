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
import java.util.List;
import java.util.ArrayList;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import edu.byu.ece.rapidSmith.RSEnvironment;
import edu.byu.ece.rapidSmith.device.PinDirection;
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

    /**
     * Test if a Net carries a clock signal by connecting and siconnecting a clock pin.
     */
    @Test
    @DisplayName("test cellNet 'isClkNet' method")
    void testIsClkNet() {
        // create the nets and cells that will be used by this test
        CellNet clk_net = new CellNet("clk_net", NetType.WIRE);
        CellNet non_clk_net = new CellNet("non_clk_net", NetType.WIRE);
        CellNet mix_net = new CellNet("mix_net", NetType.WIRE);
        Cell cell = new Cell("test_cell", cell_library.get("FDRE"));
        Cell cell2 = new Cell("test_cell2", cell_library.get("FDRE"));

        // connect and disconnect a clock pin and check isClkNet
        assertFalse(clk_net.isClkNet(), "Net shouldn't be a ClkNet before connecting to a clock pin.");
        clk_net.connectToPin(cell.getPin("C"));
        assertTrue(clk_net.isClkNet(), "Net should be a ClkNet after connecting to a clock pin.");
        clk_net.disconnectFromPin(cell.getPin("C"));
        assertFalse(clk_net.isClkNet(), "Net shouldn't be a ClkNet after disconnecting from a clock pin.");

        // connect and disconnect a non clock pin and check isClkNet
        assertFalse(non_clk_net.isClkNet(), "Net shouldn't be a ClkNet without any pins.");
        non_clk_net.connectToPin(cell.getPin("D"));
        assertFalse(non_clk_net.isClkNet(), "Net shouldn't be a ClkNet with only non clock pins.");
        non_clk_net.disconnectFromPin(cell.getPin("D"));
        assertFalse(non_clk_net.isClkNet(), "Net shouldn't be a ClkNet after removing all pins.");

        // connect a non clock pin, then connect a clock pin. verify isClkNet at each step
        assertFalse(mix_net.isClkNet(), "Net shouldn't be a ClkNet before connecting any pins.");
        mix_net.connectToPin(cell.getPin("D"));
        assertFalse(mix_net.isClkNet(), "Net shouldn't be a ClkNet after connecting a non clock pin.");
        mix_net.connectToPin(cell.getPin("C"));
        assertTrue(mix_net.isClkNet(), "Net should be a ClkNet after connecting at least one clock pin.");
        // connect and disconnect a non clock pin
        mix_net.connectToPin(cell.getPin("Q"));
        assertTrue(mix_net.isClkNet(), "Net should still be a ClkNet after connecting another non-clock pin.");
        mix_net.disconnectFromPin(cell.getPin("D"));
        assertTrue(mix_net.isClkNet(), "Net should still be a ClkNet after disconnecting a non-clock pin.");
        // connect and disconnect another clock pin
        mix_net.connectToPin(cell2.getPin("C"));
        assertTrue(mix_net.isClkNet(), "Net should still be a ClkNet after connecting another clock pin.");
        mix_net.disconnectFromPin(cell.getPin("C"));
        // disconnect all pins
        assertTrue(mix_net.isClkNet(), "Net should still be a ClkNet if another clock pin is still connected.");
        mix_net.disconnectFromPin(cell2.getPin("C"));
        assertFalse(mix_net.isClkNet(), "Net shouldn't be a ClkNet after disconnecting all clock pins.");
        mix_net.disconnectFromPin(cell.getPin("Q"));
        assertFalse(mix_net.isClkNet(), "Net shouldn't be a ClkNet after removing all pins.");
    }

    /**
     * Iteratively test if a Net is connected to a set of pins.
     */
    @Test
    @DisplayName("test CellNet 'isConnectedToPin' method")
    void testIsConnectedToPin() {
        CellNet net = new CellNet("test_net", NetType.WIRE);
        Cell cell = new Cell("test_cell", cell_library.get("LUT3"));
        List<CellPin> pins = new ArrayList<CellPin>(cell.getPins());
        // slowly connect all the pins from the test cell
        for (int index = 0; index < pins.size(); index++) {
            net.connectToPin(pins.get(index));
            // verify the right pin and pin size are connected
            assertTrue(net.isConnectedToPin(pins.get(index)), "CellNet not connected to pin after adding it");
            assertEquals(index+1, net.getPins().size(), "CellNet has improper pin count after adding a new pin.");
        }
        // slowly disconnect all the pins on the net
        for (int index = 0; index < pins.size(); index++) {
            net.disconnectFromPin(pins.get(index));
            // verify the right pin and pin size are connected
            assertFalse(net.isConnectedToPin(pins.get(index)), "CellNet still connected to pin after being removed.");
            assertEquals(pins.size()-index-1, net.getPins().size(), "CellNet has improper pin count after removing a pin.");
        }
    }

    /**
     * Test a Net's source pin(s).
     * When multiple source pins are connected, the Net should return the OUT pin instead of the INOUT pin.
     * When a single source pin is connected of type OUT or INOUT, the Net should return that pin.
     * In either case, the Net should return all output pins when getOutputPins is called.
     */
    @Test
    @DisplayName("test CellNet source pins")
    void testSourcePins() {
        Cell lut3_cell = new Cell("test_lut", cell_library.get("LUT3"));
        Cell ioport_cell = new Cell("test_inout", cell_library.get("IOPORT"));
        CellNet net = new CellNet("test_net", NetType.WIRE);
        // connect several inputs, outputs, and inoutputs to the net
        net.connectToPins(lut3_cell.getPins());
        net.connectToPins(ioport_cell.getPins());
        // verify that the net has a source pin (LUT3-O)
        assertNotNull(net.getSourcePin(), "Net has no source pin.");
        assertEquals(lut3_cell.getPin("O"), net.getSourcePin(), "Net has improper source pin.");
        // verify the net has the right source pins (LUT3-O, IOPORT-PAD)
        assertEquals(lut3_cell.getOutputPins().size() + ioport_cell.getPins().size(), net.getAllSourcePins().size(), "Net has improper source pin count");
        verify_collection(lut3_cell.getOutputPins(), net.getAllSourcePins(), "CellNet", "source pin", true);
        verify_collection(ioport_cell.getPins(), net.getAllSourcePins(), "CellNet", "source pin", true);
        // verify the net has the right sink pins (LUT3-I0, LUT3-I1, LUT3-I2, IOPORT-PAD)
        assertEquals(lut3_cell.getInputPins().size() + ioport_cell.getPins().size(), net.getSinkPins().size(), "Net has improper sink pin count");
        verify_collection(lut3_cell.getInputPins(), net.getSinkPins(), "CellNet", "sink pin", true);
        verify_collection(ioport_cell.getPins(), net.getSinkPins(), "CellNet", "sink pin", true);
        // disconnect the source pin and verify that it changes to the other source pin
        net.disconnectFromPin(lut3_cell.getPin("O"));
        assertEquals(ioport_cell.getPin("PAD"), net.getSourcePin(), "Net has improper source pin after removing original source pin");
        verify_collection(ioport_cell.getPins(), net.getAllSourcePins(), "CellNet", "source pin", false);
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

    /**
     * Test a Net for the pseudo pins attached to it.
     * This can be tested by asserting the proper pseudo pin count after connecting and disconnecting a pseudo pin.
     */
    @Test
    @DisplayName("test CellNet 'getPseudoPinCount' method")
    void testGetPseudoPinCount() {
        CellNet net = new CellNet("test_net", NetType.WIRE);
        Cell cell = new Cell("test_cell", cell_library.get("LUT5"));
        CellPin pin = cell.attachPseudoPin("test_pin", PinDirection.INOUT);
        // connect and then disconnect a pseudo pin to the net. verify the return value of getPseudoPinCount
        net.connectToPin(pin);
        assertEquals(1, net.getPseudoPinCount(), "Net returns wrong pseudo pin count after connecting a pseudo pin");
        net.disconnectFromPin(pin);
        assertEquals(0, net.getPseudoPinCount(), "Net returns wrong pseudo pin count after disconnecting a pseudo pin");
    }

    /**
     * Detach a Net and test the results.
     * After detaching, the net should have no pins connected to it.
     */
    @Test
    @DisplayName("test CellNet 'detachNet' method")
    void testDetachNet() {
        CellNet net = new CellNet("test_net", NetType.WIRE);
        Cell cell = new Cell("test_cell", cell_library.get("LUT3"));
        // attach a lot of pins
        net.connectToPins(cell.getPins());
        assertEquals(cell.getPins().size(), net.getPins().size(), "Net didn't connect to pins correctly");
        // disconnect all the pins
        net.detachNet();
        assertEquals(0, net.getPins().size(), "Net didn't detach from pins correctly");
    }

    /**
     * Test a Net's fan-out
     * The fan-out is basically just a count of all the Net's sink pins.
     * To test the fan-out, iteratively connect and check some sink pins to the Net.
     */
    @Test
    @DisplayName("test CellNet 'getFanOut' method")
    void testGetFanOut() {
        CellNet net = new CellNet("test_net", NetType.WIRE);
        List<CellPin> sink_pins = new ArrayList<CellPin>(new Cell("test_cell", cell_library.get("LUT3")).getInputPins());
        // slowly attach sink pins to the net
        for (int index = 0; index < sink_pins.size(); index++) {
            net.connectToPin(sink_pins.get(index));
            // verify the fan out
            assertEquals(index+1, net.getFanOut(), "CellNet has improper fan out after adding a new sink pin.");
        }
        // slowly disconnect sink pins from the net
        for (int index = 0; index < sink_pins.size(); index++) {
            net.disconnectFromPin(sink_pins.get(index));
            // verify the fan out
            assertEquals(sink_pins.size()-index-1, net.getFanOut(), "CellNet has improper fan out after removing a sink pin.");
        }
    }
}
