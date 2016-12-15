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
 * Each packet, except for NOP, reads/writes to a specific register.  Every register in the
 * Virtex-4 series is given here.  Also included is the DataToString function which gives
 * information about the data written to those registers. See Xilinx UG071 for more information
 * about the data being written.
 * 
 * Bits 26:13 contain the register address, but Bits 26:18 are reserved and should be set
 * to zeros.
 * 
 * @author Benjamin Sellers
 * Brigham Young University
 * Created: May 2008
 * Last Modified: 4/30/09
 * 
 */

/** 
 *  In a type 1 packet, the register is specified by bits 26:13, however only 17:13 are currently
 *  used, 26:18 are reserved
 *  <CODE>
 *  <br>    CRC    Read/Write 00000 CRC register
 *  <br>    FAR    Read/Write 00001 Frame Address Register
 *  <br>    FDRI   Write      00010 Frame Data Register, Input (write configuration data)
 *  <br>    FDRO   Read       00011 Frame Data Register, Output register (read configuration data)
 *  <br>    CMD    Read/Write 00100 Command Register
 *  <br>    CTL    Read/Write 00101 Control Register
 *  <br>    MASK   Read/Write 00110 Masking Register for CTL
 *  <br>    STAT   Read       00111 Status Register
 *  <br>    LOUT   Write      01000 Legacy Output Register (DOUT for daisy chain)
 *  <br>    COR    Read/Write 01001 Configuration Option Register
 *  <br>    MFWR   Write      01010 Multiple Frame Write
 *  <br>    CBC    Write      01011 Initial CBC value register
 *  <br>    IDCODE Read/Write 01100 Device ID register
 *  <br>    AXSS   Read/Write 01101 User bitstream access register
 *	<br>    COR1	Read/Write 01110 Configuration Option Register 1
 * 	<br>    CSOB   Write 	   01111 Used for daisy chain parallel interface
 *  <br>    WBSTAR	Read/Write 10000 Warm Boot Start Address Register
 *  <br>    TIMER	Read/Write 10001 Watchdog Timer Register
 *  <br>    BOOTSTSRead/Write 10110 Watchdog Timer Register
 *  <br>    CTL1	Read/Write 11000 Watchdog Timer Register
 *  <br>    UNKNOWN0 Unknown  10011 This is an undocumented register in V5 bitstreams
 *  </CODE>
 */


public enum RegisterType {
	// Virtex 4, 5, 6
	CRC(0x00000000), 
	FAR(0x00000001), 
	FDRI(0x00000002),
	FDRO(0x00000003), 
	CMD(0x00000004), 
	CTL0(0x00000005),
	MASK(0x00000006), 
	STAT(0x00000007), 
	LOUT(0x00000008),
	COR(0x00000009),   // This register is named COR for Virtex4
	COR0(0x00000009),  // This register is named COR0 for Virtex4 & 5
	MFWR(0x0000000A), 
	CBC(0x0000000B),
	IDCODE(0x0000000C), 
	AXSS(0x0000000D),
	// Virtex 5 & 6
	COR1(0x0000000E), 
	CSOB(0x0000000F), 
	WBSTAR(0x00000010),
	TIMER(0x00000011), 
	BOOTSTS(0x00000016), 
	CTL1(0x00000018),
	UNKNOWN0(0x00000013),
	// 
	NONE(0);
	
	private final int address;				//Shifted all the way to the right
	
	RegisterType(int address)
	{
		this.address = address;
	}
	
	public int Address() { return address; }//end Address
	
	public int PacketValue() {
	    return address << 13;
	}
	
	public static String DataToString(int data, RegisterType type)
	{
		if(type == CTL0) { return CTL0DataToString(data); }
		else if(type == COR0) {	return COR0DataToString(data); }
		else if(type == COR1) {	return COR1DataToString(data); }
		else if(type == CMD) { return CMDDataToString(data); }
		else if(type == WBSTAR) { return WBSTARDataToString(data); }
		else if(type == TIMER) { return TIMERDataToString(data); }
		return "";
	}

	public static int REGISTER_MASK = 0x0003E000;

	/**
	 * Gets the register type based on bits 17:13.
	 * @param header The integer version of the header.
	 * @return The RegisterType based on header.
	 */
	public static RegisterType getRegisterType(int header) {
		RegisterType reg = RegisterType.NONE;
		header = (REGISTER_MASK & header) >> 13;
		for (RegisterType registerType : values()) {
		    if (registerType.Address() == header) {
		        reg = registerType;
		        break;
		    }
		}
		return reg;
	}

