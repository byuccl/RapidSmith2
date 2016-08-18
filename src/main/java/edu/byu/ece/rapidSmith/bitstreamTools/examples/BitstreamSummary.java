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

import java.io.PrintWriter;

import joptsimple.OptionSet;
import edu.byu.ece.rapidSmith.bitstreamTools.bitstream.Bitstream;
import edu.byu.ece.rapidSmith.bitstreamTools.examples.support.BitstreamOptionParser;

/**
 * This executable prints the contents of a bitstream as an XML file.
 *
 */
public class BitstreamSummary {

		
	//public static final String PRINT_LEVEL_OPTION = "p";
	//public static final String FAR_START_ADDRESS_OPTION_HELP = 
	//	"Determines the print level option";
	public static final String PRINT_HEADER_OPTION = "h";
	public static final String PRINT_HEADER_OPTION_HELP = "Print only the bitstream header.";
	public static final String PRINT_XML_OPTION = "xml";
	public static final String PRINT_XML_OPTION_HELP = "Print bitstream in older XML format.";
	
	/**
	 * Prints the contents of a bitstream as an XML file.
	 * 
	 * @param args bitstream name
	 */
	public static void main(String[] args) {

		/** Setup parser **/
		BitstreamOptionParser cmdLineParser = new BitstreamOptionParser();
		cmdLineParser.addInputBitstreamOption();
		//cmdLineParser.addPartNameOption();
		cmdLineParser.addHelpOption();
		cmdLineParser.accepts(PRINT_HEADER_OPTION, PRINT_HEADER_OPTION_HELP);
		cmdLineParser.accepts(PRINT_XML_OPTION, PRINT_XML_OPTION_HELP);

		OptionSet options = cmdLineParser.parseArgumentsExitOnError(args);

		// 
		BitstreamOptionParser.printExecutableHeaderMessage(NonEmptyFrames.class);
		
		/////////////////////////////////////////////////////////////////////
		// Begin basic command line parsing
		/////////////////////////////////////////////////////////////////////		
		cmdLineParser.checkHelpOptionExitOnHelpMessage(options);
		
		/////////////////////////////////////////////////////////////////////
		// 1. Parse bitstream
		/////////////////////////////////////////////////////////////////////
		Bitstream bitstream = cmdLineParser.parseRequiredBitstreamFromOptionsExitOnError(options, true);
		
		//int printLevel = 1;
		
		// header only
		if (options.has(PRINT_HEADER_OPTION)) {
			System.out.println(bitstream.getHeader().toString());
			System.exit(1);
		}
		// print XML
		if (options.has(PRINT_XML_OPTION)) {
			System.out.println(bitstream.toXMLString());
			System.exit(1);			
		}
		// Print full bitstream
		PrintWriter pw = new PrintWriter(System.out);
		bitstream.toStream(true, true, pw);
		pw.flush();
		pw.close();        
    }

	
}
