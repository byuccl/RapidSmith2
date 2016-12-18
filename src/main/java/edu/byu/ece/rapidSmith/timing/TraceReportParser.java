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
package edu.byu.ece.rapidSmith.timing;

import java.io.BufferedReader;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;

import edu.byu.ece.rapidSmith.design.xdl.XdlDesign;
import edu.byu.ece.rapidSmith.design.xdl.XdlInstance;
import edu.byu.ece.rapidSmith.design.xdl.XdlNet;
import edu.byu.ece.rapidSmith.design.xdl.XdlPin;
import edu.byu.ece.rapidSmith.interfaces.ise.XDLReader;
import edu.byu.ece.rapidSmith.util.Exceptions;

public class TraceReportParser{
	
	public static final String DELAY = "Delay:";
	public static final String OFFSET = "Offset:";
	
	private XdlDesign design = null;

	private BufferedReader br;
	private String line;

	private ArrayList<PathDelay> pathDelays;
	private ArrayList<PathOffset> pathOffsets;
	
	/**
	 * @return the pathDelays
	 */
	public ArrayList<PathDelay> getPathDelays() {
		return pathDelays;
	}

	/**
	 * @return the pathOffsets
	 */
	public ArrayList<PathOffset> getPathOffsets() {
		return pathOffsets;
	}

	public void parseTWR(String twrFileName, String xdlFileName) throws IOException {
		design = new XDLReader().readDesign(Paths.get(xdlFileName));
		parseTWR(twrFileName, design);
	}
	
	public void parseTWR(String twrFileName, XdlDesign design) throws IOException {
		this.design = design;
		parseTWR(twrFileName);
	}
	
