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
 * 
 */
package edu.byu.ece.rapidSmith.constraints;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

import edu.byu.ece.rapidSmith.device.SiteType;
import edu.byu.ece.rapidSmith.util.MessageGenerator;

/**
 * This is an elementary UCF parser which will create an array of Constraint
 * objects from a given UCF file.
 * Created on: May 5, 2011
 */
public class UCFParser {
	/** Character input stream from the UCF file */
	private BufferedInputStream reader;
	/** Current line number */
	private int line;
	
	/**
	 * Helper method to create a constraint object and have the constraint
	 * parsed. 
	 * @param constraint The string containing the constraint.
	 * @return The newly created constraint object.
	 */
	public Constraint createConsraint(String constraint){
		Constraint c = new Constraint();
		if(!c.setConstraintString(constraint)){
			MessageGenerator.briefErrorAndExit("Error: Failed parsing constraint: <" + constraint + ">, on line: " + line);
		}
		return c;
	}
	
	/**
	 * This is the main method to parse a UCF file
	 * @param fileName Name of the UCF file to parse
	 * @return A list of Constraint objects representing the constraints 
	 */
	public ArrayList<Constraint> parseUCF(String fileName){
		ArrayList<Constraint> constraints = new ArrayList<Constraint>();
		line = 0;
		try {
			reader = new BufferedInputStream(new FileInputStream(fileName));
			
			int ch = -1;
			int idx = 0;
			char[] buffer = new char[8192];
			boolean inComment = false;
			boolean usePreviousLine = false;
			String tempString = "";
			while(true){
				if(!usePreviousLine)
					ch = reader.read();
				if(ch == -1)
					break;
				if(ch == '#'){
					inComment = true;
				}
				else if((ch == ';' && !inComment && idx > 0) || usePreviousLine){
					String c;
					if(usePreviousLine) {
						c = tempString;
						usePreviousLine = false;
					}
					else
						c = new String(buffer, 0, idx);
					if(!c.trim().equals("")) {
//						System.out.println("ch: " + (char)ch + "\tstring: " + c);
						Constraint new_constraint = createConsraint(c);
						constraints.add(new_constraint);
						
						//check if this constraint is an area_group constraint, with an INST statement
						if(new_constraint.getConstraintType() == ConstraintType.AREA_GROUP && new_constraint.getStatementType() == StatementType.INST) {
//							System.out.println("Just read in an INST - AREA_GROUP declaration: \"" + c + "\"" + "\tidx: " + idx);
							
							boolean readingAreaGroupRanges = true;
							while(readingAreaGroupRanges ) {
								idx = 0;
								//weed out the \n and \r before they mess things up royally
								while(ch != -1 && (ch == '\n' || ch == '\r' || ch == ';'))
									ch = reader.read();
								if(ch == '#')
									inComment = true;
								//read in the next line, we are assuming it is an "area_group range" statement
								while(ch != -1 && !inComment){
									//Add to buffer
									buffer[idx++] = (char) ch;
									ch = reader.read();
									if(ch == '\n'){
										inComment = false;
										line++;
										break;
									}
								}
								
								c = new String(buffer, 0, idx);
								if(!c.trim().equals("")) {
//									System.out.println("Reading something funky:\"" + c + "\"\tp_idx: " + idx + "\tch:'" + ch + "'");
									if(!c.startsWith("AREA_GROUP")) {
										readingAreaGroupRanges = false;
										usePreviousLine = true;
										tempString = new String(buffer, 0, idx);
//										System.out.println("String to be passed on: \"" + tempString + "\"");
									}
									else {
//										System.out.println("Parsing ag range");
										parseAreaGroupRange(new_constraint, c);
									}
								}
								else {
//									System.out.println("String c has nothing in it.");
									readingAreaGroupRanges = false;
									usePreviousLine = true;
									tempString = new String(buffer, 0, idx);
//									System.out.println("String to be passed on: \"" + tempString + "\"");
								}
							}
						}
					}
					idx = 0;
				}
				else if(!inComment && ch != '\r' && ch != '\n'){
					// Add to buffer
					buffer[idx++] = (char) ch;
				}
				
				if(ch == '\n'){
					inComment = false;
					line++;
				}

			}
		} catch(IOException e){
			e.printStackTrace();
		}
			
		return constraints;
	}
	
	//assuming that the passed String c is an "AREA_GROUP name RANGE=range" statement
	public void parseAreaGroupRange(Constraint new_constraint, String c) {
//		System.out.println("parseAreaGroupRange(): c:" + c);
		String UPPERCASE = c.toUpperCase();
		String range_string = UPPERCASE.substring(UPPERCASE.indexOf("RANGE=") + 6, UPPERCASE.indexOf(";"));
//		System.out.println("parseAreaGroupRange(): range_string:" + range_string);
		UPPERCASE = UPPERCASE.substring(0, UPPERCASE.indexOf(" RANGE="));
		String group_name = c.substring(UPPERCASE.indexOf("AREA_GROUP ") + 12, UPPERCASE.length() - 1);
//		System.out.println("parseAreaGroupRange(): group_name:" + group_name);
		
		String range_type_string;
		SiteType range_type;
		int ll_x = Integer.parseInt(range_string.substring(range_string.indexOf("X") + 1, range_string.indexOf("Y")));
		int ll_y = Integer.parseInt(range_string.substring(range_string.indexOf("Y") + 1, range_string.indexOf(":")));
		int ur_x = Integer.parseInt(range_string.substring(range_string.lastIndexOf("X") + 1, range_string.lastIndexOf("Y")));
		int ur_y = Integer.parseInt(range_string.substring(range_string.lastIndexOf("Y") + 1, range_string.length()));
		
//		System.out.println("ll coordinate: (" + ll_x + ", " + ll_y + ")");
//		System.out.println("ur coordinate: (" + ur_x + ", " + ur_y + ")");
		
		range_type_string = range_string.substring(0, range_string.indexOf("_X"));
//		System.out.println("parseAreaGroupRange(): range_type:" + range_type_string);
		
		//TODO: Will have to expand this at some point in the future
		if(range_type_string.equals("RAMB18"))
			range_type = SiteType.RAMB18E1;
		else if(range_type_string.equals("RAMB36"))
			range_type = SiteType.RAMB36E1;
		else
			range_type = SiteType.valueOf(range_type_string);
		new_constraint.addAreaGroupRange(new AreaGroupRange(group_name, range_type, ll_x, ll_y, ur_x, ur_y));
	}
		
	public static void main(String[] args) {
		UCFParser p = new UCFParser();
		ArrayList<Constraint> constraints = p.parseUCF(args[0]);
		
		for(Constraint c : constraints){
			System.out.println(c.toString());
		}
	}
}
