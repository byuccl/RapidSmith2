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
import edu.byu.ece.rapidSmith.bitstreamTools.bitstream.Packet;
import edu.byu.ece.rapidSmith.bitstreamTools.bitstream.PacketListCRC;
import edu.byu.ece.rapidSmith.bitstreamTools.bitstream.PacketOpcode;
import edu.byu.ece.rapidSmith.bitstreamTools.bitstream.PacketUtils;
import edu.byu.ece.rapidSmith.bitstreamTools.bitstream.RegisterType;
import edu.byu.ece.rapidSmith.bitstreamTools.configurationSpecification.XilinxConfigurationSpecification;

public class V6BitstreamGenerator extends BitstreamGenerator {

public static int V6_ENDING_FAR = 0x00EF8000;
    
    private static V6BitstreamGenerator _singleton = null;
    
    private V6BitstreamGenerator() {
        
    }
    
    public static V6BitstreamGenerator getSharedInstance() {
        if (_singleton == null) {
            _singleton = new V6BitstreamGenerator();
        }
        return _singleton;
    }
    
    /**
     * This method creates the initial part of most configuration bitstreams.
     **/
    public PacketListCRC createInitialFullBitstream(int idcode) {
    
        PacketListCRC packets = new PacketListCRC();
    
        packets.add(PacketUtils.NOP_PACKET);
        packets.add(Packet.buildOneWordPacket(PacketOpcode.WRITE, RegisterType.WBSTAR, 0));
        packets.add(PacketUtils.NULL_CMD_PACKET);
        packets.add(PacketUtils.NOP_PACKET);
        packets.add(PacketUtils.RCRC_CMD_PACKET);
        packets.addAll(PacketUtils.NOP_PACKETS(2));
        packets.add(Packet.buildOneWordPacket(PacketOpcode.WRITE, RegisterType.TIMER, 0));
        packets.add(Packet.buildOneWordPacket(PacketOpcode.WRITE, RegisterType.UNKNOWN0, 0));
        packets.add(PacketUtils.COR_PACKET(0x00003FE5));
        packets.add(Packet.buildOneWordPacket(PacketOpcode.WRITE, RegisterType.COR1, 0));
        packets.add(PacketUtils.IDCODE_PACKET(idcode));
        packets.add(PacketUtils.SWITCH_CMD_PACKET);
        packets.add(PacketUtils.NOP_PACKET);
        packets.add(PacketUtils.MASK_PACKET(0x00000001));
        packets.add(PacketUtils.CTL_PACKET(0x00000101));
        packets.add(PacketUtils.MASK_PACKET(0));
        packets.add(Packet.buildOneWordPacket(PacketOpcode.WRITE, RegisterType.CTL1, 0));
        packets.addAll(PacketUtils.NOP_PACKETS(8));
        
        return packets;
    }

    /**
     * Creates the ending packets of a bitstream
     */
    public PacketListCRC createEndingFullBitstream(PacketListCRC packets, XilinxConfigurationSpecification spec) {

        packets.addCRCWritePacket();
        packets.addAll(PacketUtils.NOP_PACKETS(2));        
        packets.add(PacketUtils.GRESTORE_CMD_PACKET);       
        packets.add(PacketUtils.NOP_PACKET);
        packets.add(PacketUtils.LFRM_CMD_PACKET);
        packets.addAll(PacketUtils.NOP_PACKETS(100));
        packets.add(PacketUtils.START_CMD_PACKET);
        packets.add(PacketUtils.NOP_PACKET);
        packets.add(PacketUtils.FAR_WRITE_PACKET(V6_ENDING_FAR));
        packets.add(PacketUtils.MASK_PACKET(0x00000101));
        packets.add(PacketUtils.CTL_PACKET(0x00000101));
        packets.addCRCWritePacket();
        packets.addAll(PacketUtils.NOP_PACKETS(2));
        packets.add(PacketUtils.DESYNC_CMD_PACKET);
        packets.addAll(PacketUtils.NOP_PACKETS(400));
        
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

    
    /**
     * TODO:
     */
    public PacketListCRC createInitialPartialBitstream(int idcode) { return null; }
    public PacketListCRC createEndingPartialBitstream(PacketListCRC packets) { return null; }

}
