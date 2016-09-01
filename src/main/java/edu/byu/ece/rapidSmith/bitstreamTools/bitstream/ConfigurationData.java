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
/**
 * 
 */
package edu.byu.ece.rapidSmith.bitstreamTools.bitstream;

import java.util.List;

/**
 * abstract class meant to be the base class of anything that can produce
 * configuration data (DummySyncData, Packet, and PacketList).
 * 
 * With this base class, tools can use any of the sub classes to send
 * configuration data to a configuration port, regardless of the specific source
 * of the data.
 * 
 * @author Peter Lieber
 * 
 */
public abstract class ConfigurationData {
	public abstract List<Byte> toByteArray();
}
