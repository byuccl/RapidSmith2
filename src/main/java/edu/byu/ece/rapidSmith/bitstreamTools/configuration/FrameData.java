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
import java.util.Arrays;
import java.util.List;

import edu.byu.ece.rapidSmith.bitstreamTools.bitstream.BitstreamUtils;
import edu.byu.ece.rapidSmith.bitstreamTools.configurationSpecification.XilinxConfigurationSpecification;

/**
 * Defines the data contents of a configuration frame. Provides methods 
 * for accessing and setting configuration frame data. Frame data is
 * stored as an array of ints.
 *
 * TODO
 * - Need methods for initializing the contents of the frame data and make them safe.
 * 
 */
public class FrameData {

	/**
	 * The frame is initialized to zero with size number of 32-bit words.  All frames in a given device 
	 * should be the same size.
	 * @param size The size of the frame in 32-bit words. 
	 */
	public FrameData(int size) {
		_words = new int[size];
		zeroData();
	}
	
	public FrameData(XilinxConfigurationSpecification spec) {
		this(spec.getFrameSize());
	}
	
	// constructs a frame from frame data stored in a byte array
	public FrameData(List<Byte> byteData) {
		this(byteData.size()/4);		
		copy(BitstreamUtils.toIntArray(byteData));
	}

	/**
	 * Copy constructor
	 */
	public FrameData(FrameData copy) {
		this(copy.size());
		copy(copy);
	}
	
	public void copy(FrameData copy) {
		for (int i = 0; i < size(); i++)
			setData(i, copy.get(i));
	}
	
	public void copy(List<Integer> copy) {
		for (int i = 0; i < size(); i++)
			setData(i, copy.get(i));
	}
	 
	/**
	 * Sets all parameters and data to zero.  The frame looks freshly initialized.
	 */
	public void zeroData() {
		for(int i = 0; i < _words.length; i++) {
			_words[i] = 0;
		}
	}//end ZeroData
	
	/**
	 * XORs the data words of this frame with those of Frame f and stores them in this frame.
	 * Sets hasData appropriately. 
	 * @param f The frame containing the data to be XOR'ed with this frame
	 * @return true if the operation succeeds, false otherwise.
	 */
	public boolean XORDATA(FrameData f) {
		if(size() != f.size())
			return false;
		
		for(int i = 0; i < size(); i++) {
			int xorValue = this.get(i) ^ f.get(i);
			setData(i, xorValue);
		}
		return true;
	}//end XORData
	
	/**
	 * ANDs the data words of this frame with those of Frame f and stores them in this frame.
	 * Sets hasData appropriately. 
	 * @param f The frame containing the data to be AND'ed with this frame
	 * @return true if the operation succeeds, false otherwise.
	 */
	public boolean ANDData(FrameData f) {
		if(size() != f.size())
			return false;
		
		for(int i = 0; i < size(); i++) {
			int andValue = get(i) & f.get(i);
			setData(i, andValue);
		}
		return true;
	}//end ANDData
	
	/**
	 * ORs the data words of this frame with those of Frame f and stores them in this frame.
	 * Sets hasData appropriately. 
	 * @param f The frame containing the data to be OR'ed with this frame
	 * @return true if the operation succeeds, false otherwise.
	 */
	public boolean ORData(FrameData f) {
		if(size() != f.size())
			return false;
		
		for(int i = 0; i < size(); i++) {
			int orValue = get(i) | f.get(i);
			setData(i, orValue);
		}
		return true;
	}//end ORData
	
	/**
	 * Zeros out data in this FrameData that are "ones" in f. This is used for mask
	 * bitfiles.
	 *  
	 * @param f The frame containing the data to be MASK'ed with this frame
	 * @return true if the operation succeeds, false otherwise.
	 */
	public boolean MASKData(FrameData f) {
		if(size() != f.size())
			return false;
		
		for(int i = 0; i < size(); i++) {
			// result = curValue & ( ~Mask)
			int orValue = get(i) & ( ~f.get(i) ) ;
			setData(i, orValue);
		}
		return true;
	}//end ORData

	/**
	 * Inverts the data words stored in the frame.
	 */
	public void NOTData() {
		for(int i = 0; i < size(); i++) {
			int notValue = ~get(i);
			setData(i, notValue);
		}
	}//end NOTData

	/** 
	 * Returns the word at the specified index of the frame.
	 * @param index Index of the desired word.
	 * @return The word at the specified index of the frame.  
	 */
	public int getFrameWord(int index) {
		return get(index);
	}

	/** Set the data of this frame with the data of the given frame.
	 * 
	 */
	public boolean setData(FrameData data) {
		if (data.size() != size())
			return false;
		for(int i = 0; i < size(); i++) {
			setData(i,data.getFrameWord(i));
		}
		return true;		
	}
	
