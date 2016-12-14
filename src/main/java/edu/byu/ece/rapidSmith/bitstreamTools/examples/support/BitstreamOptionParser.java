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
package edu.byu.ece.rapidSmith.bitstreamTools.examples.support;

import java.io.IOException;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import edu.byu.ece.rapidSmith.bitstreamTools.bitstream.Bitstream;
import edu.byu.ece.rapidSmith.bitstreamTools.bitstream.BitstreamParser;
import edu.byu.ece.rapidSmith.bitstreamTools.configuration.FPGA;
import edu.byu.ece.rapidSmith.bitstreamTools.configuration.ReadbackFPGA;
import edu.byu.ece.rapidSmith.bitstreamTools.configurationSpecification.DeviceLookup;
import edu.byu.ece.rapidSmith.bitstreamTools.configurationSpecification.XilinxConfigurationSpecification;

/**
 * Provides a number of handy argument parsing and help functions for
 * bitstream related executables.
 *  
 */
public class BitstreamOptionParser extends OptionParser {

	/**
	 * Construct a simple bitstream parser.
	 */
	public BitstreamOptionParser() {
		super();
	}

	/**
	 * Construct a bitstream parser with a default help message.
	 */
	public BitstreamOptionParser(String helpMessage[]) {
		super();
		this.helpString = helpMessage;
	}

	/** Standard copyright string. **/
	public static final String COPYRIGHT = "Copyright (c) 2008-2009 Brigham Young University";

	// Input Bitstream option constants
	public static final String INPUT_BITSTREAM_OPTION = "i";
	public static final String BITSTREAM_OPTION_HELP = "Filename of bitfile";

	// Raw bitstream options
	public static final String READBACK_RAW_FILE_OPTION = "ri";
	public static final String READBACK_RAW_FILE_OPTION_HELP = "Readback raw input file (instead of bitstream input file)";

	// Output Bitstream option constants
	public static final String OUTPUT_BITSTREAM_OPTION = "o";
	public static final String OUTPUT_BITSTREAM_OPTION_HELP = "Filename of output bitfile";
	public static final String OUTPUT_BITSTREAM_MISSING_OPTION = "The output bitstream filename must be specified with the "+
		INPUT_BITSTREAM_OPTION + " option";
	
	// Part option constants
	public static final String PART_OPTION = "p";
	public static final String PART_OPTION_HELP = "Indicate name of part";

	// Help option constants
	public static final String HELP_OPTION = "help";
	public static final String HELP_OPTION_HELP = "Prints help and command line options";

	// Debug options
	public static final String DEBUG_OPTION = "debug";
	public static final String DEBUG_OPTION_HELP = "Print additional debug commands";
	
	/** Add an input bitstream option: -i &lt;bitstream file name&gt;
	 */
	public void addInputBitstreamOption() {
		addBitstreamOption(INPUT_BITSTREAM_OPTION);
	}

	public void addRawReadbackInputOption() {
		accepts(READBACK_RAW_FILE_OPTION, 
				READBACK_RAW_FILE_OPTION_HELP).withRequiredArg().ofType(String.class);
	}

	public void addBitstreamOption(String bitstreamOption) {
		accepts(bitstreamOption, 
				BITSTREAM_OPTION_HELP).withRequiredArg().ofType(String.class);
	}

	public void addOutputBitstreamOption() {
		accepts(OUTPUT_BITSTREAM_OPTION, 
				OUTPUT_BITSTREAM_OPTION_HELP).withRequiredArg().ofType(String.class);
	}
	
	public void addPartNameOption() {
		accepts(PART_OPTION, PART_OPTION_HELP).withRequiredArg().ofType(String.class);
	
	}
	
	public void addHelpOption() {
		accepts(HELP_OPTION, HELP_OPTION_HELP);
	}

	public void addDebugOption() {
		accepts(DEBUG_OPTION, DEBUG_OPTION_HELP);
	}
	
	public boolean debugEnabled(OptionSet options) {
		return options.has(DEBUG_OPTION);
	}

