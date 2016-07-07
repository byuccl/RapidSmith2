/*
 * Copyright (c) 2010 Brigham Young University
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
package edu.byu.ece.rapidSmith.device;

import java.io.Serializable;

/**
 * Keeps track of tile offsets for switch matrix sinks in Device/Tile class.
 * @author Chris Lavin
 * Created on: Jul 13, 2010
 */
public class SinkPin implements Serializable{
	/** Keeps track of the wire which drives this sink from the nearest switch matrix */
	public int switchMatrixSinkWire;
	
	/** Keeps track of the switch matrix which drives this sink
	 * &lt;31-16: X Tile Offset, 15-0: Y Tile Offset&gt; */
	public int switchMatrixTileOffset;

	/**
	 * Constructs a new SinkPin object.
	 * @param switchMatrixSinkWire the wire sourcing the sink
	 * @param xOffset the tile offset in the X direction
	 * @param yOffset the tile offset in the Y direction
	 */
	public SinkPin(int switchMatrixSinkWire, int xOffset, int yOffset) {
		this.switchMatrixSinkWire = switchMatrixSinkWire;
		this.switchMatrixTileOffset = xOffset << 16 | (yOffset & 0xFFFF);
	}
	
	/**
	 * Returns the X offset of the switch matrix tile
	 * @return the X offset of the switch matrix tile
	 */
	public int getXSwitchMatrixTileOffset() {
		return switchMatrixTileOffset >> 16;
	}
	
	/**
	 * Returns the Y offset of the switch matrix tile
	 * @return the Y offset of the switch matrix tile
	 */
	public int getYSwitchMatrixTileOffset() {
		// The Y tile offset is the lowest 16 bits. 
		// This needs to be trimmed and signed extended
		return (switchMatrixTileOffset << 16) >> 16;
	}

	/**
	 * Returns the switch matrix tile (relative to the tile used as a parameter).
	 * @param tile the tile to get the switch matrix tile of
	 * @return the switch matrix tile of the specified tile
	 */
	public Tile getSwitchMatrixTile(Tile tile) {
		int tileRow = tile.getRow()+getYSwitchMatrixTileOffset();
		int tileColumn = tile.getColumn()+getXSwitchMatrixTileOffset();
		Device dev = tile.getDevice();
		return dev.getTile(tileRow, tileColumn);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode(){
		final int prime = 31;
		int result = 1;
		result = prime * result + switchMatrixSinkWire;
		result = prime * result + switchMatrixTileOffset;
		return result;
	}
	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj){
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SinkPin other = (SinkPin) obj;
		if (switchMatrixSinkWire != other.switchMatrixSinkWire)
			return false;
		if (switchMatrixTileOffset != other.switchMatrixTileOffset)
			return false;
		return true;
	}
	
	public String toString(){
		return ""; // we.getWireName(switchMatrixSinkWire) + " " + switchMatrixTileOffset;
	}
}
