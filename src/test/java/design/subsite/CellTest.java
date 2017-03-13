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

 /**
  * jUnit test for the Cell class in RapidSmith2
  * @author Mark Crossen
  */
 @RunWith(JUnitPlatform.class)
 public class CellTest {

     private static Device device;
     private static CellLibrary libCells;
     private static Cell lutcell;
     private static Cell iportcell;
     private static Cell oportcell;
     private static Cell ioportcell;
     private static Cell gndcell;
     private static Cell vcccell;
     private static List<Cell> testcells;

     /**
      * Initializes the Cell test.
      */
     @BeforeAll
     public static void initializeTest() {
         // Get a CellLibrary to use
         try {
             device = RSEnvironment.defaultEnv().getDevice("xc7a100tcsg324");
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
         testcells = Arrays.asList(lutcell, iportcell, oportcell, ioportcell, gndcell, vcccell);
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
      * helper function to test a Cell for a certain value of the Dir Property
      * cell the Cell to test
      * property the Property to check for
      */
     private void verifyDirProperty(Cell cell, PortDirection property) {
         assertTrue(cell.getProperties().has("Dir"), cell.getName() + " Cell doesn't have 'Dir' property.");
         assertEquals(property, cell.getProperties().get("Dir").getValue(), cell.getName() + " Cell has improper 'Dir' property value.");
     }

     @Test
     @DisplayName("test Cell method 'getPossibleAnchors'")
     public void testGetPossibleAnchors() {
         verifyAnchors(lutcell, Arrays.asList("SLICEL-D6", "SLICEL-D5", "SLICEL-C6", "SLICEL-C5", "SLICEL-B6", "SLICEL-B5", "SLICEL-A5", "SLICEL-A6", "SLICEM-D6", "SLICEM-D5", "SLICEM-C6", "SLICEM-C5", "SLICEM-B6", "SLICEM-B5", "SLICEM-A5", "SLICEM-A6"));
         verifyAnchors(iportcell, Arrays.asList("IOB33-PAD", "IOB33S-PAD", "IOB33M-PAD", "IPAD-PAD"));
         verifyAnchors(oportcell, Arrays.asList("IOB33-PAD", "IOB33S-PAD", "IOB33M-PAD", "OPAD-PAD"));
         verifyAnchors(ioportcell, Arrays.asList("IOB33-PAD", "IOB33S-PAD", "IOB33M-PAD"));
         verifyAnchors(gndcell, Arrays.asList());
         verifyAnchors(vcccell, Arrays.asList());
     }

     /**
      * helper function to test a Cell's possible anchors
      * cell the Cell to test
      * expected the list of possible anchors to check for
      */
     private void verifyAnchors(Cell cell, List<String> expected) {
         List<String> actual = cell.getPossibleAnchors().stream()
                 .map(belid -> (belid.getSiteType()+"-"+belid.getName()).replace("ARTIX7.", "").replace("LUT", ""))
                 .collect(Collectors.toList());
         assertEquals(expected.size(), actual.size(), "Expected anchor count for " + cell.getName() + " doesn't match calculated.");
         for (String bel : expected) {
             assertTrue(actual.contains(bel), cell.getName() + " Cell doesn't have " + bel + " anchor.");
         }
     }

     @Test
     @DisplayName("test Cell placement")
     public void testCellPlacement() {
         // test each of the sample test cells
         for (Cell cell : testcells) {
             // Attempt to place and remove the cell on each type of anchor
             for (BelId anchortype : cell.getPossibleAnchors()) {
                 // Create a new empty CellDesign for the designated FPGA part
                 CellDesign design = new CellDesign("CellPlacementTest", "xc7a100tcsg324");
                 // Add the cell to the design
                 design.addCell(cell);
                 // Get the first site from the device that matches the site type
                 Site site = device.getAllSitesOfType(anchortype.getSiteType()).get(0);
                 // Get the Bel in the site that matches the particular sitetype we're testing.
                 Bel bel = site.getBel(anchortype.getName());
                 // Place the cell on the bel
                 design.placeCell(cell, bel);
                 // verify that getSite returns the correct value
                 assertEquals(cell.getSite(), site, "Site mismatch after placing " + cell.getName() + " Cell on site " + anchortype.getSiteType());
                 // verify that getBel return the correct value
                 assertEquals(cell.getBel(), bel, "Bel mismatch after placing " + cell.getName() + " Cell on bel " + anchortype.getName());
                 // remove the cell from the design so that it can be reused
                 design.removeCell(cell);
                 // verify that the cell is no longer attached to a site or Bel
                 assertNull(cell.getSite(), cell.getName() + " Cell still attached to Site " + site.getName() + " after being removed from design.");
                 assertNull(cell.getBel(), cell.getName() + " Cell still attached to " + bel.getName() + " after being removed from design.");
             }
         }
     }

     @Test
     @DisplayName("test Cell method 'getNetList'")
     public void testGetNetList() {
         // test each of the sample test cells
         for (Cell cell : testcells) {
             // keep track of which nets have been added
             List<CellNet> addednets = new ArrayList<CellNet>();
             // keep track of all the cell pins
             List<CellPin> cellpins = new ArrayList<CellPin>(cell.getPins());
             // connect each cell pin to a net one at a time
             for (int index = 0; index < cellpins.size(); index++) {
                 // create a new net for the pin
                 CellNet newnet = new CellNet("input_net_" + Integer.toString(index), NetType.WIRE);
                 // keep track of the new net
                 addednets.add(newnet);
                 // connect the new net to the pin
                 newnet.connectToPin(cellpins.get(index));
                 // verify the netlist
                 verifyNetList(cell, addednets);
             }
             // remove the nets one at a time
             for (int index = 0; index < cellpins.size(); index++) {
                 // disconnect the pin from the net
                 addednets.get(0).disconnectFromPin(cellpins.get(index));
                 // remove the net from the expected netlist
                 addednets.remove(0);
                 // verify the netlist
                 verifyNetList(cell, addednets);
             }
         }
     }

     /**
      * helper function to verify a Cell's netlist
      * cell the Cell to test
      * expected the expected netlist
      */
     private void verifyNetList(Cell cell, Collection<CellNet> expected) {
         Collection<CellNet> actual = cell.getNetList();
         assertEquals(expected.size(), actual.size(), "netlist size mismatch after adding new net to " + cell.getName());
         for (CellNet net : expected) {
             assertTrue(actual.contains(net), cell.getName() + " Cell doesn't contain expected net while connecting pins.");
         }
     }

     @Test
     @DisplayName("test Cell psuedo pins")
     public void testPsuedoPins() {
         for (Cell cell : testcells) {
             // create and attach a pseudo pin to the cell
             CellPin pseudopin = cell.attachPseudoPin("test_pin", PinDirection.INOUT);
             // make sure the cell has the pseudo pin
             assertEquals(1, cell.getPseudoPinCount(), cell.getName() + " Cell doesn't contain a psuedo pin after attaching one.");
             // verify its the right pseudo pin
             assertEquals(pseudopin, cell.getPseudoPins().iterator().next(), cell.getName() + " Cell doesn't contain the right pseudo pin.");
             // detach the pseudo pin, and make sure it returns True for successful
             assertTrue(cell.removePseudoPin(pseudopin), cell.getName() + "Cell didn't detach pseudo pin properly.");
             // verify the cell no longer has any pseudo pins attached.
             assertEquals(0, cell.getPseudoPinCount(), cell.getName() + " Cell still contains a psuedo pin after removing it.");
         }
     }
 }
