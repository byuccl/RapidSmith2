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

package edu.byu.ece.rapidSmith.interfaces.vivado;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;

import edu.byu.ece.rapidSmith.design.subsite.ImplementationMode;

import static edu.byu.ece.rapidSmith.util.Exceptions.ParseException;

/**
 * This class is used for parsing and writing design.info files in a RSCP. <br>
 * Currently, the design.info file only two items: 
 *<ul>
 *<li> The part name the design is implemented on
 *<li> The implementation mode of the design (regular or out-of-context)
 *</ul>
 * 
 * The design.info file may be updated to add additional information in future releases.
 */
public class DesignInfoInterface {
	
	String part;
	ImplementationMode mode;
	
	/**
	 * Default constructor. Initializes the part to {@code NULL} and the the
	 * implementation mode to {@code REGULAR}
	 */
	public DesignInfoInterface() {
		part = null;
		mode = ImplementationMode.REGULAR;
	}
	
	/**
	 * Parses the design.info file of a RSCP. Currently, this parser looks for two tokens:
	 * <ul>
	 * <li> part=partname
	 * <li> mode=implementationMode
	 * </ul>
	 * 
	 * Other token types may be added in the future. The functions {@link DesignInfoInterface#getPart()} and
	 * {@link DesignInfoInterface#getMode()} can be used to obtain the values parsed from this file.
	 * 
	 * @param rscp RSCP file path
	 * @throws IOException if the specified design.info file cannot be found
	 */
	public void parse(Path rscp) throws IOException {
	
		try (BufferedReader br = new BufferedReader(new FileReader(rscp.resolve("design.info").toString()))) {
			String line;
			while ((line = br.readLine()) != null) {
				String[] toks = line.split("=");
				assert (toks.length==2) : "Incorrect format for key/value pair in design.info file. Expected \"key=value\"";
				
				switch (toks[0]) {
					case "part": setPart(toks[1]);
						break;
					case "mode": setMode(toks[1]);
						break;
					default: throw new ParseException("Unknown token found in design.info file \"" + toks[0] + "\""); 
				}
			}
		}
	}
	
	// Getter and Setter functions
	public void setPart(String part) {
		this.part = part;
	}
	
	public void setMode(String mode) {
		this.mode = ImplementationMode.valueOf(mode.toUpperCase());
	}
	
	public void setMode(ImplementationMode mode) {
		this.mode = mode;
	}
	
	public String getPart(){
		return this.part;
	}
	
	public ImplementationMode getMode(){
		return this.mode;
	}
	
	/*
	/**
	 * Reads the part name from the TINCR checkpoint file
	 *  
	 * @param partInfoFile Placement.xdc file
	 * @param design Design to apply placement
	 * @param device Device which the design is implemented on
	 * @throws IOException
	 *
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
	*/
	
	/**
	 * Creates a design.info file given a partName<br>
	 * 
	 * TODO: Update this file to output the mode as well?
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
