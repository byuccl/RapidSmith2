/*
 * Copyright (c) 2010-2011 Brigham Young University
 * 
 * This file is part of the BYU RapidSmith Tools.
 * 
 * BYU RapidSmith Tools is free software: you may redistribute it 
 * and/or modify it under the terms of the GNU General Public License 
 * as published by the Free Software Foundation, either version 2 of 
 * the License, or (at your option) any later version.
 * 
 * BYU RapidSmith Tools is distributed in the hope that it will be 
 * useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU 
 * General Public License for more details.
 * 
 * A copy of the GNU General Public License is included with the BYU 
 * RapidSmith Tools. It can be found at doc/gpl2.txt. You may also 
 * get a copy of the license at <http://www.gnu.org/licenses/>.
 * 
 */
package edu.byu.ece.rapidSmith.bitstreamTools.configuration;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;

import edu.byu.ece.rapidSmith.bitstreamTools.bitstream.BitstreamParser;
import edu.byu.ece.rapidSmith.bitstreamTools.configurationSpecification.XilinxConfigurationSpecification;

/**
 * Generates a configured FPGA object from a readback file. Since a readback bitstream
 * does not have any of the packet information, we cannot use the bitstream parser
 * to parser the bitstream into a data structure.
 * 
 */
public class ReadbackFPGA extends FPGA {

	public ReadbackFPGA(File file, XilinxConfigurationSpecification part) throws IOException { 
		super(part);
		FileInputStream input = new FileInputStream(file);
	    BufferedInputStream buffer = new BufferedInputStream(input);   
		loadFPGAFromReadbackData(buffer, part);
	}

	public ReadbackFPGA(BufferedInputStream istream, XilinxConfigurationSpecification part) throws IOException { 
		super(part);
		loadFPGAFromReadbackData(istream, part);
	}
	
	/** Generate an FPGA object from a readback file. **/
	public static FPGA parseRawReadbackDataFromOptionsExitOnError(String filename, 
			XilinxConfigurationSpecification part) {
		FPGA fpga = null;
		try {
			File file = new File(filename);
			fpga = new ReadbackFPGA(file,part);
		} catch (IOException e) {
			System.err.println(e);
		}
		return fpga;
	}
	
	
	protected void loadFPGAFromReadbackData(BufferedInputStream istream, 
			XilinxConfigurationSpecification part) 
		throws IOException {
		loadFPGAFromReadbackData(istream, part, 0);
	}
	
	protected void loadFPGAFromReadbackData(BufferedInputStream istream, 
			XilinxConfigurationSpecification part, int far) 
		throws IOException {
		
		// Read the bytes into a list
		int numBytes = istream.available();
		ArrayList<Byte> bytes = new ArrayList<Byte> (numBytes);
	    for(int i = 0; i < numBytes; i++) {
	    		bytes.add((byte)istream.read());
	    }
	    istream.close();
	    
	    
	    // Create a list 
	    int numWords = bytes.size() / 4;
	    ArrayList<Integer> data = new ArrayList<Integer>(numWords);
		for (int j = 0; j < numWords; j++) {
		    data.add(BitstreamParser.getWordAsInt(bytes, (j*4)));
		}
		
	    // Remove the first frame of data: this appears to be extra (based on visual
	    // comparisions)
	    for (int i = 0; i < part.getFrameSize(); i++)
	    	data.remove(i);

	    // Configure bitstream
	    setFAR(far);
	    configureWithData(data);
	    
	}

	
}
