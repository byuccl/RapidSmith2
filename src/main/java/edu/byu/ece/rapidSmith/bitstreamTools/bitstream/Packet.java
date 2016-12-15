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
package edu.byu.ece.rapidSmith.bitstreamTools.bitstream;

import java.util.ArrayList;
import java.util.List;

/**
 * A bitstream is made up of packets.  Each packet has two parts.  The first part is the
 * packet header and the second part is the packet data.
 * 
 * All packets have a packet header. If it is a type 1 packet, then the packet header
 * contains the header type, opcode,
 * register address, and word count.  If it is a type 2 packet, then the header contains the header type,
 * opcode, and word count (see Xilinx UG071 Chpt. 7 for more information).  
 * The second part of the packet is the data that will be written to the registers specified
 * in the header.  The type 2 packets look at the previous type 1 packet to know which register it
 * should write to.
 * 
 */
public class Packet extends ConfigurationData {
    
    public static final int MAX_TYPE_ONE_SIZE = (2 << 10) - 1; // 11 bits of precision 
    public static final int MAX_TYPE_TWO_SIZE = (2 << 26) - 1; // 27 bits of precision
	
    public Packet(int header, List<Integer> data) throws BitstreamException {
        _header = header;
        _data = data;
        setFieldsFromHeader(_header);
    }
    
    public Packet(int header, int data) throws BitstreamException {
        _header = header;
        _data = new ArrayList<>(1);
        _data.set(0, data);
        setFieldsFromHeader(_header);
    }

    /**
     * Gets the current ArrayList of data and returns it.
     * @return The current ArrayList of data.
     */
    public List<Integer> getData() {
        return _data;
    }

	/**
	 * Gets the current header and returns it.
	 * @return The header.
	 */
	public int getHeader() {
	    return _header;
	}
	
	/**
     * Gets the current Opcode and returns it.
     * @return The current Opcode.
     */
    public PacketOpcode getOpcode() {
        return _opcode;
    }

    /**
	 * Gets current packet type and returns it.
	 * @return The current packet type.
	 */
	public PacketType getPacketType() {
	    return _type;
	}
	
	/**
	 * Gets the current register type and returns it.
	 * @return The current register type.
	 */
	public RegisterType getRegType() {
	    return _register;
	}
	
	/**
	 * Gets the current number of words in the packet and returns it.
	 * @return The number of words in the packet.
	 */
	public int getNumWords() {
	    return _numWords;
	}

	/**
     * Converts the packet into a byte array that can be written to a bit file
     * @return An ArrayList of Bytes ready to be written to a file. 
     */
	@Override
    public ArrayList<Byte> toByteArray() {
    	ArrayList<Byte> ba = new ArrayList<>();
    	ba.addAll(BitstreamUtils.ToByteArray(_header));
    	for(Integer i : _data) {
    		ba.addAll(BitstreamUtils.ToByteArray(i));
    	}
    	return ba;
    }

    /**
     * TODO: provide some options so that we can print the actual contents of packets with data.
     *  (use output stream instead of a String)
     */
    @Override
	public String toString() {
		String string = "";
		string += "<packet>\n";
		string += "\t<packet_header>";
		string += Integer.toHexString(_header);
		string += "</packet_header>\n";
		string += "\t<packet_header_info>";
		if (_type == PacketType.ONE) {
		    string += _type + " " + _opcode + " " + _register + " Word Data: " + _data.size();
		}
		else {
		    string += _type + " " + _opcode + " Word Data: " + _data.size();
		}
		string += "</packet_header_info>\n";
		if( !(_type == PacketType.TWO || _register == RegisterType.FDRI) ) {
			string += "\t<packet_data>";
			for(Integer i : _data) {
				string += BitstreamUtils.toHexString(i) + " ";
			}
			string += "</packet_data>\n";
			string += "\t<packet_data_info>";
			for(Integer i : _data) {
				string += RegisterType.DataToString(i, _register);
			}
			string += "</packet_data_info>\n";
		}
		string += "</packet>";
		return string;
	}
    
	public String toString(boolean printHeader) {

		String string = "";
		String extraInfo = "";
		
		// Packet summary
		string += _opcode;
		
		if (_type == PacketType.TWO)
			extraInfo += "type " + _type + " ";

		if (_type == PacketType.ONE && _register != RegisterType.NONE /*_register.NONE*/) {
			extraInfo += "reg="+_register;
		}
		
		if (_data.size() != 0) {
			if (_data.size() == 1) {
				extraInfo += " word=0x" + BitstreamUtils.toHexString(_data.get(0));
			} else {
				extraInfo += " # words=" + _data.size();				
			}
			String dataString = RegisterType.DataToString(_data.get(0),_register);
			if (dataString.length() > 0)
				extraInfo += " " + dataString;
		}
			
		if (extraInfo.length() > 0)
			string += " (" + extraInfo + ")";
		
		return string;
	}
    
    /**
     * Create a type 1 packet with one data word.
     */
    public static Packet buildOneWordPacket(PacketOpcode opcode, RegisterType registerType, int dataWord) {
        int header = 0;
        try {
            header = getHeader(PacketType.ONE, opcode, registerType, 1);
        } catch (BitstreamException e) {
            // this exception won't happen because we know we are using a type 1 packet
            // (the exception is raised for type NONE)
        }
        List<Integer> data = new ArrayList<>(1);
        data.add(dataWord);
        Packet result = null;
        try {
            result = new Packet(header, data);
        } catch (BitstreamException e) {
            // this exception won't happen because we know we are using a type 1 packet
            // (the exception is raised for type NONE)
        }
        
        return result;
    }

