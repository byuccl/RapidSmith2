/*
 * Copyright (c) 2018 Brigham Young University
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
package design.tcpExport;

import edu.byu.ece.edif.core.EdifCell;
import edu.byu.ece.edif.core.EdifCellInstance;
import edu.byu.ece.edif.core.EdifEnvironment;
import edu.byu.ece.edif.util.parse.EdifParser;
import edu.byu.ece.edif.util.parse.ParseException;
import edu.byu.ece.rapidSmith.RSEnvironment;
import edu.byu.ece.rapidSmith.design.subsite.Cell;
import edu.byu.ece.rapidSmith.design.subsite.CellDesign;
import edu.byu.ece.rapidSmith.design.subsite.Property;
import edu.byu.ece.rapidSmith.design.subsite.PropertyType;
import edu.byu.ece.rapidSmith.interfaces.vivado.EdifInterface;
import edu.byu.ece.rapidSmith.interfaces.vivado.VivadoCheckpoint;
import edu.byu.ece.rapidSmith.interfaces.vivado.VivadoInterface;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;


public class EdifCellPropertyTest {
    private static final Path rscpDirectory = RSEnvironment.defaultEnv().getEnvironmentPath()
            .resolve("src")
            .resolve("test")
            .resolve("resources")
            .resolve("ImportTests")
            .resolve("RSCP")
            .resolve("artix7")
            .resolve("count16.rscp");

    private static final Path testEdifPath = rscpDirectory.resolve("edifTestOut.edf");
    private static CellDesign design;

    @BeforeAll
    public static void initializeTest() throws IOException {
        // Get the pieces out of the checkpoint for use in manipulating it in following tests
        VivadoCheckpoint vcp = VivadoInterface.loadRSCP(rscpDirectory.toString());
        design = vcp.getDesign();
        Files.deleteIfExists(testEdifPath);
    }


    /**
     * Cleans up after the test by deleting the test EDIF file.
     */
    @AfterAll
    public static void cleanupTest() {
        // The file will not be deleted on Windows because Windows does not allow you to delete
        // a file that has an open handle to it. The EdifParser in BYU's EDIF tools leave a file
        // stream open, causing this issue.
        File tmpFile = new File(testEdifPath.toString());
        tmpFile.delete();
    }

    @Test
    public void designPropertyTest() throws IOException {
        Cell cell = design.getCell("q[8]_i_2");
        Property designProp = new Property("DesignPropKey", PropertyType.DESIGN, "DesignPropVal");
        cell.getProperties().add(designProp);
        EdifInterface.writeEdif(testEdifPath.toString(), design);

        try {
            EdifEnvironment env = EdifParser.translate(testEdifPath.toString());
            EdifCell topLevelCell = env.getTopCell();
            EdifCellInstance edifCell = topLevelCell.getCellInstance("q_8__i_2");
            edu.byu.ece.edif.core.Property prop = edifCell.getProperty("DesignPropKey");
            cell.getProperties().remove("DesignPropKey");
            assertNull(prop, "Property with DESIGN type not removed.");
        } catch (ParseException e) {
            e.printStackTrace();
        }

    }

    @Test
    public void userPropertyTest() throws IOException {
        Cell cell = design.getCell("q[8]_i_2");
        Property userProp = new Property("userPropKey", PropertyType.USER, "userPropVal");
        cell.getProperties().add(userProp);
        EdifInterface.writeEdif(testEdifPath.toString(), design);

        try {
            EdifEnvironment env = EdifParser.translate(testEdifPath.toString());
            EdifCell topLevelCell = env.getTopCell();
            EdifCellInstance edifCell = topLevelCell.getCellInstance("q_8__i_2");
            edu.byu.ece.edif.core.Property prop = edifCell.getProperty("userPropKey");
            cell.getProperties().remove("userPropKey");
            assertNull(prop, "Property with USER type not removed.");
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void belpropPropertyTest() throws IOException {
        Cell cell2 = design.getCell("q[8]_i_2");
        Property belProp = new Property("belPropKey", PropertyType.BELPROP, "belPropVal");
        cell2.getProperties().add(belProp);
        EdifInterface.writeEdif(testEdifPath.toString(), design);

        try {
            EdifEnvironment env = EdifParser.translate(testEdifPath.toString());
            EdifCell topLevelCell = env.getTopCell();
            EdifCellInstance edifCell = topLevelCell.getCellInstance("q_8__i_2");
            edu.byu.ece.edif.core.Property prop = edifCell.getProperty("belPropKey");
            cell2.getProperties().remove("belPropKey");
            assertNull(prop, "Property with BELPROP type not removed.");
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void edifPropertyTest() throws IOException {
        Cell cell2 = design.getCell("q[8]_i_2");
        Property edifProp = new Property("edifPropKey", PropertyType.EDIF, "edifPropVal");
        cell2.getProperties().add(edifProp);
        EdifInterface.writeEdif(testEdifPath.toString(), design);

        try {
            EdifEnvironment env = EdifParser.translate(testEdifPath.toString());
            EdifCell topLevelCell = env.getTopCell();
            EdifCellInstance edifCell = topLevelCell.getCellInstance("q_8__i_2");
            edu.byu.ece.edif.core.Property prop = edifCell.getProperty("edifPropKey");
            cell2.getProperties().remove("edifPropKey");
            assertNotNull(prop, "Property with EDIF type not included in exported EDIF.");
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

}