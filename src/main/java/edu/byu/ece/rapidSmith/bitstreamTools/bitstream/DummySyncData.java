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
 * Represents the dummy sync data found in Xilinx bitstreams. This object contains
 * a List&lt;Byte&gt; member that holds all of the sync data. This class contains two
 * prebuilt static DummySyncData objects for V5 and V6.
 * 
 * TODO: Create separate classes for V4 sync data vs. v5/v6 sync data
 */
public class DummySyncData extends ConfigurationData {

	/**
	 * A static DummySyncData object for V4.
	 */
    public static final DummySyncData V4_STANDARD_DUMMY_SYNC_DATA = new DummySyncData(new byte[] {(byte)0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xAA, (byte) 0x99, (byte) 0x55, (byte) 0x66});

	/**
	 * A static DummySyncData object for V5 and V6.
	 */    
    public static final DummySyncData V5_V6_STANDARD_DUMMY_SYNC_DATA = new DummySyncData(new byte[] {(byte)0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte)0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte)0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte)0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte)0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte)0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte)0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte)0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xBB, (byte) 0x11, (byte) 0x22, (byte) 0x00, (byte) 0x44, (byte)0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte)0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xAA, (byte) 0x99, (byte) 0x55, (byte) 0x66});
    
    /**
	 * dummy syncronization data for v5 and v6 ICAP.  Note: does not need as much sync data is the regular bitstream
	 */
    public static final DummySyncData V5_V6_ICAP_DUMMY_SYNC_DATA = new DummySyncData(new byte[] {(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xBB, (byte) 0x11, (byte) 0x22, (byte) 0x00, (byte) 0x44, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xAA, (byte) 0x99, (byte) 0x55, (byte) 0x66});
    
    public static final byte[] SYNC_DATA = new byte[] {(byte) 0xAA, (byte) 0x99, (byte) 0x55, (byte) 0x66};
    
    public DummySyncData(byte[] bytes) {
        _data = new ArrayList<Byte>(bytes.length);
        for (Byte b : bytes) {
            _data.add(b);
        }        
    }
    
    public DummySyncData(List<Byte> bytes) {
    	_data = new ArrayList<Byte>(bytes);
    }
    
    /**
     * Find the dummy/sync data in the bitstream starting at the given index and
     * return it as a DummySyncData object. If no sync word (0xaa995566) is found,
     * null is returned. The startIndex is assumed to be the beginning of the dummy
     * data and the dummy/sync data ends after the sync word is found.
     * 
     * @param data The data to search through.
     * @param startIndex The starting index in the data given.
     * @return The dummy sync data if found or null otherwise.
     */
    public static DummySyncData findDummySyncData(List<Byte> data, int startIndex) {
    	int numMatched = 0;
    	int index = startIndex;
    	int dataLength = data.size();
    	while (index < dataLength) {
    		if (data.get(index) == SYNC_DATA[numMatched]) {
    			numMatched++;
    		}
    		else {
    			numMatched = 0;
    		}
    		if (numMatched == SYNC_DATA.length) {
    			return new DummySyncData(data.subList(startIndex, index+1));
    		}
    		index++;
    	}
    	return null;
    }
    
    public boolean matchesData(List<Byte> data) {
        return _data.equals(data);
    }
    
    public List<Byte> getData() {
        return new ArrayList<Byte>(_data);
    }
    
    public int getDataSize() {
        return _data.size();
    }
    
    /**
     * The bytes that make up the dummy sync data of the bitstream.
     */
    protected List<Byte> _data;

	@Override
	public List<Byte> toByteArray() {
		return this.getData();
	}

}
