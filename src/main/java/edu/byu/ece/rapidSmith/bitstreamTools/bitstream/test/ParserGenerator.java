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
package edu.byu.ece.rapidSmith.bitstreamTools.bitstream.test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import edu.byu.ece.rapidSmith.bitstreamTools.bitstream.Bitstream;
import edu.byu.ece.rapidSmith.bitstreamTools.bitstream.BitstreamParseException;
import edu.byu.ece.rapidSmith.bitstreamTools.bitstream.BitstreamParser;

public class ParserGenerator {

    /**
     * This class parses a bitstream (.bit) and creates a Java represtation of it. It then outputs
     * the bitstream as a .bit file and a .mcs file. The .bit file should match the original .bit file
     * exactly. The .mcs file should match exactly an .mcs generated from the original .bit file using
     * promgen -u 0 &lt;bitfile.bit&gt;
     * 
     * @param args
     */
    public static void main(String[] args) {
        
        if (args.length != 3) {
            System.err.println("Usage: java edu.byu.ece.bitstreamTools.bitstream.test.ParserGenerator <input.bit> <output.bit> <output.mcs>");
            System.exit(1);
        }
        
        String inputName = args[0];
        String outputName = args[1];
        String mcsName = args[2];
        
        Bitstream bs = null;
        try {
            bs = BitstreamParser.parseBitstream(inputName);
        } catch (BitstreamParseException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        FileOutputStream os_bit = null;
        try {
            os_bit = new FileOutputStream(new File(outputName));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        
        try {
            bs.outputHeaderBitstream(os_bit);
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        FileOutputStream os_mcs = null;
        try {
            os_mcs = new FileOutputStream(new File(mcsName));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        
        try {
            bs.writeBitstreamToMCS(os_mcs);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
