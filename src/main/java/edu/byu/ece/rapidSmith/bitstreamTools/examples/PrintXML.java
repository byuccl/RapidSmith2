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

import edu.byu.ece.rapidSmith.bitstreamTools.bitstream.Bitstream;
import edu.byu.ece.rapidSmith.bitstreamTools.bitstream.BitstreamParser;

/**
 * This executable prints the contents of a bitstream as an XML file.
 *
 * @deprecated This class is kept for compatibility purposes. This class has been replaced
 * by the BitstreamSummary class with more flexible bitstream reporting methods.
 */
public class PrintXML {

	/**
	 * Prints the contents of a bitstream as an XML file.
	 * 
	 * @param args bitstream name
	 */
	public static void main(String[] args) {

		int printLevel = 0;
		
		if (args.length < 1) {
            System.out.println("Usage: java edu.byu.ece.bitstreamTools.bitstream.test.PrintXML <bitstream.bit> [print level]");
            System.exit(1);
        }
        if (args.length > 1) {
        	try {
        		printLevel = Integer.parseInt(args[1]);
        	} catch (NumberFormatException e) {
        		System.err.println("Not a valid number:"+args[1]);
        		System.exit(1);
        	}
        }
        
        Bitstream bitStream = BitstreamParser.parseBitstreamExitOnError(args[0]);
        System.out.println(bitStream.toString(printLevel));
        
    }

	
}
