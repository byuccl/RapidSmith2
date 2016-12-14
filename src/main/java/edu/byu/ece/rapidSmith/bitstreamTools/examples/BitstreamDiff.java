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

import java.util.ArrayList;

import joptsimple.OptionSet;
import edu.byu.ece.rapidSmith.bitstreamTools.bitstream.Bitstream;
import edu.byu.ece.rapidSmith.bitstreamTools.configuration.FPGA;
import edu.byu.ece.rapidSmith.bitstreamTools.configuration.FPGAOperation;
import edu.byu.ece.rapidSmith.bitstreamTools.configuration.Frame;
import edu.byu.ece.rapidSmith.bitstreamTools.configuration.FrameAddressRegister;
import edu.byu.ece.rapidSmith.bitstreamTools.configuration.FrameData;
import edu.byu.ece.rapidSmith.bitstreamTools.configurationSpecification.XilinxConfigurationSpecification;
import edu.byu.ece.rapidSmith.bitstreamTools.examples.support.BitstreamOptionParser;

/**
 * This executable loads two bitstreams and determines the differences. It also contains
 * a number of static methods that can be used within beanshell scripts or other executables.
 *
 * TODO:
 *  - Provide ability to check differences between the headers or not (it currently ignores this)
 */
public class BitstreamDiff {

		
	//public static final String PRINT_LEVEL_OPTION = "p";
	//public static final String FAR_START_ADDRESS_OPTION_HELP = 
	//	"Determines the print level option";
	public static final String COMPARE_BITSTREAM_OPTION = "c";
	public static final String COMPARE_BITSTREAM_OPTION_HELP = "Filename of bitstream to compare";
	
	public static final String COMPARE_READBACK_OPTION = "cr";
	public static final String COMPARE_READBACK_OPTION_HELP = "Filename of raw readback bitstream to compare";
	
	public static final String MASK_BITSTREAM_OPTION = "mask";
	public static final String MASK_BITSTREAM_OPTION_HELP = "Mask bitfile to apply to both bitstreams";

	public static final String IGNORE_UNCONFIGURED_FRAMES_OPTION = "ignore-unconfigured";
	public static final String IGNORE_UNCONFIGURED_FRAMES_OPTION_HELP = 
		"Ignore differences in frames when one of the frames is unconfigured.";
	public static final String SILENT_MODE_OPTION = "s";
	public static final String SILENT_MODE_OPTION_HELP = 
		"Supress individual frame differences and produce a summary only.";
	public static final String PRINT_DATA_OPTION = "d";
	public static final String PRINT_DATA_OPTION_HELP = 
		"Print the frame contents of frames that differ.";
	
	public static final String[] HELP_DESCRIPTION = {
		"Compares the values of two different bitstreams and reports any differences",
	};
	
