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

import java.io.IOException;
import java.util.HashSet;

import edu.byu.ece.rapidSmith.bitstreamTools.bitstream.Bitstream;
import edu.byu.ece.rapidSmith.bitstreamTools.bitstream.BitstreamParser;
import edu.byu.ece.rapidSmith.bitstreamTools.configuration.FPGA;
import edu.byu.ece.rapidSmith.bitstreamTools.configuration.Frame;
import edu.byu.ece.rapidSmith.bitstreamTools.configuration.FrameAddressRegister;
import edu.byu.ece.rapidSmith.bitstreamTools.configurationSpecification.DeviceLookup;
import edu.byu.ece.rapidSmith.bitstreamTools.configurationSpecification.XilinxConfigurationSpecification;

public class CheckFrameECC{

	private static HashSet<Integer> bits = new HashSet<Integer>();
	private static int altECC;
	private static int bitNumber;
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
		
		StringBuffer buf = new StringBuffer();
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
	
	private static String toBinaryString(String s){
		int index;
		String tmp;
		StringBuilder binaryString = new StringBuilder(96);
		String[] binaryLUT = {"0000","0001","0010","0011",
							  "0100","0101","0110","0111",
							  "1000","1001","1010","1011",
							  "1100","1101","1110","1111"};
		
		
		for(int i=0; i < s.length(); i++){
			tmp = (String) s.subSequence(i, i+1);
			index = Integer.valueOf(tmp, 16);
			binaryString.append(binaryLUT[index]);
			if(i % 2 != 0){
				binaryString.append(" ");
			}
		}
		
		return binaryString.toString();
	}
	
	public static String getBinaryFrameString(Frame frame){
		StringBuilder string = new StringBuilder(1792);
		StringBuilder data = new StringBuilder(512);
		int i = 0;
		
			for(i=0; i < frame.getData().getAllFrameWords().size(); i++){
				data.append(toHexString(frame.getData().getFrameWord(i)));
			}
			string.append("\n" +
					 toBinaryString(data.substring(  0, 20)) + "\n" +
					 toBinaryString(data.substring( 20, 40)) + "\n" +
					 toBinaryString(data.substring( 40, 60)) + "\n" +
					 toBinaryString(data.substring( 60, 80)) + "\n" +
					 toBinaryString(data.substring( 80,100)) + "\n" +
					 toBinaryString(data.substring(100,120)) + "\n" +
					 toBinaryString(data.substring(120,140)) + "\n" +
					 toBinaryString(data.substring(140,160)) + "\n" +
					 
					 toBinaryString(data.substring(160,168)) + "\n" +					 

					 toBinaryString(data.substring(168,188)) + "\n" +
					 toBinaryString(data.substring(188,208)) + "\n" +
					 toBinaryString(data.substring(208,228)) + "\n" +
					 toBinaryString(data.substring(228,248)) + "\n" +
					 toBinaryString(data.substring(248,268)) + "\n" +
					 toBinaryString(data.substring(268,288)) + "\n" +
					 toBinaryString(data.substring(288,308)) + "\n" +
					 toBinaryString(data.substring(308,328)) + "\n");	
		return string.toString();		
	}
	
	@SuppressWarnings("unused")
	private static int getBit(int data, int index){
		return (data >> 31-index) & 0x1;
	}
	
	@SuppressWarnings("unused")
	private static int getBitReversedWord(Frame frame, int index){
		// Try alternate endian
		int word = frame.getData().get(index/32);
		return (Integer.reverse(word) >> (31 - (index % 32))) & 0x1;
		//return (_words[index/32] >> (31 - (index % 32))) & 0x1; 
		//int wordIdx = ((~index) & 0x1F);
		//return frame.getData().getBit((index & 0xFFFFFFE0) | wordIdx);
	}
	
	public static int calculateFrameECCBits(Frame frame, Frame mask, XilinxConfigurationSpecification spec){
		int xored = 0;
		int hcode = 0;
		int count = 0;
		xored = 704;
		
		for(int bit=0; bit < 320; bit++){
			if(frame.getData().getBit(bit) == 1 && mask.getData().getBit(bit) == 0){
				hcode ^= xored;
				count++;
				bitNumber = bit;
			}
			xored++;			
		}
		
		xored += 32;
		for (int bit = 320; bit < 660; bit++){
			if(frame.getData().getBit(bit) == 1 && mask.getData().getBit(bit) != 1){
				hcode ^= xored;
				count++;
				bitNumber = bit;
			}

			xored++;			

		}

		// skip the code bits themselves 
		xored += 12;

		for(int bit = (660+12); bit < 1312; bit++){
			if(frame.getData().getBit(bit) == 1 && mask.getData().getBit(bit) != 1){
				hcode ^= xored;
				count++;
				bitNumber = bit;
			}
			xored++;			
		}
		altECC = hcode ^ 0x1F;
		altECC |= ((Integer.bitCount(altECC) + count) & 0x1) << 11;
		
		hcode |= ((Integer.bitCount(hcode) + count) & 0x1) << 11;
		return (count & 0x1) ==1 ? altECC : hcode;
	}

