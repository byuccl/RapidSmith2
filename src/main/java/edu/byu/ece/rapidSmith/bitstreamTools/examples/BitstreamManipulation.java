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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import joptsimple.OptionSet;
import edu.byu.ece.rapidSmith.bitstreamTools.bitstream.Bitstream;
import edu.byu.ece.rapidSmith.bitstreamTools.bitstream.BitstreamHeader;
import edu.byu.ece.rapidSmith.bitstreamTools.bitstream.BitstreamParser;
import edu.byu.ece.rapidSmith.bitstreamTools.bitstream.PacketList;
import edu.byu.ece.rapidSmith.bitstreamTools.configuration.FPGA;
import edu.byu.ece.rapidSmith.bitstreamTools.configuration.FPGAOperation;
import edu.byu.ece.rapidSmith.bitstreamTools.configuration.Frame;
import edu.byu.ece.rapidSmith.bitstreamTools.configurationSpecification.XilinxConfigurationSpecification;
import edu.byu.ece.rapidSmith.bitstreamTools.examples.support.BitstreamOptionParser;

/**
 * Implements the same bitstream manipulation routines that Ben Sellers created in
 * his original bitmanip tool. This class demonstrates how to perform the same
 * functions using the new API.
 *
 */
public class BitstreamManipulation {
	
	public static final String XOR_STRING = "xor";
	public static final String OVERWRITE_STRING = "overwrite";
	public static final String CLEAR_STRING = "clear";
	public static final String AND_STRING = "and";
	public static final String NOT_STRING = "not";
	public static final String OR_STRING = "or";
	
	public static final String BRAM_OPTION_STRING = "b";
	public static final String PARTIAL_OPTION_STRING = "t";
	public static final String FULL_OPTION_STRING = "f";
	public static final String CONDENSE_OPTION_STRING = "c";

	public static final String NEW_ALGORITHM = "new";

	public static final String APPEND_OPTION_STRING = "append";
	public static final String APPEND_OPTION_STRING_HELP = 
		"Append the packets of the operational bitstream to the bitstream of the input.";
	
	public static boolean DEBUG = false;
	
