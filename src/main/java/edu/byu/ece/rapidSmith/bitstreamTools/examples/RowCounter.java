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

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import edu.byu.ece.rapidSmith.bitstreamTools.bitstream.Bitstream;
import edu.byu.ece.rapidSmith.bitstreamTools.bitstream.BitstreamParseException;
import edu.byu.ece.rapidSmith.bitstreamTools.bitstream.BitstreamParser;
import edu.byu.ece.rapidSmith.bitstreamTools.bitstream.Packet;
import edu.byu.ece.rapidSmith.bitstreamTools.bitstream.PacketList;
import edu.byu.ece.rapidSmith.bitstreamTools.bitstream.RegisterType;
import edu.byu.ece.rapidSmith.bitstreamTools.configuration.FrameAddressRegister;
import edu.byu.ece.rapidSmith.bitstreamTools.configurationSpecification.DeviceLookup;
import edu.byu.ece.rapidSmith.bitstreamTools.configurationSpecification.XilinxConfigurationSpecification;

public class RowCounter {

public static void main(String[] args) {
        
        if (args.length != 1) {
            System.err.println("Usage: java edu.byu.ece.bitstreamTools.examples.RowCounter <input.bit>");
        }
        
        Bitstream bitstream = null;
        try {
            bitstream = BitstreamParser.parseBitstream(args[0]);
        } catch (BitstreamParseException | IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    XilinxConfigurationSpecification spec = DeviceLookup.lookupPartV4V5V6withPackageName(bitstream.getHeader().getPartName());
        
        FrameAddressRegister far = new FrameAddressRegister(spec);
        
        Set<Integer> topRows = new HashSet<>();
        Set<Integer> bottomRows = new HashSet<>();
        
        PacketList packets = bitstream.getPackets();
        for (Packet p : packets) {
            if (p.getRegType() == RegisterType.LOUT) {
                int farAddress = p.getData().get(0);
                far.setFAR(farAddress);
                int currentRow = far.getRow();
                int currentTopBottom = far.getTopBottom();
                Set<Integer> rowSet = (currentTopBottom == 0) ? topRows : bottomRows;
                rowSet.add(currentRow);
            }
        }

        System.out.println("Top rows: " + topRows.size());
        System.out.println("Bottom rows: " + bottomRows.size());       
    }
}
