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

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * This class contains all of the information in a configuration bitstream header.
 * The bitstream header is the extra information added at the top of a true
 * bitstream and is used by Impact and other bitstream tools to figure out
 * what is in the actual bitstream. A bitstream header contains the following information:
 * - Name of the source NCD file
 * - the part name
 * - The date created
 * - The time created
 * - The length of the bitstream
 * 
 * The length of the bitstream is not saved in this class. This is calculated based
 * on the actual contents of the bitstream.
 *  
 *  
 *  TODO: update the toString method so that it prints the parsed fields of the header.
 *  
 */
public class BitstreamHeader {

	/**
	 * A String version of the initial header of the bitstream header.
	 */
    public static final String INIT_HEADER = "0x0FF00FF00FF00FF000";

	/**
	 * A byte version of the initial header of the bitstream header.
	 */
    public static final byte[] INIT_HEADER_BYTES = {(byte)0x0F, (byte)0xF0, (byte)0x0F, (byte)0xF0, (byte)0x0F, (byte)0xF0, (byte)0x0F, (byte)0xF0, (byte)0x00};
	    
    /**
     * Create a new bitstream header using the parameters given to fill the header fields
     * 
     * @param sourceNCDFileName The String name of the source NCD file
     * @param partName The name of the part (currently this does not check to see if it is valid)
     * @param dateCreated The date the bitstream was created
     * @param timeCreated The time the bitstream was created
     */
    public BitstreamHeader(String sourceNCDFileName, String partName, String dateCreated, String timeCreated) {
        _sourceNCDFileName = sourceNCDFileName;
        _partName = partName;
        _dateCreated = dateCreated;
        _timeCreated = timeCreated;
    }
    
    // TODO: Test this
	public BitstreamHeader(String ncd, String partName) {
		this(ncd, partName, new Date());
	}

	// TODO: test this
	public BitstreamHeader(String ncd, String partName, Date date) {
		this(ncd, partName,
				// create a Date String
				DateFormat.getDateInstance().format(date),
				DateFormat.getTimeInstance().format(date)
			);		 
	}
    
	/**
     * Returns the date the bitstream was created as a string year/month/day, for example: 2009/02/19.
     * @return The date the bitstream was created as a string.
     */
    public String getDateCreated(){
    	return _dateCreated;
    }

    /**
     * Returns the raw bytes for a bitstream header and includes the
     * length of the bitstream in the header. The length is passed
     * in as a parameter rather than using the class bitstreamLength
     * member as this value may change when generating the bitstream.
     *    
     * This method does NOT generate the sync word.
     * 
     * @param length The length of the current bitstream header.
     * @return The list of bytes corresponding to the header.
     */
    public List<Byte> getHeaderBytes(int length) {
    	List<Byte> bytes = getHeaderBytesWithoutLength();
    
    	// Field 7, bytes in raw bitstream
    	addCharacter(bytes, 'e');
    	bytes.add((byte)((length & 0xff000000) >>> 24 ));
    	bytes.add((byte)((length & 0xff0000) >>> 16 ));
    	bytes.add((byte)((length & 0xff00) >>> 8 ));
    	bytes.add((byte)((length & 0xff) >>> 0 ));
    
    	return bytes;
    	
    }

    /**
     * This function will assemble the raw bytes of the header from the
     * values contained within this object. This method will NOT
     * add the length field of to the array (other methods are
     * available for adding the length). This method will also
     * NOT include the SYNC word.
    
     * This method will also make sure that the header ends on
     * a 4 byte boundary so the sync word starts on a 4 byte
     * boundary.
     */
    public List<Byte> getHeaderBytesWithoutLength(){
    	Character nullChar = 0;
        
        List<Byte> bytes = new ArrayList<Byte>();
    	
    	// Field 1, 2 bytes length, 9 bytes data
    	addByteArrayField(bytes, INIT_HEADER_BYTES);
    	
    	// Field 2, 'a'
    	addField(bytes, "a");
    	
    	// Field 3, source NCD file name
    	addField(bytes, _sourceNCDFileName + nullChar);
    	
    	// Field 4, 'b', part name
    	addCharacter(bytes, 'b');
    	addField(bytes, _partName + nullChar);
    	
    	// Field 5, 'c', date created
    	addCharacter(bytes, 'c');
    	addField(bytes, _dateCreated + nullChar);
    	
    	// Field 6, 'd', time created
    	addCharacter(bytes, 'd');
    	addField(bytes, _timeCreated + nullChar);
   	
    	return bytes;
    }

