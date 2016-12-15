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

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import edu.byu.ece.rapidSmith.bitstreamTools.bitstream.Bitstream;
import edu.byu.ece.rapidSmith.bitstreamTools.bitstream.BitstreamException;
import edu.byu.ece.rapidSmith.bitstreamTools.bitstream.BitstreamHeader;
import edu.byu.ece.rapidSmith.bitstreamTools.bitstream.PacketListCRC;
import edu.byu.ece.rapidSmith.bitstreamTools.configuration.BitstreamGenerator;
import edu.byu.ece.rapidSmith.bitstreamTools.configuration.FrameAddressRegister;
import edu.byu.ece.rapidSmith.bitstreamTools.configuration.V4BitstreamGenerator;
import edu.byu.ece.rapidSmith.bitstreamTools.configurationSpecification.BlockType;
import edu.byu.ece.rapidSmith.bitstreamTools.configurationSpecification.DeviceLookup;
import edu.byu.ece.rapidSmith.bitstreamTools.configurationSpecification.XilinxConfigurationSpecification;

/**
 * This example demonstrates how to create partial bitstreams for configuring the BRAMs
 * on V4 devices. 
 * 
 *  
 */
public class BRAMPartialBitstreams {

	
	public static void main(String args[]) {

		
		if (args.length < 1) {
			System.err.println("usage: <part name>\n");
			System.exit(1);
		}
		String partname = args[0];
			
		XilinxConfigurationSpecification spec = DeviceLookup.lookupPartV4V5V6(partname);
		
		int idCode = spec.getIntDeviceIDCode();

		// Creates the initial packets
		BitstreamGenerator v4gen = V4BitstreamGenerator.getSharedInstance();
		PacketListCRC packets = v4gen.createInitialPartialBitstream(idCode);

		// TODO: Create data packets here
		final int size = 1312;
		List<Integer> data = new ArrayList<>(size);
		for (int i = 0; i < size; i++ )
			data.add(0);

		// TODO: figure out the far address
		int topBottom = 0;
		int row = 0;
		int column = 0;
		int minor = 0;

		// Set block type in FAR
		// TODO: need to make it easier to figure out the block number
		int blockType = 0;
		List<BlockType> blockTypes = spec.getBlockTypes();
		
		int i = 0;
		for (BlockType blockTypeI : blockTypes) {
		    if (blockTypeI == spec.getBRAMContentBlockType()) {
		        blockType = i;
		    }
		    i++;
		}
			
		int farAddress = FrameAddressRegister.createFAR(spec, topBottom, blockType,
				row, column, minor);

		try {
			BitstreamGenerator.createPartialWritePackets(packets, spec, farAddress, data);
		} catch (BitstreamException e) {
			System.exit(1);
		}
		
		// Creates the ending packets
		v4gen.createEndingPartialBitstream(packets);

		// 4. Create bitstream from the packets
		// TODO: create a non header bitstream and see if it can be parsed
		BitstreamHeader header = new BitstreamHeader("temp.ncd","4VSX550-pg125");		
		Bitstream bitstream = new Bitstream(header,spec.getSyncData(), packets);

		// 5. Write the bitstream to a file
		// TODO: create an Ostream
		FileOutputStream fos;
		try {
			fos = new FileOutputStream("test.dat");
			//bitstream.outputRawBitstream(fos);
			bitstream.outputHeaderBitstream(fos);
		} catch (IOException e) {
			System.exit(1);
		}
		
	}

}
