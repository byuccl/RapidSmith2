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

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a Xilinx bitstream. It contains a BitstreamHeader object and
 * a PacketList object that contains all of the bitstream packets.
 * 
 */
public class Bitstream {
    
    /**
     * Create a bitstream with the given header, dummy word/sync data, and packets.
     * 
     * @param header
     * @param dummySyncData
     * @param packets
     */
    public Bitstream(BitstreamHeader header, DummySyncData dummySyncData, PacketList packets) {
        _header = header;
        _dummySyncData = dummySyncData;
        _packets = packets;
    }
    
    /**
     * Create a bitstream with the given dummy word/sync data and packets.
     * 
     * @param dummySyncData
     * @param packets
     */
    public Bitstream(DummySyncData dummySyncData, PacketList packets) {
        _header = null;
        _dummySyncData = dummySyncData;
        _packets = packets;
    }

    /**
     * Get the bitstream's header.
     */
    public BitstreamHeader getHeader() {
        return _header;
    }

    /**
     * Get the bitstream's DummySyncData
     */
    public DummySyncData getDummySyncData(){
    	return _dummySyncData;
    }
    
    /**
     * Get the bitstream's packet list.
     */
    public PacketList getPackets() {
        return _packets;
    }

    /**
     * Get the raw bytes in the bitstream, not including the header. This method is safe
     * to call whether or not the bitstream has a header.
     */
    public List<Byte> getRawBytesNoHeader() {
        List<Byte> bytes = new ArrayList<Byte>();
        // Add sync word at top of file
        bytes = addDummyAndSyncWords(bytes);
        
        // Add packet data
        bytes.addAll(_packets.toByteArray());
        return bytes;
    }

    /**
     * Get the data length of the bitstream. This includes the bytes in the dummy/sync
     * data and all of the packets in the packet list.
     */
    public int getDataLength() {
        int length = 0;
        length += _dummySyncData.getDataSize();
        for (Packet packet : _packets) {
            length += (4 + packet.getNumWords() * 4); // 4 bytes for the header, plus the data size
        }
        return length;
    }
    
    /**
     * Get the raw bytes in the bistream. This method is safe to call whether or not
     * the bitstream actually has a header. It includes the header only if the
     * bitstream has one.
     */
    public List<Byte> getRawBytes() {
        List<Byte> result = null;
        if (hasHeader()) {
            result = getRawBytesWithHeader();
        }
        else {
            result = getRawBytesNoHeader();
        }
        return result;
    }

    /**
     * Get the raw bytes in the bitstream, including the header. This method is only
     * safe to call if the bitstream actually has a header (use hasHeader() to determine
     * whether the bitstream has a header).
     */
    public List<Byte> getRawBytesWithHeader() {
        int length = getDataLength();
    
        List<Byte> bytes = _header.getHeaderBytes(length);
        bytes = addDummyAndSyncWords(bytes);
        bytes.addAll(_packets.toByteArray());
        return bytes;
    }
    
    /**
     * Determine whether the bitstream has a header.
     */
    public boolean hasHeader() {
        return (_header != null);
    }
    
    /**
     * Write the bitstream, including the header, to the given OutputStream. This
     * method is only safe to call if the bitstream has a header.
     * 
     * @param ostream
     * @throws IOException
     */
    public void outputHeaderBitstream(OutputStream ostream) throws IOException {
        writeBitstreamBytes(getRawBytesWithHeader(), ostream);
    }

    /**
     * Write the bitstream, not including a header, to the given OutputStream. This
     * method is safe to call whether or not the bitstream has a header.
     */
    public void outputRawBitstream(OutputStream ostream) throws IOException {
        writeBitstreamBytes(getRawBytesNoHeader(), ostream);
    }

    /**
     * Write the bitstream. If the header exists, write the bitstream with
     * the header. If the header does not exist, write the bitstream wihtout
     * the header.
     * 
     * @param ostream
     * @throws IOException
     */
    public void outputBitstream(OutputStream ostream) throws IOException {
    	if (this.hasHeader())
    		outputHeaderBitstream(ostream);
    	else
    		outputRawBitstream(ostream);
    }
    
