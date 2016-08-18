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

import java.util.ArrayList;

import edu.byu.ece.rapidSmith.bitstreamTools.bitstream.BitstreamException;
import edu.byu.ece.rapidSmith.bitstreamTools.bitstream.PacketListCRC;
import edu.byu.ece.rapidSmith.bitstreamTools.bitstream.PacketUtils;
import edu.byu.ece.rapidSmith.bitstreamTools.bitstream.RegisterType;
import edu.byu.ece.rapidSmith.bitstreamTools.configurationSpecification.BlockType;
import edu.byu.ece.rapidSmith.bitstreamTools.configurationSpecification.XilinxConfigurationSpecification;

public class V4BitstreamGenerator extends BitstreamGenerator {

    private static V4BitstreamGenerator _singleton = null;
    
    protected V4BitstreamGenerator() {
        
    }
    
    public static V4BitstreamGenerator getSharedInstance() {
        if (_singleton == null) {
            _singleton = new V4BitstreamGenerator();
        }
        return _singleton;
    }

/*
    public PacketListCRC createPartialWritePackets(PacketListCRC packets, 
            XilinxConfigurationSpecification spec, int farAddress,
            List<Integer> data) throws BitstreamException {

        packets.add(PacketUtils.FAR_WRITE_PACKET(farAddress));
        packets.add(PacketUtils.NOP_PACKET);

        packets.addAll(PacketUtils.WRITE_PACKETS(data));
        
        return packets;     
        
    }
*/
    
    /**
     * This method is based on the method Bitstream.GetPartialWritePackets written by
     * Ben Sellers. It creates a set of packets to configure a partial bitstream.
     * 
     * This method will add two frames of zeros at the end.
     * 
     */
 /*
    public PacketListCRC createPartialWritePackets(PacketListCRC packets, FPGA fpga,
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
     * The initial packets of a V4 bitstream.
     */
    public PacketListCRC createInitialFullBitstream(int idcode) {
    
        PacketListCRC packets = new PacketListCRC();
    
        packets.add(PacketUtils.NOP_PACKET);
        packets.add(PacketUtils.RCRC_CMD_PACKET);       
        packets.addAll(PacketUtils.NOP_PACKETS(2));
        packets.add(PacketUtils.DEFAULT_COR_PACKET);
        packets.add(PacketUtils.IDCODE_PACKET(idcode));
        packets.add(PacketUtils.SWITCH_CMD_PACKET);
        packets.add(PacketUtils.NOP_PACKET);
        packets.add(PacketUtils.DEFAULT_MASK_PACKET);
        packets.add(PacketUtils.DEFAULT_CTL_PACKET);
        packets.addAll(PacketUtils.NOP_PACKETS(1150));
        packets.add(PacketUtils.DEFAULT_MASK_PACKET);
        packets.add(PacketUtils.NULL_CTL_PACKET);
        packets.add(PacketUtils.NULL_CMD_PACKET);
        packets.add(PacketUtils.NOP_PACKET);        
        
        return packets;
    }
    

    /**
     * Creates the ending packets of a bitstream
     */
    public PacketListCRC createEndingFullBitstream(PacketListCRC packets, XilinxConfigurationSpecification spec) {
    
        packets.addCRCWritePacket();
        packets.add(PacketUtils.GRESTORE_CMD_PACKET);       
        packets.add(PacketUtils.NOP_PACKET);
        packets.add(PacketUtils.LFRM_CMD_PACKET);
        packets.addAll(PacketUtils.NOP_PACKETS(100));
        packets.add(PacketUtils.GRESTORE_CMD_PACKET);       
        packets.add(PacketUtils.NOP_PACKET);
        packets.add(PacketUtils.NULL_CMD_PACKET);       
        packets.add(PacketUtils.NOP_PACKET);
        int farAddrss = getEndingV4FARAddress(spec);
        packets.add(PacketUtils.FAR_WRITE_PACKET(farAddrss));
        packets.add(PacketUtils.START_CMD_PACKET);
        packets.add(PacketUtils.NOP_PACKET);
        packets.add(PacketUtils.NULL_MASK_PACKET);      
        packets.add(PacketUtils.NULL_CTL_PACKET);           
        packets.addCRCWritePacket();
        packets.add(PacketUtils.DESYNC_CMD_PACKET);
        packets.addAll(PacketUtils.NOP_PACKETS(16));
        
        return packets;
        
    }

