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
package edu.byu.ece.rapidSmith.placer;

import edu.byu.ece.rapidSmith.design.xdl.XdlDesign;
import edu.byu.ece.rapidSmith.design.xdl.XdlInstance;
import edu.byu.ece.rapidSmith.device.Site;
import edu.byu.ece.rapidSmith.interfaces.ise.XDLReader;
import edu.byu.ece.rapidSmith.interfaces.ise.XDLWriter;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.Random;

public class RandomPlacer{
  public static void main(String[] args) throws IOException {
    // Create and load a design
    XdlDesign design = new XDLReader().readDesign(Paths.get(args[0]));

    // Create a random number generator
    Random rng = new Random(0);

    // Place all unplaced instances
    for(XdlInstance i : design.getInstances()){
	    if(i.isPlaced()) continue;
	    List<Site> sites = design.getDevice().getAllCompatibleSites(i.getType());
	    int idx = rng.nextInt(sites.size());
	    int watchDog = 0;
	    // Find a free primitive site
	    while(design.isSiteUsed(sites.get(idx))){
	    	if(++idx > sites.size()) idx = 0;
	    	if(++watchDog > sites.size()) {
			    System.err.println("Placement failed.");
			    System.exit(1);
		    }
	    }
	    i.place(sites.get(idx));
    }
    
    // Save the placed design
    new XDLWriter().writeXDL(design, Paths.get(args[1]));
  }
}

