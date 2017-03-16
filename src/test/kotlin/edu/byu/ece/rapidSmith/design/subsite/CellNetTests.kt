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

package edu.byu.ece.rapidSmith.design.subsite;

import edu.byu.ece.rapidSmith.RSEnvironment
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import edu.byu.ece.rapidSmith.design.NetType

// The cell library is needed to create different types of pins. These pins are found by creating various Cells.
private val cell_library = CellLibrary(RSEnvironment.defaultEnv().getPartFolderPath("xc7a100tcsg324").resolve("cellLibrary.xml"))

/**
 * unit test for the CellNet class in RapidSmith2
 * @author Mark Crossen
 */
class CellNetTests {
    @Test
    @DisplayName("test CellNet 'isConnectedToPin' method")
    fun testIsConnectedToPin() {
        var net = CellNet("test_net", NetType.WIRE)
        val cell = Cell("test_cell", cell_library.get("LUT3"))
        cell.getPins().forEachIndexed { index, pin ->
            net.connectToPin(pin)
            assertTrue(net.isConnectedToPin(pin), "CellNet not connected to pin after adding it")
            assertEquals(index+1, net.getPins().size, "CellNet has improper pin count after adding a new pin.")
        }
        cell.getPins().forEachIndexed { index, pin ->
            net.disconnectFromPin(pin)
            assertFalse(net.isConnectedToPin(pin), "CellNet still connected to pin after being removed.")
            assertEquals(cell.getPins().size-index-1, net.getPins().size, "CellNet has improper pin count after removing a pin.")
        }
    }

    @Test
    @DisplayName("test CellNet source pins")
    fun testSourcePins() {
        val lut3_cell = Cell("test_lut", cell_library.get("LUT3"))
        val ioport_cell = Cell("test_inout", cell_library.get("IOPORT"))
        var net = CellNet("test_net", NetType.WIRE)
        net.connectToPins(lut3_cell.getPins())
        net.connectToPins(ioport_cell.getPins())
        assertNotNull(net.sourcePin, "Net has no source pin.")
        assertEquals(lut3_cell.getPin("O"), net.sourcePin, "Net has improper source pin.")
        assertEquals(lut3_cell.outputPins.size + ioport_cell.pins.size, net.allSourcePins.size, "Net has improper source pin count")
        verify_collection(lut3_cell.outputPins, net.allSourcePins, element_name = "source pin", ignore_different_size = true)
        verify_collection(ioport_cell.pins, net.allSourcePins, element_name = "source pin", ignore_different_size = true)
        assertEquals(lut3_cell.inputPins.size + ioport_cell.pins.size, net.sinkPins.size, "Net has improper sink pin count")
        verify_collection(lut3_cell.inputPins, net.sinkPins, element_name = "sink pin", ignore_different_size = true)
        verify_collection(ioport_cell.pins, net.sinkPins, element_name = "sink pin", ignore_different_size = true)
        net.disconnectFromPin(lut3_cell.getPin("O"))
        assertEquals(ioport_cell.getPin("PAD"), net.sourcePin, "Net has improper source pin after removing original source pin")
        verify_collection(ioport_cell.pins, net.allSourcePins, element_name = "source pin")
    }

    /**
     * helper function to assert that two Collections are equal to each other
     * expected the Collection to check against
     * actual the Coollection to test
     * collection_name optional collection name to be used in debug message
     * element_name optional element name to be used in debug message
     * ignore_different_size optionally set this to true if the expected Collection contains more elements than the actual Collection.
     */
    private fun <type>verify_collection(expected : Collection<type>, actual : Collection<type>, collection_name : String = "CellNet", element_name : String = "element", ignore_different_size : Boolean = false) {
        if (!ignore_different_size) {
            assertEquals(expected.size, actual.size, collection_name + " " + element_name + " size mismatch")
        }
        expected.forEach { element ->
            assertTrue(actual.contains(element), collection_name + " is missing a " + element_name)
        }
    }
}