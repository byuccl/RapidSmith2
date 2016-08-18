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
package edu.byu.ece.rapidSmith.bitstreamTools.bitstream;

import java.util.List;



/**
 * Contains a number of static Packet objects of commonly used packets and
 * several static methods for creating useful packet lists.
 *  
 */
public class PacketUtils {

	/**
	 * Default command register values
	 */
	public static final int RCRC_COMMAND = 0x00000007;
	public static final int SWITCH_COMMAND = 0x00000009;
	public static final int NULL_COMMAND = 0x00000000;
	public static final int DESYNC_COMMAND = 0x0000000D;
	public static final int RCFG_COMMAND = 0x00000004;
	public static final int START_COMMAND = 0x00000005;
	public static final int GRESTORE_COMMAND = 0x0000000A;
	public static final int GCAPTURE_COMMAND = 0x0000000C;
	// Last Frame: Deasserts the GHIGH_B signal, activating all interconnect.
	// The GHIGH_B signal is asserted with the AGHIGH command.
	public static final int LFRM_COMMAND = 0x00000003;
	public static final int WCFG_COMMAND = 0x00000001;
	public static final int MFWR_COMMAND = 0x00000002;

	/**
	 * Default Configuration Options Register (V4)
	 * 
	 * 2:0   GWE_CYCLE       101 (Start global write enable in cycle 6)
	 * 5:3   GTS_CYCLE       100 (Start global try state in cycle 5)
	 * 8:6   LOCK_CYCLE      111 (No startup wait for DCM lock)
	 * 11:9  MATCH_CYCLE     111 (no DCI startup wait)
	 * 14:12 DONE_CYCLE      011 (Release Done during startup cycle 4)
	 * 16:15 SSCLKSRC        00  (CCLK is the startup source clock)
	 * 22:17 OSCFSEL         000010 (CCLK frequency)
	 * 23    SINGLE          0 Readback is not single shot
	 * 24    DRIVE_DONE      0 Done pin is open drain
	 * 25    DONE_PIPE       0
	 * 27:26 Reserved        00     
	 * 28    CRC_BYPASS      0  CRC enabled
	 * 31:29 Reserved        000
	 */
	public static final int DEFAULT_COR = 0x00043FE5; // 000000_0_0_0_000010_00_011_111_111_100_101

	// Mask for setting the GLUTMASK_B
	public static final int DEFAULT_MASK_REGISTER =  0x00000600;
	public static final int NULL_MASK_REGISTER = 0x00000000;

	public static final int DEFAULT_CTL_REGISTER = 0x00000600;
	public static final int NULL_CTL_REGISTER = 0x00000000;
	
	public static final int DEFAULT_CTL1_REGISTER = 0x0;
	
	// There are a variety of packets that are so common that static versions of these
	// packets are created to simplify the process of bitstream creation.


	public static Packet NOP_PACKET = Packet.buildZeroWordPacket(PacketOpcode.NOP, RegisterType.NONE);
	public static Packet DEFAULT_COR_PACKET = COR_PACKET(DEFAULT_COR);
	public static Packet DEFAULT_MASK_PACKET = MASK_PACKET(DEFAULT_MASK_REGISTER);
	public static Packet NULL_MASK_PACKET = MASK_PACKET(NULL_MASK_REGISTER);
	public static Packet DEFAULT_CTL_PACKET = CTL_PACKET(DEFAULT_CTL_REGISTER);
	public static Packet NULL_CTL_PACKET = CTL_PACKET(NULL_CTL_REGISTER);
	
	// Command packets
	public static Packet RCRC_CMD_PACKET = CMD_PACKET(RCRC_COMMAND);
	public static Packet SWITCH_CMD_PACKET = CMD_PACKET(SWITCH_COMMAND);
	public static Packet NULL_CMD_PACKET = CMD_PACKET(NULL_COMMAND);
	public static Packet DESYNC_CMD_PACKET = CMD_PACKET(DESYNC_COMMAND);
	public static Packet RCFG_CMD_PACKET = CMD_PACKET(RCFG_COMMAND);
	public static Packet START_CMD_PACKET = CMD_PACKET(START_COMMAND);
	public static Packet GRESTORE_CMD_PACKET = CMD_PACKET(GRESTORE_COMMAND);
	public static Packet GCAPTURE_CMD_PACKET = CMD_PACKET(GCAPTURE_COMMAND);
	public static Packet LFRM_CMD_PACKET = CMD_PACKET(LFRM_COMMAND);
	public static Packet WCFG_CMD_PACKET = CMD_PACKET(WCFG_COMMAND);
	public static Packet MFWR_CMD_PACKET = CMD_PACKET(MFWR_COMMAND);
	// I don't know what this is or why it is used.
	public static Packet MFWR_CMD_TWOWORD_PACKET =
		Packet.buildTwoWordPacket(PacketOpcode.WRITE, RegisterType.CMD, 0, 0);

	public static Packet FDRI_ZERO_WORD_WRITE_PACKET = ZERO_WORD_WRITE_PACKET(RegisterType.FDRI);

	// V5 packets
    public static Packet DEFAULT_CTL1_PACKET = CTL1_PACKET(DEFAULT_CTL1_REGISTER);
	
	
	
