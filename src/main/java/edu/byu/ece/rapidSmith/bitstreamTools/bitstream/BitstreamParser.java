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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Parses an existing binary bitfile (.bit) and generates a Bitstream object.
 * 
 * 
 * TODO: Look into the different file formats and be able to parse any of them and
 * also to generate any of them.
 * 
 * See page 53 of Xilinx configuration guide (ug071.pdf).
 */
public class BitstreamParser {
    
	/**
	 * TODO: Why is this necessary? Shouldn't the DummySyncData class be used here instead?
	 */
    public static final byte[] SYNC_SEQUENCE = {(byte)0x0FF, (byte)0x0FF, (byte)0x0FF, (byte)0x0FF, (byte)0x0AA, (byte)0x099, (byte)0x055, (byte)0x066};

	public BitstreamParser(InputStream istream) throws BitstreamParseException, IOException {
		this._istream = istream;
		loadFile();
		_bitstream = parseFile();
	}
	
	/**
	 * @deprecated Will - Lets find a better way for you to extend the parser.
	 * 
	 * This is only used by the CompressionBitstreamParser class
	 */
    protected BitstreamParser() {

    }

	public Bitstream getBitstream() {
		return _bitstream;
	}
	
	/**
	 * Static method to return a Bitstream objet from a filename
	 */
	public static Bitstream parseBitstream(String filename) throws BitstreamParseException, IOException {
		return parseBitstream(new File(filename));		
	}
	
	/**
	 * Static method to return a Bitstream objet from a File object
	 */
	public static Bitstream parseBitstream(File file) throws BitstreamParseException, IOException {
	    FileInputStream input = new FileInputStream(file);
	    BufferedInputStream buffer = new BufferedInputStream(input);   
        BitstreamParser bp = new BitstreamParser(buffer);
        return bp.getBitstream();
	}
	
