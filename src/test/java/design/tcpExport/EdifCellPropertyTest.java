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
import edu.byu.ece.rapidSmith.interfaces.vivado.VivadoEdifInterface;
import edu.byu.ece.rapidSmith.interfaces.vivado.VivadoCheckpoint;
import edu.byu.ece.rapidSmith.interfaces.vivado.VivadoInterface;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
/**tests the {@link VivadoEdifInterface} to verify that edif Property lists only contain EDIF type properties
  *Only EDIF Property Types are allowed in the netlist.edf to be imported into Vivado using Tincr.
  * @author Dallon Glick
  * @author Jesse Grigg
  *
  */

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
        /**Get the pieces out of the checkpoint for use in manipulating it in following tests*/
        VivadoCheckpoint vcp = VivadoInterface.loadRSCP(rscpDirectory.toString());
        design = vcp.getDesign();
        Files.deleteIfExists(testEdifPath);
    }


    /**
    * Cleans up after the test by deleting the test EDIF file.
    */
    @AfterAll
    public static void cleanupTest() {
        /** The file will not be deleted on Windows because Windows does not allow you to delete
         *a file that has an open handle to it. The EdifParser in BYU's EDIF tools leave a file
         *stream open, causing this issue.
         */
        File tmpFile = new File(testEdifPath.toString());
                tmpFile.delete();
        }

    /**Test to make sure output netlist.edf does not have DESIGN type properties*/
    @Test
    @DisplayName("EdifCellPropertyTest DESIGN Property Test")
    public void designPropertyTest() throws IOException {
        Cell cell = design.getCell("q[8]_i_2");
        Property designProp = new Property("DesignPropKey", PropertyType.DESIGN, "DesignPropVal");
        cell.getProperties().add(designProp);
        VivadoEdifInterface vivadoEdifInterface = new VivadoEdifInterface();
        vivadoEdifInterface.writeEdif(testEdifPath.toString(), design);

        try {
            EdifEnvironment env = EdifParser.translate(testEdifPath.toString());
            EdifCell topLevelCell = env.getTopCell();
            EdifCellInstance edifCell = topLevelCell.getCellInstance("q_8__i_2");
            edu.byu.ece.edif.core.Property prop = edifCell.getProperty("DesignPropKey");
            cell.getProperties().remove("DesignPropKey");
            assertNull(prop, "Property with DESIGN type not removed.");
        }
        catch (ParseException e) {
            e.printStackTrace();
        }

    }

    /**Test to make sure output netlist.edf does not have USER type properties*/
    @Test
    @DisplayName("EdifCellPropertyTest USER Property Test")
    public void userPropertyTest() throws IOException {
        Cell cell = design.getCell("q[8]_i_2");
        Property userProp = new Property("userPropKey", PropertyType.USER, "userPropVal");
        cell.getProperties().add(userProp);
        VivadoEdifInterface vivadoEdifInterface = new VivadoEdifInterface();
        vivadoEdifInterface.writeEdif(testEdifPath.toString(), design);

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

    /**Test to make sure output netlist.edf does not have BELPROP type properties*/
    @Test
    @DisplayName("EdifCellPropertyTest BELPROP Property Test")
    public void belpropPropertyTest() throws IOException {
        Cell cell2 = design.getCell("q[8]_i_2");
        Property belProp = new Property("belPropKey", PropertyType.BELPROP, "belPropVal");
        cell2.getProperties().add(belProp);
        VivadoEdifInterface vivadoEdifInterface = new VivadoEdifInterface();
        vivadoEdifInterface.writeEdif(testEdifPath.toString(), design);
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

    /**Test to make sure output netlist.edf does have EDIF type properties. These property
    *types work when importing netlist back into Vivado
    */
    @Test
    @DisplayName("EdifCellPropertyTest EDIF Property Test")
    public void edifPropertyTest() throws IOException {
        Cell cell2 = design.getCell("q[8]_i_2");
        Property edifProp = new Property("edifPropKey", PropertyType.EDIF, "edifPropVal");
        cell2.getProperties().add(edifProp);
        VivadoEdifInterface vivadoEdifInterface = new VivadoEdifInterface();
        vivadoEdifInterface.writeEdif(testEdifPath.toString(), design);

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