	/**
	 * Prints the contents of a bitstream as an XML file.
	 * 
	 * @param args bitstream name
	 */
	public static void main(String[] args) {

		/** Setup parser **/
		BitstreamOptionParser cmdLineParser = new BitstreamOptionParser(HELP_DESCRIPTION);
		// Options for input bitstream or readback file
		cmdLineParser.addInputBitstreamOption();
		cmdLineParser.addRawReadbackInputOption();
		cmdLineParser.addPartNameOption();
		
		cmdLineParser.accepts(COMPARE_BITSTREAM_OPTION, COMPARE_BITSTREAM_OPTION_HELP).withRequiredArg().ofType(String.class);		
		cmdLineParser.accepts(COMPARE_READBACK_OPTION, 
				COMPARE_READBACK_OPTION_HELP).withRequiredArg().ofType(String.class);
		cmdLineParser.accepts(MASK_BITSTREAM_OPTION, 
				MASK_BITSTREAM_OPTION_HELP);

		cmdLineParser.addHelpOption();
		cmdLineParser.accepts(IGNORE_UNCONFIGURED_FRAMES_OPTION, 
				IGNORE_UNCONFIGURED_FRAMES_OPTION_HELP);
		cmdLineParser.accepts(SILENT_MODE_OPTION, 
				SILENT_MODE_OPTION_HELP);
		cmdLineParser.accepts(PRINT_DATA_OPTION, 
				PRINT_DATA_OPTION_HELP);

		OptionSet options = cmdLineParser.parseArgumentsExitOnError(args);

		// 
		BitstreamOptionParser.printExecutableHeaderMessage(BitstreamDiff.class);
		
		/////////////////////////////////////////////////////////////////////
		// Begin basic command line parsing
		/////////////////////////////////////////////////////////////////////		
		cmdLineParser.checkHelpOptionExitOnHelpMessage(options);
		
		boolean ignoreUnconfiguredFrames = 
			options.has(IGNORE_UNCONFIGURED_FRAMES_OPTION);
		boolean silentMode =
			options.has(SILENT_MODE_OPTION);
		boolean printData = options.has(PRINT_DATA_OPTION);
		
		/////////////////////////////////////////////////////////////////////
		// 1. Get base FPGA object
		/////////////////////////////////////////////////////////////////////
		FPGA fpga1 = null;		
		fpga1 = cmdLineParser.createFPGAFromBitstreamOrReadbackFileExitOnError(options);
		XilinxConfigurationSpecification part = fpga1.getDeviceSpecification();
		
		/////////////////////////////////////////////////////////////////////
		// 2. Get compare FPGA object
		/////////////////////////////////////////////////////////////////////
		FPGA fpga2 = null;
		fpga2 = cmdLineParser.createFPGAFromBitstreamOrReadbackFileExitOnError(options, 
				COMPARE_READBACK_OPTION,
				COMPARE_BITSTREAM_OPTION, part);

		/////////////////////////////////////////////////////////////////////
		// 3. Get mask FPGA object (if it exists)
		/////////////////////////////////////////////////////////////////////
		Bitstream maskBitstream = cmdLineParser.parseOptionalBitstreamFromOptionsExitOnError(options, MASK_BITSTREAM_OPTION, true);
		if (maskBitstream != null) {
			XilinxConfigurationSpecification partInfo = cmdLineParser.getPartInfoExitOnError(options, maskBitstream, true);
			FPGA maskFPGA = new FPGA(partInfo);		
			// Configure FPGA
			maskFPGA.configureBitstream(maskBitstream);
			
			// Now mask the two FPGAs
			FPGAOperation.MASKoperation(fpga1, maskFPGA);
			FPGAOperation.MASKoperation(fpga2, maskFPGA);
		}
		
		diff(fpga1, fpga2, ignoreUnconfiguredFrames, printData, silentMode);
		
	}

