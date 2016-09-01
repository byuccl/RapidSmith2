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
 * Several cyclic redundancy checks are performed while the bitstream is being loaded.  For a full
 * bitstream it is performed once when the command register is loaded with the RCRC (reset CRC)
 * instruction and twice after we have finished writing to the FDRI when we load the CRC registers.
 * The data loaded into the CRC registers is used to compute a new CRC value.  This value should
 * always be zero. If it isn't, an error will be thrown and the done bit will never go high.
 *  
 * You have the ability to turn off the CRC check by replacing those instructions with NOPs and
 * setting the CRC_BYPASS bit (Bit 28) in the COR register, but obviously to insure robustness
 * this check needs to be performed.
 * 
 * This class computes and keeps track of the CRC value that will be compared against zero.
 * If the value is zero when the check is performed, it has passed that CRC check; otherwise,
 * an error is thrown.
 * We also compute the value that must be loaded into the CRC register to allow this to happen.
 * That value is just the CRC value with a full bit swap.
 * 
 * The Virtex 4 and 5 devices use a standard CRC32C (32-bit Castagnoli CRC).
 * x32 + x28 + x27 + x26 + x25 + x23 + x22 + x20 + x19 + x18 + x14 + x13 + x11 + x10 + x9 + x8 + x6 + 1
 * The CRC value is updated every time data is loaded into a register.  The data is shifted in 
 * LSB first until all bits have been use in computation.  Then the five-bit register address
 * value is shifted in.  If multiple data words are loaded into a register, it does this 
 * data/address combination for each word. 
 * 
 * We are using 0x1EDC6F41 as our XOR value. 
 * 
 * @author Benjamin Sellers
 * Brigham Young University
 * Created: March 2008
 * Last Modified: 5/6/08
 *
 */
public class CRC {
	
	/** Value of the CRC */
	private int crcValue;
	
	/** Generator Polynomial: 0x1EDC6F41 */
	private static final int XORnormal = 0x1EDC6F41;
	
	/**
	 * Constructor, simply sets crcValue to zero.
	 */
    public CRC() {
    	crcValue = 0;
    }//end constructor    
    
    /**
     * Updates the CRC value based on Packet p.
     * @param p The packet which is used to update the CRC.
     */
	public void updateCRC(Packet p) {
		List<Integer> data = p.getData();
		if(data.size() == 0) { //If there isn't any data, we don't need to update the CRC
			return;
		}
		
		int regAddress;
		if(p.getPacketType() == PacketType.ONE) {
			RegisterType regType = p.getRegType();
			if(regType == RegisterType.NONE) { //Invalid register type 	
				return; 
			}
			else if(regType == RegisterType.CMD && data.get(0) == 0x00000007) { //RCRC command
				crcValue = 0;
				return;
			}
			regAddress = regType.Address();
		}
		else regAddress = 0x00000002;			//If type 2, we will assume FDRI
		
		for(int d : data){
			for(int i = 0; i < 32; i++) {		//Shift in the data one bit at a time
				shiftIn_OneBit(d >> i);
			}
			for(int i = 0; i < 5; i++) { 		//Shift in the reg address one bit at a time
				shiftIn_OneBit(regAddress >> i);
			}
		}
	}//end UpdateCRC
	
	/**
	 * Shifts in one bit from i and updates the crcValue
	 * @param i Input data
	 */
	private void shiftIn_OneBit(int i) {
		int val = ((crcValue>>31) ^ i) & 0x00000001;
		if(val != 0) {
			crcValue <<= 1;
			crcValue ^= XORnormal;
		}
		else crcValue <<= 1;
	}//end ShiftIn_OneBit
	
	/**
	 * Gets the current CRC and returns it.
	 * @return The current crcValue
	 */
	public int getValue() { return crcValue; }//end GetValue
	
	/**
	 * The method creates the CRC Register value need to be placed in the CRC register for
	 * a CRC check to pass.  Essentially, this reverses the 32 bits of the CRC.  Any time
	 * a CRC write is performed, this function should be called.
	 * @return The value to be placed in the CRC Register for the CRC check to pass
	 */
	public int computeCRCRegValue() {
		int crcRegValue = 0x00000000;
		int mask = 0x00000001;		
		for(int i = 0; i < 32; i++){
			crcRegValue |= ((crcValue >> i) & mask) << (31 - i);
		}
		return crcRegValue;
	}//end ComputeCRCRegValue
}//end class CRC
