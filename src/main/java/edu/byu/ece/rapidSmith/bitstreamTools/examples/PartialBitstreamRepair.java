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
import edu.byu.ece.rapidSmith.bitstreamTools.bitstream.Bitstream;
import edu.byu.ece.rapidSmith.bitstreamTools.configuration.FPGA;
import edu.byu.ece.rapidSmith.bitstreamTools.configuration.Frame;
import edu.byu.ece.rapidSmith.bitstreamTools.configuration.FrameAddressRegister;
import edu.byu.ece.rapidSmith.bitstreamTools.configuration.FrameData;
import edu.byu.ece.rapidSmith.bitstreamTools.configurationSpecification.XilinxConfigurationSpecification;
import edu.byu.ece.rapidSmith.bitstreamTools.examples.support.BitstreamOptionParser;

public class PartialBitstreamRepair {

	public static void main(String args[]) {

		/////////////////////////////////////////////////////////////////////
		// Begin command line parsing
		/////////////////////////////////////////////////////////////////////		
		BitstreamOptionParser cmdLineParser = new BitstreamOptionParser();
		cmdLineParser.addHelpOption();
		cmdLineParser.addDebugOption();		

		// Custom options
		String FULL_OPTION = "full";
		String STATIC_OPTION = "static";
		String PARTIAL_OPTION = "partial";
		String VERBOSE = "v";
		String VERBOSE_HELP = "Verbose output";
		cmdLineParser.accepts(FULL_OPTION,"Name of full bitfile (static+partial)").withRequiredArg().ofType(String.class);
		cmdLineParser.accepts(STATIC_OPTION,"Name of static bitfile (static that is empty)").withRequiredArg().ofType(String.class);
		cmdLineParser.accepts(PARTIAL_OPTION,"Name of partial bitfile").withRequiredArg().ofType(String.class);
		//cmdLineParser.accepts(VERBOSE,VERBOSE_HELP).withOptionalArg().ofType(String.class);
		cmdLineParser.accepts(VERBOSE,VERBOSE_HELP).withRequiredArg().ofType(String.class);

		String PATCH_PARTIAL = "patchPartial";
		cmdLineParser.accepts(PATCH_PARTIAL,"Changes will be made in the partial, not static").withRequiredArg().ofType(String.class);

		// Parse arguments
		OptionSet options = cmdLineParser.parseArgumentsExitOnError(args);
		// Print executable header
		BitstreamOptionParser.printExecutableHeaderMessage(BitstreamManipulation.class);
		// Check for and print help
		cmdLineParser.checkHelpOptionExitOnHelpMessage(options);

		// Get bitstreams
		Bitstream fullBitstream = cmdLineParser.parseRequiredBitstreamFromOptionsExitOnError(options, FULL_OPTION, 
				true);		
		Bitstream staticBitstream = cmdLineParser.parseRequiredBitstreamFromOptionsExitOnError(options, STATIC_OPTION, 
				true);		
		Bitstream partialBitstream = cmdLineParser.parseRequiredBitstreamFromOptionsExitOnError(options, PARTIAL_OPTION, 
				true);		
		
		boolean patchPartial = false;
		if (options.has(PATCH_PARTIAL))
			patchPartial = true;
		
		boolean verbose = false;
		if (options.has(VERBOSE)) {
			verbose = true;
		}	
		boolean printData = false;
		
		// Get and check parts
		XilinxConfigurationSpecification partInfo = cmdLineParser.getPartInfoExitOnError(options, fullBitstream, true);
		XilinxConfigurationSpecification staticPartInfo = cmdLineParser.getPartInfoExitOnError(options, staticBitstream, false);
		XilinxConfigurationSpecification partialPartInfo = cmdLineParser.getPartInfoExitOnError(options, partialBitstream, false);
		
		if (staticPartInfo != partInfo || partialPartInfo != partInfo) { 
			System.err.println("Bitstreams do not match");
			System.exit(1);
		}

		// 3. Create FPGA object
		FPGA fullFPGA = new FPGA(partInfo);		
		FPGA staticFPGA = new FPGA(partInfo);		
		FPGA partialFPGA = new FPGA(partInfo);		

		// Configure FPGA
		fullFPGA.configureBitstream(fullBitstream);
		staticFPGA.configureBitstream(staticBitstream);
		partialFPGA.configureBitstream(partialBitstream);
		
		boolean staticChanged = false;
		boolean partialChanged = false;
		
		// Iterate over all of the frames
		FrameAddressRegister far = new FrameAddressRegister(partInfo);
		for (; far.validFARAddress(); far.incrementFAR()) {
			//System.out.println(far);
			
			Frame fullFrame = fullFPGA.getFrame(far);
			if (!fullFrame.isConfigured()) {
				System.err.println("Error: Unconfigured Frame in the full bitstream at FAR:"+far.getHexAddress());
				System.exit(1);
			}
			
			Frame staticFrame = staticFPGA.getFrame(far);
			Frame partialFrame = partialFPGA.getFrame(far);

			FrameData fullData = fullFrame.getData();
			FrameData staticData = staticFrame.getData();
			FrameData partialData = partialFrame.getData();
			
			if (staticFrame.isConfigured() && partialFrame.isConfigured()) {
				// Both static and partial frames configured
				if (fullData.isEqual(staticData)) {
					if (fullData.isEqual(partialData)) {
						// TODO: All equal: could probably remove the partial frame (future)
						if (verbose)
							System.out.println(far.getHexAddress()+" Static, full, and partial all equal");						
					} else {
						// full and static equal, partial not equal
						System.out.println("Full and static equal, partial not equal:"+far.getHexAddress());						
						if (printData) {
							System.out.println("Full Data\n"+fullData);
							System.out.println("Static Data\n"+staticData);							
							System.out.println("Partial Data\n"+partialData);							
						}
					}				
				} else {
					// full is not equal to static
					if (fullData.isEqual(partialData)) {
						// This is OK. Don't need to do anything here.
						if (staticData.isEmpty())
							System.out.println(far.getHexAddress() + " Full and partial equal, static empty");
						else
							System.out.println(far.getHexAddress() + " Full and partial equal, static not equal and not empty");
						if (printData) {
							System.out.println("Full Data\n"+fullData);
							System.out.println("Static Data\n"+staticData);							
							System.out.println("Partial Data\n"+partialData);							
						}
					} else {
						// Full data is NOT equal to partial data
						if (staticData.isEqual(partialData)) {
							System.out.println("*** Full not equal but static and partial equal:"+far.getHexAddress());													
						} else {
							System.out.println("*** All three not equal:"+far.getHexAddress());													
						}
					}
				}			
			} else if (staticFrame.isConfigured()) {
				// static frame is configured and partial frame is NOT configured
				if (fullData.isEqual(staticData)) {
					if (verbose)
						System.out.println(far.getHexAddress()+" Static and full Equal (partial not configured)");
				} else {
					if (!fullData.isEmpty()) {
						// full has data
						if (!staticData.isEmpty()) {
							// Full and static have data
							System.out.println(far.getHexAddress()+" * Full and static NOT equal (partial not configured)");
							if (printData) {
								System.out.println("Full Data\n"+fullData);
								System.out.println("Static Data\n"+staticData);
							}
						} else {
							// full has data , static is empty
							System.out.println(far.getHexAddress()+" * Full has data, static is empty (partial not configured)");
						}
					} else {
						// full does not have data
						System.out.println(far.getHexAddress()+" * Full empty and static NOT equal (partial not configured)");
					}
					if (!patchPartial) {
						staticData.setData(fullData);
						staticChanged = true;
					} else {
						partialData.setData(fullData);
						partialChanged = true;
					}
				}
			} else if (partialFrame.isConfigured()) {
				// partial frame is configured, static frame is NOT configured
				if (fullData.isEqual(partialData)) {
					System.out.println("Partial and full Equal (static not configured):"+far.getHexAddress());
				} else {
					System.out.println("Partial Not Equal:"+far.getHexAddress());					
				}
			} else {
				// neither frame is configured
				if (!fullData.isEmpty()) {
					System.out.println("Frame not empty and static/partial not conifgured:"+far);
				} 
			}
		}
		
		if (staticChanged) {
			String oldStaticFileName = cmdLineParser.getBitstreamFileNameFromOptions(options,STATIC_OPTION);
			String newStaticFilename = oldStaticFileName + ".new";
			// create the bitstream
			Bitstream newBitstream = partInfo.getBitstreamGenerator().createFullBitstream(staticFPGA, staticBitstream.getHeader());
			BitstreamManipulation.writeBitstreamToBIT(newBitstream, newStaticFilename);
		}
		if (partialChanged) {
			String oldPartialFileName = cmdLineParser.getBitstreamFileNameFromOptions(options,PARTIAL_OPTION);
			String newPartialFilename = oldPartialFileName + ".new";
			// create the bitstream
			Bitstream newBitstream = partInfo.getBitstreamGenerator().createPartialBitstream(partialFPGA, partialBitstream.getHeader());
			BitstreamManipulation.writeBitstreamToBIT(newBitstream, newPartialFilename);
		}
	}
	
}
