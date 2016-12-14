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

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import edu.byu.ece.rapidSmith.design.*;
import edu.byu.ece.rapidSmith.design.xdl.*;
import edu.byu.ece.rapidSmith.device.*;
import edu.byu.ece.rapidSmith.interfaces.ise.XDLReader;
import edu.byu.ece.rapidSmith.interfaces.ise.XDLWriter;

/**
 * A simple class to illustrate how to use some of the basic methods in RapidSmith.
 * @author Chris Lavin
 */
public class HelloWorld{

	public static void main(String[] args) throws IOException {
		// Create a new Design from scratch rather than load an existing design
		XdlDesign design = new XdlDesign();
		
		// Set its name
		design.setName("helloWorld");
		
		// When we set the part name, it loads the corresponding Device and WireEnumerator
		// Always include package and speed grade with the part name
		design.setPartName("xc4vfx12ff668-10");
		FamilyType family = FamilyType.valueOf("VIRTEX4");

		// Create a new instance
		XdlInstance myInstance = new XdlInstance();
		myInstance.setName("Bob");
		myInstance.setType(SiteType.valueOf(family, "SLICEL"));
		// We need to add the instance to the design so it knows about it
		design.addInstance(myInstance);
		// Make the F LUT an Inverter Gate
		myInstance.addAttribute(new XdlAttribute("F","LUT_of_Bob","#LUT:D=~A1"));
		
		// Add the instance to the design
		design.addInstance(myInstance);
		
		// This is how we can get the reference to the instance from the design, by name
		XdlInstance bob = design.getInstance("Bob");
		
		// Let's find a primitive site for our instance Bob
		Map<String, Site> primitiveSites = design.getDevice().getPrimitiveSites();
		for(Site site : primitiveSites.values()){
			// Some primitive sites can have more than one type reside at the site, such as SLICEM 
			// sites which can also have SLICELs placed there.  Checking if the site is compatible
			// makes sure you get the best possible chance of finding a place for bob to live.
			if(site.isCompatiblePrimitiveType(bob.getType())){
				// Let's also make sure we don't place bob on a site that is already used
				if(!design.isPrimitiveSiteUsed(site)){
					bob.place(site);
					System.out.println("We placed bob on tile: " + bob.getTile() +
							" and site: " + bob.getPrimitiveSiteName());
					break;
				}
			}
		}
		
		// Another way to find valid primitive sites if we want to use an exclusive site type
		List<Site> allSitesOfTypeSLICEL = design.getDevice().getAllSitesOfType(bob.getType());
		for(Site site : allSitesOfTypeSLICEL){
			// Let's also make sure we don't place bob on a site that is already used
			if(!design.isPrimitiveSiteUsed(site)){
				bob.place(site);
				System.out.println("We placed bob on tile: " + bob.getTile() +
						" and site: " + bob.getPrimitiveSiteName());
				break;
			}
		}
		
		// Let's create an IOB to drive our Inverter gate in Bob's LUT
		XdlInstance myIOB = new XdlInstance();
		myIOB.setName("input");
		myIOB.setType(SiteType.valueOf(family, "IOB"));
		design.addInstance(myIOB);
		// These are typical attributes that need to be set to configure the IOB
		// the way you like it
		myIOB.addAttribute(new XdlAttribute("INBUFUSED","","0"));
		myIOB.addAttribute(new XdlAttribute("IOATTRBOX","","LVCMOS25"));
		// Another way to find a primitive site is by name, this is the pin name 
		// that you might find in a UCF file
		myIOB.place(design.getDevice().getPrimitiveSite("C17"));
		
		// Let's also create a new net to connect the two pins
		XdlNet fred = new XdlNet();
		// Be sure to add fred to the design
		design.addNet(fred);
		fred.setName("fred");
		// All nets are normally of type WIRE, however, some are also GND and VCC
		fred.setType(NetType.WIRE);
		// Add the IOB pin as an output pin or the source of the net
		fred.addPin(new XdlPin(true,"I",myIOB));
		// Add Bob as the input pin or sink, which is the input to the inverter
		fred.addPin(new XdlPin(false, "F1", bob));
		
		// Now let's write out our new design
		// We'll print the standard XDL comments out
		String fileName = design.getName() +".xdl";
		new XDLWriter().writeXDL(design, Paths.get(fileName));

		// We can load XDL files the same way.
		XdlDesign inputFromFile = new XDLReader().readDesign(Paths.get(fileName));

		// Hello World
		System.out.println(inputFromFile.getName());
	}
}
