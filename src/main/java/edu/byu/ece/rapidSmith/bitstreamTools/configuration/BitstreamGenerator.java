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
package edu.byu.ece.rapidSmith.bitstreamTools.configuration;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import edu.byu.ece.rapidSmith.bitstreamTools.bitstream.Bitstream;
import edu.byu.ece.rapidSmith.bitstreamTools.bitstream.BitstreamException;
import edu.byu.ece.rapidSmith.bitstreamTools.bitstream.BitstreamHeader;
import edu.byu.ece.rapidSmith.bitstreamTools.bitstream.PacketListCRC;
import edu.byu.ece.rapidSmith.bitstreamTools.bitstream.PacketUtils;
import edu.byu.ece.rapidSmith.bitstreamTools.configurationSpecification.XilinxConfigurationSpecification;

/**
 * Generates a valid bitstream object from configured FPGA objects.
 * 
 * This abstract class is architecture independent and contains
 * abstract classes that need to be defined for extending, architecture
 * specific classes. The architecture specific abstract methods defined 
 * in this class include the following:
 * 
 * getEndingFARAddress
 * createInitialBitstream
 * createEndingBitstream
 * 
 */
public abstract class BitstreamGenerator {
    
    /**
     * Creates a full bitstream from an FPGA object.
     * 
     * TODO: Create a method that creates the header
     * 
     * @param fpga The configured FPGA object that contains the data going into the bitstream.
     * @param header A preinitialized header object that will go into the full bitstream.
     */
    public Bitstream createFullBitstream(FPGA fpga, BitstreamHeader header) {
        
        // 1. Create the initial packets
        PacketListCRC packets = createInitialFullBitstream(fpga.getDeviceSpecification().getIntDeviceIDCode());

        // 2. Create the packets of frame data
        try {
            addFDRIWriteCommandForAllFrames(packets, fpga);
        } catch (BitstreamException e) {
            System.err.println("Trying to create a bitstream with too many words");
            return null;
        }

        // 3. Add ending packets
        createEndingFullBitstream(packets, fpga.spec);
        
        // 4. Create bitstream from the packets
        return new Bitstream(header, fpga.getDeviceSpecification().getSyncData(), packets);
    }

    /**
     * Create a FDRI write command to write all frames of the FPGA.
     */
    protected void addFDRIWriteCommandForAllFrames(PacketListCRC packets, FPGA fpga) throws BitstreamException {
        addFDRIWriteCommandFromConsecutiveFrames(packets, fpga.getAllFrames());	
    }
    
    /**
     * Creates a a set of Packets to perform a write of configuration data. 
     * This is based on a set of sequential Frames in the FPGA object.
     */
    protected void addFDRIWriteCommandFromConsecutiveFrames(PacketListCRC packets, List<Frame> frames) throws BitstreamException
    {
    	// Create a data object that contains all of the frame data
        int initial_far_address = frames.get(0).getFrameAddress();
        ArrayList<Integer> data = new ArrayList<Integer>();
        for (Frame i : frames) {
            data.addAll(i.getData().getAllFrameWords());
        }
        addFDRIWritePackets(packets, initial_far_address, data);

    }
    
    /**
     * Creates and adds a set of Packets to perform a write of configuration data.
     * The data is sequential Frame data.
     * 
     * This is common among all three architectures (V4, V5, and V6)
     * 
     */
    protected static PacketListCRC addFDRIWritePackets(PacketListCRC packets, int farAddress, List<Integer> data) 
    	throws BitstreamException {
    
        packets.add(PacketUtils.FAR_WRITE_PACKET(farAddress));
        packets.add(PacketUtils.WCFG_CMD_PACKET);
        packets.add(PacketUtils.NOP_PACKET);
        packets.addAll(PacketUtils.FDRI_WRITE_PACKETS(data));
        
        return packets;
    }

