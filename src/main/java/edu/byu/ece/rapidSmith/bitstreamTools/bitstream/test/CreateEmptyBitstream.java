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
package edu.byu.ece.rapidSmith.bitstreamTools.bitstream.test;

import java.io.IOException;

import joptsimple.OptionParser;
import joptsimple.OptionSet;

public class CreateEmptyBitstream {

    public static void main(String[] args) {

        OptionParser parser = new OptionParser() {
            {
                accepts("p", "Name of the part to create an empty bitstream for").withRequiredArg().ofType(String.class);
                accepts("d", "Create a debug bitstream instead of a regular bitstream").withOptionalArg().ofType(Boolean.class);
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


        //List<String> arguments = options.nonOptionArguments();

        // Print help options
        if(options.has("h")){
            try {
                System.out.println("Usage: java edu.byu.ece.bitstreamTools.bitstream.test.CreateEmptyBitstreams <architecture>\n");
                parser.printHelpOn(System.out);
            } catch (IOException e) {
                System.out.println(e.getMessage());
                System.exit(1);
            }
            System.exit(0);
        }

        // Find part name
        String partName = (String) options.valueOf("p");
        boolean debug = options.has("d");        
        
        TestTools.generateBlankBitstream(partName, debug);        

    }
    
}
