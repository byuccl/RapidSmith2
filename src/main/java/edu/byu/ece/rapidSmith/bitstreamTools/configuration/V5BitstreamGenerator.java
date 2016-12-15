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
package edu.byu.ece.rapidSmith.bitstreamTools.configuration;

import java.util.ArrayList;

import edu.byu.ece.rapidSmith.bitstreamTools.bitstream.BitstreamException;
import edu.byu.ece.rapidSmith.bitstreamTools.bitstream.Packet;
import edu.byu.ece.rapidSmith.bitstreamTools.bitstream.PacketListCRC;
import edu.byu.ece.rapidSmith.bitstreamTools.bitstream.PacketOpcode;
import edu.byu.ece.rapidSmith.bitstreamTools.bitstream.PacketUtils;
import edu.byu.ece.rapidSmith.bitstreamTools.bitstream.RegisterType;
import edu.byu.ece.rapidSmith.bitstreamTools.configurationSpecification.XilinxConfigurationSpecification;

/**
 * 
 * Question for Jon: How different are the initial and ending bitstreams
 * for V5 and V6? Is it necessary to have separate classes?
 * 
 */
public class V5BitstreamGenerator extends BitstreamGenerator {

	public static int V5_ENDING_FAR = 0x00EF8000;
    
    private static V5BitstreamGenerator _singleton = null;
    
    private V5BitstreamGenerator() {
        
    }
    
    public static V5BitstreamGenerator getSharedInstance() {
        if (_singleton == null) {
            _singleton = new V5BitstreamGenerator();
        }
        return _singleton;
    }
    
    /**
     * Initial packets of a V5 bitstream.
     */
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
        packets.add(PacketUtils.MASK_PACKET(0x00400000));
        packets.add(PacketUtils.CTL_PACKET(0x00400000));
        packets.add(PacketUtils.MASK_PACKET(0));       
        //packets.add(Packet.buildOneWordPacket(PacketOpcode.WRITE, RegisterType.CTL1, 0));
        packets.add(PacketUtils.DEFAULT_CTL1_PACKET);
        packets.addAll(PacketUtils.NOP_PACKETS(8));
        
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
        packets.addAll(PacketUtils.NOP_PACKETS(30));
        packets.add(PacketUtils.START_CMD_PACKET);
        packets.add(PacketUtils.NOP_PACKET);
        packets.add(PacketUtils.FAR_WRITE_PACKET(V5_ENDING_FAR));
        packets.add(PacketUtils.MASK_PACKET(0x00400000));
        packets.add(PacketUtils.CTL_PACKET(0x00400000));
        packets.addCRCWritePacket();
        packets.add(PacketUtils.DESYNC_CMD_PACKET);
        packets.addAll(PacketUtils.NOP_PACKETS(61));
        
        return packets;

    }

    /**
     * Creates the initial part of a packet for partial configurations. This
     * list of packets was created based on a bitstream given to us from
     * Sandia.
     */
    public PacketListCRC createInitialPartialBitstream(int idcode) {
    
        PacketListCRC packets = new PacketListCRC();

        // This turns out to be the same as V4
        packets.add(PacketUtils.NOP_PACKET);
        packets.add(PacketUtils.RCRC_CMD_PACKET);       
        packets.addAll(PacketUtils.NOP_PACKETS(2));
        packets.add(PacketUtils.IDCODE_PACKET(idcode));
        packets.add(PacketUtils.WCFG_CMD_PACKET);       
        packets.add(PacketUtils.NOP_PACKET);
        return packets;
    }

    
    /**
     * Creates the initial part of a packet for partial configurations. This
     * list of packets was created based on a bitstream given to us from
     * Sandia.
     */
    public PacketListCRC createEndingPartialBitstream(PacketListCRC packets) {
        		
        packets.add(PacketUtils.MASK_PACKET(0x00001000));
    	packets.add(PacketUtils.DEFAULT_CTL1_PACKET);
        packets.add(PacketUtils.LFRM_CMD_PACKET);
        packets.addAll(PacketUtils.NOP_PACKETS(101));
        packets.add(PacketUtils.FAR_WRITE_PACKET(V5_ENDING_FAR));
        packets.addCRCWritePacket();
        packets.add(PacketUtils.DESYNC_CMD_PACKET);
        // Jonathan Donaldson requested this final NOP. Is it necessary? Should we make
        // this an option?
        packets.add(PacketUtils.NOP_PACKET);
        return packets;
    }
    
    public PacketListCRC createPartialFDRIPackets(PacketListCRC packetList, ArrayList<Integer> data) throws BitstreamException {
    	// The example PR bitfiles from Xilinx only use type two writes. We are not sure why but
    	// will do the same.
    	packetList.add(PacketUtils.ZERO_WORD_WRITE_PACKET(RegisterType.FDRI));
        packetList.add(PacketUtils.TYPE_TWO_WRITE_PACKET(data));
    	return packetList;
    }

}
