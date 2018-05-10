package edu.byu.ece.rapidSmith.interfaces.vivado;

import static org.junit.jupiter.api.Assertions.*;

//import com.sun.java.util.jar.pack.Package;
import edu.byu.ece.edif.core.EdifCell;
import edu.byu.ece.edif.core.EdifCellInstance;
import edu.byu.ece.edif.core.EdifEnvironment;
import edu.byu.ece.edif.util.parse.EdifParser;
import edu.byu.ece.edif.util.parse.ParseException;
import edu.byu.ece.rapidSmith.design.subsite.*;
import edu.byu.ece.rapidSmith.device.Device;
import edu.byu.ece.rapidSmith.RSEnvironment;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.*;
import java.io.File;

public class EdifInterfaceTest {
    private static final Path testDirectory= RSEnvironment.defaultEnv().getEnvironmentPath()
            .resolve("src")
            .resolve("test")
            .resolve("resources")
            .resolve("ImportTests")
            .resolve("RSCP")
            .resolve("artix7")
            .resolve("count16.rscp");
    private static final Path testFilePath = testDirectory.resolve("edifTestOut.edf");
    private static String checkpointIn = testDirectory.toString();
    private static String testFileName = testDirectory.resolve("edifTestOut.edf").toString();

    private static CellDesign design = null;
    private static Device device = null;
    private static CellLibrary libCells = null;

    @BeforeAll
    public static void initializeTest() throws IOException{
        // Load in in a TINCR checkpoint .rscp
        // Get the pieces out of the checkpoint for use in manipulating it in following tests
        VivadoCheckpoint vcp = VivadoInterface.loadRSCP(checkpointIn);
        design = vcp.getDesign();//grab design, etc from RSCP
        device= vcp.getDevice();
        libCells = vcp.getLibCells();
    }

    @Test
    public void DESIGNpropertyTest() throws IOException {
        Cell cell = design.getCell("q[8]_i_2");//grab a cell
        Property coolTest = new Property("coolnessFactor", PropertyType.DESIGN, "NotCool");//create made up property
        cell.getProperties().add(coolTest); //add property to cell
        File edifOutFile = new File(testFileName);
        EdifInterface.writeEdif(testFileName, design);// write a new Edif file

        try {
            EdifEnvironment env = EdifParser.translate(testFileName);
            EdifCell topLevelCell = env.getTopCell();
            EdifCellInstance edifCell = topLevelCell.getCellInstance("q_8__i_2");
            edu.byu.ece.edif.core.Property prop = edifCell.getProperty("coolnessFactor");
            cell.getProperties().remove("coolnessFactor");
            System.out.println(edifOutFile.getAbsolutePath());

            //delete(testFilePath);
            assertNull(prop, "Property with DESIGN type not removed. Error!");


        } catch (ParseException e) {
            e.printStackTrace();
        }



    }
    @Test
    public void USERPropertyTest() throws IOException{
            Cell cell1 = design.getCell("q[8]_i_2");//grab a cell
            Property coolTest1 = new Property("coolnessFactor", PropertyType.USER, "NotCool");//create made up property
            cell1.getProperties().add(coolTest1); //add property to cell
            File edifOutFile = new File(testFileName);
            EdifInterface.writeEdif(testFileName, design);// write a new Edif file
            try {
                EdifEnvironment env = EdifParser.translate(testFileName);
                EdifCell topLevelCell = env.getTopCell();
                EdifCellInstance edifCell = topLevelCell.getCellInstance("q_8__i_2");
                edu.byu.ece.edif.core.Property prop = edifCell.getProperty("coolnessFactor");
                cell1.getProperties().remove("coolnessFactor");
                edifOutFile.delete();
                assertNull(prop, "Property with USER type not removed. Error!");
            } catch (ParseException e) {
                e.printStackTrace();
            }

    }

    @Test
    public void BELPROPPropertyTest() throws IOException{
        Cell cell2 = design.getCell("q[8]_i_2");//grab a cell
        Property coolTest2 = new Property("coolnessFactor", PropertyType.BELPROP, "NotCool");//create made up property
        cell2.getProperties().add(coolTest2); //add property to cell
        File edifOutFile = new File(testFileName);
        EdifInterface.writeEdif(testFileName, design);// write a new Edif file
        try {
            EdifEnvironment env = EdifParser.translate(testFileName);
            EdifCell topLevelCell = env.getTopCell();
            EdifCellInstance edifCell = topLevelCell.getCellInstance("q_8__i_2");
            edu.byu.ece.edif.core.Property prop = edifCell.getProperty("coolnessFactor");
            cell2.getProperties().remove("coolnessFactor");
//            if(edifOutFile.delete()){
//                System.out.println("File Deleted");
//            }
            assertNull(prop, "Property with BELPROP type not removed. Error!");


        } catch (ParseException e) {
            e.printStackTrace();
        }

    }

}