	private static String CTL0DataToString(int data)
	{
		String string = "";
		
		int bitmask;
		
		// Virtex 4, 5
		bitmask = 0x60000000;			//ICAP_SEL
		if((bitmask & data) != 0) { string += "Bottom ICAP Port Enabled, "; }
		else { string += "Top ICAP Port Enabled, "; }
		
		// Virtex 5
		bitmask = 0x00001000;			//OverTempPowerDown
		if((bitmask & data) != 0) { string += "(V5 only)OverTempPowerDown enabled, "; }
		else { string += "(V5 only)OverTempPowerDown disabled, "; }
		
		// Virtex 5
		bitmask = 0x00000400;			//ConfigFallback
		if((bitmask & data) != 0) { string += "(V5 only)ConfigFallback disabled, "; }
		else { string += "(V5 only)ConfigFallback enabled, "; }
		
		// Virtex 5
		bitmask = 0x00000200;			//SelectMAPAbort
		if((bitmask & data) != 0) { string += "(V5 only)SelectMAPAbort disabled, "; }
		else { string += "(V5 only)SelectMAPAbort enabled, "; }
		
		// Virtex 4, 5
		bitmask = 0x00000100;			//GLUTMASK
		if((bitmask & data) != 0) { string += "GLUTMASK 1, "; }
		else { string += "GLUTMASK 0, "; }
		
		// Virtex 5
		bitmask = 0x00000040;			//DEC - AES Decryptor enable bit
		if((bitmask & data) != 0) { string += "(V5 only)DEC enabled, "; }
		else { string += "(V5 only)DEC disabled, "; }
		
		// Virtex 4, 5
		bitmask = 0x00000030;			//SBITS
		switch (bitmask & data)
		{
			case 0x00000030:
			case 0x00000020: string += "Rdbk Dsabld, Wr Dsabld excpt CRC, "; break;
			case 0x00000010: string += "Rdbk Dsabld, "; break;
			default : string += "Rd/Wr OK, ";			
		}
		
		// Virtex 4, 5
		bitmask = 0x00000008;			//PERSIST
		if((bitmask & data) != 0) { string += "Persist Yes, "; }
		else { string += "Persist No, "; }
		
		// Virtex 4, 5
		bitmask = 0x00000001;			//GTS_USER_B
		if((bitmask & data) != 0) { string += "I/Os active"; }
		else { string += "I/Os placed in high-Z state"; }
		
		return string;
	}//end CTL0DataToString
	
	
	private static String COR0DataToString(int data)
	{
		String string = "";
		
		int bitmask;
		
		// Virtex 4, 5
		bitmask = 0x10000000;			//CRC_BYPASS
		if((bitmask & data) == 0) { string += "CRC enabled, "; }
		else { string += "CRC disabled, "; }
		
		// Virtex 5
		bitmask = 0x08000000;			//PWRDWN_STAT
		if((bitmask & data) == 0) { string += "(V5 only)DONE pin, "; }
		else { string += "Powerdown pin, "; }
		
		// Virtex 4, 5
		bitmask = 0x02000000;			//DONE_PIPE
		if((bitmask & data) == 0) { string += "No pipeline stage for DONEIN, "; }
		else { string += "Add pipeline stage for DONEIN, "; }

		// Virtex 4, 5
		bitmask = 0x01000000;			//DRIVE_DONE
		if((bitmask & data) == 0) { string += "DONE pin is open drain, "; }
		else { string += "DONE pin is actively driven high, "; }
		
		// Virtex 4, 5
		bitmask = 0x00800000;			//SINGLE
		if((bitmask & data) == 0) { string += "Readback not single-shot, "; }
		else { string += "Readback single-shot, "; }
		
		// Virtex 4, 5
		bitmask = 0x007E0000;			//OSCFSEL
		int tmp = (bitmask & data) >>> 17;
		string += "OSCFSEL: " + tmp + ", ";

		// Virtex 4, 5		
		bitmask = 0x00018000;			//SSCLKSRC
		switch (bitmask & data)
		{
			case 0x00018000:
			case 0x00010000: string += "JTAGClk, "; break;
			case 0x00008000: string += "UserClk, "; break;
			default : string += "CCLK, ";			
		}
		
		// Virtex 4, 5
		bitmask = 0x00007000;			//DONE_CYCLE
		tmp = (bitmask & data) >>> 12;
		string += "DONE_CYCLE: " + (tmp + 1) + ", ";
		
		// Virtex 4, 5
		bitmask = 0x00000E00;			//MATCH_CYCLE
		tmp = (bitmask & data) >>> 9;
		if(tmp == 7) { string += "MATCH_CYCLE: No Wait, "; }
		else { string += "MATCH_CYCLE: " + (tmp + 1) + ", "; }
		
		// Virtex 4, 5
		bitmask = 0x0000001C0;			//LOCK_CYCLE
		tmp = (bitmask & data) >>> 6;
		if(tmp == 7) { string += "LOCK_CYCLE: No Wait, "; }
		else { string += "LOCK_CYCLE: " + (tmp + 1) + ", "; }
		
		// Virtex 4, 5
		bitmask = 0x00000038;			//GTS_CYCLE
		tmp = (bitmask & data) >>> 3;
		if(tmp == 0x00000006) { string += "GTS tracks DONE pin"; }
		else if(tmp == 0x00000007) { string += "GTS_CYCLE: Keep"; }
		else { string += "GTS_CYCLE: " + (tmp + 1) + ", "; }
		
		// Virtex 4, 5
		bitmask = 0x00000007;			//GWE_CYCLE
		tmp = (bitmask & data);
		if(tmp == 0x00000006) { string += "GWE tracks DONE pin"; }
		else if(tmp == 0x00000007) { string += "GWE_CYCLE: Keep"; }
		else { string += "GWE_CYCLE: " + (tmp + 1) + " "; } 
		
		return string;
	}//end COR0DataToString
	