	public void parseTWR(String twrFileName) throws IOException {
		pathDelays = new ArrayList<>();
		pathOffsets = new ArrayList<>();
		
		try (BufferedReader br = new BufferedReader(new FileReader(twrFileName))) {
			this.br = br;
			while((line = br.readLine()) != null){
				if(line.startsWith(DELAY)){
					pathDelays.add(parsePathStatement());
				}
				else if(line.startsWith(OFFSET)){
					pathOffsets.add(parseOffsetStatement(line));
				}
			}
		}
	}
	
	
	private String[] getNextLineTokens() {
		line = null;
		try {
			line = br.readLine();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return line.split("\\s+");
	}
	
	private PathOffset parseOffsetStatement(String line){
		PathOffset curr = new PathOffset();
		String[] parts;

		// Offset:                 -2.114ns (data path - clock path + uncertainty)
		parts = line.split("\\s+");
		curr.setOffset(Float.parseFloat(parts[1].substring(0, parts[1].length()-2)));
		
		// Source:               Gateway_In(4) (PAD)
		parts = getNextLineTokens();
		curr.setSource(parts[2]);
		
		// Destination:          sysgen_mult_x0/mult/comp0.core_instance0/blk00000003/blk00000423 (FF)
		parts = getNextLineTokens();
		curr.setDestination(parts[2]);

		// Destination Clock:    clk_net rising
		parts = getNextLineTokens();
		if(design != null) curr.setDestinationClock(design.getNet(parts[3]));

		// Data Path Delay:      3.437ns (Levels of Logic = 2)
		parts = getNextLineTokens();
		curr.setDataPathDelay(Float.parseFloat(parts[4].substring(0, parts[4].length()-2)));
		curr.setLevelsOfLogic(Integer.parseInt(parts[9].substring(0, parts[9].length()-1)));

		// Clock Path Delay:     5.551ns (Levels of Logic = 2)
		parts = getNextLineTokens();
		curr.setClockPathDelay(Float.parseFloat(parts[4].substring(0, parts[4].length()-2)));
		curr.setClockLevelsOfLogic(Integer.parseInt(parts[9].substring(0, parts[9].length()-1)));

		// Clock Uncertainty:    0.000ns
		parts = getNextLineTokens();
		curr.setClockUncertainty(Float.parseFloat(parts[3].substring(0, parts[3].length()-2)));			
		
		curr.setMaxDataPath(parsePathElements());
		curr.setMinDataPath(parsePathElements());
		
		return curr;
	}
	
	private PathDelay parsePathStatement(){
		PathDelay curr = new PathDelay();
		String[] parts;
		
		// Delay:                  4.867ns (data path - clock path skew + uncertainty)
		parts = line.split("\\s+");
		curr.setDelay(Float.parseFloat(parts[1].substring(0, parts[1].length()-2)));

		// Source:               sysgen_mult_x0/mult/comp0.core_instance0/blk00000003/blk00000366 (FF)
		parts = getNextLineTokens();
		curr.setSource(parts[2]);
		
		// Destination:          sysgen_mult_x0/mult/comp0.core_instance0/blk00000003/blk0000033a (FF)
		parts = getNextLineTokens();
		curr.setDestination(parts[2]);
		
		// Data Path Delay:      4.867ns (Levels of Logic = 14)
		parts = getNextLineTokens();
		curr.setDataPathDelay(Float.parseFloat(parts[4].substring(0, parts[4].length()-2)));
		curr.setLevelsOfLogic(Integer.parseInt(parts[9].substring(0, parts[9].length()-1)));
		
		// Clock Path Skew:      0.000ns
		parts = getNextLineTokens();
		curr.setDataPathDelay(Float.parseFloat(parts[4].substring(0, parts[4].length()-2)));
		
		// Source Clock:         clk_net rising
		parts = getNextLineTokens();
		if(design != null) curr.setSourceClock(design.getNet(parts[3]));
		
		// Destination Clock:    clk_net rising
		parts = getNextLineTokens();
		if(design != null) curr.setDestinationClock(design.getNet(parts[3]));
		
		// Clock Uncertainty:    0.000ns
		parts = getNextLineTokens();
		curr.setClockUncertainty(Float.parseFloat(parts[3].substring(0, parts[3].length()-2)));
		curr.setMaxDataPath(parsePathElements());

		return curr;
	}
	
	private ArrayList<PathElement> parsePathElements(){
		ArrayList<PathElement> currPath = new ArrayList<>();
		String[] parts;
		String dashedLine = "-------------------------------------------------";
		// Move forward to the ------ line
		while(!line.contains(dashedLine)){
			getNextLineTokens();
		}		
		parts = getNextLineTokens();
		
		// Parse the path elements
		PathElement currElement = null;
		while(!line.contains(dashedLine)){
			String pinName = parts[1].substring(parts[1].indexOf('.')+1);
			if(parts.length > 2){
				if(parts[2].equals("net")){
					String siteName = parts[1].substring(0, parts[1].indexOf('.'));

					currElement = new RoutingPathElement();
					currElement.setType("net");
					int offset = 0;
					if(parts[4].equals("e")){
						offset = 1;
					}
					
					currElement.setDelay(Float.parseFloat(parts[4+offset]));
					if(design != null){
						XdlNet net = design.getNet(parts[5+offset]);
						if(net == null){
							throw new Exceptions.FileFormatException("no net \"" + parts[4+offset] + "in design");
						}
						((RoutingPathElement)currElement).setNet(net);
						for(XdlPin p : net.getPins()){
							if(p.getName().equals(pinName) && p.getInstance().getSiteName().equals(siteName)){
								currElement.setPin(p);
								break;
							}
						}
					}
					
				}
				else{
					currElement = new LogicPathElement();
					if(parts.length < 5){
						String[] newParts = new String[5];
						newParts[1] = parts[1].substring(0, 21);
						newParts[2] = parts[1].substring(21);
						newParts[3] = parts[2];
						newParts[4] = parts[3];
						parts = newParts;
					}
					currElement.setType(parts[2]);
					//System.out.println("line="+line+", parts=" + parts.length);
					if(design != null){
						XdlInstance instance = design.getInstance(parts[4]);
						((LogicPathElement)currElement).setInstance(instance);
						if(instance == null){
							throw new Exceptions.FileFormatException("no instance \"" + parts[4] + " in design");
						}
						XdlPin p = instance.getPin(pinName);
						if(p == null){
							throw new Exceptions.FileFormatException("no pin \"" + pinName + " in instance " + parts[4]);
						}
						currElement.setPin(p);						
					}
					currElement.setDelay(Float.parseFloat(parts[3]));
				}
				
				currPath.add(currElement);
			}
			else{
				((LogicPathElement)currElement).addLogicalResource(parts[parts.length-1]);
			}
			parts = getNextLineTokens();
		}
		getNextLineTokens();
		
		return currPath;
	}
	
	
	public static void main(String[] args) throws IOException {
		TraceReportParser test = new TraceReportParser();
		test.parseTWR(args[0], args[1]);
		
		
		System.out.println("Path Delays: " + test.pathDelays.size());
		System.out.println("Path Offsets: " + test.pathOffsets.size());
		
	}
}
