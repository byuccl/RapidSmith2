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
package edu.byu.ece.rapidSmith.examples;

import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;

import edu.byu.ece.rapidSmith.design.Design;
import edu.byu.ece.rapidSmith.design.Net;
import edu.byu.ece.rapidSmith.design.PIP;
import edu.byu.ece.rapidSmith.device.Device;
import edu.byu.ece.rapidSmith.device.TileType;
import edu.byu.ece.rapidSmith.device.WireEnumerator;
import edu.byu.ece.rapidSmith.device.WireType;

/**
 * Generated to help answer a request from Vincent in 
 * https://sourceforge.net/p/rapidsmith/discussion/1347397/thread/b3d9ceb0/
 * Created on: May 2, 2014
 */
public class CountingExample {

	private static HashSet<WireType> wireTypesOfInterest = null;

	static{
		wireTypesOfInterest = new HashSet<WireType>();
		// Example: Virtex 4 & 5 wire types
		wireTypesOfInterest.add(WireType.DOUBLE);
		wireTypesOfInterest.add(WireType.DOUBLE_TURN);
		wireTypesOfInterest.add(WireType.TRIPLE);
		wireTypesOfInterest.add(WireType.TRIPLE_TURN);
		wireTypesOfInterest.add(WireType.PENT);
		wireTypesOfInterest.add(WireType.PENT_TURN);
		wireTypesOfInterest.add(WireType.HEX);
		wireTypesOfInterest.add(WireType.HEPT);
		wireTypesOfInterest.add(WireType.HEPT_TURN);
		// TODO - Add support for long lines which can be driven from multiple 		
	}
	
	/**
	 * Counts all the switch matrices used based on assigned PIPs.
	 * @param nets The nets to analyze.
	 * @return The number of unique switch matrices used in the nets provided.
	 */
	public static int countSwitchMatricesUsed(Collection<Net> nets){
		HashSet<String> tiles = new HashSet<String>();
		HashSet<TileType> intTypes = null;
		for(Net n : nets){
			if(intTypes == null) {
				// TODO Looks like getSwitchMatrixTypes should be static
				intTypes = n.getSourceTile().getDevice().getSwitchMatrixTypes();
			}
			for(PIP p : n.getPIPs()){
				if(intTypes.contains(p.getTile().getType())){
					tiles.add(p.getTile().getName());
				}
			}
		}
		return tiles.size();
	}
	
	/**
	 * Counts the number of wires used based on their resource length.
	 * Note: This method assumes that the nets are valid and routed 
	 * without sharing resources.
	 * 
	 * @param nets Nets to analyze
	 * @return
	 */
	public static Map<WireType,Integer> countUsedWireLengths(Collection<Net> nets){
		HashMap<WireType, Integer> wireCounts = new HashMap<WireType, Integer>();
		HashSet<TileType> intTypes = null;
		Device dev = null;
		// points
		
		for(Net n : nets){
			if(intTypes == null) {
				// TODO Looks like getSwitchMatrixTypes should be static
				intTypes = n.getSourceTile().getDevice().getSwitchMatrixTypes();
				dev = n.getSourceTile().getDevice();
			}
			for(PIP p : n.getPIPs()){
				countWire(p.getStartWire(), wireCounts, dev);
				countWire(p.getEndWire(), wireCounts, dev);
			}
		}		
		return wireCounts;
	}
	
	/**
	 * Helper function to countUsedWireLengths() to eliminate code duplication
	 * in counting wire types
	 * @param pipWire The wire to count
	 * @param wireCounts The map that is storing the total counts
	 * @param dev The relevant Device
	 */
	private static void countWire(int pipWire, HashMap<WireType,Integer> wireCounts, Device dev){
		WireEnumerator we = dev.getWireEnumerator();
		WireType pipWireType = we.getWireType(pipWire);
		if(wireTypesOfInterest.contains(pipWireType)){
			// If a wire name has BEG in it, this is the only entry 
			// point to drive the resource, long lines 
			// have multiple points
			if(we.getWireName(pipWire).contains("BEG")){
				Integer count = wireCounts.get(pipWireType);
				if(count == null){
					wireCounts.put(pipWireType, new Integer(1));
				}else{
					wireCounts.put(pipWireType, new Integer(count.intValue()+1));
				}
			}
		}
		if(pipWireType.equals(WireType.LONG)){
			// TODO
		}		
	}
	
	public static void main(String[] args) {
		if(args.length == 0){
			System.out.println("Please supply an XDL file name");
			System.exit(0);
		}
		Design d = new Design(Paths.get(args[0]));
		int switchMatricesUsed = countSwitchMatricesUsed(d.getNets());
		System.out.println("      Number of nets: " + d.getNets().size());
		System.out.println("Switch matrices used: " + switchMatricesUsed);
		Map<WireType,Integer> typeCounts = countUsedWireLengths(d.getNets());
		System.out.println("    Type counts used:");
		for(Entry<WireType,Integer> entry : typeCounts.entrySet()){
			
			System.out.println("\t" + entry.getKey() + ": " + entry.getValue());
		}
	}
}
