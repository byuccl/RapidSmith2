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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class is to aid manipulating and converting Xilinx part names
 * and the FamilyType enum.  It has facilities to convert part names
 * to its corresponding family type as well as many other methods.
 * @author Chris Lavin
 * Created on: Jan 27, 2011
 */
public class PartNameTools{
	/** A static regular expression to aid in parsing part names */
	private static Pattern partNamePattern = Pattern.compile("([a-z]+)|([0-9]+)|([a-z]*)|([0-9]*)|([a-z]*)");
	/** Stores a set of FamilyTypes which require ISE 10.1.03 or older to function. */
	private static HashSet<FamilyType> legacyTypes;
	
	private static FamilyType[] basicTypes = {FamilyType.KINTEX7,
								  			FamilyType.SPARTAN2, FamilyType.SPARTAN2E, FamilyType.SPARTAN3, 
											FamilyType.SPARTAN3A, FamilyType.SPARTAN3ADSP, FamilyType.SPARTAN3E,
											FamilyType.SPARTAN6, FamilyType.VIRTEX, FamilyType.VIRTEX2,
											FamilyType.VIRTEX2P, FamilyType.VIRTEX4, FamilyType.VIRTEX5,
											FamilyType.VIRTEX6, FamilyType.VIRTEX7, FamilyType.VIRTEXE};
	
	static{
		legacyTypes = new HashSet<>();
		legacyTypes.add(FamilyType.SPARTAN2);
		legacyTypes.add(FamilyType.SPARTAN2E);
		legacyTypes.add(FamilyType.VIRTEX);
		legacyTypes.add(FamilyType.VIRTEXE);
		legacyTypes.add(FamilyType.VIRTEX2);
		legacyTypes.add(FamilyType.VIRTEX2P);
	}
	
	public static FamilyType[] getBasicFamilyTypes(){
		return basicTypes;
	}
	
	/**
	 * This method determines which family types require the older version
	 * of the Xilinx tools (10.1.03 or older).  
	 * @param familyType The family type to check.
	 * @return True if this part requires older tools (10.1.03) or older, false otherwise.
	 */
	public static boolean isFamilyTypeLegacy(FamilyType familyType){
		getBaseTypeFromFamilyType(familyType);
		return legacyTypes.contains(familyType);
	}
	
	/**
	 * This method removes the speed grade (ex: -10) from a conventional Xilinx part name.
	 * @param partName The name of the part to remove the speed grade from.
	 * @return The base part name with speed grade removed.  If no speed grade is present, returns
	 * the original string.
	 */
	public static String removeSpeedGrade(String partName){
		if(partName != null && partName.contains("-")){
			return partName.substring(0, partName.indexOf("-"));
		}
		else{
			return partName;
		}
	}
	
	/**
	 * This method will take a Xilinx part name and determine its base family 
	 * architecture name. Ex: xq4vlx100 will return VIRTEX4.  For differentiating
	 * family types (qvirtex4 rather than virtex4) use getExactFamilyTypeFromPart().
	 * @param partName Name of the part
	 * @return The base family architecture type or null if invalid partName.
	 */
	public static FamilyType getFamilyTypeFromPart(String partName){
		return getBaseTypeFromFamilyType(getExactFamilyTypeFromPart(partName));
	}
	
