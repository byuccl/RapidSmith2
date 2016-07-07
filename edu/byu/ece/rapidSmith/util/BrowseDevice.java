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
package edu.byu.ece.rapidSmith.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import edu.byu.ece.rapidSmith.RapidSmithEnv;
import edu.byu.ece.rapidSmith.device.*;

/**
 * This class is a simple method to browse device information by tile.
 * @author Chris Lavin
 * Created on: Jul 12, 2010
 */
public class BrowseDevice{

	public static void run(Device dev){
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		WireEnumerator we = dev.getWireEnumerator();
		Tile t = null;
		while(true){
			System.out.println("Commands: ");
			System.out.println(" 1: Get wire connections in tile");
			System.out.println(" 2: Check if wire is a PIP wire");
			System.out.println(" 3: List RouteThrough wires");
			System.out.println(" 4: Follow wire connections");
			System.out.println(" 5: List primitives of a tile");
			System.out.println(" 6: Get tile of a primitive site");
			System.out.println(" 7: Exit");
			try {
				Integer cmd = Integer.parseInt(br.readLine().trim());
				switch(cmd){
					case 1:
						System.out.println("Enter tile name: ");
						t = dev.getTile(br.readLine().trim());
						System.out.println("Choosen Tile: " + t.getName());

						System.out.println("Enter wire name: ");
						String wire = br.readLine().trim();
						WireConnection[] wires = t.getWireConnections(we.getWireEnum(wire));
						if(wires != null){
							for(WireConnection w : wires){
								System.out.println("  " + w.toString());
							}
						}
						else{
							System.out.println(" No Connections");
						}
						break;
					case 2:
						System.out.println("No longer supported");
//						System.out.println("Enter wire name:");
//						String wire1 = br.readLine().trim();
//						System.out.println("isPIP? " + dev.isPIPWire(wire1));
						break;
					case 3:
						System.out.println("No longer supported");
//						System.out.println("PIPRouteThroughs");
//						for(WireConnection w : dev.getRouteThroughMap().keySet()){
//							System.out.println("  " + w.toString() + " " + dev.getRouteThroughMap().get(w).toString());
//						}
						break;
					case 4:
						System.out.println("Enter start tile name: ");
						t = dev.getTile(br.readLine().trim());
						System.out.println("Choosen start tile: " + t.getName());

						System.out.println("Enter start wire name: ");
						String startWire = br.readLine().trim();
						
						while(true){
							if(t.getWireHashMap() == null){
								System.out.println("This tile has no wires.");
								break;
							}
							if(t.getWireConnections(we.getWireEnum(startWire)) == null){
								System.out.println("This wire has no connections, it may be a sink");
								break;
							}
							WireConnection[] wireConnections = t.getWireConnections(we.getWireEnum(startWire));
							System.out.println(t.getName() + " " + startWire + ":");
							for (int i = 0; i < wireConnections.length; i++) {
								System.out.println("  " + i + ". " + wireConnections[i].getTile(t) +" " + we.getWireName(wireConnections[i].getWire()));
							}
							System.out.print("Choose a wire: ");
							int ndx;
							try{
								ndx = Integer.parseInt(br.readLine().trim());
								t = wireConnections[ndx].getTile(t);
								startWire = we.getWireName(wireConnections[ndx].getWire());
							}
							catch(Exception e){
								System.out.println("Did not understand, try again.");
								continue;
							}
							
						}
						break;
					case 5:
						System.out.println("Enter tile name: ");
						t = dev.getTile(br.readLine().trim());
						System.out.println("Choosen Tile: " + t.getName());

						if(t.getPrimitiveSites() == null){
							System.out.println(t.getName() + " has no primitive sites.");
						}
						else{
							for(PrimitiveSite p : t.getPrimitiveSites()){
								System.out.println("  " + p.getName());
							}
						}
					
						break;
					case 6:
						System.out.println("Enter tile name: ");
						String siteName = br.readLine().trim();
						PrimitiveSite site = dev.getPrimitiveSite(siteName);
						if(site == null){
							System.out.println("No primitive site called \"" + siteName +  "\" exists.");
						}
						else {
							System.out.println(site.getTile());
						}
						break;
					case 7:
						System.exit(0);
						
				}
			} catch (Exception e) {
				System.out.println("Bad input, try again.");
			}
		}
	}
	public static void main(String[] args){
		MessageGenerator.printHeader(" RapidSmith Device Browser");		
		if(args.length != 1){
			MessageGenerator.briefMessageAndExit("USAGE: <device part name, ex: xc4vfx12ff668 >");
		}
		Device dev = RapidSmithEnv.getDefaultEnv().getDevice(args[0]);

		run(dev);
	}
}
