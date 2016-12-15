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
package edu.byu.ece.rapidSmith.examples;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;
import java.util.Map.Entry;

import edu.byu.ece.rapidSmith.design.xdl.XdlDesign;
import edu.byu.ece.rapidSmith.design.xdl.XdlNet;
import edu.byu.ece.rapidSmith.design.PIP;
import edu.byu.ece.rapidSmith.device.*;
import edu.byu.ece.rapidSmith.device.families.FamilyInfo;
import edu.byu.ece.rapidSmith.device.families.FamilyInfos;
import edu.byu.ece.rapidSmith.interfaces.ise.XDLReader;

/**
 * Generated to help answer a request from Vincent in 
 * https://sourceforge.net/p/rapidsmith/discussion/1347397/thread/b3d9ceb0/
 * Created on: May 2, 2014
 */
public class CountingExample {

	private static HashSet<WireType> wireTypesOfInterest = null;

	static{
		wireTypesOfInterest = new HashSet<>();
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
	public static int countSwitchMatricesUsed(Collection<XdlNet> nets){
		HashSet<String> tiles = new HashSet<>();
		Set<TileType> intTypes = null;
		for(XdlNet n : nets){
			if(intTypes == null) {
				intTypes = loadIntTypes(n);
			}
			for(PIP p : n.getPIPs()){
				if(intTypes.contains(p.getTile().getType())){
					tiles.add(p.getTile().getName());
				}
			}
		}
		return tiles.size();
	}

	private static Set<TileType> loadIntTypes(XdlNet n) {
		Set<TileType> intTypes;
		Device device = n.getSourceTile().getDevice();
		FamilyInfo info = FamilyInfos.get(device.getFamily());
		intTypes = info.switchboxTiles();
		return intTypes;
	}

	/**
	 * Counts the number of wires used based on their resource length.
	 * Note: This method assumes that the nets are valid and routed 
	 * without sharing resources.
	 * 
	 * @param nets Nets to analyze
	 * @return
	 */
	public static Map<WireType,Integer> countUsedWireLengths(Collection<XdlNet> nets){
		HashMap<WireType, Integer> wireCounts = new HashMap<>();
		Set<TileType> intTypes = null;
		Device dev = null;
		// points
		
		for(XdlNet n : nets){
			if(intTypes == null) {
				intTypes = loadIntTypes(n);
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
	private static void countWire(Wire pipWire, HashMap<WireType,Integer> wireCounts, Device dev){
		WireEnumerator we = dev.getWireEnumerator();
		WireType pipWireType = we.getWireType(pipWire.getWireEnum());
		if(wireTypesOfInterest.contains(pipWireType)){
			// If a wire name has BEG in it, this is the only entry 
			// point to drive the resource, long lines 
			// have multiple points
			if(pipWire.getWireName().contains("BEG")){
				Integer count = wireCounts.get(pipWireType);
				if(count == null){
					wireCounts.put(pipWireType, 1);
				}else{
					wireCounts.put(pipWireType, count + 1);
				}
			}
		}
		if(pipWireType.equals(WireType.LONG)){
			// TODO
		}		
	}
	
	public static void main(String[] args) throws IOException {
		if(args.length == 0){
			System.out.println("Please supply an XDL file name");
			System.exit(0);
		}
		XdlDesign d = new XDLReader().readDesign(Paths.get(args[0]));
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
