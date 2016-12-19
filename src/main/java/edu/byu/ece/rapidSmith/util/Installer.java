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
package edu.byu.ece.rapidSmith.util;

import edu.byu.ece.rapidSmith.RSEnvironment;
import edu.byu.ece.rapidSmith.device.Device;
import edu.byu.ece.rapidSmith.device.creation.*;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Objects;

/**
 * This class will create the device and wire enumerator files 
 * necessary for RapidSmith to operate.   
 * @author Chris Lavin
 */
public class Installer {
	public static final String nl = System.getProperty("line.separator");
	public static final String disclaimer =
		"This material is based upon work supported by the National" + nl + 
		"Science Foundation under Grant No. 0801876 and 1265957. Any opinions," + nl + 
		"findings, and conclusions or recommendations expressed in this" + nl + 
		"material are those of the author(s) and do not necessarily" + nl + 
		"reflect the views of the National Science Foundation.";
	
	public static void main(String[] args){
		MessageGenerator.printHeader("RapidSmith Release " + Device.rapidSmithVersion + " - Installer");

		ArgumentParser parser = buildArgParser();

		Namespace options = null;
		try {
			options = parser.parseArgs(args);
		} catch (ArgumentParserException e) {
			parser.handleError(e);
			System.exit(-1);
		}

		RSEnvironment env = RSEnvironment.defaultEnv();
		long timeStart = System.currentTimeMillis();

		System.out.println("DISCLAIMER:");
		System.out.println(disclaimer + nl + nl);

		if (options.getBoolean("ignore_disclaimer")) {
			System.out.println("By using this software you agree to the GPLv3 license" + nl +
					"agreement accompanying this software (/docs/LICENSE.GPL3.TXT)");
		} else {
			System.out.println("Have you read the above disclaimer and agree to the GPLv3 license" + nl +
					"agreement accompanying this software (/docs/LICENSE.GPL3.TXT)");
			try {
				MessageGenerator.agreeToContinue();
			} catch (IOException e) {
				System.err.println("Error reading from stdin");
				System.exit(1);
			}
		}

		System.out.println("START: " + FileTools.getTimeString());

		// Check if user supplied file with parameters
		ArrayList<String> partNames = new ArrayList<>();
		for(Object opart : options.getList("device")) {
			String part = (String) opart;

			XDLRCRetriever retriever;
			switch (options.getString("generate")) {
				case "file":
					Path xdlrcFile = Paths.get(part);
					retriever = new UserProvidedXDLRCRetriever(xdlrcFile);
					System.out.println("Creating device from file " + part);
					break;
				case "ise":
					retriever = new ISE_XDLRCRetriever(part);
					System.out.println("Creating device for " + part);
					break;
				default:
					System.err.println("Invalid generate option");
					System.exit(-1);
					throw new AssertionError();
			}

			try {
				DeviceFilesCreator creator = new DeviceFilesCreator(retriever);
				creator.createDevice();
			} catch (IOException e) {
				System.err.println("Encountered error handling file");
				System.err.println(e.getMessage());
				e.printStackTrace();
			} catch (DeviceCreationException e) {
				System.err.println("Error creating device file");
				System.err.println(e.getMessage());
				e.printStackTrace();
			}
		}
		System.out.println("END: " + FileTools.getTimeString());
		System.out.println("Time Elapsed: " + (System.currentTimeMillis() - timeStart)/60000.0 + " minutes");
		MessageGenerator.printHeader("Installer Completed Successfully!");
	}

	private static ArgumentParser buildArgParser() {
		ArgumentParser parser = ArgumentParsers.newArgumentParser("RapidSmith Installer")
				.defaultHelp(true)
				.description("Adds support to RapidSmith for a device.");
		parser.addArgument("--generate")
				.choices("file", "ise")
				.setDefault("file")
				.help("Generate the XDLRC part");
		parser.addArgument("--ignore_disclaimer")
				.action(Arguments.storeTrue())
				.help("Ignore the disclaimer");
		parser.addArgument("device")
				.nargs("+")
				.help("XDLRC file or device");
		return parser;
	}
}

