package edu.byu.ece.rapidSmith.interfaces.vivado;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import static edu.byu.ece.rapidSmith.util.Exceptions.ParseException;

/**
 * This class is used for parsing and writing design.info files in a TINCR checkpoint. <br>
 * Currently, the design.info file only contains the part-name of the TCP device. <br>
 * We may add to what is in this file in the future
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
	public static String parseInfoFile (String tcp) throws IOException {
		
		BufferedReader br = null;
		String part;
		
		try {
			br = new BufferedReader(new FileReader(tcp));
			String line = br.readLine();
			part = line.split("=")[1];
		}
		catch (IndexOutOfBoundsException e) {
			throw new ParseException("No part name found in the design.info file.");
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