	/**
	 * This helper method will parse a part name into smaller parts which 
	 * are alternating letters and numbers.  For example: xc5vlx110tff1136 
	 * becomes ['xc', '5', 'vlx', '110', 'tff', '1136']
	 * @param partName Part name to parse.
	 * @return The parts of the part name
	 */
	private static String[] splitPartName(String partName){
		if(partName == null){
			partName = "";
		}

	    int last_match = 0;
	    LinkedList<String> splitted = new LinkedList<>();
		Matcher m = partNamePattern.matcher(partName);
        while(m.find()){
        	if(!partName.substring(last_match,m.start()).trim().isEmpty()){
        		splitted.add(partName.substring(last_match,m.start()));
        	}
        	if(!m.group().trim().isEmpty()){
        		splitted.add(m.group());
        	}
            last_match = m.end();
        }
        if(!partName.substring(last_match).trim().isEmpty()){
            splitted.add(partName.substring(last_match));        	
        }
        return splitted.toArray(new String[splitted.size()]);
	}
	
	
	/**
	 * Gets and returns the exact Xilinx family type of the part name 
	 * (ex: qvirtex4 instead of virtex4). DO NOT use exact family 
	 * methods if it is to be used for accessing device or wire enumeration 
	 * files as RapidSmith does not generate files for devices that have 
	 * XDLRC compatible files.  
	 * @return The exact Xilinx family type from the given part name.
	 */
	public static FamilyType getExactFamilyTypeFromPart(String partName){
		partName = removeSpeedGrade(partName);
		partName = partName.toLowerCase();
		if(!partName.startsWith("x")){
			return null;
		}
		// Chop up partName into regular pieces for matching
		String[] tokens = splitPartName(partName);

		// Match part name with family
		switch (tokens[0]) {
			case "xcv":
				if (tokens.length >= 3 && tokens[2].startsWith("e")) {
					return FamilyType.VIRTEXE;
				} else {
					return FamilyType.VIRTEX;
				}
			case "xc":
				switch (tokens[1]) {
					case "2":
						if (tokens[2].equals("s")) {
							if (tokens.length >= 5 && tokens[4].startsWith("e")) {
								return FamilyType.SPARTAN2E;
							} else {
								return FamilyType.SPARTAN2;
							}
						} else if (tokens[2].startsWith("vp")) {
							return FamilyType.VIRTEX2P;
						} else if (tokens[2].startsWith("v")) {
							return FamilyType.VIRTEX2;
						}
						break;
					case "3":
						if (tokens[2].equals("sd")) {
							return FamilyType.SPARTAN3ADSP;
						} else if (tokens[2].startsWith("s")) {
							if (tokens.length >= 5 && tokens[4].startsWith("e")) {
								return FamilyType.SPARTAN3E;
							} else if (tokens.length >= 5 && tokens[4].startsWith("a")) {
								return FamilyType.SPARTAN3A;
							} else {
								return FamilyType.SPARTAN3;
							}
						}
						break;
					case "4":
						if (tokens[2].startsWith("v")) {
							return FamilyType.VIRTEX4;
						}
						break;
					case "5":
						if (tokens[2].startsWith("v")) {
							return FamilyType.VIRTEX5;
						}
						break;
					case "6":
						if (tokens[2].startsWith("v")) {
							if (tokens.length >= 5 && (tokens[4].startsWith("l") || tokens[4].startsWith("tl"))) {
								return FamilyType.VIRTEX6L;
							} else {
								return FamilyType.VIRTEX6;
							}
						} else if (tokens[2].startsWith("s")) {
							if (tokens.length >= 5 && tokens[4].startsWith("l")) {
								return FamilyType.SPARTAN6L;
							} else {
								return FamilyType.SPARTAN6;
							}
						}
						break;
					case "7":
						if (tokens[2].startsWith("v")) {
							return FamilyType.VIRTEX7;
						} else if (tokens[2].startsWith("a")) {
							return FamilyType.ARTIX7;
						} else if (tokens[2].startsWith("k")) {
							return FamilyType.KINTEX7;
						} else if (tokens[2].startsWith("z")) {
							return FamilyType.ZYNQ;
						}
						break;
				}
				break;
			case "xa":
				if (tokens[1].equals("2") && tokens.length >= 5 && tokens[4].startsWith("e")) {
					return FamilyType.ASPARTAN2E;
				} else if (tokens[1].equals("3")) {
					if (tokens[2].equals("sd")) {
						return FamilyType.ASPARTAN3ADSP;
					} else if (tokens[2].startsWith("s")) {
						if (tokens.length >= 5 && tokens[4].startsWith("e")) {
							return FamilyType.ASPARTAN3E;
						} else if (tokens.length >= 5 && tokens[4].startsWith("a")) {
							return FamilyType.ASPARTAN3A;
						} else {
							return FamilyType.ASPARTAN3;
						}
					}
				} else if (tokens[1].equals("6")) {
					return FamilyType.ASPARTAN6;
				}
				break;
			case "xq":
				switch (tokens[1]) {
					case "2":
						if (tokens[2].equals("v")) {
							return FamilyType.QVIRTEX2;
						} else if (tokens[2].equals("vp")) {
							return FamilyType.QVIRTEX2P;
						}

						break;
					case "4":
						if (tokens[2].startsWith("v")) {
							return FamilyType.QVIRTEX4;
						}
						break;
					case "5":
						if (tokens[2].startsWith("v")) {
							return FamilyType.QVIRTEX5;
						}
						break;
					case "6":
						if (tokens[2].startsWith("v")) {
							return FamilyType.QVIRTEX6;
						} else if (tokens[2].startsWith("s")) {
							if (tokens.length >= 5 && tokens[4].startsWith("l")) {
								return FamilyType.QSPARTAN6L;
							} else {
								return FamilyType.QSPARTAN6;
							}
						}
						break;
					case "7":
						if (tokens[2].startsWith("v")) {
							return FamilyType.QVIRTEX7;
						} else if (tokens[2].startsWith("a")) {
							return FamilyType.QARTIX7;
						} else if (tokens[2].startsWith("k")) {
							return FamilyType.QKINTEX7;
						}
						break;
				}
				break;
			case "xqv":
				if (tokens.length >= 3 && tokens[2].startsWith("e")) {
					return FamilyType.QVIRTEXE;
				} else {
					return FamilyType.QVIRTEX;
				}
			case "xqvr":
				return FamilyType.QRVIRTEX;
			case "xqr":
				switch (tokens[1]) {
					case "2":
						return FamilyType.QRVIRTEX2;
					case "4":
						return FamilyType.QRVIRTEX4;
					case "5":
						return FamilyType.QRVIRTEX5;
					case "6":
						return FamilyType.QRVIRTEX6;
					case "7":
						return FamilyType.QRVIRTEX7;
				}
				break;
		}
		return null;
	}
	