    /**
     * This method is based on Ben Sellers original Bitstream.CreateBRAMPartialBody method.
     * This original method has been divided into three parts:
     * 1) createInitialPartialBitstream
     *    V4: NOP, RCRC, NOPx2, IDCODE, WCFG, NOP
     * 2) 
     *    GetPartialWritePackets(crc, BRAM_CONTENT_ADDRESS)
     * 3) createEndingPartialBitstream
     *    V4: LFRM, NOPx101, CRC, DESYNC, NOP
     *    
     */
	public Bitstream createPartialBitstream(FPGA fpga, BitstreamHeader header, int farAddress) {

		// Create initial packets for partial bitstreams
		int idCode = fpga.getDeviceSpecification().getIntDeviceIDCode();
		PacketListCRC packets = createInitialPartialBitstream(idCode);
		//XilinxConfigurationSpecification spec = fpga.getDeviceSpecification();
		
		// Get the frame address
		//int bramContentFAR = FrameAddressRegister.getBRAMContentFrameAddress(spec);
		//int numBramContentFrames = FrameAddressRegister.getNumberOfBRAMContentFrames(spec);
		
		// Get the actual frames
		//List<Frame> bramContentFrames = fpga.getConsecutiveFrames(bramContentFAR, numBramContentFrames);
		
		try {
			getPartialWritePacketsBenSellers(packets, fpga, farAddress);
		} catch (Exception e) {
			System.err.println(e);
		}

		createEndingPartialBitstream(packets);

		// 4. Create bitstream from the packets
        return new Bitstream(header, fpga.getDeviceSpecification().getSyncData(), packets);
	}

	public Bitstream createPartialBitstream(FPGA fpga, BitstreamHeader header) {
		return createPartialBitstream(fpga, header, 0);
	}
	
	public Bitstream createPartialBRAMBitstream(FPGA fpga, BitstreamHeader header) {
		XilinxConfigurationSpecification spec = fpga.getDeviceSpecification();
		int bramContentFAR = FrameAddressRegister.getBRAMContentFrameAddress(spec);
		return createPartialBitstream(fpga, header, bramContentFAR);
	}

	/**
	 * This is a method that creates partial write packets and is based exclusively off
	 * of Ben Sellers Bitstream.GetPartialWritePackets method. This is not the best way
	 * to handle this but it is added for backwards compatibility.
	 * 
	 */
	public PacketListCRC getPartialWritePacketsBenSellers(PacketListCRC packetList, FPGA fpga, int farAddr) {

		fpga.setFAR(farAddr);
		boolean isIdle = true;     // When true, we are looking for a frame with data
		boolean validFAR = true;
		
		ArrayList<Integer> data = new ArrayList<Integer>();
		while(validFAR) {
			if (DEBUG) {
				FrameAddressRegister far = fpga.getFAR();
				System.out.print(far.getHexAddress()+":");
			}
			Frame f = fpga.getCurrentFrame();
			boolean frameHasData = !(f.getData().isEmpty());

			/* 
			 * To create our condensed FDRI writes, we first wait until we find data within the
			 * bitstream.  When we do, we create an FAR packet set to that location followed by
			 * a NOP.  We then start collecting the data that will be added to our FAR instruction.
			 */
			if(isIdle && frameHasData) {
				// First frame in search with data. Add FAR_WRITE packet and NOP packet
				isIdle = false;
				// Create FAR Packet
				int curFAR = fpga.getFAR().getAddress();
				
				packetList.add(PacketUtils.FAR_WRITE_PACKET(curFAR));					
				
				// NOP
		        packetList.add(PacketUtils.NOP_PACKET);			
		        // Add data from this packet
				data.addAll(f.getData().getAllFrameWords());
				if (DEBUG) System.out.print("First frame with data");
			}
			//As long as a frame has data we will continue to add it to the FDRI data
			else if(!isIdle && frameHasData){
				data.addAll(f.getData().getAllFrameWords());
				if (DEBUG) System.out.print("Additional Data added to packet");
			}			
			/* 
			 * We want two frames in a row of zeros before we finish our FDRI packet
			 * The first frame of zeros is added as a buffer for the write, the other is ignored
			 * We then set the state to idle again and then go look for more data
			 */
			else if(!isIdle && !frameHasData){
				// Adding data to packets and found an empty frame.
				if (DEBUG) System.out.print("Empty frame - create packet from previous data");				
				validFAR = fpga.incrementFAR();
				if(validFAR){
					Frame tmp = f;
					f = fpga.getCurrentFrame();
					if(!f.getData().isEmpty()){
						data.addAll(tmp.getData().getAllFrameWords());
						data.addAll(f.getData().getAllFrameWords());
						validFAR = fpga.incrementFAR();
						continue;
					}
				}
				for(int i = 0; i < fpga.getDeviceSpecification().getFrameSize(); i++){
					data.add(0x00000000);
				}

				// Add data packet
				try {
					createPartialFDRIPackets(packetList, data);
				} catch (BitstreamException e) {
					System.err.println(e);
					System.exit(1);
				}
				
				isIdle = true;
				data = new ArrayList<Integer>();
			} else {
				if (DEBUG) System.out.print("Empty frame - looking for data");				
			}
			if (DEBUG) System.out.println();

			validFAR = fpga.incrementFAR();			
		}
		
		return packetList;
		
	}
	