    /**
     * Get an XML representation of the bitstream.
     */
    public String toString() {
        return toXMLString();
    }

    public String toString(int level) {
    	// level 0 is conventional toString
    	if (level == 0)
    		return toString();
    	
    	return toString(true, true);
    }
    
    public String toString(boolean printHeaderSummary, boolean printHeaderData) {
    	String str = new String();
    	
    	if (printHeaderSummary) {
    		if (hasHeader()) {
    			str += "***** Header Summary *****\n";
    			str += getHeader().toString();
    		} else {
    			str += "***** No Header *****\n";
    		}
    	}
    	
    	if (printHeaderData) {
    		if (hasHeader()) {
    			str += "***** Header Data *****\n";
    	        //Add header to string (if it exists)
    			List<Byte> headerBytes = getHeader().getHeaderBytesWithoutLength();
    			for (int i = 0; i < headerBytes.size(); i += 30) {
    				for (int j = i; (j < i + 30) && (j < headerBytes.size()); j++) {
    					str += BitstreamUtils.toHexString(headerBytes.get(j)) + " ";
    				}
    				str += "\n";
    	        }
    			str += "\n\n";
    		} else {
    			str += "***** No Header *****\n";
    		}
    	}

    	str += "***** Packets *****\n";

    	str += _packets.toString(true);
    	
    	return str;
    }
    
    public void toStream(boolean printHeaderSummary, boolean printHeaderData, PrintWriter pw) {
        
        if (printHeaderSummary) {
            if (hasHeader()) {
                pw.print("***** Header Summary *****\n");
                pw.print(getHeader().toString());
            } else {
                pw.print("***** No Header *****\n");
            }
        }
        
        if (printHeaderData) {
            if (hasHeader()) {
                pw.print("***** Header Data *****\n");
                //Add header to string (if it exists)
                List<Byte> headerBytes = getHeader().getHeaderBytesWithoutLength();
                for (int i = 0; i < headerBytes.size(); i += 30) {
                    for (int j = i; (j < i + 30) && (j < headerBytes.size()); j++) {
                        pw.print(BitstreamUtils.toHexString(headerBytes.get(j)) + " ");
                    }
                    pw.print("\n");
                }
                pw.print("\n\n");
            } else {
                pw.print("***** No Header *****\n");
            }
        }

        pw.print("***** Packets *****\n");

        _packets.toStream(true, pw); 
    }

    
    /**
     * Get an XML representation of the bitstream.
     */
    public String toXMLString() {
        StringBuffer sb = new StringBuffer();
    
        
    
        // XML XSL header for formatting
        sb.append("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>\n"
                + "<?xml-stylesheet type=\"text/xsl\" href=\"v4.xsl\"?>");

        sb.append("<bitstream>\n");

        //Add header to string (if it exists)
        if (hasHeader()) {
            sb.append("<header>\n");
            List<Byte> headerBytes = getHeader().getHeaderBytesWithoutLength();
            for (int i = 0; i < headerBytes.size(); i += 30) {
                for (int j = i; (j < i + 30) && (j < headerBytes.size()); j++) {
                    sb.append(BitstreamUtils.toHexString(headerBytes.get(j)) + " ");
                }
                sb.append("\n");
            }
            sb.append("</header>\n\n");
        }
        //Add body to sb
        sb.append("<body>\n");
        sb.append(_packets.toXML());
        sb.append("</body>\n\n");

        return sb.toString();
    }