	public static String getFARString(XilinxConfigurationSpecification spec, Frame frame){
		String s = "FAR: 0x" + toHexString(frame.getFrameAddress()) + " ";
		s += FrameAddressRegister.getTopBottomFromAddress(spec, frame.getFrameAddress()) == 0 ? "TOP " : "BOTTOM ";
		s += "Type: " + FrameAddressRegister.getBlockTypeFromAddress(spec, frame.getFrameAddress()) + " ";
		s += "Row: " + FrameAddressRegister.getRowFromAddress(spec, frame.getFrameAddress()) + " ";
		s += "Column: " + FrameAddressRegister.getColumnFromAddress(spec, frame.getFrameAddress()) + " ";
		s += "Minor: " + FrameAddressRegister.getMinorFromAddress(spec, frame.getFrameAddress());
		return s; 
	}
	
	public static void main(String[] args){
		Bitstream bitstream = BitstreamParser.parseBitstreamExitOnError(args[0]);
		XilinxConfigurationSpecification spec = DeviceLookup.lookupPartFromPartnameOrBitstreamExitOnError(bitstream);
		FPGA fpga = new FPGA(spec);
		fpga.configureBitstream(bitstream);
			
		Bitstream bitstreamMask = BitstreamParser.parseBitstreamExitOnError(args[1]);
		XilinxConfigurationSpecification specMask = DeviceLookup.lookupPartFromPartnameOrBitstreamExitOnError(bitstream);
		FPGA fpgaMask = new FPGA(specMask);
		fpgaMask.configureBitstream(bitstreamMask);
		int correct = 0, incorrect = 0, close = 0;
		
		for(Frame frame : fpga.getAllFrames()){
			if(frame.getData().isEmpty()) continue;
			Frame maskFrame = fpgaMask.getFrame(frame.getFrameAddress());
			
			//ArrayList<Integer> oneBits = new ArrayList<Integer>();
			int calcECC = calculateFrameECCBits(frame, maskFrame, spec);
			int trueECC = frame.getData().getECCBits(); 
			if(calcECC == trueECC){
				//bits.addAll(oneBits);
				correct++;
				
				/*System.out.println(getFARString(spec, frame));
				System.out.println("(True)ECCBits = 0x" + Integer.toHexString(trueECC) + " " + toBinaryString(Integer.toHexString(trueECC)));
				System.out.println("(Calc)ECCBits = 0x" + Integer.toHexString(calcECC) + " " + toBinaryString(Integer.toHexString(calcECC)));
				System.out.println("(Alt )ECCBits = 0x" + Integer.toHexString(altECC) + " " + toBinaryString(Integer.toHexString(altECC)));
				System.out.println(getBinaryFrameString(frame));
				System.out.println(getBinaryFrameString(maskFrame));
				try{
					System.in.read();
				}
				catch(IOException e){
					// TODO Auto-generated catch block
					e.printStackTrace();
				}*/
			}
			else if(altECC == trueECC){
				close++;
				if(frame.getData().countBitsSet() == 1){
					bits.add(bitNumber);
				}
			}
			else{
				incorrect++;
				
				System.out.println(getFARString(spec, frame));
				System.out.println("(True)ECCBits = 0x" + Integer.toHexString(trueECC) + " " + toBinaryString(Integer.toHexString(trueECC)));
				System.out.println("(Calc)ECCBits = 0x" + Integer.toHexString(calcECC) + " " + toBinaryString(Integer.toHexString(calcECC)));
				System.out.println("(Alt )ECCBits = 0x" + Integer.toHexString(altECC) + " " + toBinaryString(Integer.toHexString(altECC)));
				System.out.println(getBinaryFrameString(frame));
				System.out.println(getBinaryFrameString(maskFrame));
				try{
					System.in.read();
				}
				catch(IOException e){
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
		}
		System.out.println("correct=" + correct);
		System.out.println("close=" + close);
		System.out.println("incorrect=" + incorrect);
		
		
		Frame tmp = new Frame(41, 0);
		for(Integer i : bits){
			tmp.getData().setBit(i, 1);
		}
		System.out.println(getBinaryFrameString(tmp));
	}
}
