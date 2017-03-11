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

package DesignAssembly;
import java.io.IOException;
import java.lang.Integer;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.junit.platform.runner.JUnitPlatform;
import static org.junit.jupiter.api.Assertions.*;
import edu.byu.ece.rapidSmith.util.DotFilePrinter;
import edu.byu.ece.rapidSmith.RSEnvironment;
import edu.byu.ece.rapidSmith.device.Device;
import edu.byu.ece.rapidSmith.design.subsite.CellLibrary;
import edu.byu.ece.rapidSmith.design.subsite.LibraryCell;
import edu.byu.ece.rapidSmith.design.subsite.CellDesign;
import edu.byu.ece.rapidSmith.design.subsite.Cell;
import edu.byu.ece.rapidSmith.design.subsite.CellPin;
import edu.byu.ece.rapidSmith.design.subsite.PropertyType;
import edu.byu.ece.rapidSmith.design.subsite.CellNet;
import edu.byu.ece.rapidSmith.design.NetType;

/**
 * tests overview of design creation in RapidSmith2
 * @author Mark Crossen
 */
@RunWith(JUnitPlatform.class)
public class DesignTest {

    private static CellLibrary libCells;
    private static Device device;

    /**
     * Initializes the design creation test by loading a CellLibrary.
     */
    @BeforeAll
    public static void initializeTest() {
        try {
            device = RSEnvironment.defaultEnv().getDevice("xc7a100tcsg324");
            libCells = new CellLibrary(RSEnvironment.defaultEnv()
                    .getPartFolderPath("xc7a100tcsg324")
                    .resolve("cellLibrary.xml"));
        } catch (IOException e) {
            fail("Cannot find cell library XML in test directory. Setup is incorrect.");
        }
    }

    @Test
    @DisplayName("Device Overview Test")
    public void testDeviceOverview() {
        assertEquals("xc7a100tcsg324", device.getPartName(), "Invalid part loaded");
    }

    /**
     * Creates a design similar to to CreateDesignExample.java, and tests several
     * aspects of the design flow.
     */
    @Test
    @DisplayName("Design Overview Test")
    public void testDesignOverview() {
        // create the top level design object
        CellDesign design = new CellDesign("TestDesignOverview", device.getPartName());
        assertEquals("TestDesignOverview", design.getName(), "Design name doesn't match constructor");

        // find a LUT cell to use for this design
        LibraryCell lutlibcell = libCells.get("LUT3");
        assertEquals("LUT3", lutlibcell.getName(), "LUT LibraryCell name doesn't match");
        assertEquals(new Integer(3), lutlibcell.getNumLutInputs(), "Incorrect input count for LUT LibraryCell");

        // create a LUT to use for the sum logic
        Cell addcell = testAddCell(design, "addcell", lutlibcell);
        // Program the LUT
        addcell.getProperties().update("INIT", PropertyType.EDIF, "8'b01101001");
        /* truth table for sum logic
          a b c | o
          0 0 0 | 0
          0 0 1 | 1
          0 1 0 | 1
          0 1 1 | 0
          1 0 0 | 1
          1 0 1 | 0
          1 1 0 | 0
          1 1 1 | 1
        */
        testLUT3(addcell);

        // create a LUT cell for the carry logic
        Cell carrycell = testAddCell(design, "carrycell", lutlibcell);
        // Program the LUT
        carrycell.getProperties().update("INIT", PropertyType.EDIF, "8'b00010111");
        /* truth table for carry logic
          a b c | o
          0 0 0 | 0
          0 0 1 | 0
          0 1 0 | 0
          0 1 1 | 1
          1 0 0 | 0
          1 0 1 | 1
          1 1 0 | 1
          1 1 1 | 1
        */
        testLUT3(carrycell);

        // find a flip flop to use fo this design
        LibraryCell fflibcell = libCells.get("FDRE");
        assertEquals("FDRE", fflibcell.getName(), "Flip Flop LibraryCell name doesn't match");

        // Create a flip flop cell to register the sum output
        Cell sumreg = testAddCell(design, "sumreg", fflibcell);
        // program the flip flop
        sumreg.getProperties().update("INIT", PropertyType.EDIF, "INIT0");
        sumreg.getProperties().update("SR", PropertyType.EDIF, "SRLOW");
        testFF(sumreg);

        // Create a flip flop cell to register the carry output
        Cell coutreg = testAddCell(design, "coutreg", fflibcell);
        // program the flip flop
        coutreg.getProperties().update("INIT", PropertyType.EDIF, "INIT0");
        coutreg.getProperties().update("SR", PropertyType.EDIF, "SRLOW");
        testFF(coutreg);

        // create and test the input ports
        Cell aport = testAddCell(design, "aport", libCells.get("IPORT"));
        testIPORT(aport);
        Cell bport = testAddCell(design, "bport", libCells.get("IPORT"));
        testIPORT(bport);
        Cell cinport = testAddCell(design, "cinport", libCells.get("IPORT"));
        testIPORT(cinport);

        // create and test the output ports
        Cell sumport = testAddCell(design, "sumport", libCells.get("OPORT"));
        testOPORT(sumport);
        Cell coutport = testAddCell(design, "coutport", libCells.get("OPORT"));
        testOPORT(coutport);

        // create and test the wires
        CellNet anet = testAddNet(design, "anet");
        CellNet bnet = testAddNet(design, "bnet");
        CellNet cnet = testAddNet(design, "cnet");
        CellNet sumnet = testAddNet(design, "sumnet");
        CellNet coutnet = testAddNet(design, "coutnet");
        CellNet sumregnet = testAddNet(design, "sumregnet");
        CellNet coutregnet = testAddNet(design, "coutregnet");

        // connect the pins and test the result
        testAddNetPins(anet, aport.getPin("PAD"), Arrays.asList(addcell.getPin("I0"), carrycell.getPin("I0")));
        testAddNetPins(bnet, bport.getPin("PAD"), Arrays.asList(addcell.getPin("I1"), carrycell.getPin("I1")));
        testAddNetPins(cnet, cinport.getPin("PAD"), Arrays.asList(addcell.getPin("I2"), carrycell.getPin("I2")));
        testAddNetPins(sumnet, addcell.getPin("O"), Arrays.asList(sumreg.getPin("D")));
        testAddNetPins(coutnet, carrycell.getPin("O"), Arrays.asList(coutreg.getPin("D")));
        testAddNetPins(sumregnet, sumreg.getPin("Q"), Arrays.asList(sumport.getPin("PAD")));
        testAddNetPins(coutregnet, coutreg.getPin("Q"), Arrays.asList(coutport.getPin("PAD")));

        // test the dot file
        DotFilePrinter dotFilePrinter = new DotFilePrinter(design);
        // each node in the dot file can have a different id or line on each print
        // this avoids comparing the complex contents by comparing the line counts
        assertEquals(20, dotFilePrinter.getNetlistDotString().split("\r\n|\r|\n").length, "Incorrect dot file from design");
    }

