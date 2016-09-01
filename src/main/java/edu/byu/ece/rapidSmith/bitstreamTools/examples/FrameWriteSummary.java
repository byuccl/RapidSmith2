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

import java.util.Iterator;

import joptsimple.OptionSet;
import edu.byu.ece.rapidSmith.bitstreamTools.bitstream.Bitstream;
import edu.byu.ece.rapidSmith.bitstreamTools.bitstream.Packet;
import edu.byu.ece.rapidSmith.bitstreamTools.bitstream.PacketList;
import edu.byu.ece.rapidSmith.bitstreamTools.bitstream.PacketOpcode;
import edu.byu.ece.rapidSmith.bitstreamTools.bitstream.PacketType;
import edu.byu.ece.rapidSmith.bitstreamTools.bitstream.RegisterType;
import edu.byu.ece.rapidSmith.bitstreamTools.configuration.FrameAddressRegister;
import edu.byu.ece.rapidSmith.bitstreamTools.configurationSpecification.XilinxConfigurationSpecification;
import edu.byu.ece.rapidSmith.bitstreamTools.examples.support.BitstreamOptionParser;

/**
 * This example will search the bitstream for FDRI write commands and will
 * summarize the FAR locations for each FDRI command. It supports bitstreams
 * with multiple FDRI commands.
 *
 */
public class FrameWriteSummary {

	/**
	 * Prints information about each FDRI write command (i.e., for each FDRI command,
	 * it prints a text location of the FAR address and the # of frames).
	 */
	public static void main(String[] args) {
	
		String PRINT_ALL_FRAMES = "a";
		
		/** Setup parser **/
		BitstreamOptionParser cmdLineParser = new BitstreamOptionParser();
		cmdLineParser.addInputBitstreamOption();
		cmdLineParser.addPartNameOption();
		cmdLineParser.addHelpOption();
		cmdLineParser.accepts(PRINT_ALL_FRAMES, "Prints all frames");
		
		OptionSet options = cmdLineParser.parseArgumentsExitOnError(args);

		cmdLineParser.checkHelpOptionExitOnHelpMessage(options);

		BitstreamOptionParser.printExecutableHeaderMessage(FrameWriteSummary.class);

		Bitstream bitstream = cmdLineParser.parseRequiredBitstreamFromOptionsExitOnError(options, true);

		XilinxConfigurationSpecification partInfo = cmdLineParser.getPartInfoExitOnError(options, bitstream, true);	

		boolean printAllFrames =(options.has(PRINT_ALL_FRAMES));

		// Get part packets
		PacketList packets = bitstream.getPackets();
		Iterator<Packet> p = packets.iterator();

		while (p.hasNext()) {
			printFDRIWrite(partInfo, p, printAllFrames);
		}
						
	}

	public static void printFDRIWrite(XilinxConfigurationSpecification spec, Iterator<Packet> pi,
			boolean printAllFrames) {
		boolean debug = false;
		while (pi.hasNext()) {
			Packet p = pi.next();
			if (debug) System.out.println(p.toString(false));
			
			if (p.getPacketType() == PacketType.ONE &&
				p.getOpcode() == PacketOpcode.WRITE &&
				p.getRegType() == RegisterType.FAR) {
				
				// Get FAR address
				int farAddress = p.getData().get(0);

				// Skip all following commands until the FDRI command arrives
				while (pi.hasNext() && !(p.getOpcode() == PacketOpcode.WRITE && p.getRegType() == RegisterType.FDRI)) {
					p = pi.next();
					if (debug) System.out.println("skip:"+p.toString(false));
				}
				// get the write command after the FDRI
				if (pi.hasNext()) {
					p = pi.next();
					if (debug) System.out.println("final write:"+p.toString(false));
					if (p.getOpcode() == PacketOpcode.WRITE) {
						// Print out command
						int words = p.getData().size();
						int frames = (words/spec.getFrameSize());
						if (!printAllFrames) {
							System.out.println("Initial FAR:"+ FrameAddressRegister.toString(spec,farAddress));
							System.out.println("FDRI words="+words + " (" + frames + " frames)");
							//System.out.println("Ending FAR:"+ FrameAddressRegister.toString(spec,farAddress+frames-1));
						} else 
							printIntermediateFAR(spec, farAddress,frames);
					}
				}

			}
		}
	}

	public static void printIntermediateFAR(XilinxConfigurationSpecification spec,
			int farAddress, int frames) {
		FrameAddressRegister far = new FrameAddressRegister(spec,farAddress);
		for (int i = 0; i < frames; i++) {
			System.out.println("Frame #"+i+":"+far);
			far.incrementFAR();
		}
	}
}

