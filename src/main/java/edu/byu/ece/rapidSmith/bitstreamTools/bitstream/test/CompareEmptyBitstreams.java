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

import java.io.IOException;
import java.util.List;

import joptsimple.OptionParser;
import joptsimple.OptionSet;

public class CompareEmptyBitstreams {

    public static void main(String[] args) {
        
        OptionParser parser = new OptionParser() {
            {
                accepts("h", "Print help message");
            }
        };
        
        
        OptionSet options = null;
        try {
            options = parser.parse(args);
        }
        catch (Exception e) {
            System.err.println(e);
            System.exit(1);
        }
        
        @SuppressWarnings("unchecked")
        List<String> arguments = (List<String>) options.nonOptionArguments();
        
        // Print help options
        if(options.has("h") || (arguments.size() != 1)){
            try {
                System.out.println("Usage: java edu.byu.ece.bitstreamTools.bitstream.test.CompareEmptyBitstreams <architecture>\n");
                parser.printHelpOn(System.out);
            } catch (IOException e) {
                System.out.println(e.getMessage());
                System.exit(1);
            }
            System.exit(0);
        }

        boolean matches = TestTools.compareBlankBitstreams(arguments.get(0));

        if (matches) {
            System.out.println("All .bit and .mcs files match");
        }
        else {
            System.out.println("Some (or all) of the .bit and/or .mcs files didn't match");
        }
        int result = matches ? 0 : 1;
        System.exit(result);
        
    }
    
}
