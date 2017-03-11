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
 import java.io.IOException;
 import java.util.Arrays;
 import java.util.List;
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
 import edu.byu.ece.rapidSmith.design.subsite.CellDesign;
 import edu.byu.ece.rapidSmith.design.subsite.CellLibrary;
 import edu.byu.ece.rapidSmith.design.subsite.LibraryCell;
 import edu.byu.ece.rapidSmith.design.subsite.Cell;
 import edu.byu.ece.rapidSmith.design.subsite.CellPin;

 /**
  * jUnit test for the Cell class in RapidSmith2
  * @author Mark Crossen
  */
 @RunWith(JUnitPlatform.class)
 public class CellTest {

     private static CellLibrary libCells;
     private static Cell lutcell;
     private static Cell iportcell;
     private static Cell oportcell;
     private static Cell ioportcell;
     private static Cell gndcell;
     private static Cell vcccell;

     /**
      * Initializes the Cell test.
      */
     @BeforeAll
     public static void initializeTest() {
         // Get a CellLibrary to use
         try {
             Device device = RSEnvironment.defaultEnv().getDevice("xc7a100tcsg324");
             libCells = new CellLibrary(RSEnvironment.defaultEnv()
                     .getPartFolderPath("xc7a100tcsg324")
                     .resolve("cellLibrary.xml"));
         } catch (IOException e) {
             fail("Cannot find cell library XML in test directory. Setup is incorrect.");
         }
         // Create a few Cells to test.
         lutcell = new Cell("LUT3", libCells.get("LUT3"));
         iportcell = new Cell("IPORT", libCells.get("IPORT"));
         oportcell = new Cell("OPORT", libCells.get("OPORT"));
         ioportcell = new Cell("IOPORT", libCells.get("IOPORT"));
         gndcell = new Cell("GND", libCells.get("GND"));
         vcccell = new Cell("VCC", libCells.get("VCC"));
     }

     @Test
     @DisplayName("test Cell method 'isVccSource'")
     public void testIsVccSource() {
        assertFalse(lutcell.isVccSource(), "LUT3 Cell isn't a vcc source.");
        assertFalse(iportcell.isVccSource(), "IPORT Cell isn't a vcc source.");
        assertFalse(oportcell.isVccSource(), "OPORT Cell isn't a vcc source.");
        assertFalse(ioportcell.isVccSource(), "IOPORT Cell isn't a vcc source.");
        assertFalse(gndcell.isVccSource(), "GND Cell isn't a vcc source.");
        assertTrue(vcccell.isVccSource(), "VCC Cell should be a vcc source.");
     }

     @Test
     @DisplayName("test Cell method 'isGndSource'")
     public void testIsGndSource() {
        assertFalse(lutcell.isGndSource(), "LUT3 Cell isn't a gnd source.");
        assertFalse(iportcell.isGndSource(), "IPORT Cell isn't a gnd source.");
        assertFalse(oportcell.isGndSource(), "OPORT Cell isn't a gnd source.");
        assertFalse(ioportcell.isGndSource(), "IOPORT Cell isn't a gnd source.");
        assertTrue(gndcell.isGndSource(), "GND Cell should be a gnd source.");
        assertFalse(vcccell.isGndSource(), "VCC Cell isn't a gnd source.");
     }

     @Test
     @DisplayName("test Cell method 'isPort'")
     public void testIsPort() {
        assertFalse(lutcell.isPort(), "LUT3 Cell isn't a port.");
        assertTrue(iportcell.isPort(), "IPORT Cell should be a port.");
        assertTrue(oportcell.isPort(), "OPORT Cell should be a port.");
        assertTrue(ioportcell.isPort(), "IOPORT Cell should be a port.");
        assertFalse(gndcell.isPort(), "GND Cell isn't a port.");
        assertFalse(vcccell.isPort(), "VCC Cell isn't a port.");
     }

    @Test
    @DisplayName("test Cell method 'isLut'")
    public void testIsLut() {
        assertTrue(lutcell.isLut(), "LUT3 Cell should be a LUT.");
        assertFalse(iportcell.isLut(), "IPORT Cell isn't a LUT.");
        assertFalse(oportcell.isLut(), "OPORT Cell isn't a LUT.");
        assertFalse(ioportcell.isLut(), "IOPORT Cell isn't a LUT.");
        assertFalse(gndcell.isLut(), "GND Cell isn't a LUT.");
        assertFalse(vcccell.isLut(), "VCC Cell isn't a LUT.");
    }

    @Test
    @DisplayName("test Cell method 'getOutputPins'")
    public void testGetOutputPins() {
        verifyOutputPins(lutcell, Arrays.asList("O"));
        verifyOutputPins(iportcell, Arrays.asList("PAD"));
        verifyOutputPins(oportcell, Arrays.asList());
        verifyOutputPins(ioportcell, Arrays.asList("PAD"));
        verifyOutputPins(gndcell, Arrays.asList("G"));
        verifyOutputPins(vcccell, Arrays.asList("P"));
    }

    @Test
    @DisplayName("test Cell method 'getInputPins'")
    public void testGetInputPins() {
        verifyInputPins(lutcell, Arrays.asList("I0", "I1", "I2"));
        verifyInputPins(iportcell, Arrays.asList());
        verifyInputPins(oportcell, Arrays.asList("PAD"));
        verifyInputPins(ioportcell, Arrays.asList("PAD"));
        verifyInputPins(gndcell, Arrays.asList());
        verifyInputPins(vcccell, Arrays.asList());
    }

    /**
     * helper function to test a Cell to make sure it has the given output pins
     * cell the Cell to test
     * expected the list of output pin names the cell should have
     */
    private void verifyOutputPins(Cell cell, List<String> expected) {
        List<String> actual = cell.getOutputPins().stream()
            .map(CellPin::getName)
            .collect(Collectors.toList());
        assertEquals(expected.size(), actual.size(), "Expected output pin count for " + cell.getName() + " Cell doesn't match calculated.");
        for (String pin : expected) {
            assertTrue(actual.contains(pin), cell.getName() + " Cell doesn't have " + pin + " output pin.");
        }
    }

    /**
     * helper function to test a Cell to make sure it has the given input pins
     * cell the Cell to test
     * pins the list of input pin names the cell should have
     */
    private void verifyInputPins(Cell cell, List<String> expected) {
        List<String> actual = cell.getInputPins().stream()
            .map(CellPin::getName)
            .collect(Collectors.toList());
        assertEquals(expected.size(), actual.size(), "Expected input pin count for " + cell.getName() + " Cell doesn't match calcualted.");
        for (String pin : expected) {
            assertTrue(actual.contains(pin), cell.getName() + " Cell doesn't have " + pin + " input pin.");
        }
    }

    @Test
    @DisplayName("test 'Dir' property of PORT Cells.")
    public void testDirProperty() {
        verifyDirProperty(iportcell, PortDirection.IN);
        verifyDirProperty(oportcell, PortDirection.OUT);
        verifyDirProperty(ioportcell, PortDirection.INOUT);
    }

    /**
     * helper functino to test a Cell for a certain value of the Dir Property
     * cell the Cell to test
     * property the Property to check for
     */
     private void verifyDirProperty(Cell cell, PortDirection property) {
        assertTrue(cell.getProperties().has("Dir"), cell.getName() + " Cell doesn't have 'Dir' property.");
        assertEquals(property, cell.getProperties().get("Dir").getValue(), cell.getName() + " Cell has improper 'Dir' property value.");
     }
}
