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

import edu.byu.ece.rapidSmith.RSEnvironment;
import edu.byu.ece.rapidSmith.device.Device;
import edu.byu.ece.rapidSmith.device.creation.DeviceFilesCreator;
import edu.byu.ece.rapidSmith.device.creation.ISE_XDLRCRetriever;
import edu.byu.ece.rapidSmith.device.creation.Vivado_XDLRCRetriever;
import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * This class will create the device and wire enumerator files 
 * necessary for RapidSmith to operate.   
 * @author Chris Lavin
 */
public class Installer {
	public static String nl = System.getProperty("line.separator");
	public static String disclaimer = 
		"This material is based upon work supported by the National" + nl + 
		"Science Foundation under Grant No. 0801876. Any opinions," + nl + 
		"findings, and conclusions or recommendations expressed in this" + nl + 
		"material are those of the author(s) and do not necessarily" + nl + 
		"reflect the views of the National Science Foundation.";
	
	public static void main(String[] args){
		MessageGenerator.printHeader("RapidSmith Release " + Device.rapidSmithVersion + " - Installer");

		OptionParser parser = new OptionParser();
		parser.accepts("env", "Output directory for generated device files").withRequiredArg();
		parser.acceptsAll(Arrays.asList("force", "f"), "Overwrite existing device files");
		parser.acceptsAll(Arrays.asList("ise"), "Generate XDLRC using ISE. Otherwise, it will use an existing XDLRC");
		parser.accepts("ignore_disclaimer", "Ignores the disclaimer");
		parser.nonOptions("<device or family> ...");

		OptionSet options = null;
		try {
			options = parser.parse(args);
		} catch (OptionException e) {
			try {
				parser.printHelpOn(System.err);
			} catch (IOException ignored) {
			}
			System.exit(-1);
		}

		RSEnvironment env;
		if (options.has("env"))
			env = new RSEnvironment(Paths.get((String) options.valueOf("env")));
		else
			env = RSEnvironment.getDefault();
		boolean forceRebuild = options.has("force");
		boolean generateFromISE = options.has("ise");

		if (options.nonOptionArguments().size() < 1) {
			try {
				parser.printHelpOn(System.err);
			} catch (IOException ignored) {
			}
			System.exit(-1);
		}

		long timeStart = System.currentTimeMillis();

		System.out.println("DISCLAIMER:");
		System.out.println(disclaimer + nl + nl);

		if (options.has("ignore_disclaimer")) {
			System.out.println("By using this software you agree to the GPLv2 license" + nl +
					"agreement accompanying this software (/docs/gpl2.txt)");
		} else {
			System.out.println("Have you read the above disclaimer and agree to the GPLv2 license" + nl +
					"agreement accompanying this software (/docs/gpl2.txt)");
			MessageGenerator.agreeToContinue();
		}

		System.out.println("START: " + FileTools.getTimeString());

		// Check if user supplied file with parameters
		ArrayList<String> partNames = new ArrayList<>();
		for(Object oname : options.nonOptionArguments()) {
			String name = (String) oname;
			name = name.toLowerCase();
			if(name.startsWith("x")){
				partNames.add(name);
			}
			else{
				name = name.toUpperCase();
				partNames.addAll(RunXilinxTools.getPartNames(name, false));
			}
			
			for(String partName : partNames) {
				if (!forceRebuild) {
					Device device;
					try {
						device = env.getDevice(partName);
					} catch (Exception ignored) {
						device = null;
					}
					if (device != null) {
						System.out.println("File already exists for part " + partName +
								".  Use --force to overwrite.");
						continue;
					}
				}
				System.out.println("Creating files for " + partName);
				
				DeviceFilesCreator creator;
				if (generateFromISE) {
					creator = new DeviceFilesCreator(new ISE_XDLRCRetriever(), env);
				} else { 
					creator = new DeviceFilesCreator(new Vivado_XDLRCRetriever(), env);
				}
				
				creator.createDevice(partName);
			}
		}
		System.out.println("END: " + FileTools.getTimeString());
		System.out.println("Time Elapsed: " + (System.currentTimeMillis() - timeStart)/60000.0 + " minutes");
		MessageGenerator.printHeader("Installer Completed Successfully!");
	}
}