	/**
	 * This method will take a string of a family name and return a family type.
	 * @param name The given family name.
	 * @return The family type or null if the string name is an invalid family name.
	 */
	public static FamilyType getFamilyTypeFromFamilyName(String name) {
		try{
			return FamilyType.valueOf(name.toUpperCase().trim());
		}
		catch(IllegalArgumentException e) {
			return null;
		}
	}
	
	/**
	 * This method will take a familyType and return the base familyType 
	 * architecture.  For example, the XDLRC RapidSmith uses for Automotive 
	 * Spartan 6, Low Power Spartan 6 and Military Grade Spartan 6 all have
	 * the same base architecture: Spartan 6.  This method determines the
	 * base architecture based on the familyType.
	 * @param type The given family type.
	 * @return The base family type architecture.
	 */
	public static FamilyType getBaseTypeFromFamilyType(FamilyType type){
		switch(type){
			case AARTIX7: return FamilyType.ARTIX7;
			case ARTIX7: return FamilyType.ARTIX7;
			case ARTIX7L: return FamilyType.ARTIX7;
			case ASPARTAN2E: return FamilyType.SPARTAN2E;
			case ASPARTAN3: return FamilyType.SPARTAN3;
			case ASPARTAN3A: return FamilyType.SPARTAN3A;
			case ASPARTAN3ADSP: return FamilyType.SPARTAN3ADSP;
			case ASPARTAN3E: return FamilyType.SPARTAN3E;
			case ASPARTAN6: return FamilyType.SPARTAN6;
			case AZYNQ: return FamilyType.ZYNQ;
			case KINTEX7: return FamilyType.KINTEX7;
			case KINTEX7L: return FamilyType.KINTEX7;
			case QARTIX7: return FamilyType.ARTIX7;
			case QKINTEX7: return FamilyType.KINTEX7;
			case QKINTEX7L: return FamilyType.KINTEX7;
			case QRVIRTEX: return FamilyType.VIRTEX;
			case QRVIRTEX2: return FamilyType.VIRTEX2;
			case QRVIRTEX4: return FamilyType.VIRTEX4;
			case QRVIRTEX5: return FamilyType.VIRTEX5;
			case QRVIRTEX6: return FamilyType.VIRTEX6;
			case QRVIRTEX7: return FamilyType.VIRTEX7;
			case QSPARTAN6: return FamilyType.SPARTAN6;
			case QSPARTAN6L: return FamilyType.SPARTAN6;
			case QVIRTEX: return FamilyType.VIRTEX;
			case QVIRTEX2: return FamilyType.VIRTEX2;
			case QVIRTEX2P: return FamilyType.VIRTEX2P;
			case QVIRTEX4: return FamilyType.VIRTEX4;
			case QVIRTEX5: return FamilyType.VIRTEX5;
			case QVIRTEX6: return FamilyType.VIRTEX6;
			case QVIRTEX6L: return FamilyType.VIRTEX6;
			case QVIRTEX7: return FamilyType.VIRTEX7;
			case QVIRTEXE: return FamilyType.VIRTEXE;
			case QZYNQ: return FamilyType.ZYNQ;
			case SPARTAN2: return FamilyType.SPARTAN2;
			case SPARTAN2E: return FamilyType.SPARTAN2E;
			case SPARTAN3: return FamilyType.SPARTAN3;
			case SPARTAN3A: return FamilyType.SPARTAN3A;
			case SPARTAN3ADSP: return FamilyType.SPARTAN3ADSP;
			case SPARTAN3E: return FamilyType.SPARTAN3E;
			case SPARTAN6: return FamilyType.SPARTAN6;
			case SPARTAN6L: return FamilyType.SPARTAN6;
			case VIRTEX: return FamilyType.VIRTEX;
			case VIRTEX2: return FamilyType.VIRTEX2;
			case VIRTEX2P: return FamilyType.VIRTEX2P;
			case VIRTEX4: return FamilyType.VIRTEX4;
			case VIRTEX5: return FamilyType.VIRTEX5;
			case VIRTEX6: return FamilyType.VIRTEX6;
			case VIRTEX6L: return FamilyType.VIRTEX6;
			case VIRTEX7: return FamilyType.VIRTEX7;
			case VIRTEX7L: return FamilyType.VIRTEX7;
			case VIRTEXE: return FamilyType.VIRTEXE;
			case ZYNQ: return FamilyType.ZYNQ;
			default: return null;
		}
	}
		
