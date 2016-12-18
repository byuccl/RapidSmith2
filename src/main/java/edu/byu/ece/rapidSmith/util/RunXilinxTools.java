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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

/**
 * This is a generic class which has several static methods to call Xilinx tools from within the RapidSmith
 * framework.
 * @author Chris Lavin
 */
public class RunXilinxTools {
	/**
	 * Generates the brief version of the XDLRC file specified by partName.
	 * @param partName The part name of the Xilinx device for which to generate the XDLRC file.
	 * @param optionalOutputFileName Provides a way to generated a custom output file name.  If parameter is null, the
	 * output file name is [part name + _brief.xdlrc]
	 * @return True if completed successfully, false otherwise.
	 */
	public static boolean generateBriefXDLRCFile(String partName, String optionalOutputFileName){
		return generateXDLRCFile(partName, optionalOutputFileName, true);
	}

	/**
	 * Generates the full version of the XDLRC file specified by partName.
	 * @param partName The part name of the Xilinx device for which to generate the XDLRC file.
	 * @param optionalOutputFileName Provides a way to generated a custom output file name.  If parameter is null, the
	 * output file name is [part name + _full.xdlrc]
	 * @return True if completed successfully, false otherwise.
	 */
	public static boolean generateFullXDLRCFile(String partName, String optionalOutputFileName){
		return generateXDLRCFile(partName, optionalOutputFileName, false);
	}

	/**
	 * Generates the XDLRC file specified by partName.
	 * @param partName The part name of the Xilinx device for which to generate the XDLRC\ file.
	 * @param optionalOutputFileName Provides a way to generated a custom output file name.  If parameter is null, the
	 * output file name is [part name + _brief/_full.xdlrc]
	 * @param briefFile Determines if it should generate a brief or full XDLRC file.
	 * @return True if completed successfully, false otherwise.
	 */
	public static boolean generateXDLRCFile(String partName, String optionalOutputFileName, boolean briefFile){
		String fileNameSuffix = briefFile ? "_brief.xdlrc" : "_full.xdlrc";
		String defaultFileName = partName + fileNameSuffix;
		String xdlrcFileName = optionalOutputFileName==null ? defaultFileName : optionalOutputFileName;
		String commandParameters = briefFile ? "" : "-pips -all_conns ";
		String command = "xdl -report " + commandParameters + PartNameTools.removeSpeedGrade(partName) + " " + xdlrcFileName;

		// Check to see if the file already exists
		if(new File(xdlrcFileName).exists()){
			// It already exists
			return true;
		}

		try{
			// Run the XDL command
			Process p = Runtime.getRuntime().exec(command);
			if(p.waitFor() != 0){
				System.err.println("XDLRC Generation failed, 'xdl' execution failed." +
						" COMMAND: " + command);
				return false;
			}
			if(p.exitValue() != 0){
				System.err.println("XDLRC Generation failed, is the part \"" +
						PartNameTools.removeSpeedGrade(partName) + "\" name valid?" + " COMMAND: " + command);
				return false;
			}
		}
		catch (IOException e){
			e.printStackTrace();
			System.err.println("XDLRC generation failed: error during 'xdl' execution.");
			return false;
		}
		catch (InterruptedException e){
			e.printStackTrace();
			System.err.println("XDLRC generation failed: process interrupted.");
			return false;
		}
		return true;
	}

	/**
	 * Convenient method to get the minimum clock period for a given NCD file.
	 * This method assumes single clock domain.
	 * @param ncdFileName Name of the existing NCD file to get the clock period
	 * from.
	 * @return The minimum clock period in nanoseconds, or null if
	 * none is found.
	 */
	public static Float getMinClkPeriodFromTRCE(String ncdFileName) throws IOException {
		String twrFileName = ncdFileName.replace(".ncd", ".twr");
		String cmd = "trce -a -v 100 " + ncdFileName + " -o " + twrFileName;
		int returnValue = runCommand(cmd, false);
		if(returnValue != 0){
			return null;
		}
		return getMinClkPeriodFromTWRFile(twrFileName);
	}

	/**
	 * Parses a TWR file to get the minimum clock period.  Assumes
	 * a single clock domain.
	 * @param twrFileName Name of the TWR file to parse.
	 * @return The number of nanoseconds of the minimum clock period
	 * or null if none found.
	 */
	public static Float getMinClkPeriodFromTWRFile(String twrFileName) throws IOException {
		boolean nextLine = false;
		boolean secondLine = false;
		try(BufferedReader br = new BufferedReader(new FileReader(twrFileName))) {
			String line;
			while((line = br.readLine()) != null){
				if(secondLine){
					String[] parts = line.split("\\s+");
					return Float.parseFloat(parts[2].substring(0, parts[2].indexOf('|')));
				}
				if(nextLine){
					secondLine = true;
					nextLine = false;
				}
				if(line.contains("Source Clock   |Dest:Rise")){
					nextLine = true;
				}
			}
		} catch (NumberFormatException e) {
			return null;
		}
		return null;
	}


