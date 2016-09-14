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
package edu.byu.ece.rapidSmith.examples;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Objects;

import edu.byu.ece.rapidSmith.design.*;
import edu.byu.ece.rapidSmith.design.subsite.Connection;
import edu.byu.ece.rapidSmith.design.subsite.RouteTree;
import edu.byu.ece.rapidSmith.design.xdl.Design;
import edu.byu.ece.rapidSmith.design.xdl.Net;
import edu.byu.ece.rapidSmith.design.xdl.Pin;
import edu.byu.ece.rapidSmith.device.*;
import edu.byu.ece.rapidSmith.interfaces.ise.XDLReader;
import edu.byu.ece.rapidSmith.interfaces.ise.XDLWriter;
import edu.byu.ece.rapidSmith.util.FileConverter;
import edu.byu.ece.rapidSmith.util.MessageGenerator;

import static edu.byu.ece.rapidSmith.util.FileTools.getFileExtension;

/**
 * This class is an example of how to do some simple routing by hand. It
 * is more used a demonstration of how to do real routing with RapidSmith,
 * rather than being a useful way to route.
 * @author Chris Lavin
 * Created on: Jul 15, 2010
 */
public class HandRouter{

	/** This is the current device of the design that was loaded */
	private Device dev;
	/** This is the current design we are loading */
	private Design design;
	/** Standard Input */
	private BufferedReader br;

	/**
	 * Initialize the HandRouter with the design
	 * @param inputFileName The input file to load
	 */
	public HandRouter(String inputFileName) throws IOException {
		Path inputFile = Paths.get(inputFileName);
		
		// Check if we are loading an NCD file, convert accordingly
		if(Objects.equals(getFileExtension(inputFile), "ncd"))
			inputFile = FileConverter.convertNCD2XDL(inputFile);

		// Load the design
		design = new XDLReader().readDesign(inputFile);
		dev = design.getDevice();

		// Delete the temporary XDL file, if needed
		//FileTools.deleteFile(inputFileName);
	}

	/**
	 * Prompt the user with options to perform routing of a particular net.
	 * @param netName Name of the net to route
	 */
	private void HandRoute(String netName){
		Net net = design.getNet(netName);
		int choice;
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

		ArrayList<PIP> path =  new ArrayList<PIP>();
		ArrayList<PIP> pipList = new ArrayList<PIP>();

		RouteTree currTree = null;
		Connection currConn;
		ArrayList<Connection> wiresList = null;
		ArrayList<RouteTree> choices;

		// This keeps track of all the possible starting points (or sources) that
		// we can use to route this net.
		ArrayList<RouteTree> sources = new ArrayList<RouteTree>();

		// Add the original source from the net
		Wire w = dev.getPrimitiveExternalPin(net.getSource());
		sources.add(new RouteTree(w));

		// In this loop we'll route each sink pin of the net separately
		for(Pin sinkPin : net.getPins()){
			if(sinkPin.isOutPin()) continue; // Don't try to route the source to the source
			boolean start = true;
			boolean finishedRoute = false;

			// Here is where we create the current sink that we intend to target in this
			//routing iteration.
			Wire sink = dev.getPrimitiveExternalPin(sinkPin);

			MessageGenerator.printHeader("Current Sink: " + sink);

			// Almost all routes must pass through a particular switch matrix and wire to arrive
			// at the particular sink.  Here we obtain that information to help us target the routing.
			SinkPin sp = sink.getTile().getSinkPin(sink.getWireEnum());
			Wire switchMatrixSink = new TileWire(sp.getSwitchMatrixTile(sink.getTile()), sink.getWireEnum());
			System.out.println("** Sink must pass through switch matrix: " + switchMatrixSink + " **");

			while(!finishedRoute){
				// Here we prompt the user to choose a source to start the route from.  If this
				// is the first tile we are routing in this net there will only be one choice.
				if(start){
					start = false;
					System.out.println("Sources:");
					for(int i=0; i < sources.size(); i++){
						RouteTree src = sources.get(i);
						System.out.println("  " + i+". " + src.getWire());
					}
					System.out.print("Choose a source from the list above: ");
					try {
						choice = Integer.parseInt(br.readLine());
					} catch (Exception e){
						System.out.println("Error, could not get choice, defaulting to 0");
						choice = 0;
					}

					// Once we get the user's choice, we can determine what wires the source
					// can connect to by calling Tile.getWireConnections(int wire)
					currTree = sources.get(choice);
					wiresList = new ArrayList<>(currTree.getWire().getWireConnections());
					if(wiresList.isEmpty()){
						// We'll have to choose something else, this source had no other connections.
						System.out.println("Wire had no connections");
						continue;
					}
				}

				// Print out some information about the sink we are targeting
				if(sink.getTile().getSinks().get(sink.getWireEnum()).switchMatrixSinkWire == -1){
					System.out.println("\n\nSINK: " + sink + " " + net.getName());
				} else {
					System.out.println("\n\nSINK: " + sink + " " + net.getName()
							+ " thru(" + switchMatrixSink + ")");
				}

				// Print out a part of the corresponding PIP that we have chosen
				System.out.println("  pip " + currTree.getWire() + " -> ");

				// Check if we have reached the sink node
				if (sink.equals(currTree.getWire())){
					System.out.println("You completed the route!");
					// If we have, let's print out all the PIPs we used
					for (PIP pip : path){
						System.out.print(pip.toString());
						pipList.add(pip);
						finishedRoute = true;
					}
				}
				if(!finishedRoute){
					// We didn't find the sink yet, let's print out the set of
					// choices we can follow given our current wire
					choices = new ArrayList<RouteTree>();
					for (int i = 0; i < wiresList.size(); i++) {
						currConn = wiresList.get(i);
						RouteTree rt = currTree.addConnection(currConn);
						rt.setCost(currTree.getCost() + 1);
						choices.add(rt);

						System.out.println("    " + i + ". " + currConn.getSinkWire() +
								" " + choices.get(i).getCost());
					}
					System.out.print("\nChoose a route (s to start over): ");
					try {
						String cmd = br.readLine();
						if (cmd.equals("s")) {
							start = true;
							continue;
						}
						choice = Integer.parseInt(cmd);
					} catch (IOException e){
						System.out.println("Error reading response, try again.");
						continue;
					}
					if(wiresList.get(choice).isPip()){
						Wire sourceWire = currTree.getWire();
						path.add(new PIP(sourceWire.getTile(), sourceWire.getWireEnum(),
								wiresList.get(choice).getSinkWire().getWireEnum()));
					}

					currTree = choices.get(choice);
					wiresList = new ArrayList<>(currTree.getWire().getWireConnections());

					System.out.println("PIPs so far: ");
					for (PIP p : path){
						System.out.print("  " + p.toString());
					}
				}
			}
		}
		// Apply the PIPs we have choosen to the net
		net.setPIPs(pipList);
	}