    /**
     * Creates the initial list of packets that are necessary for full bitstreams. These
     * packets are those that occur BEFORE the actual configuration data. Different
     * architectures have different preconfig data packets.
     **/
    public abstract PacketListCRC createInitialFullBitstream(int idcode);
    
    /**
     * Creates the ending list of packets that are necessary for full bitstreams. These
     * packets are those that occur AFTER the actual configuration data. Different
     * architectures have different post configuration data packets.
     **/
    public abstract PacketListCRC createEndingFullBitstream(PacketListCRC packets, 
    		XilinxConfigurationSpecification spec);
    
    /**
     * Creates the initial list of packets that are necessary for partial bitstreams. 
     * The initial list of packets for a partial bitstream are different than
     * those for a full bitstream.
     **/
    public abstract PacketListCRC createInitialPartialBitstream(int idcode);

    /**
     * Creates the ending list of packets that are necessary for partial bitstreams. 
     * The ending list of packets for a partial bitstream are different than
     * those for a full bitstream.
     **/
    public abstract PacketListCRC createEndingPartialBitstream(PacketListCRC packets);

    
    public abstract PacketListCRC createPartialFDRIPackets(PacketListCRC packetList, ArrayList<Integer> data) throws BitstreamException;

    // TODO
	/*
	public static Bitstream getCompressedBitstream(FPGA fpga, String ncdFileName) {		
	}
	
	// TODO
	public static Bitstream getRandomFrameBitstream(FPGA fpga, String ncdFileName) {
	}
	*/

    /**
     * Helper class for generating a .bit file from a Bitstream.
     * 
     */
	public static int writeBitstreamToBIT(Bitstream bit, String outputFilename) {
		FileOutputStream out=null;
		try {
			out = new FileOutputStream(new File(outputFilename));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return 1;
		}
		try {
			bit.outputHeaderBitstream(out);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return 1;
		}
		return 0;
	}
	
	/**
	 * This method is based on the method Bitstream.GetPartialWritePackets written by
	 * Ben Sellers. It creates a set of packets to configure a partial bitstream.
	 * 
	 * This method will add two frames of zeros at the end.
	 * 
	 */
	/*
	public static PacketListCRC createPartialWritePackets(PacketListCRC packets, FPGA fpga,
			List<Frame> frames) throws BitstreamException {

		int initial_far_address = frames.get(0).getFrameAddress();
		packets.add(PacketUtils.FAR_WRITE_PACKET(initial_far_address));
		packets.add(PacketUtils.NOP_PACKET);

		// Data to add
		ArrayList<Integer> data = new ArrayList<Integer>();
		for (Frame i : frames) {
			data.addAll(i.getData().getAllFrameWords());
		}
		// Add a frame of zeros (it is not clear how Ben gets two frames. I see only one in the code
		for (int i = 0; i < fpga.getDeviceSpecification().getFrameSize(); i++) {
			data.add(0x0);
		}
		
		packets.addAll(PacketUtils.WRITE_PACKETS(data));
		
		return packets;		
		
	}
	*/
	/**
	 * TODO: Issues that need to be resolveds:
	 * - Passing around lists of Integer is not a good idea as they may not be 32 bits. We really
	 *   need to inforce the use of 32 bit integers
	 * - We need a way to create initialized FrameData objects
	 */
	/**
	 * This method is based on the method Bitstream.GetPartialWritePackets written by
	 * Ben Sellers. It creates a set of packets to configure a partial bitstream.
	 * 
	 * This method will add two frames of zeros at the end.
	 * 
	 */
	public static PacketListCRC createPartialWritePackets(PacketListCRC packets, 
			XilinxConfigurationSpecification spec, int farAddress,
			List<Integer> data) throws BitstreamException {

		packets.add(PacketUtils.FAR_WRITE_PACKET(farAddress));
		packets.add(PacketUtils.NOP_PACKET);

		packets.addAll(PacketUtils.FDRI_WRITE_PACKETS(data));
		
		return packets;		
		
	}
    
	protected boolean DEBUG = false;
	
	protected boolean backwardCompatibility = true;

    
}
