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
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;

import edu.byu.ece.rapidSmith.design.xdl.Design;
import edu.byu.ece.rapidSmith.design.xdl.Instance;
import edu.byu.ece.rapidSmith.design.xdl.Net;
import edu.byu.ece.rapidSmith.design.PIP;
import edu.byu.ece.rapidSmith.device.Site;
import edu.byu.ece.rapidSmith.interfaces.ise.XDLReader;
import edu.byu.ece.rapidSmith.device.TileWire;
import edu.byu.ece.rapidSmith.device.Wire;

/**
 * This class is meant to debug problem with XDL files.  It
 * is not quite complete, but will check for a few kinds of errors.
 * @author Chris Lavin
 */
public class XDLDesignChecker{


	public static ArrayList<Net> setNets(ArrayList<Net> nets, int size){
		ArrayList<Net> removed = new ArrayList<>();
		while(size > 0){
			removed.add(nets.remove(0));
			size--;
		}
		return removed;
	}

	public static ArrayList<PIP> removeFirstHalf(ArrayList<PIP> pips){
		ArrayList<PIP> removed = new ArrayList<>();
		int size = pips.size();
		int halfSize = size/2;
		while(halfSize > 0) {
			removed.add(pips.remove(0));
			halfSize--;
		}
		return removed;
	}

	public static ArrayList<PIP> removeLastHalf(ArrayList<PIP> pips){
		ArrayList<PIP> removed = new ArrayList<>();
		int size = pips.size();
		int halfSize = size/2;
		while(halfSize > 0) {
			removed.add(pips.remove(pips.size()-1));
			halfSize--;
		}
		return removed;
	}

	public static String readLineFromStdIn(){
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

		try{
			return br.readLine();
		}
		catch(IOException e){
			e.printStackTrace();
			return null;
		}
	}

	public static void runCommandWithoutOutput(String command){

		// Generate NCD
		try {
			Process p = Runtime.getRuntime().exec(command);
			StreamGobbler err = new StreamGobbler(p.getErrorStream(), false);
			StreamGobbler input = new StreamGobbler(p.getInputStream(), false);
			input.start();
			err.start();
			try {
				if(p.waitFor() != 0){
					throw new IOException();
				}
			}
			catch (InterruptedException e) {
				e.printStackTrace();
				System.out.println("Unknown Error While converting XDL to NCD/NMC.");
				System.exit(1);
			}
			p.destroy();
		} catch (IOException e){
			System.out.println("COMMAND FAILED:");
			System.out.println("\""+ command +"\"");
		}
	}

	public static void runCommandAndPrintOutput(String command){

		// Generate NCD
		try {
			Process p = Runtime.getRuntime().exec(command);
			StreamGobbler err = new StreamGobbler(p.getErrorStream(), true);
			StreamGobbler input = new StreamGobbler(p.getInputStream(), true);
			input.start();
			err.start();
			try {
				if(p.waitFor() != 0){
					throw new IOException();
				}
			}
			catch (InterruptedException e) {
				e.printStackTrace();
				System.out.println("Unknown Error While converting XDL to NCD/NMC.");
				System.exit(1);
			}
			p.destroy();
		} catch (IOException e){
			System.out.println("COMMAND FAILED:");
			System.out.println("\""+ command +"\"");
		}
	}
		
	public static void main(String[] args) throws IOException {
		if(args.length != 1){
			MessageGenerator.briefMessageAndExit("USAGE: <input.xdl>");
		}

		Design design = new XDLReader().readDesign(Paths.get(args[0]));

		// Check for unique placement of primitives
		MessageGenerator.printHeader("CHECKING FOR UNIQUE PRIMITIVE PLACEMENTS ... ");
		HashMap<Site, Instance> usedSites = new HashMap<>();
		for(Instance inst : design.getInstances()){
			if(inst.getPrimitiveSite() == null){
				System.out.println("Warning: " + inst.getName() +" is unplaced.");
			}
			else if(usedSites.containsKey(inst.getPrimitiveSite())){
				System.out.println("ERROR: Placement conflict at site: " + inst.getPrimitiveSiteName() +" (tile: "+inst.getTile()+")");
				System.out.println("  Involving at least these two instances:");
				System.out.println("    " + inst.getName());
				System.out.println("    " + usedSites.get(inst.getPrimitiveSite()).getName());
			}
			else{
				usedSites.put(inst.getPrimitiveSite(), inst);
			}
		}



		// Check for duplicate PIPs
		HashMap<PIP,Net> pipMap = new HashMap<>();
		MessageGenerator.printHeader("CHECKING FOR DUPLICATE PIPS ... ");
		for(Net net : design.getNets()){
			for(PIP pip : net.getPIPs()){
				Net tmp = pipMap.get(pip);
				if(tmp == null){
					pipMap.put(pip, net);
				}
				else{
					System.out.print("  Duplicate PIP: " + pip.toString());
					System.out.println("  in nets: " + net.getName());
					System.out.println("           " + tmp.getName());
				}
			}
		}

		// Checking for duplicate PIP sinks
		HashMap<Wire, Net> pipSinks = new HashMap<>();
		MessageGenerator.printHeader("CHECKING FOR DUPLICATE PIP SINKS ... ");
		for(Net net : design.getNets()){
			for(PIP pip : net.getPIPs()){
				Wire n = new TileWire(pip.getTile(), pip.getEndWire());
				Net tmp = pipSinks.get(n);
				if(tmp == null){
					pipSinks.put(n, net);
				}
				else{
					System.out.print("  Duplicate PIP Sink: " + n.toString());
					System.out.println("  in nets: ");
					System.out.println("           " + net.getName());
					System.out.println("           " + tmp.getName());
				}
			}
		}
	}
}
