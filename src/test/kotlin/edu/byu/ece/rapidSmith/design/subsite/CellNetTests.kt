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
}