	public OptionSet parseArgumentsExitOnError(String args[]) {
		OptionSet options = null;
		try {
			options = parse(args);
		}
		catch(Exception e){
			System.err.println(e.getMessage());
			System.exit(1);			
		}			
		return options;
	}

	/**
	 * This method will look to see if the HELP_OPTION is specified. If it is,
	 * it will exit and print the argument usage of the executable.
	 */
	public void checkHelpOptionExitOnHelpMessage(OptionSet options) {
		// Print help options
		if(options.has(HELP_OPTION)){
			printUsageAndExit();
		}
	}

	/** Parse a bitstream using the default INPUT_BITSTREAM_OPTION. **/
	public Bitstream parseRequiredBitstreamFromOptionsExitOnError(OptionSet options, boolean printResultMessage) {
		return parseRequiredBitstreamFromOptionsExitOnError(options, INPUT_BITSTREAM_OPTION, printResultMessage);
	}

	/** Parse the options and determine the bitstream filename */
	public String getBitstreamFileNameFromOptions(OptionSet options, String bitstreamOption) {
		if (!options.has(bitstreamOption)) {
			return null;
		}
		return (String) options.valueOf(bitstreamOption);		
	}
	
	
	/**
	 * Determine the filename of a bitstream from an option that requires a parameter, parse
	 * the bitstream, and return the bitstream. Exit if there is any error along the 
	 * way. 
	 */
	public Bitstream parseRequiredBitstreamFromOptionsExitOnError(OptionSet options, String bitstreamOption, 
			boolean printResultMessage) {
		String bitstreamFileName = getBitstreamFileNameFromOptions(options,bitstreamOption);
		if (bitstreamFileName == null) {
			System.err.println("A bitstream filename must be specified with the "+
					bitstreamOption + " option");
			printUsageAndExit();
		}

		Bitstream bitstream = BitstreamParser.parseBitstreamExitOnError(bitstreamFileName);		
		if (printResultMessage)
			System.out.println("Bitstream parsed correctly:"+bitstreamFileName);
		return bitstream;
	}
	
	public Bitstream parseOptionalBitstreamFromOptionsExitOnError(OptionSet options, String bitstreamOption, 
			boolean printResultMessage) {
		String bitstreamFileName = getBitstreamFileNameFromOptions(options,bitstreamOption);
		if (bitstreamFileName == null) {
			return null;
		}

		Bitstream bitstream = BitstreamParser.parseBitstreamExitOnError(bitstreamFileName);		
		if (printResultMessage)
			System.out.println("Bitstream parsed correctly:"+bitstreamFileName);
		return bitstream;
	}

	/**
	 * Creates an FPGA object from either a bitstream input file or a readback input file (they
	 * are mutually exclusive).
	 */
	public FPGA createFPGAFromBitstreamOrReadbackFileExitOnError(OptionSet options, String readback_raw_file_option,
			String regular_bitstream_file_option) {
		return createFPGAFromBitstreamOrReadbackFileExitOnError(options, readback_raw_file_option, regular_bitstream_file_option, null);
	}
	
	public FPGA createFPGAFromBitstreamOrReadbackFileExitOnError(OptionSet options, String readback_raw_file_option,
			String regular_bitstream_file_option, XilinxConfigurationSpecification part) {

		FPGA fpga = null;
		
		if(options.has(readback_raw_file_option)){
			// get data from a readback file
			if (part == null)
				part = getRequiredPartInfoExitOnError(options);
			String readbackfilename = (String) options.valueOf(readback_raw_file_option);		
			
			fpga = ReadbackFPGA.parseRawReadbackDataFromOptionsExitOnError(readbackfilename, 
					part);

		} else {
			Bitstream bitstream = parseRequiredBitstreamFromOptionsExitOnError(options, regular_bitstream_file_option, true);
			// get data from a bitstream
			/////////////////////////////////////////////////////////////////////
			// 2. Obtain part information
			/////////////////////////////////////////////////////////////////////
			XilinxConfigurationSpecification partInfo = getPartInfoExitOnError(options, bitstream, true);
			fpga = new FPGA(partInfo);		
			// Configure FPGA
			fpga.configureBitstream(bitstream);
		}
		return fpga;
	}