	/**
	 * Gets and returns the all lower case exact Xilinx family name of the 
	 * part name (ex: qvirtex4 instead of virtex4). DO NOT use exact family 
	 * methods if it is to be used for accessing device or wire enumeration 
	 * files as RapidSmith does not generate files for devices that have 
	 * XDLRC compatible files.  
	 * @return The exact Xilinx family name of the part name.
	 */
	public static String getExactFamilyNameFromPart(String partName){
		return getExactFamilyTypeFromPart(partName).toString().toLowerCase();
	}
	
	/**
	 * Gets and returns the all lower case base family name of the part name.
	 * This ensures compatibility with all RapidSmith files. For 
	 * differentiating family names (qvirtex4 rather than virtex4) use 
	 * getExactFamilyName().
	 * @return The base family name of the given part name.
	 */
	public static String getFamilyNameFromPart(String partName){
		return getFamilyTypeFromPart(partName).toString().toLowerCase();
	}
	
	
	public static String getSubFamilyFromPart(String partName){
		partName = partName.toLowerCase();
		switch(getFamilyTypeFromPart(partName)){
			case SPARTAN3A:
				return partName.contains("an") ? "AN" : "A";
			case SPARTAN6:
				if(partName.contains("t") && !(partName.contains("tq") || partName.contains("tg"))){
					return "LXT";
				}else{
					return "LX";
				}
			case VIRTEX4: 
				if(partName.contains("lx")){
					return "LX";
				}else if(partName.contains("sx")){
					return "SX";
				}else if(partName.contains("fx")){
					return "FX";
				}
			case VIRTEX5: 
				if(partName.contains("lx")){
					if(partName.contains("t")){
						return "LXT";
					}else{
						return "LX";
					}
				}else if(partName.contains("sx")){
					return "SXT";
				}else if(partName.contains("fx")){
					return "FXT";
				}else if(partName.contains("tx")){
					return "TXT";
				}
			case VIRTEX6:
			case VIRTEX6L: 
				if(partName.contains("lx")){
					if(partName.contains("t")) return "LXT";
					else return "LX";					
				}else if(partName.contains("sx")){
					if(partName.contains("t")) return "SXT";
					else return "SX";	
				}else if(partName.contains("hx")){
					if(partName.contains("t")) return "HXT";
					else return "HX";	
				}else if(partName.contains("cx")){
					if(partName.contains("t")) return "CXT";
					else return "CX";	
				}
			case VIRTEX7:
				if(partName.contains("ht")){
					return "HT";
				}else if(partName.contains("xt")){
					return "XT";
				}else if(partName.contains("t")){
					return "T";
				}
			default: 
				return null;
		}
	}
	