    public PacketListCRC createPartialFDRIPackets(PacketListCRC packetList, ArrayList<Integer> data) throws BitstreamException {
    	boolean backwardCompatibility = true;

    	if (backwardCompatibility) {
			// This is used to make the bitstreams match Jonathan's old bitstreams. It should be removed
			// once Jonathon is happy with the results.
			if(data.size() < 1024){
				packetList.add(PacketUtils.TYPE_ONE_WRITE_PACKET(RegisterType.FDRI, data));
			}
			else{
		        packetList.add(PacketUtils.ZERO_WORD_WRITE_PACKET(RegisterType.FDRI));
		        packetList.add(PacketUtils.TYPE_TWO_WRITE_PACKET(data));
			}
		} else {
			// This is the function we want to use in the future
			packetList.addAll(PacketUtils.FDRI_WRITE_PACKETS(data));
		}
    	return packetList;
    }

 /*
    public PacketListCRC addMFWRPackets(PacketListCRC packets, List<Integer> farAddresses, List<Integer> data) throws BitstreamException {
    
        // WCFG command
        packets.add(PacketUtils.WCFG_CMD_PACKET);
        // NOP
        packets.add(PacketUtils.NOP_PACKET);    
        // FAR
        packets.add(PacketUtils.FAR_WRITE_PACKET(farAddresses.get(0)));     
        // NOP
        packets.add(PacketUtils.NOP_PACKET);    
        // FDRI                                 // Loads the frame that will be written to the several
                                                // locations into the frame buffer.
        packets.add(PacketUtils.FDRI_PACKET);
        packets.addAll(PacketUtils.WRITE_PACKETS(data));
        // MFWR command
        packets.add(PacketUtils.MFWR_CMD_PACKET);       
        // NOP
        packets.add(PacketUtils.NOP_PACKET);    
    
        for (int i = 1; i < data.size(); i++) {
            // FAR
            packets.add(PacketUtils.FAR_WRITE_PACKET(farAddresses.get(i)));     
            // MFWR command (with 2 zeros - why?) TODO: need to checkup on this
            packets.add(PacketUtils.MFWR_CMD_TWOWORD_PACKET);                   
        }
        
        return packets;
        
    }
*/
    
    /**
     * Determines the "ending" FAR address used for the last FAR write command in 
     * V4 bitstreams. This method demonstrates how the FAR address is calculated from
     * the part information.
     * 
     */
    protected int getEndingV4FARAddress(XilinxConfigurationSpecification spec) {
        // The last FAR write command in a bitstream has a non zero FAR address. After looking at
        // several devices and FAR addresses, these is the algorithm we found is used to compute 
        // this far address:
        // - topBottom = 0 (top)
        // - block type = 0 (which is logic)
        // - row = last row + 1 (i.e. if there are 2 rows, row = 2)
        // - column = last column in the row (which is logic overhead)
        // - minor - 0
        
        int topBottom = 0;
        int blockType = 0;
        int row = spec.getTopNumberOfRows();
        BlockType logicBlockType = spec.getLogicBlockType();
        int numberOfColumns = spec.getBlockSubTypeLayout(logicBlockType).size();
        int column = numberOfColumns - 1;
        int minor = 0;
        return FrameAddressRegister.createFAR(spec, topBottom, blockType, row, column, minor);
        
    }

    /**
     * Creates the initial part of a packet for partial configurations. This assumes that
     * the device has already been configured.
     *  
     * This was created based on Ben Sellers method: Bitstream.CreateBRAMPartialBody
     */
    public PacketListCRC createInitialPartialBitstream(int idcode) {
    
        PacketListCRC packets = new PacketListCRC();
    
        packets.add(PacketUtils.NOP_PACKET);
        packets.add(PacketUtils.RCRC_CMD_PACKET);       
        packets.addAll(PacketUtils.NOP_PACKETS(2));
        packets.add(PacketUtils.IDCODE_PACKET(idcode));
        packets.add(PacketUtils.WCFG_CMD_PACKET);       
        packets.add(PacketUtils.NOP_PACKET);
        return packets;
    }

    
    /**
     * This was created based on Ben Sellers method: Bitstream.CreateBRAMPartialBody
     */
    public PacketListCRC createEndingPartialBitstream(PacketListCRC packets) {
        packets.add(PacketUtils.LFRM_CMD_PACKET);
        packets.addAll(PacketUtils.NOP_PACKETS(101));
        packets.addCRCWritePacket();
        packets.add(PacketUtils.DESYNC_CMD_PACKET);
        packets.add(PacketUtils.NOP_PACKET);
        return packets;
    }
   
    
}