	/**
	 * A generic method to run a command from the system command line.
	 * @param command The command to execute.  This method blocks until the command finishes.
	 * @param verbose When true, it will first print to std.out the command and also all of the
	 * command's output (both std.out and std.err) to std.out.
	 * @return The return value of the process if it terminated, if there was a problem it returns null.
	 */
	public static Integer runCommand(String command, boolean verbose){
		if(verbose) System.out.println(command);
		int returnValue;
		try {
			Process p = Runtime.getRuntime().exec(command);
			StreamGobbler input = new StreamGobbler(p.getInputStream(), verbose);
			StreamGobbler err = new StreamGobbler(p.getErrorStream(), verbose);
			input.start();
			err.start();
			try {
				returnValue = p.waitFor();
				p.destroy();
			} catch (InterruptedException e){
				e.printStackTrace();
				System.err.println("ERROR: The command was interrupted: \"" + command + "\"");
				return null;
			}
		} catch (IOException e){
			e.printStackTrace();
			System.err.println("ERROR: The command was interrupted: \"" + command + "\"");
			return null;
		}
		return returnValue;
	}

	/**
	 * An easy way to just get one family of part names at a time.
	 * See getPartNames(String[] familyNames, boolean includeSpeedGrades)
	 * @param familyName The single family name to get part names for.
	 * @param includeSpeedGrades A flag indicating to include all parts by various speed grades.
	 * @return All installed part names for the specified family, or null if none were specified.
	 */
	public static ArrayList<String> getPartNames(String familyName, boolean includeSpeedGrades){
		if(familyName == null) return null;
		String[] family = {familyName};
		return getPartNames(family, includeSpeedGrades);
	}

	/**
	 * Runs Xilinx PartGen to obtain all installed Xilinx FPGA part names for a given set of families.
	 * @param familyNames Names of the Xilinx FPGA families (virtex4, virtex5, ...) to generate names for.
	 * @param includeSpeedGrades A flag indicating to include all parts by various speed grades.
	 * @return All installed part names for the specified families, or null if none were specified.
	 */
	public static ArrayList<String> getPartNames(String[] familyNames, boolean includeSpeedGrades){
		String executableName = "partgen -arch ";
		String pathToPartgen = "";
		if(familyNames == null) return null;
		String line;
		String lastPartName;
		String[] tokens;
		Process p;
		BufferedReader input;
		ArrayList<String> partNames = new ArrayList<>();
		ArrayList<String> speedGrades = new ArrayList<>();
		try{
			for (String familyName : familyNames) {
				// Run partgen for each family
				p = Runtime.getRuntime().exec(executableName + familyName);
				input = new BufferedReader(new InputStreamReader(p.getInputStream()));
				lastPartName = null;
				while ((line = input.readLine()) != null) {
					tokens = line.split("\\s+");
					if (tokens.length > 0 && tokens[0].startsWith("x")) {
						lastPartName = tokens[0];
						if (includeSpeedGrades && tokens[1].equals("SPEEDS:")) {
							speedGrades.clear();
							int j = 2;
							while (j < tokens.length && !tokens[j].equals("(Minimum") && tokens[j].startsWith("-")) {
								speedGrades.add(tokens[j]);
								j++;
							}
						}
					} else if (lastPartName != null) {
						if (includeSpeedGrades && !speedGrades.isEmpty()) {
							for (String speedGrade : speedGrades) {
								partNames.add(lastPartName + tokens[1] + speedGrade);
							}
						} else {
							partNames.add(lastPartName + tokens[1]);
						}
					}
				}

				if (p.waitFor() != 0) {
					Logger logger = LoggerFactory.getLogger(RunXilinxTools.class);
					String msg = "Part name generation failed: partgen failed to execute " +
							"correctly.  Check spelling and make sure the families: " +
							MessageGenerator.createStringFromArray(familyNames) +
							System.getProperty("line.separator") + "are installed.";
					logger.error(msg);
					return null;
				}
			}
		} 
		catch (IOException e){
			Logger logger = LoggerFactory.getLogger(RunXilinxTools.class);
			logger.error("Part name generation failed: error reading partgen output.", e);
			return null;
		}
		catch (InterruptedException e){
			Logger logger = LoggerFactory.getLogger(RunXilinxTools.class);
			logger.error("Part name generation failed: interruption during partgen.", e);
			return null;
		}
		return partNames;
	}
}
