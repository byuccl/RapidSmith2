package edu.byu.ece.rapidSmith.interfaces.vivado;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import edu.byu.ece.rapidSmith.design.subsite.Cell;
import edu.byu.ece.rapidSmith.design.subsite.CellDesign;
import edu.byu.ece.rapidSmith.design.subsite.CellPin;
import edu.byu.ece.rapidSmith.device.Bel;
import edu.byu.ece.rapidSmith.device.BelPin;
import edu.byu.ece.rapidSmith.device.Device;
import edu.byu.ece.rapidSmith.device.Site;
import edu.byu.ece.rapidSmith.device.SiteType;
import edu.byu.ece.rapidSmith.util.MessageGenerator;

import static edu.byu.ece.rapidSmith.util.Exceptions.ParseException;

/**
 * This class is used for parsing and writing design.info files in a TINCR checkpoint. <br>
 * design.info files are used to specify the physical location of cells in a Vivado netlist.
 * 
 * @author Thomas Townsend
 *
 */
public class DesignInfoInterface {

	/**
	 * Reads the part name from the TINCR checkpoint file
	 *  
	 * @param partInfoFile Placement.xdc file
	 * @param design Design to apply placement
	 * @param device Device which the design is implemented on
	 * @throws IOException
	 */
	/*
	 * Parses the Vivado part name from the "design.info" file of a TINCR checkpoint 
	 */
	public static String parseInfoFile (String tcp) throws IOException {
		
		BufferedReader br = null;
		String part = "";
		
		try {
			br = new BufferedReader(new FileReader(tcp));
			String line = br.readLine();
			part = line.split("=")[1];
		}
		catch (IndexOutOfBoundsException e) {
			MessageGenerator.briefErrorAndExit("[ERROR]: No part name found in the design.info file.");
		}
		finally {
			if (br != null)
				br.close();
		}
		
		return part;
	}
	
	/**
	 * Creates a design.info file given a partName<br>
	 * 
	 * @param partInfoOut Output design.info file location
	 * @param partname Name of part this design is mapped to
	 * @throws IOException
	 */
	public static void writeInfoFile(String partInfoOut, String partName) throws IOException {
		
		BufferedWriter fileout = new BufferedWriter (new FileWriter(partInfoOut));
		
		fileout.write("part=" + partName + "\n");
		fileout.close();
	}
	
}
