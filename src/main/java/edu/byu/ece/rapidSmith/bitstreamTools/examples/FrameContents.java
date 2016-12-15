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
package edu.byu.ece.rapidSmith.bitstreamTools.examples;

import joptsimple.OptionSet;
import edu.byu.ece.rapidSmith.bitstreamTools.configuration.FPGA;
import edu.byu.ece.rapidSmith.bitstreamTools.configuration.FrameAddressRegister;
import edu.byu.ece.rapidSmith.bitstreamTools.configurationSpecification.XilinxConfigurationSpecification;
import edu.byu.ece.rapidSmith.bitstreamTools.examples.support.BitstreamOptionParser;

/**
 * This method is used to print the contents of frames within the bitstream.
 *
 */
public class FrameContents {

	public static final String FAR_START_ADDRESS_OPTION = "s";
	public static final String FAR_START_ADDRESS_OPTION_HELP = 
		"The starting frame address to investigate (in hex)." +
		"If this option is not given, the first frame in the bitstream is used.";
	public static final String NUMBER_OF_FRAMES_OPTION = "n";
	public static final String NUMBER_OF_FRAMES_OPTION_HELP = "The number of frames to investigate (default is all frames)";


	/**
	 * Prints information about each FDRI write command (i.e., for each FDRI command,
	 * it prints a text location of the FAR address and the # of frames).
	 */
	public static void main(String[] args) {

		/////////////////////////////////////////////////////////////////////
		// Setup class and Options
		/////////////////////////////////////////////////////////////////////
				
		/** Setup parser **/
		BitstreamOptionParser cmdLineParser = new BitstreamOptionParser();
		cmdLineParser.addInputBitstreamOption();
		cmdLineParser.addPartNameOption();
		cmdLineParser.addHelpOption();
		cmdLineParser.addRawReadbackInputOption();
		
		cmdLineParser.accepts(FAR_START_ADDRESS_OPTION, FAR_START_ADDRESS_OPTION_HELP).
				withRequiredArg().ofType(String.class);		
		cmdLineParser.accepts(NUMBER_OF_FRAMES_OPTION, NUMBER_OF_FRAMES_OPTION_HELP).
			withRequiredArg().ofType(String.class);

		OptionSet options = null;
		try {
			options = cmdLineParser.parse(args);
		}
		catch(Exception e){
			System.err.println(e.getMessage());
			System.exit(1);			
		}		

		BitstreamOptionParser.printExecutableHeaderMessage(FrameContents.class);
				
		/////////////////////////////////////////////////////////////////////
		// Begin basic command line parsing
		/////////////////////////////////////////////////////////////////////
		cmdLineParser.checkHelpOptionExitOnHelpMessage(options);

		
		/////////////////////////////////////////////////////////////////////
		// 1. Parse bitstream
		/////////////////////////////////////////////////////////////////////
		FPGA fpga;
		
		fpga = cmdLineParser.createFPGAFromBitstreamOrReadbackFileExitOnError(options);

		/*
		if(options.has(READBACK_RAW_FILE_OPTION)){
			// get data from a readback file
			XilinxConfigurationSpecification part = cmdLineParser.getRequiredPartInfoExitOnError(options);
			String readbackfilename = (String) options.valueOf(READBACK_RAW_FILE_OPTION);		

			fpga = ReadbackFPGA.parseRawReadbackDataFromOptionsExitOnError(readbackfilename, 
					part);
			
		} else {
			Bitstream bitstream = cmdLineParser.parseBitstreamFromOptionsExitOnError(options, true);
			// get data from a bitstream
			/////////////////////////////////////////////////////////////////////
			// 2. Obtain part information
			/////////////////////////////////////////////////////////////////////
			XilinxConfigurationSpecification partInfo = cmdLineParser.getPartInfoExitOnError(options, bitstream, true);
			fpga = new FPGA(partInfo);		
			// Configure FPGA
			fpga.configureBitstream(bitstream);

		}
		*/
		
		/////////////////////////////////////////////////////////////////////
		// 3. Parameters
		/////////////////////////////////////////////////////////////////////
		
		// Start frame
		int startFrame = cmdLineParser.getIntegerStringExitOnError(options, FAR_START_ADDRESS_OPTION, 16, 0);

		// Number of frames
		XilinxConfigurationSpecification partInfo = fpga.getDeviceSpecification();
		int defaultNumberOfFrames = FrameAddressRegister.getNumberOfFrames(partInfo);
		int numberOfFrames = cmdLineParser.getIntegerStringExitOnError(options, NUMBER_OF_FRAMES_OPTION, 10, defaultNumberOfFrames);			

		System.out.println(fpga.getFrameContents(startFrame, numberOfFrames));
	}	
	
}