	/**
	 * Saves the design to a file.
	 * @param outputFileName
	 */
	private void saveDesign(String outputFileName) throws IOException {
		Path outputFile = Paths.get(outputFileName);
		if(Objects.equals(getFileExtension(outputFile), "ncd")) {
			Path xdlFile = Paths.get(outputFileName+"temp.xdl");
			new XDLWriter().writeXDL(design, xdlFile);
			FileConverter.convertXDL2NCD(xdlFile, outputFile);

			// Delete the temporary XDL file, if needed
			//FileTools.deleteFile(xdlFileName); 
		}
		else{
			new XDLWriter().writeXDL(design, outputFile);
		}
	}
	
	public static void main(String[] args) throws IOException {
		if(args.length != 2){
			MessageGenerator.briefMessageAndExit("USAGE: <input.xdl|input.ncd> <output.xdl|output.ncd>");
		}
		HandRouter hr = new HandRouter(args[0]);

		String nl = System.getProperty("line.separator");
		MessageGenerator.printHeader("Hand Router Example");
		System.out.println("This program will read in a design and allow the user to " +
				"route (or reroute) a "+nl+"net.");


		boolean continueRouting = true;
		hr.br = new BufferedReader(new InputStreamReader(System.in));

		while(continueRouting){
			System.out.println(nl+"Commands:");
			System.out.println("  1: Route a net by name");
			System.out.println("  2: Route an arbitrary net (for fun)");
			System.out.println("  3: Save design");
			System.out.println("  4: Exit");

			try{
				System.out.print(">> ");
				Integer cmd = Integer.parseInt(hr.br.readLine().trim());
				switch(cmd){
					case 1:
						System.out.println("Enter net name:");
						hr.HandRoute(hr.br.readLine().trim());
						break;
					case 2:
						for(Net net : hr.design.getNets()){
							System.out.println("Routing net: " + net.getName());
							hr.HandRoute(net.getName());
							break;
						}
						break;
					case 3:
						hr.saveDesign(args[1]);
						System.out.println("Design saved to " + args[1]);
						continueRouting = false;
						break;
					case 4:
						System.exit(0);
					default:
						continueRouting = false;
				}
			}
			catch(IOException e){
				System.out.println("Error, try again.");
			}
		}
	}
}