    public String getPartName() {
    	return _partName;
    }

    /**
	 * Allows access to the source NCD filename and command parameters that generated the bitstream. 
	 * @return The string of the name of the NCD file which was used to create the bitstream. 
	 */
	public String getSourceNCDFileName(){ 
		return _sourceNCDFileName;
	}
	
	/**
	 * Returns the time the bitstream was created in 24-hour format, for example: 16:12:04.
	 * @return The time the bitstream was created as a string.
	 */
	public String getTimeCreated(){
		return _timeCreated;
	}
	
	public String toString() {
		return toXML(); 
	}
	/**
	 * Creates a text string detailing all the fields in the header in ASCII and Hexadecimal
	 * @return A string representing the header object
	 */
	public String toXML(){
		StringBuffer output = new StringBuffer();
		output.append("\n");

		output.append("Field 1 (Initial Header)  : <" + INIT_HEADER + "> 0x" 
				+ BitstreamUtils.toHexString(INIT_HEADER) + "\n");
		output.append("Field 2 (Source NCD file) : <" + _sourceNCDFileName + "> 0x"
				+ BitstreamUtils.toHexString(_sourceNCDFileName) + "\n");
		output.append("Field 3 (Device name)     : <" + this._partName + "> 0x"
				+ BitstreamUtils.toHexString(_partName) + "\n");
		output.append("Field 4 (Date created)    : <" + this._dateCreated + "> 0x"
				+ BitstreamUtils.toHexString(_dateCreated) + "\n");
		output.append("Field 5 (Time created)    : <" + this._timeCreated + "> 0x"
				+ BitstreamUtils.toHexString(_timeCreated) + "\n");
		
		return output.toString();
	}
	
	
	protected static void addByteArrayField(List<Byte> bytes, byte[] byteArray) {
	    int length = byteArray.length;
	    bytes.add(((Integer)((length & 0xff00) >> 8)).byteValue());
        bytes.add(((Integer)(length & 0xff)).byteValue());
	    for (int i = 0; i < length; i++) {
	        bytes.add((byte) (0xff & byteArray[i]));
	    }
	}

	protected static void addCharacter(List<Byte> bytes, char character) {
        bytes.add((byte) (0xff & character));
    }

    protected static void addField(List<Byte> bytes, String fieldData) {
	    int length = fieldData.length();
        bytes.add(((Integer)((length & 0xff00) >> 8)).byteValue());
        bytes.add(((Integer)(length & 0xff)).byteValue());
        bytes.addAll(getLowerBytes(fieldData));
	}
	
	/**
     * Takes a string and returns an ArrayList of bytes of the lower bytes of each character in the string.
     * @param s The string to be converted.
     * @return An ArrayList of the lower bytes of the characters in the string.
     */
    protected static ArrayList<Byte> getLowerBytes(String s){
    	ArrayList<Byte> bytes = new ArrayList<Byte>();
    	int len = s.length();
    	for(int i=0; i < len; i++){
    		bytes.add( ((Integer)(s.charAt(i) & 0xff)).byteValue() );
    	}
    	return bytes;
    }

    /**
	 * This is the source NCD from which the bitstream was created. It may also include switches.
	 */
	protected String _sourceNCDFileName;

	/**
	 * This is the target device name, which also includes the package.
	 */
	protected String _partName;

	/**
	 * The date the bit file was created.
	 */
	protected String _dateCreated;

	/**
	 * The time the bit file was created.
	 */
	protected String _timeCreated;
	
}