	/**
	 * Perform a frame by frame comparision of two different configured FPGAs.
	 * 
	 * @return A List of Integer objects that correspond to the Frame Address Registers of the 
	 * Frames that differ.
	 */
	public static ArrayList<Integer> diff(FPGA fpga1, FPGA fpga2, 
			boolean ignoreUnconfiguredFrames, boolean printData, boolean silentMode) {
		
		XilinxConfigurationSpecification spec1 = fpga1.getDeviceSpecification();
		XilinxConfigurationSpecification spec2 = fpga2.getDeviceSpecification();
		if (spec1 != spec2) {
			System.err.println("Not the same device");
			System.exit(1);
		}

		ArrayList<Integer> diffFARs = new ArrayList<>();
		
		FrameAddressRegister far = new FrameAddressRegister(spec1);
		
		// Diff counters
		int configuredDataNonEmptyDifferences = 0;
		int configuredFPGA1EmptyDifferences = 0;
		int configuredFPGA2EmptyDifferences = 0;
		int configuredFramesEqualWithData = 0;
		int configuredFramesEqualEmpty = 0;
		
		int fpga1ConfiguredFrames = 0;
		int fpga2ConfiguredFrames = 0;
		int fpga1NonEmptyFrames = 0;
		int fpga2NonEmptyFrames = 0;
		
		for (; far.validFARAddress(); far.incrementFAR()) {
			Frame f1 = fpga1.getFrame(far);
			Frame f2 = fpga2.getFrame(far);
			String msg = null;

			// Collect statistics
			if (f1.isConfigured()) {
				fpga1ConfiguredFrames++;
				if (!f1.getData().isEmpty())
					fpga1NonEmptyFrames++;
			}
			if (f2.isConfigured()) {
				fpga2ConfiguredFrames++;
				if (!f2.getData().isEmpty())
					fpga2NonEmptyFrames++;
			}
			
			// Check #1: see if frames are configured or not
			if ( !f1.isConfigured() || !f2.isConfigured()) {
				String umsg = null;
				
				// Don't create a message if the ignore unconfigured frames option was set
				if (ignoreUnconfiguredFrames) continue;
				
				if (!f1.isConfigured() && f2.isConfigured() ) {					
					umsg = "FPGA 1 not configured";
					if (!f2.getData().isEmpty())
						umsg += " (non empty frame in FPGA2)";
					else
						umsg += " (empty frame in FPGA2)";
				} else if (f1.isConfigured() && !f2.isConfigured()) {
					umsg = "FPGA 2 not configured";
					if (!f1.getData().isEmpty())
						umsg += " (non empty frame in FPGA1)";
					else
						umsg += " (empty frame in FPGA1)";
				}
				msg = umsg;
				
			} else {
			
				// both frames configured
				FrameData d1 = f1.getData();
				FrameData d2 = f2.getData();
				if (!d1.isEqual(d2)) {
					
					diffFARs.add(far.getAddress());
					
					if (d1.isEmpty())
						configuredFPGA1EmptyDifferences++;
					else if (d2.isEmpty())
						configuredFPGA2EmptyDifferences++;
					else
						configuredDataNonEmptyDifferences++;
					
					msg = "Frames differ in contents";
					if (printData) {
						msg += "\nFPGA 1\n";
						msg += d1.toString();
						msg += "FPGA2 2\n";
						msg += d2.toString();
					}
				} else {
					// frame data is equal
					if (d1.isEmpty())
						configuredFramesEqualEmpty++;
					else
						configuredFramesEqualWithData++;
				}
			
			}

			// print out message if there is one
			if (!silentMode && msg != null) {
				System.out.println(far.getHexAddress() + " (" + far + "):"+ msg);
			}
			
		}	

		// Print summary
		System.out.println("FPGA1:");
		System.out.println("\t"+fpga1ConfiguredFrames+" configured frames");
		System.out.println("\t"+fpga1NonEmptyFrames+" configured frames with data");
		System.out.println("FPGA2:");
		System.out.println("\t"+fpga2ConfiguredFrames+" configured frames");
		System.out.println("\t"+fpga2NonEmptyFrames+" configured frames with data");
		int totalSame = configuredFramesEqualEmpty+configuredFramesEqualWithData;
		System.out.println("# configured frames with no data differences:"+totalSame);
		System.out.println("\t"+configuredFramesEqualEmpty+" empty frames that are equal");
		System.out.println("\t"+configuredFramesEqualWithData+" non-empty frames that are equal");
		// Data diff counters
		int totalDiffs = configuredDataNonEmptyDifferences + configuredFPGA1EmptyDifferences + configuredFPGA2EmptyDifferences;
		System.out.println("# configured frames with data differences:"+totalDiffs);
		System.out.println("\t"+configuredDataNonEmptyDifferences+" Non empty frame differences");
		System.out.println("\t"+configuredFPGA1EmptyDifferences+" FPGA1 empty frame differences");
		System.out.println("\t"+configuredFPGA2EmptyDifferences+" FPGA2 empty frame differences");

		return diffFARs;

	}
	
}
