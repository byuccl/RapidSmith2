package edu.byu.ece.rapidSmith.device.vsrt.primitiveDefs;
/*
 * Copyright (c) 2010-2011 Brigham Young University
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
 */

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

/**
 * This class is designed to parse the XDLRC files to create the compact device
 * files used by XDL Tools.  It also extracts the primitive definitions from the 
 * XDLRC.  This parser is an improved version of the JavaCC parser which is no
 * longer in use.
 * @author Chris Lavin
 * Created on: Jul 7, 2010
 * Updated on: Jul 2, 2014
 * 		For use with the VSRTool, this parser was modified to only parse one Primitive def at a time. 
 */
public class XDLRCPrimitiveDefsParser {
	/** The list of extracted primitive definitions */
	 private PrimitiveDefList defs = new PrimitiveDefList();
	/** Set of seen primitive_defs */
	 private Set<String> processed = new HashSet<String>();

	/** This is the file input stream for reading the XDLRC */
	private BufferedReader in;

	/** The current line split into tokens by whitespace */
	private String[] tokens;
	private String currentFileName;
	private int lineCount;
	private Element lastElement = null;
	private boolean isAlternate = false;

	 public PrimitiveDefList getPrimitiveDefs() {
		  return defs;
	 }

	/**
	 * Parses the XDLRC files specified by fileNames and populates the
	 * PrimitiveDefList.
	 * @param file Names of the XDLRC files to parse.
	 * @return The populated PrimitiveDefList.
	 */
	public void parseXDLRCFile(String file) {
		isAlternate = file.endsWith("_ALTERNATE.def") ;
			
		loadFile(file);
		parseFile();
		closeFile();
	}

	private void parseFile() {

		// find each primitive_def section
		while(readLine()) {
			/////////////////////////////////////////////////////////////////////
			//	(primitive_def BSCAN 8 10
			/////////////////////////////////////////////////////////////////////
			if(tokens.length >= 1 && tokens[0].equals("(primitive_def")){
				parsePrimitiveDef();
			}
		}
	}

	/**
	 * Parses the primitive_def construct in XDLRC and creates
	 * the appropriate objects.
	 */
	private void parsePrimitiveDef() {
		PrimitiveDef def = new PrimitiveDef();
		String name = tokens[1].toUpperCase();
		def.setType(PrimitiveType.valueOf(name));
		int pinCount = Integer.parseInt(tokens[2]);
		int elementCount = Integer.parseInt(tokens[3]);
		ArrayList<PrimitiveDefPin> pins = new ArrayList<PrimitiveDefPin>(pinCount);
		ArrayList<Element> elements = new ArrayList<Element>(elementCount);
		
		for (int i = 0; i < pinCount; i++)
			pins.add(parsePrimitiveDefPin());

		for (int i = 0; i < elementCount; i++){
			Element tmp = parseElement(pins);
			if( tmp != null )
				elements.add(tmp);
		}

		def.setPins(pins);
		def.setElements(elements);
		if (!processed.contains(name)) {
			defs.add(def);
			processed.add(name);
		}
		
		//if (def.belCount() == 1)
		//	def.initializeOneBelPrimitiveDef(this.isAlternate);
	}
	
	private PrimitiveDefPin parsePrimitiveDefPin() {
		readLine();
		PrimitiveDefPin p = new PrimitiveDefPin();
		p.setExternalName(tokens[1]);
		p.setInternalName(tokens[2]);
		
		if ( tokens[3].equals("output)") )
			p.setDirection(PrimitiveDefPinDirection.OUTPUT );
		else if ( tokens[3].equals("input)") )
			p.setDirection(PrimitiveDefPinDirection.INPUT );
		else {
			p.setDirection(PrimitiveDefPinDirection.INOUT );
		}
		//p.setOutput(tokens[3].equals("output)"));
		return p;
	}

	private Element parseElement(ArrayList<PrimitiveDefPin> pins) {
		readLine();

		if(tokens[tokens.length-1].startsWith("#"))
		{
			StringBuilder cfgElement = new StringBuilder();
			cfgElement.append(tokens[1] + ":");
			readLine();
			for(int i = 1; i < tokens.length; i++)
				cfgElement.append(" " + tokens[i].replace(")", ""));
			this.lastElement.addCfgElement(cfgElement.toString());
			readLine();
			return null;
		}
		else {
			Element e = new Element();
			e.setName(tokens[1]);
			int elementPinCount = Integer.parseInt(tokens[2].replace(")", ""));
			e.setBel(tokens.length >= 5 && tokens[3].equals("#") && tokens[4].equals("BEL"));
			e.setIsTest(e.isBel() && tokens.length >=6 && tokens[5].equals("TEST"));
						
			//This can be optimized
			for (PrimitiveDefPin pin : pins) {
				if ( e.getName().equals(pin.getInternalName()) ) {
					e.setPin(true);
					break;
				}
			}
	
			for (int j = 0; j < elementPinCount; j++) {
				readLine();
				PrimitiveDefPin elementPin = new PrimitiveDefPin();
				elementPin.setInternalName(tokens[1]);
				
				if ( tokens[2].equals("output)") )
					elementPin.setDirection(PrimitiveDefPinDirection.OUTPUT );
				else if ( tokens[2].equals("input)") )
					elementPin.setDirection(PrimitiveDefPinDirection.INPUT );
				else {
					elementPin.setDirection(PrimitiveDefPinDirection.INOUT );
				}
				//elementPin.setOutput(tokens[2].equals("output)"));
				e.addPin(elementPin);
			}
			while (true) {
				readLine();
				if (tokens[0].equals("(cfg")) {
					for(int k = 1; k < tokens.length; k++){
						e.addCfgOption(tokens[k].replace(")", ""));
					}
				} else if(tokens[0].equals("(conn")) {
					Connection c = new Connection();
					c.setElement0(tokens[1]);
					c.setPin0(tokens[2]);
					c.setForwardConnection(tokens[3].equals("==>"));
					c.setElement1(tokens[4]);
					c.setPin1(tokens[5].substring(0, tokens[5].length()-1));
					e.addConnection(c);
				} else {
					break;
				}
			}
			this.lastElement = e;
			return e;
		}
	}

	/**
	 * Loads a new file into a buffered reader.
	 * @param fileName Name of the file to load
	 */
	private void loadFile(String fileName) {
		try {
			in = Files.newBufferedReader(Paths.get(fileName), Charset.defaultCharset());
		} catch (IOException e) {
			System.err.println("Failed to load file " + fileName);
			System.exit(1);
		}

		currentFileName = fileName;
		lineCount = 0;  
	}

	/**
	 * Closes the current reader
	 */
	private void closeFile() {
		try {
			in.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Reads the next line from the file and parses it into tokens
	 */
	private boolean readLine() {
		// read next line
		String line = null;
		try {
			line = in.readLine();
		} catch (IOException e) {
			System.err.println("Error reading file " + currentFileName);
			System.exit(1);
		}

		// check if end of file
		if (line == null) {
			tokens = null;
			return false;
		}

		// track line count and occasionally report heartbeat
		lineCount++;
		if(lineCount % 10000000 == 0)
			System.out.println("  Processing line number " + lineCount + " of file " + currentFileName);

		tokens = line.trim().split("\\s+");
		return true;
	}
}
