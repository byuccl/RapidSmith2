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

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;

import edu.byu.ece.rapidSmith.design.xdl.Design;
import edu.byu.ece.rapidSmith.design.xdl.Net;
import edu.byu.ece.rapidSmith.design.NetType;
import edu.byu.ece.rapidSmith.design.xdl.Pin;
import edu.byu.ece.rapidSmith.interfaces.ise.XDLReader;
import edu.byu.ece.rapidSmith.interfaces.ise.XDLWriter;


public class UnrouteNets {
	
	public static ArrayList<Net> combineStaticNets(Collection<Net> nets){
		ArrayList<Net> gndNets = new ArrayList<Net>();
		ArrayList<Net> vccNets = new ArrayList<Net>();
		ArrayList<Net> newNets = new ArrayList<Net>();
		for(Net net : nets){
			if(net.getType().equals(NetType.GND)) {
				gndNets.add(net);
			}
			else if(net.getType().equals(NetType.VCC)) {
				vccNets.add(net);
			}
		}
		
		if(!nets.removeAll(gndNets)) {
			System.out.println("Problem separating GND nets");
		}
		
		if (!nets.removeAll(vccNets)) {
			System.out.println("Problem separating VCC nets");
		}
		
		
		Net gndNet = new Net();
		gndNet.setName("GLOBAL_LOGIC0");
		gndNet.setType(NetType.GND);
		for(Net net : gndNets){
			for(Pin pin : net.getPins()) {
				if(!pin.isOutPin()) {
					gndNet.addPin(pin);
				}
			}
		}
		
		Net vccNet = new Net();
		vccNet.setName("GLOBAL_LOGIC1");
		vccNet.setType(NetType.VCC);
		for(Net net : vccNets){
			for(Pin pin : net.getPins()) {
				if(!pin.isOutPin()) {
					vccNet.addPin(pin);
				}
			}
		}
		
		newNets.addAll(nets);
		newNets.add(gndNet);
		newNets.add(vccNet);
		return newNets;
	}
	
	
	public static void main(String[] args) throws IOException {
		if(args.length != 2){
			System.out.println("USAGE: <input.xdl> <output.xdl>");
			System.exit(0);
		}
		Design design = new XDLReader().readDesign(Paths.get(args[0]));
		design.unrouteDesign();
		design.setNets(combineStaticNets(design.getNets()));
		new XDLWriter().writeXDL(design, Paths.get(args[1]));
	}
	
}
