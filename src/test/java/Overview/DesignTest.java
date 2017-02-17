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

package Overview;
import java.io.IOException;
import java.lang.Integer;
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
        assertEquals("xc7a100tcsg324", device.getPartName());
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
        assertEquals("TestDesignOverview", design.getName());

        // find a LUT cell to use for this design
        LibraryCell lutlibcell = libCells.get("LUT3");
        assertEquals("LUT3", lutlibcell.getName());
        assertEquals(new Integer(3), lutlibcell.getNumLutInputs());

        // create a LUT to use for the sum logic
        Cell addcell = design.addCell(new Cell("addcell", lutlibcell));
        assertEquals("addcell", addcell.getName());
        // Program the LUT
        addcell.getProperties().update("INIT", PropertyType.DESIGN, "8'b01101001");
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
        Cell carrycell = design.addCell(new Cell("carrycell", lutlibcell));
        assertEquals("carrycell", carrycell.getName());
        // Program the LUT
        carrycell.getProperties().update("INIT", PropertyType.DESIGN, "8'b00010111");
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

        // Create a flip flop cell to register the sum output
        Cell sumreg = design.addCell(new Cell("sumreg", fflibcell));
        assertEquals("sumreg", sumreg.getName());
        sumreg.getProperties().update("INIT", PropertyType.DESIGN, "INIT0");
        sumreg.getProperties().update("SR", PropertyType.DESIGN, "SRLOW");
        testFF(sumreg);

        // Create a flip flop cell to register the carry output
        Cell coutreg = design.addCell(new Cell("coutreg", fflibcell));
        assertEquals("coutreg", coutreg.getName());
        coutreg.getProperties().update("INIT", PropertyType.DESIGN, "INIT0");
        coutreg.getProperties().update("SR", PropertyType.DESIGN, "SRLOW");
        testFF(coutreg);

        // create and test the input ports
        Cell aport = design.addCell(new Cell("aport", libCells.get("IPORT")));
        assertEquals("aport", aport.getName());
        testIPORT(aport);
        Cell bport = design.addCell(new Cell("bport", libCells.get("IPORT")));
        assertEquals("bport", bport.getName());
        testIPORT(bport);
        Cell cinport = design.addCell(new Cell("cinport", libCells.get("IPORT")));
        assertEquals("cinport", cinport.getName());
        testIPORT(cinport);

        // create and test the output ports
        Cell sumport = design.addCell(new Cell("sumport", libCells.get("OPORT")));
        assertEquals("sumport", sumport.getName());
        testOPORT(sumport);
        Cell coutport = design.addCell(new Cell("coutport", libCells.get("OPORT")));
        assertEquals("coutport", coutport.getName());
        testOPORT(coutport);

        // create and test the wires
        CellNet anet = design.addNet(new CellNet("anet", NetType.WIRE));
        assertEquals("anet", anet.getName());
        CellNet bnet = design.addNet(new CellNet("bnet", NetType.WIRE));
        assertEquals("bnet", bnet.getName());
        CellNet cnet = design.addNet(new CellNet("cnet", NetType.WIRE));
        assertEquals("cnet", cnet.getName());
        CellNet sumnet = design.addNet(new CellNet("sumnet", NetType.WIRE));
        assertEquals("sumnet", sumnet.getName());
        CellNet coutnet = design.addNet(new CellNet("coutnet", NetType.WIRE));
        assertEquals("coutnet", coutnet.getName());
        CellNet sumregnet = design.addNet(new CellNet("sumregnet", NetType.WIRE));
        assertEquals("sumregnet", sumregnet.getName());
        CellNet coutregnet = design.addNet(new CellNet("coutregnet", NetType.WIRE));
        assertEquals("coutregnet", coutregnet.getName());

        // connect and test the A input wire
        anet.connectToPin(aport.getPin("PAD"));
        anet.connectToPin(addcell.getPin("I0"));
        anet.connectToPin(carrycell.getPin("I0"));
        assertEquals(2, anet.getSinkPins().size());
        assertEquals(aport.getPin("PAD"), anet.getSourcePin());
        assertTrue(anet.getSinkPins().contains(addcell.getPin("I0")));
        assertTrue(anet.getSinkPins().contains(carrycell.getPin("I0")));

        // connect and test the B input wire
        bnet.connectToPin(bport.getPin("PAD"));
        bnet.connectToPin(addcell.getPin("I1"));
        bnet.connectToPin(carrycell.getPin("I1"));
        assertEquals(2, bnet.getSinkPins().size());
        assertEquals(bport.getPin("PAD"), bnet.getSourcePin());
        assertTrue(bnet.getSinkPins().contains(addcell.getPin("I1")));
        assertTrue(bnet.getSinkPins().contains(carrycell.getPin("I1")));

        // connect and test the carry input wire
        cnet.connectToPin(cinport.getPin("PAD"));
        cnet.connectToPin(addcell.getPin("I2"));
        cnet.connectToPin(carrycell.getPin("I2"));
        assertEquals(2, cnet.getSinkPins().size());
        assertEquals(cinport.getPin("PAD"), cnet.getSourcePin());
        assertTrue(cnet.getSinkPins().contains(addcell.getPin("I2")));
        assertTrue(cnet.getSinkPins().contains(carrycell.getPin("I2")));

        // connect and test the sum output wire
        sumnet.connectToPin(addcell.getPin("O"));
        sumnet.connectToPin(sumreg.getPin("D"));
        assertEquals(1, sumnet.getSinkPins().size());
        assertEquals(addcell.getPin("O"), sumnet.getSourcePin());
        assertTrue(sumnet.getSinkPins().contains(sumreg.getPin("D")));

        // connect and test the carry output wire
        coutnet.connectToPin(carrycell.getPin("O"));
        coutnet.connectToPin(coutreg.getPin("D"));
        assertEquals(1, coutnet.getSinkPins().size());
        assertEquals(carrycell.getPin("O"), coutnet.getSourcePin());
        assertTrue(coutnet.getSinkPins().contains(coutreg.getPin("D")));

        // connect and test the sum register output wire
        sumregnet.connectToPin(sumreg.getPin("Q"));
        sumregnet.connectToPin(sumport.getPin("PAD"));
        assertEquals(1, sumregnet.getSinkPins().size());
        assertEquals(sumreg.getPin("Q"), sumregnet.getSourcePin());
        assertTrue(sumregnet.getSinkPins().contains(sumport.getPin("PAD")));

        // connect and test the carry register output wire
        coutregnet.connectToPin(coutreg.getPin("Q"));
        coutregnet.connectToPin(coutport.getPin("PAD"));
        assertEquals(1, coutregnet.getSinkPins().size());
        assertEquals(coutreg.getPin("Q"), coutregnet.getSourcePin());
        assertTrue(coutregnet.getSinkPins().contains(coutport.getPin("PAD")));

        // test the dot file
        DotFilePrinter dotFilePrinter = new DotFilePrinter(design);
        // each node in the dot file can have a different id or line on each print
        // this avoids comparing the complex contents by comparing the line counts
        assertEquals(20, dotFilePrinter.getNetlistDotString().split("\r\n|\r|\n").length);
    }

    /**
     * test a LUT3 cell's input and output pins
     * @param cell the (LUT3) cell to test
     */
    public void testLUT3(Cell cell) {
        // assert that the cell really is a LUT3
        assertEquals("LUT3", cell.getLibCell().getName());
        // a LUT3 cell should have 3 inputs, and 1 output
        assertEquals(3, cell.getInputPins().size());
        assertEquals(1, cell.getOutputPins().size());
        // the INIT property of a LUT3 should have a length of 11 (8 for the bits, 3 for the number type
        assertNotNull(cell.getProperties().get("INIT"));
        assertEquals(11, cell.getProperties().get("INIT").getStringValue().length());
        for (CellPin pin : cell.getInputPins()) {
            // each input pin should begin with "I"
            assertEquals("I", pin.getName().substring(0, 1));
        }
        for (CellPin pin : cell.getOutputPins()) {
            // the only output pin should be named "O"
            assertEquals("O", pin.getName());
        }
    }

    /**
     * test an IPORT cell's input and output pins
     * @param cell the (IPORT) cell to test
     */
    public void testIPORT(Cell cell) {
        // assert that the cell really is an IPORT
        assertEquals("IPORT", cell.getLibCell().getName());
        // an IPORT cell should have no inputs and one output
        assertEquals(0, cell.getInputPins().size());
        assertEquals(1, cell.getOutputPins().size());
        // test that the output pin is a "PAD"
        for (CellPin pin : cell.getOutputPins()) {
            assertEquals("PAD", pin.getName());
        }
    }

    /**
     * test an OPORT cell's input and output pins
     * @param cell the (OPORT) cell to test
     */
    public void testOPORT(Cell cell) {
        // assert that the cell really is an OPORT
        assertEquals("OPORT", cell.getLibCell().getName());
        // an OPORT cell should have one input and no outputs
        assertEquals(1, cell.getInputPins().size());
        assertEquals(0, cell.getOutputPins().size());
        // test that the input pin is a "PAD"
        for (CellPin pin : cell.getInputPins()) {
            assertEquals("PAD", pin.getName());
        }
    }

    public void testFF(Cell cell) {
        assertEquals("FDRE", cell.getLibCell().getName());
        assertNotNull(cell.getProperties().get("INIT"));
        assertNotNull(cell.getProperties().get("SR"));
        assertEquals("INIT0", cell.getProperties().get("INIT").getStringValue());
        assertEquals("SRLOW", cell.getProperties().get("SR").getStringValue());
    }
}
