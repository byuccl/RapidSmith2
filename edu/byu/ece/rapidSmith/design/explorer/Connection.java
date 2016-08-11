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
import edu.byu.ece.rapidSmith.device.Wire;
import edu.byu.ece.rapidSmith.device.WireEnumerator;

public class Connection {
	
	private final Wire startWire;
	private final Wire endWire;

	public Connection(PIP p){
		this.startWire = p.getStartWire();
		this.endWire = p.getEndWire();
	}
	
	public Connection(Wire startWire, Wire endWire){
		this.startWire = startWire;
		this.endWire = endWire;
	}

	/**
	 * @return the startTile
	 */
	public Tile getStartTile() {
		return startWire.getTile();
	}

	/**
	 * @return the endTile
	 */
	public Tile getEndTile() {
		return endWire.getTile();
	}

	/**
	 * @return the startWire
	 */
	public Wire getStartWire() {
		return startWire;
	}

	/**
	 * @return the endWire
	 */
	public Wire getEndWire() {
		return endWire;
	}

	public String toString(){
		return startWire + " --> "  + endWire;
	}
}
