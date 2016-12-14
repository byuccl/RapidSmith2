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

import java.util.ArrayList;
import java.util.List;

/**
 * Contains a number of static methods used within the bitstream creation and parsing process.
 * 
 * Most of these methods came from Ben's original bitmanip code.
 * 
 */
public class BitstreamUtils {

	/*
	 * Takes in a byte and converts it into a hex string
	 */
	public static String toHexString(byte b)
	{
		StringBuilder buf = new StringBuilder();
		char[] hexChars = {'0', '1', '2', '3', '4', '5', '6', '7',
						   '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };
		int high = 0;
		int low = 0;
		
		high = ((b & 0xf0) >> 4);
		low = (b & 0x0f);
		buf.append(hexChars[high]);
		buf.append(hexChars[low]);

    	return buf.toString();
	}//end toHexString byte
	
	/*
	 * Takes in an array of bytes and converts it into a hex string
	 */
	public static String toHexString(byte[] block)
	{
		StringBuilder buf = new StringBuilder();
		char[] hexChars = {'0', '1', '2', '3', '4', '5', '6', '7',
						   '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };
		int len = block.length;
		int high = 0;
		int low = 0;
		for (int i = 0; i < len; i++) {
			high = ((block[i] & 0xf0) >> 4);
			low = (block[i] & 0x0f);
			buf.append(hexChars[high]);
			buf.append(hexChars[low]);
		}
    		return buf.toString();
	}//end toHexString byte[]

	/*
	 * Takes in an array of bytes and converts it into a hex string
	 */
	public static String toHexString(int integer)
	{
		byte block[] = new byte[4];
		block[0] = (byte)( (integer >> 24) & 0x000000FF );
		block[1] = (byte)( (integer >> 16) & 0x000000FF );
		block[2] = (byte)( (integer >> 8) & 0x000000FF );
		block[3] = (byte)( integer & 0x000000FF );
		
		StringBuilder buf = new StringBuilder();
		char[] hexChars = {'0', '1', '2', '3', '4', '5', '6', '7',
						   '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };
		int len = block.length;
		int high = 0;
		int low = 0;
		for (int i = 0; i < len; i++) {
			high = ((block[i] & 0xf0) >> 4);
			low = (block[i] & 0x0f);
			buf.append(hexChars[high]);
			buf.append(hexChars[low]);
		}
    		return buf.toString();
	}//end toHexString int
	
	/*
	 * Takes in a string representation of hex numbers and converts it into a byte
	 */
	public static byte toByte(String string)
	{
		assert string.length() == 2;
		return (byte) ((byte) ( decodeHexChar( string.charAt(0) ) << 4 )
				              | decodeHexChar( string.charAt(1) ));
	}//end toByte
	
	/*
	 * Takes in a string representation of hex numbers and converts it into an int
	 */
	public static int toInt(String string)
	{
		assert string.length() == 4;
		int integer = 0;
		for(int i = 0; i < string.length(); i++)
		{
			byte tmp = decodeHexChar( string.charAt(i) );
			integer <<= 4;
			integer |= tmp;
		}
		return integer;
	}//end toInt
	
	/*
	 * Converts the character from ascii to byte form
	 */
	private static byte decodeHexChar(char c)
	{
		byte b;
		switch (c)
		{
			case 'F': b = (byte)0x0000000F; break;
			case 'E': b = (byte)0x0000000E; break;
			case 'D': b = (byte)0x0000000D; break;
			case 'C': b = (byte)0x0000000C; break;
			case 'B': b = (byte)0x0000000B; break;
			case 'A': b = (byte)0x0000000A; break;
			case '9': b = (byte)0x00000009; break;
			case '8': b = (byte)0x00000008; break;
			case '7': b = (byte)0x00000007; break;
			case '6': b = (byte)0x00000006; break;
			case '5': b = (byte)0x00000005; break;
			case '4': b = (byte)0x00000004; break;
			case '3': b = (byte)0x00000003; break;
			case '2': b = (byte)0x00000002; break;
			case '1': b = (byte)0x00000001; break;
			case '0': b = (byte)0x00000000; break;
			default : b = (byte)0x00000000; break;
		}
		return b;
	}//end decodeHexChar
	
	/**
	 * This changes a normal string into the hexadecimal string equivalent.  
	 * It will add two hexadecimal characters for each character of the string.
	 * @param s The string to be converted
	 * @return The string s converted to hexadecimal
	 */
	public static String toHexString(String s){
		char[] hexChar = {'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'};
		StringBuilder sb = new StringBuilder(s.length() * 2);
		for(int i=0; i<s.length();i++){
			sb.append( hexChar[(s.charAt(i) & 0xf0) >>> 4]);
			sb.append( hexChar[(s.charAt(i) & 0x0f)]);
		}
		
		return sb.toString();
	}
	
	/**
	 * Converts an integer into a fixed, zero-padded hexadecimal string representation.
	 * @param i The source integer to be converted.
	 * @param length The length of the string to be zero-padded
	 * @return A zero-padded hexadecimal string of the integer, i of length length.
	 */
	public static String intToFixedLengthHexString(int i, int length){
		String s = Integer.toHexString(i);
		String pad = "0";

		while(s.length() < length){
			s = pad.concat(s);
		}

		if(s.length() > length){
			s = s.substring(0, length);
		}

		return s.toUpperCase();
	}
	
	/**
	 * Reverses the 8 least significant bits of the integer n.
	 * @param n The integer to be changed
	 * @return The integer n with the least significant byte with its bits reversed.
	 */
	public static int reverseLSB(int n){
		n = ((n >> 1) & 0x55) | ((n << 1) & 0xaa);
		n = ((n >> 2) & 0x33) | ((n << 2) & 0xcc);
		n = ((n >> 4) & 0x0f) | ((n << 4) & 0xf0);
		return n;		
	}

	
	//Takes in a 32-bit integer and converts it into an ArrayList of bytes
	public static ArrayList<Byte> ToByteArray(int i)
	{
		ArrayList<Byte> ba = new ArrayList<>();
		int mask = 0x000000FF;
		int tmp = i >>> 24;
			tmp &= mask;
		ba.add((byte)tmp);
			tmp = i >>> 16;
			tmp &= mask;
		ba.add((byte)tmp);	
			tmp = i >>> 8;
			tmp &= mask;
		ba.add((byte)tmp);
			tmp = i;
			tmp &= mask;
		ba.add((byte)tmp);

		return ba;
	}//end ToByteArray
	
	/**
	 * Coverts a data stream represented as an array of bytes to an array of
	 * integers. Packs bytes in groups of 4 into integers, most significant bit
	 * first.
	 * 
	 * @param bytes list of bytes to be converted
	 * @return list of integers containing the same data stream, MSB first order.
	 * @throws IllegalArgumentException
	 */
	public static ArrayList<Integer> toIntArray(List<Byte> bytes) throws IllegalArgumentException {
		if (bytes.size() % 4 != 0) {
			throw new IllegalArgumentException("Integer Array can only be created from byte arrays where size%4 is 0");
		}
		ArrayList<Integer> integers = new ArrayList<>();
		for (int index = 0; index < bytes.size(); index += 4) {
			integers.add(((bytes.get(index) << 24) & 0xff000000)
					| ((bytes.get(index + 1) << 16) & 0xff0000) | ((bytes.get(index + 2) << 8) & 0xff00)
					| (bytes.get(index + 3) & 0xff));
		}
		return integers;
	}
}