    /**
     * test a LUT3 cell's input and output pins
     * @param cell the (LUT3) cell to test
     */
    private void testLUT3(Cell cell) {
        // assert that the cell really is a LUT3
        assertEquals("LUT3", cell.getLibCell().getName(), "LUT3 cell has wrong library cell attached");
        // a LUT3 cell should have 3 inputs, and 1 output
        assertEquals(3, cell.getInputPins().size(), "LUT3 cell has incorrect input pin count");
        assertEquals(1, cell.getOutputPins().size(), "LUT3 cell has incorrect output pin count");
        // the INIT property of a LUT3 should have a length of 11 (8 for the bits, 3 for the number type
        assertNotNull(cell.getProperties().get("INIT"), "Cannot find INIT property for LUT3 cell");
        assertEquals(11, cell.getProperties().get("INIT").getStringValue().length(), "LUT3 cell has incrrect INIT property");
        for (CellPin pin : cell.getInputPins()) {
            // each input pin should begin with "I"
            assertEquals("I", pin.getName().substring(0, 1), "LUT3 cell has incorrect input pins");
        }
        for (CellPin pin : cell.getOutputPins()) {
            // the only output pin should be named "O"
            assertEquals("O", pin.getName(), "LUT3 cell has incorrect output pin");
        }
    }

    /**
     * test an IPORT cell's input and output pins
     * @param cell the (IPORT) cell to test
     */
    private void testIPORT(Cell cell) {
        // assert that the cell really is an IPORT
        assertEquals("IPORT", cell.getLibCell().getName(), "IPORT cell has wrong library cell attached");
        // an IPORT cell should have no inputs and one output
        assertEquals(0, cell.getInputPins().size(), "IPORT cell has incorrect input pin count");
        assertEquals(1, cell.getOutputPins().size(), "IPORT cell has incorrect output pin count");
        // test that the output pin is a "PAD"
        for (CellPin pin : cell.getOutputPins()) {
            assertEquals("PAD", pin.getName(), "IPORT cell has incorrect output pin");
        }
    }