	/**
	 * This method will return the formal family name as given by Xilinx 'partgen.'  
	 * @param type Type of family to get formal name.
	 * @return The formal family name or null if none exists.
	 */
	public static String getFormalFamilyNameFromType(FamilyType type){
		switch(type){
			case AARTIX7: return "Automotive Artix7";
			case ARTIX7: return "Artix7";
			case ARTIX7L: return "Artix7 Lower Power";
			case ASPARTAN2E: return "Automotive Spartan2E";
			case ASPARTAN3: return "Automotive Spartan3";
			case ASPARTAN3A: return "Automotive Spartan3A";
			case ASPARTAN3ADSP: return "Automotive Spartan-3A DSP";
			case ASPARTAN3E: return "Automotive Spartan3E";
			case ASPARTAN6: return "Automotive Spartan6";
			case AZYNQ: return "Automotive Zynq";
			case KINTEX7: return "Kintex7";
			case KINTEX7L: return "Kintex7 Lower Power";
			case QKINTEX7L: return "Defense-Grade Kintex-7Q Lower Power";
			case QSPARTAN6: return "Defense-Grade Spartan-6Q";
			case QSPARTAN6L: return "Defense-Grade Spartan-6Q Lower Power";
			case QVIRTEX4: return "Defense-Grade Virtex-4Q";
			case QVIRTEX5: return "Defense-Grade Virtex-5Q";
			case QVIRTEX6: return "Defense-Grade Virtex-6Q";
			case QVIRTEX6L: return "Defense-Grade Virtex-6Q Lower Power";
			case QVIRTEX: return "QPro Virtex Hi-Rel";
			case QRVIRTEX: return "QPro Virtex Rad-Hard";
			case QVIRTEX2: return "QPro Virtex2 Military";
			case QRVIRTEX2: return "QPro Virtex2 Rad Tolerant";
			case QVIRTEX2P: return "QPro Virtex2P Hi-Rel";
			case QVIRTEXE: return "QPro VirtexE Military";
			case QRVIRTEX4: return "Space-Grade Virtex-4QV";
			case QZYNQ: return "Defense-Grade Zynq";
			case SPARTAN2: return "Spartan2";
			case SPARTAN2E: return "Spartan2E";
			case SPARTAN3: return "Spartan3";
			case SPARTAN3A: return "Spartan3A and Spartan3AN";
			case SPARTAN3ADSP: return "Spartan-3A DSP";
			case SPARTAN3E: return "Spartan3E";
			case SPARTAN6: return "Spartan6";
			case SPARTAN6L: return "Spartan6 Lower Power";
			case VIRTEX: return "Virtex";
			case VIRTEX2: return "Virtex2";
			case VIRTEX2P: return "Virtex2P";
			case VIRTEX4: return "Virtex4";
			case VIRTEX5: return "Virtex5";
			case VIRTEX6: return "Virtex6";
			case VIRTEX6L: return "Virtex6 Lower Power";
			case VIRTEX7: return "Virtex7";
			case VIRTEX7L: return "Virtex7 Lower Power";
			case VIRTEXE: return "VirtexE";
			case ZYNQ: return "Zynq";
			default: return null;
		}
	}
	
	
	public static void main(String[] args){
		// Run some tests to make sure we are doing things right
		for(FamilyType type : FamilyType.values()){
			System.out.println("Current Type: " + type);
			ArrayList<String> names = RunXilinxTools.getPartNames(type.toString().toLowerCase(), false);
			if(names == null) continue;
			for(String name : names){
				System.out.println("    " + name + " " + getExactFamilyTypeFromPart(name));
			}
		}
		
		String[] testPartNames = {"xs6slx4ltqg144", "xs6slx4tqg144", 
				"xc7v285tffg484", "xc7vh290tffg1155", "xc7k30tsbg324", "xc7a20cpg236", "xc7a175tcsg324",
				"xq7v285tffg484", "xq7vh290tffg1155", "xq7k30tsbg324", "xq7a20cpg236", "xq7a175tcsg324",
				"xqr7v285tffg484", "xqr7vh290tffg1155"};
		for(String name : testPartNames){
			System.out.println("    " + name + " " + getExactFamilyTypeFromPart(name));
		}
	}
}