	public boolean setData(int index, int word) {
		if(index < _words.length && index >= 0) {
			_words[index] = word;
			return true;
		}
		else {
			return false;
		}
	}

	/**
	 * Gets all the data words in the frame and returns them in an ArrayList of Integers.
	 * @return An ArrayList of Integers containing all the data words of the frame.
	 */
	public List<Integer> getAllFrameWords() {
		Arrays.asList(_words);
		
		ArrayList<Integer> li = new ArrayList<>(_words.length);
		for (int i = 0; i < size(); i++)
			li.add(_words[i]);
		return li;
	}//end GetAll
	
	/** 
	 * Returns the size of the frame.
	 * @return The number of 32-bit words in the frame
	 */	
	public int size() { 
		return _words.length; 
	}

	/** 
	 * Returns the word at the specified index of the frame.
	 * @param index Index of the desired word.
	 * @return The word at the specified index of the frame.  
	 */
	public int get(int index) {	
		return _words[index]; 
	} //end Get

	public int getECCBits(){
		return 0x00000FFF & _words[_words.length/2];
	}
	
	public int countBitsSet(){
		int count = 0;
		for(int i=0; i < _words.length; i++) {
			if(i==_words.length/2)
				count += Integer.bitCount(_words[i] & 0xFFFFF000);
			else
				count += Integer.bitCount(_words[i]);
		}
		return count;
	}
	
	/**
	 * Extracts the bit from the frameData.
	 * @param index The index of the desired bit.
	 * @return The value of the bit (0 or 1) at the index specified.
	 */
	public int getBit(int index){
		return (_words[index/32] >> (31 - (index % 32))) & 0x1; 
	}

	/**
	 * Extracts the bit from the frameData non xilinx ordered bit index.
	 * @param index The index of the desired bit.
	 * @return The value of the bit (0 or 1) at the index specified.
	 */
	public int getBitReverse(int index) {
		return (_words[index/32] >> ((index % 32))) & 0x1;
	}
	
	/**
	 * Sets the bit in the frameData at index with value.
	 * @param index Bit index into the frame of the desired bit to set.
	 * @param value The value (0 or 1) of the bit.
	 * @return true if operation was successful, false otherwise.
	 */
	public boolean setBit(int index, int value){
		int tmp = _words[index/32];
		int currBit = getBit(index);
		if((currBit==1 && value==1) ||(currBit==0 && value==0)){
			// Value is already set
			return true;
		}
		if(value != 1 && value != 0){
			// bogus value
			return false;
		}
		else if(currBit==1 && value==0){
			tmp = tmp ^ (0x1 << (31 - (index % 32)));
		}
		else if(currBit==0 && value==1){
			tmp = tmp | (0x1 << (31 - (index % 32)));
		}
		return setData(index/32, tmp);
	}
	
	/**
	 * Flips the bit (0-&gt;1 or 1-&gt;0) at the bit index specified
	 * @param index The bit index of the bit to be flipped
	 * @return true if the operation was successful, false otherwise.
	 */
	public boolean flipBit(int index){
		return setData(index/32, _words[index/32] ^ (0x1 << (31 - (index % 32))));
	}
	
	/**
	 * Checks if two frames sizes and data words are equal.  It doesn't check for other aspects of the frame 
	 * (tagged, mustWrite, or hasData) are equal, just size and data.
	 * @param f The frame to check this frame's data words against.
	 * @return true if the data words and size of this frame and Frame f are equal, false otherwise.
	 */
	public boolean isEqual(FrameData f) {
		if(size() != f.size())
			return false;
		
		for(int i = 0; i < size(); i++) {
			if(this.get(i) != f.get(i))
				return false;
		}
		return true;
	}//end IsEqual
	
	/**
	 * Determines if the frame data is empty (i.e. all words are zero)
	 * 
	 * @return true if all words are zero, false otherwise
	 */
	public boolean isEmpty() {
		for (int i = 0; i < _words.length; i++) {
			if (get(i) != 0)
				return false;
		}
		return true;
	}
	
	/**
	 * Generate a String representing the full contents of the Frame.
	 */
	public String toString() {
		int numberOfWordColumns = 8;
		
		StringBuilder string = new StringBuilder();
		int frameSize = _words.length;
		
		if (!isEmpty()) {
			int wordNumber = 0;
			int column;
			while(wordNumber < frameSize) {
				for (column = 0; column < numberOfWordColumns && wordNumber < frameSize; column++) {
					string.append(BitstreamUtils.toHexString(_words[wordNumber]) + " ");
					wordNumber++;
				}
				string.append("\n");
			}
		} else {
			string.append("Frame is empty\n");
				
		}
		return string.toString();
	}

	/**	This is a list of all the words in a frame */
	private int[] _words;

}