    /**
     * test an OPORT cell's input and output pins
     * @param cell the (OPORT) cell to test
     */
    private void testOPORT(Cell cell) {
        // assert that the cell really is an OPORT
        assertEquals("OPORT", cell.getLibCell().getName(), "OPORT cell has wrong library cell attached");
        // an OPORT cell should have one input and no outputs
        assertEquals(1, cell.getInputPins().size(), "OPORT cell has incorrect input pin count");
        assertEquals(0, cell.getOutputPins().size(), "OPORT cell has incorrect output pin count");
        // test that the input pin is a "PAD"
        for (CellPin pin : cell.getInputPins()) {
            assertEquals("PAD", pin.getName(), "OPORT cell has incorrect input pin");
        }
    }

    /**
     * test a Flip Flop cell's properties
     * @param cell the (Flip Flop) cell to test
     */
    private void testFF(Cell cell) {
        assertEquals("FDRE", cell.getLibCell().getName(), "Flip Flop cell has incorrect library cell attached");
        assertNotNull(cell.getProperties().get("INIT"), "Cannot find INIT property for Flip Flop cell");
        assertNotNull(cell.getProperties().get("SR"), "Cannot find SR property for Flip Flop cell");
        assertEquals("INIT0", cell.getProperties().get("INIT").getStringValue(), "Flip Flop cell has incorrect INIT property");
        assertEquals("SRLOW", cell.getProperties().get("SR").getStringValue(), "Flip Flop cell has incorrect SR property");
    }

    /**
     * add a cell to the design and test that it was added correctly
     * @param design the CellDesign to add the cell to
     * @param cell_name the name of the cell to add
     * @param lib_cell the library cell to use while creating the cell
     */
     private Cell testAddCell(CellDesign design, String cell_name, LibraryCell lib_cell) {
       // create a new cell and add it to the design
       design.addCell(new Cell(cell_name, lib_cell));
       // test that the cell is in the design
       assertTrue(design.hasCell(cell_name), "Cell " + cell_name + " not found in design after adding.");
       // test that the cell can be retreived from the design
       Cell cell = design.getCell(cell_name);
       assertNotNull(cell, "Cell " + cell_name + " unable to be retrieved from design.");
       // test that the cell was created correctly
       assertEquals(cell_name, cell.getName(), lib_cell.getName() + " cell name doesn't match constructor.");
       assertEquals(cell.getLibCell(), lib_cell, "Library cell incorrect.");
       // return the cell to be used later by the test
       return cell;
     }

     /**
      * add a Net to the design and test that it was added correctly
      * @param design the CellDesign to add the cell to
      * @param net_name the name of the net to add
      */
     private CellNet testAddNet(CellDesign design, String net_name) {
       // create a new net and add it to the design
       design.addNet(new CellNet(net_name, NetType.WIRE));
       // test that the net is in the design
       assertTrue(design.hasNet(net_name), "Net " + net_name + " not found in design after adding.");
       // test that the net can be retrieved from the design
       CellNet net = design.getNet(net_name);
       assertNotNull(net, "Net " + net_name + " unable to be retrieved from design.");
       // test that the net was created correctly
       assertEquals(net_name, net.getName(), "net name from design (" + net_name + ") doesn't match constructor.");
       // return the net so it can be connected to pins
       return net;
     }

     /**
      * connect a net to its pins and test the result
      * @param net the Net to connect up
      * @param source_pin the pin to be used by the net as its source
      * @param sink_pins the list of pins to be used by the net as its sinks
      */
     private void testAddNetPins(CellNet net, CellPin source_pin, List<CellPin> sink_pins) {
       // connect the source pins
       net.connectToPin(source_pin);
       // ensure the source pin isn't included in the sink pins
       assertFalse(net.getSinkPins().contains(source_pin), "Net (" + net.getName() + ") mistook the source pin (" + source_pin.getName() + ") as a sink pin.");
       // test that the source pin exists
       assertNotNull(net.getSourcePin(), "Net (" + net.getName() + ") doesn't have source pin (" + source_pin.getName() + ") after adding.");
       // test that the source pin matches the parameter
       assertEquals(source_pin, net.getSourcePin(), "Net has incorrect source pin");
       // connect and test the sink pins
       for (CellPin sink_pin : sink_pins) {
         net.connectToPin(sink_pin);
         assertTrue(net.getSinkPins().contains(sink_pin), "Net (" + net.getName() + ") doesn't include sink pin (" + sink_pin.getName() + ") after adding.");
       }
       assertEquals(sink_pins.size(), net.getSinkPins().size(), "CellNet has incorrect sink pin count");
     }
}