    /**
     * Create a type 1 packet with two data words.
     */
    public static Packet buildTwoWordPacket(PacketOpcode opcode, RegisterType registerType, int dataWord1, int dataWord2 ) {
        int header = 0;
        try {
            header = getHeader(PacketType.ONE, opcode, registerType, 1);
        } catch (BitstreamException e) {
            // this exception won't happen because we know we are using a type 1 packet
            // (the exception is raised for type NONE)
        }
        List<Integer> data = new ArrayList<>(2);
        data.add(dataWord1);
        data.add(dataWord2);
        Packet result = null;
        try {
            result = new Packet(header, data);
        } catch (BitstreamException e) {
            // this exception won't happen because we know we are using a type 1 packet
            // (the exception is raised for type NONE)
        }
        
        return result;
    }

    /**
     * Create a zero-word type 1 packet
     * 
     * @param opcode The opcode to use in the packet header.
     * @param registerType The register to use in the packet header.
     * @return The newly created packet.
     */
    public static Packet buildZeroWordPacket(PacketOpcode opcode, RegisterType registerType) {
        int header = 0;
        try {
            header = getHeader(PacketType.ONE, opcode, registerType, 0);
        } catch (BitstreamException e) {
            // this exception won't happen because we know we are using a type 1 packet
            // (the exception is raised for type NONE)
        }
        List<Integer> data = new ArrayList<>(0);
        Packet result = null;
        try {
            result = new Packet(header, data);
        } catch (BitstreamException e) {
            // this exception won't happen because we know we are using a type 1 packet
            // (the exception is raised for type NONE)
        }
        return result;
    }
    
    /**
     * Create a multi-word type 1 packet
     * 
     * @throws BitstreamException 
     */
    public static Packet buildMultiWordType1Packet(PacketOpcode opcode, RegisterType registerType, List<Integer> data) throws BitstreamException {
        int header;
        header = getHeader(PacketType.ONE, opcode, registerType, data.size());
        return new Packet(header, data);        
    }

    /**
     * Create a zero-word type 2 packet
     * @throws BitstreamException 
     */
	public static Packet buildZeroWordType2Packet(PacketOpcode opcode, int numWords) throws BitstreamException {
		int header = 0;
        try {
            header = getHeader(PacketType.TWO, opcode, RegisterType.NONE, numWords);
        } catch (BitstreamException e) {
            // this exception won't happen because we know we are using a type 1 packet
            // (the exception is raised for type NONE)
        }
        List<Integer> data = new ArrayList<>(0);
        Packet result;
        result = new Packet(header, data);
        return result;
	}
    
    /**
     * Create a multi-word type 2 packet
     */
    public static Packet buildMultiWordType2Packet(PacketOpcode opcode, List<Integer> data) throws BitstreamException {
        int header;
        header = getHeader(PacketType.TWO, opcode, RegisterType.NONE, data.size());
        return new Packet(header, data);
    }
    
    /**
     * Create a packet header given the packet type, opcode, register type, and number of data words.
     */
	public static int getHeader(PacketType type, PacketOpcode opcode, RegisterType registerType, int numWords) throws BitstreamException {
    	if(type == PacketType.ONE) {
    	    if (numWords > MAX_TYPE_ONE_SIZE) {
    	        throw new BitstreamException("Error: Attempting to create a type 1 packet with too many words: " + numWords);
    	    }
    		return type.PacketValue() | opcode.PacketValue() | registerType.PacketValue() | numWords;
    	}
    	else if (type == PacketType.TWO) {
    	    if (numWords > MAX_TYPE_TWO_SIZE) {
    	        throw new BitstreamException("Error: Attempting to create a type 2 packet with too many words: " + numWords);
    	    }
    		return type.PacketValue() | opcode.PacketValue() | numWords;// | numWords;
    	}
    	else {
    	    throw new BitstreamException("Error: Attempting to create a packet header with an invalid packet type");
    	}
    }

	
	/**
	 * Set the _header, _type, _numWords, _opcode, and _register fields
	 * based on the information in the given packet header.
	 * 
	 * @param header
	 * @throws BitstreamException
	 */
	protected void setFieldsFromHeader(int header) throws BitstreamException {
        _header = header;
        _type = PacketType.getPacketType(header);
        if (_type == PacketType.NONE)
            throw new BitstreamException("Error: Invalid packet header");

        _numWords = _type.getNumWords(_header);

        if(_type == PacketType.ONE) {
            _opcode = PacketOpcode.getPacketOpcode(header);
            if(_opcode == PacketOpcode.NOP) {
                _register = RegisterType.NONE; 
            }
            else 
                _register = RegisterType.getRegisterType(header);
        }
        else if(_type == PacketType.TWO) {
            this._opcode = PacketOpcode.getPacketOpcode(header);            
        }
    }
	

    /** 
	 * The 32-bit packet header
	 */
	protected int _header;
	
	/**
     * Packet type, 1 or 2
     */
	protected PacketType _type;
	
	/**
	 * Packet opcode (i.e. NOP, READ, WRITE, RESERVED)
	 */
	protected PacketOpcode _opcode;
	
	/**
	 * Register type (if this isn't a NOP packet)
	 */
	protected RegisterType _register;
	
	/**
	 * In a type 1 packet, bits 10:0 are used to determine the number of words past the header.
	 * In a type 2 packet, bits 26:0 are used to determine the number of words past the header.
	 */
	protected int _numWords;
	
	/**
	 * All the data words in the packet
	 */
	// TODO: It seems waseteful to have a list of Integers for the data. Most packets are type 1 with only one data member
	// We should create an interface for a packet and provide a type 1 implementation with a single word and a type 2 implementation
	// that provides a List of words.
	protected List<Integer> _data;
	
}