	public FPGA createFPGAFromBitstreamOrReadbackFileExitOnError(OptionSet options) {
		return createFPGAFromBitstreamOrReadbackFileExitOnError(options, READBACK_RAW_FILE_OPTION, INPUT_BITSTREAM_OPTION);
	}
	
	public String getOutputFileNameStringExitOnError(OptionSet options) {
		if (!options.has(OUTPUT_BITSTREAM_OPTION)) {
			System.err.println("No output bitstream specified. The ouptput bitstream must be specified using the -o option");
			printUsageAndExit();
		}
		return (String) options.valueOf(OUTPUT_BITSTREAM_OPTION);
	}
	
	/**
	 * Extract the part information from the bitstream. If it is not available in the bitstream,
	 * extract it from the command line options.
	 */
	public XilinxConfigurationSpecification getPartInfoExitOnError(OptionSet options, Bitstream bitstream, boolean printMessage) {

		XilinxConfigurationSpecification partInfo = null;

		// first see if the part can be found from the bitstream
		String partName = null;
		if (bitstream != null && bitstream.getHeader() != null) {
			partName = bitstream.getHeader().getPartName();
			// strip the package name from the original part name
			partName = DeviceLookup.getRootDeviceName(partName);
		}
		
		if (options.has(PART_OPTION)) {
			// Assume that this is a root part name
			partName = (String) options.valueOf(PART_OPTION);
		}
		
		partInfo = DeviceLookup.lookupPartV4V5V6(partName);
		if (partInfo == null) {
			System.err.println("Invalid Part Name:"+partName);
			DeviceLookup.printAvailableParts(System.err);
			System.exit(1);
		}

		if (printMessage)
			System.out.println("Part:"+partInfo.getDeviceName());
		
		return partInfo;
	}
	
	public XilinxConfigurationSpecification getRequiredPartInfoExitOnError(OptionSet options) {

		XilinxConfigurationSpecification partInfo = null;

		if (!options.has(PART_OPTION)) {
			System.err.println("No part specified");
			System.exit(1);
		}
		
		// Assume that this is a root part name
		String partName = (String) options.valueOf(PART_OPTION);
		
		partInfo = DeviceLookup.lookupPartV4V5V6(partName);
		if (partInfo == null) {
			System.err.println("Invalid Part Name:"+partName);
			DeviceLookup.printAvailableParts(System.err);
			System.exit(1);
		}

		return partInfo;
	}
	
	public int getIntegerStringExitOnError(OptionSet options, String option, int radix, int defaultValue) {
		int intValue = defaultValue;
		if (options.has(option)) {
			String optionValue = null;
			try {
				optionValue = (String) options.valueOf(option);
				intValue = Integer.parseInt(optionValue,radix);
			} catch (NumberFormatException e) {
				System.err.println("Bad integer option"+optionValue);
				System.exit(1);
			}
		}
		return intValue;
	}

	/**
	 * Print the 'Usage' of the executable. If there is a help string, print it.
	 * If not, print the default option parser "printHelpOn" message.
	 */
	public void printUsage() {
		try {
			// If there is a help string, print it
			if (helpString != null) {
				for (int i = 0; i < helpString.length; i++) {
					System.out.println(helpString[i]);
				}
			}
			printHelpOn(System.out);
		} catch (IOException e) {
			System.out.println(e.getMessage());
		}
	}

	/**
	 * Print the usage of the executable and exit.
	 */
	public void printUsageAndExit() {
		printUsage();
		System.exit(1);
	}
	
	public static void printExecutableHeaderMessage(@SuppressWarnings("rawtypes") Class class1) {
		printExecutableHeaderMessage(class1.getName());
	}
	
	public static void printExecutableHeaderMessage(String exeName) {
		System.out.println(exeName);
		System.out.println(ExecutableRevision.getRevisionString() );
		System.out.println(COPYRIGHT);
	}
	
	/**
	 * A help string that will be printed during the "printUsageAndExit" method.
	 */
	protected String helpString[] = null;

}
