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

import java.nio.file.Paths;
import java.util.Random;
import edu.byu.ece.rapidSmith.design.*;
import edu.byu.ece.rapidSmith.device.*;
import edu.byu.ece.rapidSmith.util.MessageGenerator;

public class RandomPlacer{
  public static void main(String[] args){
    // Create and load a design
    Design design = new Design(Paths.get(args[0]));

    // Create a random number generator
    Random rng = new Random(0);

    // Place all unplaced instances
    for(Instance i : design.getInstances()){
	    if(i.isPlaced()) continue;
	    Site[] sites = design.getDevice().getAllCompatibleSites(i.getType());
	    int idx = rng.nextInt(sites.length);
	    int watchDog = 0;
	    // Find a free primitive site
	    while(design.isPrimitiveSiteUsed(sites[idx])){
	    	if(++idx > sites.length) idx = 0;
	    	if(++watchDog > sites.length) MessageGenerator.briefErrorAndExit("Placement failed.");
	    }
	    i.place(sites[idx]);
    }
    
    // Save the placed design
    design.saveXDLFile(args[1]);
  }
}