	private static String COR1DataToString(int data)
	{
		String string = "";
		
		int bitmask;
		
		// Virtex 5
		bitmask = 0x00020000;			//PERSIST_DEASSERT_AT_DESYNCH
		if((bitmask & data) == 0) { string += "PERSIST_DEASSERT_AT_DESYNCH disabled, "; }
		else { string += "PERSIST_DEASSERT_AT_DESYNCH enabled, "; }
		
		// Virtex 5
		bitmask = 0x00000200;			//RBCRC_NO_PIN
		if((bitmask & data) == 0) { string += "RBCRC_NO_PIN disabled, "; }
		else { string += "RBCRC_NO_PIN enabled, "; }
		
		// Virtex 5
		bitmask = 0x00000100;			//RBCRC_EN
		if((bitmask & data) == 0) { string += "RBCRC_EN disabled, "; }
		else { string += "RBCRC_EN enabled, "; }

		// Virtex 5
		bitmask = 0x00000003;			//BPI_1ST_READ_CYCLES
		int tmp = (bitmask & data) >>> 2;
		string += "First byte read timing: " + (tmp + 1) + " CCLKs, ";
		
		// Virtex 5		
		bitmask = 0x00000003;			//BPI_PAGE_SIZE
		switch (bitmask & data)
		{
			case 0x00000000: string += "Flash mem page size: 1 byte/word"; break;
			case 0x00000001: string += "Flash mem page size: 4 bytes/word"; break;
			case 0x00000002: string += "Flash mem page size: 8 bytes/word"; break;
			case 0x00000003: string += "Flash mem page size: Reserved"; break;
		}
		
		return string;
	}//end COR1DataToString
	

	
	private static String CMDDataToString(int data)
	{
		String string = "";
		
		switch(data)
		{
			// Virtex 4, 5
			case 0: string += "NULL"; break;
			case 1: string += "WCFG"; break;
			case 2: string += "MFWR"; break;
			case 3: string += "LFRM/DGHIGH"; break;
			case 4: string += "RCFG"; break;
			case 5: string += "START"; break;
			case 6: string += "RCAP"; break;
			case 7: string += "RCRC"; break;
			case 8: string += "AGHIGH"; break;
			case 9: string += "SWITCH"; break;
			case 10: string += "GRESTORE"; break;
			case 11: string += "SHUTDOWN"; break;
			case 12: string += "GCAPTURE"; break;
			case 13: string += "DESYNC"; break;
			// Virtex 5
			case 15: string += "IPROG"; break;
			case 16: string += "CRCC"; break;
			case 17: string += "LTIMER"; break;
			
			default: string += "NONE"; break;
		}
		return string;
	}//end CMDDataToString
	
	private static String WBSTARDataToString(int data)
	{
		String string = "";
		
		int bitmask;
		
		// Virtex 5
		bitmask = 0x18000000;			//RS[1:0]
		int tmp = (bitmask & data) >>> 27;
		string += "RS[1:0] pin value: " + tmp + ", ";
		
		// Virtex 5
		bitmask = 0x04000000;			//RS_TS_B
		if((bitmask & data) == 0) { string += "RS_TS_B disabled, "; }
		else { string += "RS_TS_B enabled, "; }
		
		// Virtex 5
		bitmask = 0x03FFFFFF;			//START_ADDR
		tmp = bitmask & data;
		string += "Fallback bitstream start address: " + Integer.toHexString(tmp);
		
		return string;
	}//end WBSTARDataToString	
		
	private static String TIMERDataToString(int data)
	{
		String string = "";
		
		int bitmask;
		
		// Virtex 5
		bitmask = 0x02000000;			//TIMER_USR_MON
		if((bitmask & data) == 0) { string += "TIMER_USR_MON disabled, "; }
		else { string += "TIMER_USR_MON enabled, "; }
		
		// Virtex 5
		bitmask = 0x01000000;			//TIMER_CFG_MON
		if((bitmask & data) == 0) { string += "TIMER_CFG_MON disabled, "; }
		else { string += "TIMER_CFG_MON enabled, "; }
		
		// Virtex 5
		bitmask = 0x00FFFFFF;			//TIMER_VALUE
		int tmp = bitmask & data;
		string += "Fallback bitstream start address: " + Integer.toHexString(tmp);
		
		return string;
	}//end TIMERDataToString	
	
}//end enum RegisterType
