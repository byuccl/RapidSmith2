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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import edu.byu.ece.rapidSmith.bitstreamTools.bitstream.Bitstream;
import edu.byu.ece.rapidSmith.bitstreamTools.bitstream.BitstreamParseException;
import edu.byu.ece.rapidSmith.bitstreamTools.bitstream.BitstreamParser;
import edu.byu.ece.rapidSmith.bitstreamTools.bitstream.Packet;
import edu.byu.ece.rapidSmith.bitstreamTools.bitstream.PacketList;
import edu.byu.ece.rapidSmith.bitstreamTools.bitstream.RegisterType;
import edu.byu.ece.rapidSmith.bitstreamTools.configuration.FrameAddressRegister;
import edu.byu.ece.rapidSmith.bitstreamTools.configurationSpecification.BlockSubType;
import edu.byu.ece.rapidSmith.bitstreamTools.configurationSpecification.BlockType;
import edu.byu.ece.rapidSmith.bitstreamTools.configurationSpecification.DeviceLookup;
import edu.byu.ece.rapidSmith.bitstreamTools.configurationSpecification.XilinxConfigurationSpecification;

/**
 * This example parses a debug bitstream and determines the size of each column. It knows
 * the size of each block sub type.
 */
public class DebugBitstreamDecoder {

    public static void main(String[] args) {
        
        if (args.length != 1) {
            System.err.println("Usage: java edu.byu.ece.bitstreamTools.examples.DebugBitstreamDecoder <input.bit>");
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
        
        Map<Integer, Integer> columnMap = new LinkedHashMap<>();
        
        PacketList packets = bitstream.getPackets();
        for (Packet p : packets) {
            if (p.getRegType() == RegisterType.LOUT) {
                int farAddress = p.getData().get(0);
                far.setFAR(farAddress);
                if (far.getRow() != 0 || far.getTopBottom() != 0) {
                    break;
                }
                int column = far.getColumn();
                int frame = far.getMinor();
                columnMap.put(column, frame + 1);
                //System.out.println("TB: " + far.getTopBottom() + ", Block: " + far.getBlockType() + ", Row: " + far.getRow() + ", Column: " + far.getColumn() + ", Frame: " + far.getMinor());
            }
        }
        
        
        
        Map<Integer, List<BlockSubType>> frameCountMap = new LinkedHashMap<>();

        for (BlockType bt : spec.getBlockTypes()) {
            for (BlockSubType bst : bt.getValidBlockSubTypes()) {
                int frameCount = bst.getFramesPerConfigurationBlock();
                frameCountMap.computeIfAbsent(frameCount, k -> new ArrayList<>()).add(bst);
            }
        }
        
        for (int column : columnMap.keySet()) {
            int numFrames = columnMap.get(column);
            String possible = "possible block sub types: [";
            List<BlockSubType> possibleSubTypes = frameCountMap.get(numFrames);
            if (possibleSubTypes != null) {
                Iterator<BlockSubType> it = possibleSubTypes.iterator();
                while (it.hasNext()) {
                    BlockSubType bst = it.next();
                    possible += bst.getName();
                    if (it.hasNext()) {
                        possible += ", ";
                    }
                }
                possible += "]";
            }
            System.out.println("Column: " + column + ",\t# frames: " + numFrames + "\t, " + possible);
        }
        
    }
}
