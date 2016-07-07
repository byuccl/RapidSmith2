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
package edu.byu.ece.rapidSmith.design.explorer;

import edu.byu.ece.rapidSmith.design.PIP;
import edu.byu.ece.rapidSmith.device.Tile;
import edu.byu.ece.rapidSmith.device.WireEnumerator;

public class Connection {
	
	Tile startTile;
	
	Tile endTile;
	
	int startWire;
	
	int endWire;

	public Connection(PIP p){
		this.startTile = p.getTile();
		this.endTile = p.getTile();
		this.startWire = p.getStartWire();
		this.endWire = p.getEndWire();
	}
	
	public Connection(Tile startTile, Tile endTile, int startWire, int endWire){
		this.startTile = startTile;
		this.endTile = endTile;
		this.startWire = startWire;
		this.endWire = endWire;
	}
	
	/**
	 * @return the startTile
	 */
	public Tile getStartTile() {
		return startTile;
	}

	/**
	 * @param startTile the startTile to set
	 */
	public void setStartTile(Tile startTile) {
		this.startTile = startTile;
	}

	/**
	 * @return the endTile
	 */
	public Tile getEndTile() {
		return endTile;
	}

	/**
	 * @param endTile the endTile to set
	 */
	public void setEndTile(Tile endTile) {
		this.endTile = endTile;
	}

	/**
	 * @return the startWire
	 */
	public int getStartWire() {
		return startWire;
	}

	/**
	 * @param startWire the startWire to set
	 */
	public void setStartWire(int startWire) {
		this.startWire = startWire;
	}

	/**
	 * @return the endWire
	 */
	public int getEndWire() {
		return endWire;
	}

	/**
	 * @param endWire the endWire to set
	 */
	public void setEndWire(int endWire) {
		this.endWire = endWire;
	}

	public String toString(){
		WireEnumerator we = endTile.getDevice().getWireEnumerator();
		return startTile + " " + we.getWireName(startWire) +
				" --> "  + endTile + " " + we.getWireName(endWire);
	}
}
