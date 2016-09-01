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

/**
 * An enumeration of the possible packet operations. A packet can either specify a read, a write, or a nop.
 * Bits 28:27 represent the opcode
 * 
 * @author Benjamin Sellers
 * Brigham Young University
 * Created: May 2008
 * Last Modified: 6/18/08
 *
 */

/** 
 * In both type 1 and 2 packets, bits 28:27 of the header determine the opcode 
 * <CODE>
 * <br>    Opcode 00: NOP
 * <br>    Opcode 01: Read
 * <br>    Opcode 10: Write
 * <br>    Opcode 11: Reserved
 * </CODE>
 */

public enum PacketOpcode {
	
	NOP(0x00000000, 0x00000000), 
	READ(0x00000001, 0x08000000), 
	WRITE(0x00000002, 0x10000000), 
	RESERVED(0x00000003, 0x18000000), 
	NONE(0x00000000, 0x00000000);

	/**
	 * Constructor for a PacketOpcode
	 * @param value the value of the opcode (two LSBs)
	 * @param packetValue This is the packet value as extracted directly from the header.
	 */
	PacketOpcode(int value, int packetValue) {
		this.value = value;
		this.packetValue = packetValue;
	}//end constructor
	
	/**
	 * Returns the value of the Opcode in bits 1:0
	 * @return the Opcode in the 2 LSBs
	 */
	public int Value() { return value; }//end Value
	
	/**
	 * Returns the value of the Opcode in bits 28:27
	 * @return The Opcode 
	 */
	public int PacketValue() { return packetValue; }//end PacketValue
	
	public static int PACKET_OPCODE_MASK = 0x18000000;
	
	/**
	 * Gets the packet opcode based on bits 28:27.
	 * @param header The integer version of the header.
	 * @return The PacketOpcode based on header.
	 */
	public static PacketOpcode getPacketOpcode(int header) {
		header = PACKET_OPCODE_MASK & header;
		if(header == 0x00000000)
			return PacketOpcode.NOP;
		else if (header == 0x08000000)
			return PacketOpcode.READ;
		else if (header == 0x10000000)
			return PacketOpcode.WRITE;
		else if (header == 0x18000000)
			return PacketOpcode.RESERVED;
		else
			return PacketOpcode.NONE;
	}//end GetPacketOpcode

	/** This is the Opcode value shifted to the two LSBs */
	private final int value;				
	/** This is the Opcode value in the exact location in the packet header */
	private final int packetValue;			
	

}//end enum PacketOpcode