	/**
	 * Static method to return a Bitstream objet from a filename.
	 * 
	 * This static method has built in exit code if there are problems with the 
	 * bitstream parsing.
	 */
	public static Bitstream parseBitstreamExitOnError(String filename) {
        Bitstream bitstream = null;
        
        try {
        	bitstream = parseBitstream(filename);
        } catch (BitstreamParseException e) {
        	System.err.println("Invalid Bitstream File");
        	//e.printStackTrace();
        } catch (FileNotFoundException e) {
        	System.err.println("File Not Found:"+filename);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        if (bitstream == null)
        	System.exit(1);
        return bitstream; 
	}

	
	/**
     * Creates the bitstream packets from the bit file.
     * A packet header is 4 bytes converted to an integer.  The header is parsed and the 
     * number of data words is determined.  Data words will be created from 4 consecutive bytes.
     * 
     * @throws BitstreamException 
     */
    protected PacketList createBody(int numHeaderBytes) throws BitstreamException {
        PacketList packets = new PacketList();
        int i = numHeaderBytes;
    	while(i < _bytes.size()) {
    	    List<Integer> data = new ArrayList<>();
    		
    	    // get packet header
    	    int header = getWordAsInt(_bytes,i);
    		i += 4;
    		
    		// get packet data
    		int numWords = PacketType.getPacketType(header).getNumWords(header);
    		for (int j = 0; j < numWords; j++) {
    		    data.add(getWordAsInt(_bytes, (j*4) + i));
    		}
    		i += numWords * 4;
    		
    		Packet packet = new Packet(header, data);
    		packets.add(packet);
    	}
    	return packets;
    }
    
    protected static String getField(List<Byte> headerBytes, int index) {
        return getField(headerBytes, index, true);
    }

    protected static String getField(List<Byte> headerBytes, int index, boolean includeNull){
    	String value = ""; // Return value of field
    	Character tmp;
    	
    	// Reads out two bytes for the length of the field and stores it in an int
    	int length = (0xff00 & (headerBytes.get(index) << 8)) | 
    				 (0xff & headerBytes.get(index+1));
    	
    	// Move past the length field
    	index += 2;
    	
    	if (!includeNull) {
    	    length -= 1;
    	}
    	// Read out length bytes into a string
    	for(int j=0; j < length; j++){
    		tmp = (char) headerBytes.get(index).intValue();
    		value = value.concat(tmp.toString());
    		index++;
    	}
    	//System.out.println("Value: <" + value + "> " + ((Integer)value.length()).toString());
    	return value;
    }

    protected List<Byte> getHeaderUpToSyncBytes() {
        List<Byte> headerUpToSyncBytes = new ArrayList<>();
    	int i = 0;
    	int syncPosition= 0;
    	while(syncPosition < SYNC_SEQUENCE.length && i < _bytes.size()) {
    		byte b = _bytes.get(i);
    		headerUpToSyncBytes.add(b);
    		if(b == SYNC_SEQUENCE[syncPosition]) {
    			syncPosition++;
    		}
    		else if(b == (byte)0x0FF) {
    			// Do nothing
    		}
    		else {
    			syncPosition = 0;
    		}
    		i++;
    	}
    	return headerUpToSyncBytes;
    }

    /**
     * Creates an integer from the bit file using the 4 bytes starting with the byte specified by 
     * start. See also AddDataToPacket().
     * 
     * @param start This is a byte offset into the file at which the data for a packet can be created.
     */
    public static int getWordAsInt(ArrayList<Byte> bytes, int start) {
    	byte a,b,c,d;
    	
    	a = bytes.get(start);
    	b = bytes.get(start + 1);
    	c = bytes.get(start + 2);
    	d = bytes.get(start + 3);

	    return (((a & 0xff) << 24) | ((b & 0xff) << 16) |
    			((c & 0xff) << 8) | (d & 0xff));
    }

    protected void loadFile() throws IOException {
    	int numBytes = _istream.available();
    	for(int i = 0; i < numBytes; i++) {
    		_bytes.add((byte)_istream.read());
    	}
    	_istream.close();
    }

    /**
     * The main function for parsing the bitstream file.
     */
    protected Bitstream parseFile() throws BitstreamParseException {
    	Bitstream result;
    	BitstreamHeader header = parseHeader();
    	int numHeaderBytes = 0;
    	if (header != null) {
    		numHeaderBytes = header.getHeaderBytes(0).size();
    	}
    	DummySyncData dummySyncData = DummySyncData.findDummySyncData(_bytes, numHeaderBytes);
    	if (dummySyncData == null) {
    	    throw new BitstreamParseException("Error: unrecognized dummy/sync word section");
    	}
    	PacketList packets;
    	try {
            packets = createBody(numHeaderBytes + dummySyncData.getDataSize());
        } catch (BitstreamException e) {
            e.printStackTrace();
            throw new BitstreamParseException(e.getMessage());
        }
        if (header != null) {
        	result = new Bitstream(header, dummySyncData, packets);
        }
        else {
        	result = new Bitstream(dummySyncData, packets);
        }
        return result;
    }

    /**
	 * This function will parse the ArrayList of bytes containing the header and extract the information fields.
	 * 
	 * @return true if parsing was performed correctly, false otherwise.
	 */
	protected BitstreamHeader parseHeader() throws BitstreamParseException {
		int i; // headerByte index pointer

		String tmp;
		
		// First we need to make sure the header indicator is the right length
		int initHeaderLength = BitstreamHeader.INIT_HEADER_BYTES.length;
		byte byte0 = (byte) (initHeaderLength >> 8);
    	byte byte1 = (byte) (initHeaderLength & 0xFF);
		
    	if (_bytes.get(0) != byte0 || _bytes.get(1) != byte1) {
    		return null; // headerless bitstream
    	}
    	
    	// Now we need to make sure the header indicator data matches
    	i=2;
    	
		for (int j = 0; j < BitstreamHeader.INIT_HEADER_BYTES.length; j++) {
			if (_bytes.get(i) != BitstreamHeader.INIT_HEADER_BYTES[j]) {
				return null; // headerless bitstream
			}
			i++;
		}
		
		
		// Second Field, 'a', for some reason, the key 'a' has a length
		// field associated with it, where the other keys do not ('b', 'c',...)
		tmp = getField(_bytes,i);
		i += 2 + tmp.length();
		
		if(!tmp.equals("a") || tmp.length() != 1){
			throw new BitstreamParseException("Strange header input processing field 'a'");
		}
		
		// Third Field, Get NCD source file name
		String sourceNCDFileName = getField(_bytes,i,false);
		i += 2 + sourceNCDFileName.length() + 1;
		
		//The next byte should be an ASCII 'b'
		if(_bytes.get(i).intValue() != 0x62){  
			throw new BitstreamParseException("Strange header input processing field 'b'");
		}
		else{
			i++;
		}
		
		// Fourth Field, Get Part Name 
		String partName = getField(_bytes,i,false);
		i += 2 + partName.length() + 1;

		//The next byte should be an ASCII 'c'
		if(_bytes.get(i).intValue() != 0x63){  
			throw new BitstreamParseException("Strange header input processing field 'c'");
		}
		else{
			i++;
		}
		
		// Fifth Field, Get Date Created
		String dateCreated = getField(_bytes,i,false);
		i += 2 + dateCreated.length() + 1;
		
		//The next byte should be an ASCII 'd'
		if(_bytes.get(i).intValue() != 0x64){  
			throw new BitstreamParseException("Strange header input processing field 'd'");
		}
		else{
			i++;
		}
		
		// Sixth Field, Get Time Created
		String timeCreated = getField(_bytes,i,false);
		i += 2 + timeCreated.length() + 1;
		
		//The next byte should be an ASCII 'e'
		if(_bytes.get(i).intValue() != 0x65){  
			throw new BitstreamParseException("Strange header input processing field 'e'");
		}

		return new BitstreamHeader(sourceNCDFileName, partName, dateCreated, timeCreated);
	}

	
	protected Bitstream _bitstream;
	protected InputStream _istream;
	protected ArrayList<Byte> _bytes = new ArrayList<>();

}
