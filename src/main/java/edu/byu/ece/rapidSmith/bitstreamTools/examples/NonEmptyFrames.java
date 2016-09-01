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
package edu.byu.ece.rapidSmith.bitstreamTools.examples;

import joptsimple.OptionSet;
import edu.byu.ece.rapidSmith.bitstreamTools.bitstream.Bitstream;
import edu.byu.ece.rapidSmith.bitstreamTools.configuration.FPGA;
import edu.byu.ece.rapidSmith.bitstreamTools.configuration.Frame;
import edu.byu.ece.rapidSmith.bitstreamTools.configuration.FrameAddressRegister;
import edu.byu.ece.rapidSmith.bitstreamTools.configurationSpecification.XilinxConfigurationSpecification;
import edu.byu.ece.rapidSmith.bitstreamTools.examples.support.BitstreamOptionParser;

/**
 * This method is used to print the contents of non empty configured frames within the bitstream.
 */
public class NonEmptyFrames {

	@SuppressWarnings("unused")
	public static void main(String[] args) {

		/////////////////////////////////////////////////////////////////////
		// Setup class and Options
		/////////////////////////////////////////////////////////////////////
		String PRINT_DETAIL = "d";
		
		/** Setup parser **/
		BitstreamOptionParser cmdLineParser = new BitstreamOptionParser();
		cmdLineParser.addInputBitstreamOption();
		cmdLineParser.addPartNameOption();
		cmdLineParser.addHelpOption();
		cmdLineParser.accepts(PRINT_DETAIL, "Prints all details");

		OptionSet options = cmdLineParser.parseArgumentsExitOnError(args);

		BitstreamOptionParser.printExecutableHeaderMessage(NonEmptyFrames.class);
		
		/////////////////////////////////////////////////////////////////////
		// Begin basic command line parsing
		/////////////////////////////////////////////////////////////////////		
		cmdLineParser.checkHelpOptionExitOnHelpMessage(options);
		
		/////////////////////////////////////////////////////////////////////
		// 1. Parse bitstream
		/////////////////////////////////////////////////////////////////////
		Bitstream bitstream = cmdLineParser.parseRequiredBitstreamFromOptionsExitOnError(options, true);
		
		/////////////////////////////////////////////////////////////////////
		// 2. Obtain part information
		/////////////////////////////////////////////////////////////////////
		XilinxConfigurationSpecification partInfo = cmdLineParser.getPartInfoExitOnError(options, bitstream, true);
		
		boolean printDetail =(options.has(PRINT_DETAIL));

		
		// Create FPGA object
		FPGA fpga = new FPGA(partInfo);		
		// Configure FPGA
		fpga.configureBitstream(bitstream);

		FrameAddressRegister far = new FrameAddressRegister(partInfo, 0);
		
		boolean empty = false;
		boolean nonempty = false;
		int startFar = 0;
		int count = 0;

		while (far.validFARAddress()) {
			Frame f = fpga.getFrame(far);			
			if (!f.isConfigured() || f.getData().isEmpty()) {
				if (printDetail) {
					if (f.isConfigured())
						System.out.println(far + " = EMPTY");
				}
				if (empty) {
					// Another empty: add count
					count++;
				} else {
					if (!nonempty) {
						// In no state. Initialize
						startFar = far.getAddress();
						count = 1;
						empty = true;
					} else {
						// End of non empty state
						System.out.println("0x"+Integer.toHexString(startFar)+" ("+count+" frames) "+far);
						empty = true; nonempty = false;
						count = 1;
						startFar = far.getAddress();
					}
				}
			} else {
				if (nonempty) {
					// another non-empty
					count++;
				} else {
					if (!empty) {
						// In no state. Initialize
						startFar = far.getAddress();
						count = 1;
						nonempty = true;
					} else {						
						// End of empty state
						if (false)
							System.out.println("FAR=0x"+Integer.toHexString(startFar)+" count="+count+" empty "+far);
						empty = false; nonempty = true;
						count = 1;
						startFar = far.getAddress();
					}
				}
				if (printDetail) System.out.println(far);
			}
			far.incrementFAR();
		}
	}


	
}

