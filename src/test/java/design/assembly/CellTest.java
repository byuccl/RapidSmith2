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
 import org.junit.jupiter.api.BeforeAll;
 import org.junit.jupiter.api.DisplayName;
 import org.junit.jupiter.api.Test;
 import org.junit.runner.RunWith;
 import org.junit.platform.runner.JUnitPlatform;
 import static org.junit.jupiter.api.Assertions.*;
 import edu.byu.ece.rapidSmith.RSEnvironment;
 import edu.byu.ece.rapidSmith.device.Device;
 import edu.byu.ece.rapidSmith.design.subsite.CellDesign;
 import edu.byu.ece.rapidSmith.design.subsite.CellLibrary;
 import edu.byu.ece.rapidSmith.design.subsite.LibraryCell;
 import edu.byu.ece.rapidSmith.design.subsite.Cell;
 /**
  * jUnit test for the Cell class in RapidSmith2
  * @author Mark Crossen
  */
 @RunWith(JUnitPlatform.class)
 public class CellTest {

     private static CellLibrary libCells;
     private static Cell lutcell;
     private static Cell portcell;
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
         lutcell = new Cell("lutcell", libCells.get("LUT3"));
         portcell = new Cell("portcell", libCells.get("IPORT"));
         gndcell = new Cell("gndcell", libCells.get("GND"));
         vcccell = new Cell("vcccell", libCells.get("VCC"));
     }

     @Test
     @DisplayName("test Cell method 'isVccSource'")
     public void testIsVccSource() {
        assertFalse(lutcell.isVccSource(), "LUT3 Cell isn't a vcc source.");
        assertFalse(portcell.isVccSource(), "IPORT Cell isn't a vcc source.");
        assertFalse(gndcell.isVccSource(), "GND Cell isn't a vcc source.");
        assertTrue(vcccell.isVccSource(), "VCC Cell should be a vcc source.");
     }

     @Test
     @DisplayName("test Cell method 'isGndSource'")
     public void testIsGndSource() {
        assertFalse(lutcell.isGndSource(), "LUT3 Cell isn't a gnd source.");
        assertFalse(portcell.isGndSource(), "IPORT Cell isn't a gnd source.");
        assertTrue(gndcell.isGndSource(), "GND Cell should be a gnd source.");
        assertFalse(vcccell.isGndSource(), "VCC Cell isn't a gnd source.");
     }

     @Test
     @DisplayName("test Cell method 'isPort'")
     public void testIsPort() {
        assertFalse(lutcell.isPort(), "LUT3 Cell isn't a port.");
        assertTrue(portcell.isPort(), "IPORT Cell should be a port.");
        assertFalse(gndcell.isPort(), "GND Cell isn't a port.");
        assertFalse(vcccell.isPort(), "VCC Cell isn't a port.");
     }

    @Test
    @DisplayName("test Cell method 'isLut'")
    public void testIsLut() {
        assertTrue(lutcell.isLut(), "LUT3 Cell should be a LUT.");
        assertFalse(portcell.isLut(), "IPORT Cell isn't a LUT.");
        assertFalse(gndcell.isLut(), "GND Cell isn't a LUT.");
        assertFalse(vcccell.isLut(), "VCC Cell isn't a LUT.");
    }
}
