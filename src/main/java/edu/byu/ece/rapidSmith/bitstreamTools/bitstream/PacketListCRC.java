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

import java.util.List;

/**
 * Extends the PacketList object by computing the CRC of packets
 * that are created. This simplifies the process of creating
 * new packets and computing the resulting CRC.
 *
 */
public class PacketListCRC extends PacketList {

	public PacketListCRC() {
		super();
		crc = new CRC();
	}
	
	public boolean add(Packet p) {
		crc.updateCRC(p);
		return super.add(p);
	}
	
	public boolean addAll(PacketList packets) {
	    boolean result = false;
        for (Packet packet : packets) {
            boolean addedThisOne = add(packet);
            result = result || addedThisOne;
        }
        return result;
	}

	public boolean addAll(List<Packet> packets) {
	    boolean result = false;
	    for (Packet packet : packets) {
	        boolean addedThisOne = add(packet);
	        result = result || addedThisOne;
	    }
	    return result;
	}

	public Packet addCRCWritePacket() {
		Packet p = PacketUtils.CRC_WRITE_PACKET(crc.computeCRCRegValue());
		add(p);
		return p;
	}

	protected CRC crc;
}