    /**
     * Generate an MCS (PROM) file. This method generates an .mcs file that should
     * match the output of the Xilinx promgen tool when used with '-u 0'. The format
     * is closely related to the INtel MCS86 (Inellec 86) file format specification. It
     * is an ASCII representation of the bitstream packets. This format does not include
     * the bitstream header.
     * 
     * This is an older method copied from Ben Sellers/Chris Lavin's old tools and
     * has not yet been tested in the new bitstream tools.
     */
    public void writeBitstreamToMCS(OutputStream os) throws IOException {
        
        PrintWriter pw = new PrintWriter(os);
        
        List<Byte> bytes = new ArrayList<Byte>();
        int i = 0; // Byte array index pointer
        int j = 0; // Modulo counter
        int length = 0; // length of bitstream
        int tmp = 0; // Current byte
        int byte_count = 16; // Number of bytes per line
        int starting_address = 0x00;
        int sba_address = 0x00;
        int record_type = 0x00;
        int checksum = 0x00;
        String line = "";

        i = 0;
        bytes = addDummyAndSyncWords(bytes);
        bytes.addAll(_packets.toByteArray());

        // Print the first SBA Address line
        pw.write(":020000040000FA\n");

        length = bytes.size();
        j = 0;
        tmp = (int) bytes.get(i);
        while (i < length) {
            // Start a new line
            if (j == 0) {
                line = ":";
                if (length - i < 16)
                    byte_count = length - i;
                line += BitstreamUtils.intToFixedLengthHexString(byte_count, 2);
                line += BitstreamUtils.intToFixedLengthHexString(
                        starting_address, 4);
                line += BitstreamUtils
                        .intToFixedLengthHexString(record_type, 2);
                pw.write(line);

                checksum = 0x00;
                checksum -= byte_count;
                checksum -= (starting_address & 0xFF00) >> 8;
                checksum -= starting_address & 0xFF;
                checksum -= record_type;
            }

            // Add the checksum
            tmp = BitstreamUtils.reverseLSB(0xFF & tmp);
            checksum -= tmp;
            // Write out byte
            pw.write(BitstreamUtils.intToFixedLengthHexString(tmp, 2));

            // If we are at the end of the line, print checksum and increment address
            if (j == 15) {
                pw.write(BitstreamUtils.intToFixedLengthHexString(
                        0xFF & checksum, 2));
                pw.write("\n");
                if (starting_address == 0xFFF0) {
                    sba_address++;
                    checksum = 0x00;
                    checksum -= 0x02;
                    checksum -= 0x04;
                    checksum -= (sba_address & 0xFF00) >> 8;
                    checksum -= sba_address & 0xFF;

                    line = ":02000004";
                    line += BitstreamUtils.intToFixedLengthHexString(
                            sba_address, 4);
                    line += BitstreamUtils.intToFixedLengthHexString(
                            0xFF & checksum, 2);
                    line += "\n";
                    pw.write(line);
                    starting_address = 0x0000;
                } else {
                    starting_address += 16;
                }

            }

            i++;
            if (i < length) {
                tmp = (int) bytes.get(i);
            }
            j = (j + 1) & 16 - 1; // j = i % 16
        }

        // still need to print the checksum if we didn't end on an even line boundary
        if (j != 0) {
            pw.write(BitstreamUtils.intToFixedLengthHexString(0xFF & checksum, 2));
            pw.write("\n");
        }

        pw.write(":00000001FF\n");
        // close the file and return true
        pw.close();
    }

    
    /**
     * Add the dummy/sync word data to the given list of bytes and return
     * the result.
     */
    protected List<Byte> addDummyAndSyncWords(List<Byte> bytes) {
        
        for (Byte b : _dummySyncData.getData()) {
            bytes.add(b);
        }
        return bytes;
        
    }

    /**
     * Output the given list of bytes to the given OutputStream.
     * 
     * @param bitstream
     * @param ostream
     * @throws IOException
     */
    protected static void writeBitstreamBytes(List<Byte> bitstream, OutputStream ostream) throws IOException {
        for (int i = 0; i < bitstream.size(); i++) {
            ostream.write(bitstream.get(i));
        }
    }

    
    /**
     * Bitstream files can have a header that provides useful information about the
     * bitstream and device. This header is not required and thus this class member 
     * may be null.
     */
    protected BitstreamHeader _header;
    
    /**
     * Dummy words, optional bus width auto detection pattern (0x000000BB 0x11220044),
     * and sync word
     */
    protected DummySyncData _dummySyncData;

    /**
     * The packets in the bitstream
     */
    protected PacketList _packets;

}
