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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;

public class FileConverter {
	
	/**
	 * Converts the file called ncdFileName to XDL with a file by the same name
	 * but with an .xdl extension.
	 * @param ncdFile The NCD file to convert
	 * @return Name of the output XDL file, or null if conversion failed.
	 */
	public static Path convertNCD2XDL(Path ncdFile){
		Path xdlFileName = FileTools.replaceFileExtension(ncdFile, "xdl");
		boolean success = convertNCD2XDL(ncdFile, xdlFileName);
		return success ? xdlFileName : null;
	}

	/**
	 * Converts the file called nmcFileName to XDL with a file by the same name
	 * but with an .xdl extension.
	 * @param nmcFile The NMC file to convert
	 * @return Name of the output XDL file.
	 */
	public static Path convertNMC2XDL(Path nmcFile){
		Path xdlFile = FileTools.replaceFileExtension(nmcFile, "xdl");
		boolean success = convertNCD2XDL(nmcFile, xdlFile);
		return success ? xdlFile : null;
	}

	/**
	 * Converts the file called ngcFileName to EDIF with the name ndfFileName
	 * @param ngcFile The input NGC file name
	 * @param ndfFile The output NDF file name
	 */
	public static boolean convertNGC2NDF(Path ngcFile, Path ndfFile){
		String command = "ngc2edif " + ngcFile + " " + ndfFile + " -w";
		// Generate XDL
		try {
			Process p = Runtime.getRuntime().exec(command);
			StreamGobbler input = new StreamGobbler(p.getInputStream(), false);
			StreamGobbler err = new StreamGobbler(p.getErrorStream(), false);
			input.start();
			err.start();
			try {
				if(p.waitFor() != 0){
					throw new IOException();
				}
				p.destroy();
			} catch (InterruptedException e) {
				Logger logger = LoggerFactory.getLogger(FileConverter.class);
				logger.warn("Unexpected InterruptedException", e);
				return false;
			}
		} catch (IOException e) {
			System.err.println("NDF Generation failed.  Are the Xilinx tools on your path?" +
					"COMMAND: \""+ command +"\"");
			return false;
		}		
		return true;
	}
	
	/**
	 * Converts the file called ncdFileName to an NDF with a file by the same name
	 * but with an .ndf extension. 
	 * @param ngcFile The NGC file to convert.
	 * @return The output file name.
	 */
	public static Path convertNGC2NDF(Path ngcFile){
		Path ndfFile = FileTools.replaceFileExtension(ngcFile, "ndf");
		convertNGC2NDF(ngcFile, ndfFile);
		return ndfFile;
	}	
	
	/**
	 * Converts the file called ncdFileName to XDL with the name xdlFileName
	 * @param ncdFile The input NCD file name
	 * @param xdlFile The output XDL file name
	 */
	public static boolean convertNCD2XDL(Path ncdFile, Path xdlFile){
		String command = "xdl -ncd2xdl " + ncdFile + " " + xdlFile;
		// Generate XDL
		try {
			Process p = Runtime.getRuntime().exec(command);
			StreamGobbler input = new StreamGobbler(p.getInputStream(), false);
			StreamGobbler err = new StreamGobbler(p.getErrorStream(), false);
			input.start();
			err.start();
			try {
				if(p.waitFor() != 0){
					throw new IOException();
				}
				p.destroy();
			} catch (InterruptedException e) {
				Logger logger = LoggerFactory.getLogger(FileConverter.class);
				logger.warn("Unexpected InterruptedException", e);
				return false;
			}
		} catch (IOException e) {
			System.err.println("XDL Generation failed.  Are the Xilinx tools on your path?");
			return false;
		}		
		return true;
	}
	
	/**
	 * Converts the file called xdlFileName to an NCD with a file by the same name
	 * but with an .ncd extension. 
	 * @param xdlFile The XDL file to convert.
	 * @return Name of the output NCD file or null if conversion failed.
	 */
	public static Path convertXDL2NCD(Path xdlFile){
		Path ncdFile = FileTools.replaceFileExtension(xdlFile, "ncd");
		boolean success = convertXDL2NCD(xdlFile, ncdFile);
		return success ? ncdFile : null;
	}
	
	/**
	 * Converts the file called xdlFileName to an NMC with a file by the same name
	 * but with an .nmc extension. Note that this will always have 1 error with Virtex 4
	 * parts as they will never contain all DCMs configured.  This error can be ignored.
	 * @param xdlFile The XDL file to convert.
	 * @return Name of the output NMC file.
	 */
	public static Path convertXDL2NMC(Path xdlFile){
		Path nmcFileName = FileTools.replaceFileExtension(xdlFile, "nmc");
		boolean success = convertXDL2NCD(xdlFile, nmcFileName);
		return success ? nmcFileName : null;
	}

	/**
	 * Converts xdlFileName to an NCD file called ncdFileName.  It uses the 
	 * -force option by default.
	 * @param xdlFile The input XDL file
	 * @param ncdFile The output NCD file
	 * @return True if operation was successful, false otherwise.
	 */
	public static boolean convertXDL2NCD(Path xdlFile, Path ncdFile){
		String command = "xdl -xdl2ncd -force " + xdlFile + " " + ncdFile;
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
			catch (InterruptedException e){
				Logger logger = LoggerFactory.getLogger(FileConverter.class);
				logger.warn("Unexpected InterruptedException", e);
				return false;
			}
			p.destroy();
		} catch (IOException e) {
			System.err.println("NCD/NMC Generation failed.  Are the Xilinx tools on your path?");
			return false;
		}		
		return true;
	}
}