	// There are a variety of common packets that need to be customized by the user
	// These static methods simplify the process of creating common user specific
	// packets.

	public static Packet ZERO_WORD_WRITE_PACKET(RegisterType register) {
		return Packet.buildZeroWordPacket(PacketOpcode.WRITE, register);
	}

	public static Packet ZERO_WORD_READ_PACKET(RegisterType register) {
		return Packet.buildZeroWordPacket(PacketOpcode.READ, register);
	}
	
	public static Packet CMD_PACKET(int cmdValue) {
		return Packet.buildOneWordPacket(PacketOpcode.WRITE, RegisterType.CMD, cmdValue);
	}
	
	public static Packet COR_PACKET(int corValue) {
		return Packet.buildOneWordPacket(PacketOpcode.WRITE, RegisterType.COR0, corValue);
	}
	
	public static Packet MASK_PACKET(int maskValue) {
		return Packet.buildOneWordPacket(PacketOpcode.WRITE, RegisterType.MASK, maskValue);
	}
	
	public static Packet CTL_PACKET(int ctlValue) {
		return Packet.buildOneWordPacket(PacketOpcode.WRITE, RegisterType.CTL0, ctlValue);
	}
	
	public static Packet CTL1_PACKET(int ctlValue) {
		return Packet.buildOneWordPacket(PacketOpcode.WRITE, RegisterType.CTL1, ctlValue);
	}
	
	public static Packet FAR_WRITE_PACKET(int addr) {
		return Packet.buildOneWordPacket(PacketOpcode.WRITE, RegisterType.FAR, addr);
	}
	
	public static Packet IDCODE_PACKET(int idcode) {
	    return Packet.buildOneWordPacket(PacketOpcode.WRITE, RegisterType.IDCODE, idcode);
	}
	
	public static Packet CRC_WRITE_PACKET(int crc) {
	    return Packet.buildOneWordPacket(PacketOpcode.WRITE, RegisterType.CRC, crc);
	}
	
	public static Packet TYPE_TWO_WRITE_PACKET(List<Integer> data) throws BitstreamException {
	    return Packet.buildMultiWordType2Packet(PacketOpcode.WRITE, data);
	}

	public static Packet TYPE_ONE_WRITE_PACKET(RegisterType registerType, List<Integer> data) throws BitstreamException {
	    return Packet.buildMultiWordType1Packet(PacketOpcode.WRITE, registerType, data);
	}

	public  static Packet TYPE_TWO_READ_PACKET(int numWords) throws BitstreamException {
		return Packet.buildZeroWordType2Packet(PacketOpcode.READ, numWords);
	}
	// Construct a read FDRO command
	public static PacketList FDRO_READ_PACKETS(int numFrames) {
		return READ_PACKETS(RegisterType.FDRO, numFrames);
	}
	// Construct a read register command packet
	public  static PacketList READ_PACKETS(RegisterType register, int numFrames) {
		PacketList result = new PacketList();
		result.add(ZERO_WORD_READ_PACKET(register));
		try {
			result.add(TYPE_TWO_READ_PACKET(42 + numFrames*41));
		} catch (BitstreamException e) {
			// we know this fits so this exception won't happen
		}
		return result;
	}

	/**
	 * Create the necessary packets for writing data. 
	 * 
	 * If the data can fit in a single type 1 packet (less than Packet.MAX_TYPE_ONE_SIZE),
	 * a single type one write packet will be created. 
	 * 
	 * If it is too large to fit in
	 * a type 1 packet but small enough for a type 2 packet, a zero word type 1 packet
	 * followed by a type 2 packet will be created. If there are too many words for a
	 * type 2 packet, an exception will be thrown.
	 * 
	 * @param data The data to write into the packets.
	 * @return The resulting packet list.
	 * @throws BitstreamException
	 */
	public static PacketList WRITE_PACKETS(RegisterType register, List<Integer> data) throws BitstreamException {
	    PacketList result = new PacketList();
	    if (data.size() <= Packet.MAX_TYPE_ONE_SIZE) {
	        try {
                result.add(TYPE_ONE_WRITE_PACKET(register, data));
            } catch (BitstreamException e) {
                // we know this fits so this exception won't happen
            }	        
	    }
	    else if (data.size() <= Packet.MAX_TYPE_TWO_SIZE) {
	        result.add(ZERO_WORD_WRITE_PACKET(register));
	        try {
                result.add(TYPE_TWO_WRITE_PACKET(data));
            } catch (BitstreamException e) {
                // we know this fits so this exception won't happen
            }	        
	    }
	    else {
	        throw new BitstreamException("Error: attempting to create a packet with too many words.");
	    }
	    return result;
	}

	/** 
	 * Create a set of packets for writing into the FDRI register.
	 */
	public static PacketList FDRI_WRITE_PACKETS(List<Integer> data) throws BitstreamException {
		return WRITE_PACKETS(RegisterType.FDRI, data);
	}
	
	public static PacketList NOP_PACKETS(int numNOPs) {
		PacketList nopPackets = new PacketList();
		for(int i = 0; i < numNOPs; i++){
			nopPackets.add(NOP_PACKET);
		}
		return nopPackets;
	}
	
}