	public static void main(String[] args) {

		/////////////////////////////////////////////////////////////////////
		// Begin command line parsing
		/////////////////////////////////////////////////////////////////////		
		BitstreamOptionParser cmdLineParser = new BitstreamOptionParser();
		cmdLineParser.addInputBitstreamOption();
		cmdLineParser.addPartNameOption();
		cmdLineParser.addHelpOption();
		cmdLineParser.addOutputBitstreamOption();
		cmdLineParser.addDebugOption();
		
		// Add More commands
		cmdLineParser.accepts(BRAM_OPTION_STRING, "Generate BRAM");
		cmdLineParser.accepts(PARTIAL_OPTION_STRING, "Generate Partial");
		cmdLineParser.accepts(FULL_OPTION_STRING, "Generate Full bitstream");
		//cmdLineParser.accepts(CONDENSE_OPTION_STRING, "Generate condensed bitstream");
		cmdLineParser.accepts(XOR_STRING, 
				"Perform XOR operation with operational bitfile");
		cmdLineParser.accepts(OVERWRITE_STRING, 
		       "Perform overwrite operation with operational bitfile");
		cmdLineParser.accepts(CLEAR_STRING, 
			"Perform clear operation with operational bitfile");
		cmdLineParser.accepts(AND_STRING, 
			"Perform and operation with operational bitfile");
		cmdLineParser.accepts(NOT_STRING, 
			"Perform not operation with operational bitfile");
		cmdLineParser.accepts(OR_STRING, 
			"Perform or operation with operational bitfile");
		cmdLineParser.accepts("r","Name of operational bitfile").withRequiredArg().ofType(String.class);
		cmdLineParser.accepts(NEW_ALGORITHM, "Use new algorithms for bit manipulation");
		cmdLineParser.accepts(APPEND_OPTION_STRING, APPEND_OPTION_STRING_HELP);
		
		// Parse arguments
		OptionSet options = cmdLineParser.parseArgumentsExitOnError(args);
		// Print executable header
		BitstreamOptionParser.printExecutableHeaderMessage(BitstreamManipulation.class);
		// Check for and print help
		cmdLineParser.checkHelpOptionExitOnHelpMessage(options);
		DEBUG = cmdLineParser.debugEnabled(options);
		
		/////////////////////////////////////////////////////////////////////
		// 1. Parse bitstream
		/////////////////////////////////////////////////////////////////////
		Bitstream bitstream = cmdLineParser.parseRequiredBitstreamFromOptionsExitOnError(options, true);
		
		/////////////////////////////////////////////////////////////////////
		// 2. Obtain part information
		/////////////////////////////////////////////////////////////////////
		XilinxConfigurationSpecification partInfo = cmdLineParser.getPartInfoExitOnError(options, bitstream, true);
		
		// 3. Create FPGA object
		FPGA fpga = new FPGA(partInfo);		
		// Configure FPGA
		fpga.configureBitstream(bitstream);
		
		String outputBitstreamFileName = cmdLineParser.getOutputFileNameStringExitOnError(options);
		
		/////////////////////////////////////////////////////////////////////
		// Perform Bitstreams Operations
		/////////////////////////////////////////////////////////////////////

		if (options.has(XOR_STRING) || options.has(OVERWRITE_STRING) ||options.has(CLEAR_STRING) ||
				options.has(AND_STRING) ||options.has(NOT_STRING) ||options.has(OR_STRING) 
				|| options.has(APPEND_OPTION_STRING)) { 

			if (!options.has("r")) {
				System.err.println("Operational bitfile name needed for bitstream operation");
				cmdLineParser.printUsageAndExit();
			}
			String operationalBitstreamFileName = (String) options.valueOf("r");
			
			// load operational bitstream (this is an odd way of doing it but I am copying Ben's code)
			Bitstream opBitstream = BitstreamParser.parseBitstreamExitOnError(operationalBitstreamFileName);			
			System.out.print("Successfully loaded operational bitstream - ");
			
			// TODO: Make sure the two bitstreams are the same size. Same part?			
			FPGA fpga2 = new FPGA(partInfo);
			fpga2.configureBitstream(opBitstream);

			
			if(options.has(XOR_STRING))
			{
				FPGAOperation.operation(fpga, fpga2, FPGAOperation.OPERATORS.XOR );
				System.out.println("XOR operation performed"); // use this to find differences in BRAM data.
			}
			else if(options.has(AND_STRING))
			{
				FPGAOperation.operation(fpga, fpga2, FPGAOperation.OPERATORS.AND );
				System.out.println("AND operation performed");
			}
			else if(options.has(OR_STRING))
			{
				FPGAOperation.operation(fpga, fpga2, FPGAOperation.OPERATORS.OR );
				System.out.println("OR operation performed");
			}
			else if(options.has(NOT_STRING))
			{
				FPGAOperation.operation(fpga, fpga2, FPGAOperation.OPERATORS.NOT );
				System.out.println("NOT operation performed");
			}
			else if(options.has(OVERWRITE_STRING))
			{
				fpga.configureBitstream(opBitstream);
				System.out.println("Overwrite operation performed");
			}
			else if(options.has(CLEAR_STRING))
			{	
				// Find all of the frames that are configured on fpga2
				ArrayList<Frame> fpga2configuredFrames = fpga2.getConfiguredFrames();
				
				// Clear all the corresponding frames on fpga1
				for (Frame fd : fpga2configuredFrames) {
					fd.clear();
				}
				System.out.println("Clear operation performed\n");
			}
			else if(options.has(APPEND_OPTION_STRING)) {
				// Append the packets of the operational bitstream to the packets of the original bitstream
				PacketList opPackets = opBitstream.getPackets();
				PacketList inPackets = bitstream.getPackets();
				inPackets.addAll(opPackets);
				if (writeBitstreamToBIT(bitstream, outputBitstreamFileName) == 0) {
					System.out.println("Generated BRAM Bitstream:"+outputBitstreamFileName);
				} else {
					System.err.println("Problem generating BRAM bitstream");
					System.exit(1);
				}				

				System.out.println("Append operation performed");
			}
		}
				
		/////////////////////////////////////////////////////////////////////
		// Perform Bitstreams Write Operations
		/////////////////////////////////////////////////////////////////////

		// create the bitstream
		Bitstream newBitstream = null;
		BitstreamHeader newHeader;

		

		if (options.has(NEW_ALGORITHM)) {
			// TODO
			// for the new algorithm, create a new header with different values
			newHeader = bitstream.getHeader();
		} else {
			// for the old algorithm, copy the header from the original bitstream
			newHeader = bitstream.getHeader();
		}
		
		// BRAM operation		
		if (options.has(BRAM_OPTION_STRING)) {
			if (options.has(NEW_ALGORITHM)) {
				// New algorithm
				System.err.println("New Algorithm not yet supported");
				System.exit(1);
			} else {
				// Use old algorithm
				newBitstream = partInfo.getBitstreamGenerator().createPartialBRAMBitstream(fpga, newHeader);
			}
			if (writeBitstreamToBIT(newBitstream, outputBitstreamFileName) == 0) {
				System.out.println("Generated BRAM Bitstream:"+outputBitstreamFileName);
			} else {
				System.err.println("Problem generating BRAM bitstream");
				System.exit(1);
			}				
		}
		
		// PARTIAL operation
		if (options.has(PARTIAL_OPTION_STRING)) {
			if (newBitstream != null) {
				System.err.println("Only one write can be performed.");
				System.exit(1);
			}
			if (options.has(NEW_ALGORITHM)) {
				// New algorithm (currently same as old algorithm)
				newBitstream = partInfo.getBitstreamGenerator().createPartialBitstream(fpga, newHeader);
			} else {
				// Use old algorithm
				newBitstream = partInfo.getBitstreamGenerator().createPartialBitstream(fpga, newHeader);
			}
			if (writeBitstreamToBIT(newBitstream, outputBitstreamFileName) == 0) {
				System.out.println("Generated Partial Bitstream:"+outputBitstreamFileName);
			} else {
				System.err.println("Problem generating Partial bitstream");
				System.exit(1);
			}				

		}

		if (options.has(FULL_OPTION_STRING)) {
			if (newBitstream != null) {
				System.err.println("Only one write can be performed. Option -f ignored");
				System.exit(1);
			}
			newBitstream = partInfo.getBitstreamGenerator().createFullBitstream(fpga, bitstream.getHeader());
			if (writeBitstreamToBIT(newBitstream, outputBitstreamFileName) == 0) {
				System.out.println("Generated Full Bitstream:"+outputBitstreamFileName);
			} else {
				System.err.println("Problem generating Full bitstream");
				System.exit(1);
			}				
		}

		if (options.has(CONDENSE_OPTION_STRING)) {
			if (newBitstream != null) {
				System.err.println("Only one write can be performed. Option -t ignored");
				System.exit(1);
			}
			// TODO
			System.err.println("Condense Operation is currently not supported");
			System.exit(1);
			/*
			if(fpga.WriteCondensedBitstreamToBIT(outputBitstreamFileName)) {
				System.out.println("Generated Condensed Bitstream:"+outputBitstreamFileName);
				bitstreamWritten = true;
			} else {
				System.err.println("Problem generating condensed bitstream");
				System.exit(1);
			}	
			*/			
		}
		
		// Check for an operation: must have at least one operation specified
		if (newBitstream == null) {			
			System.err.println("Must specifiy at least one write operation - no bitstream written");
			System.exit(1);									
		}
		
		//System.out.println("Exiting Properly");
	}//end main
	
	public static int writeBitstreamToBIT(Bitstream bit, String outputFilename) {
		FileOutputStream out;
		try {
			out = new FileOutputStream(new File(outputFilename));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return 1;
		}
		try {
			bit.outputBitstream(out);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return 1;
		}
		return 0;
	}
	

}


