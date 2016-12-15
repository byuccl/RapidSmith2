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
package edu.byu.ece.rapidSmith.constraints;

import java.util.ArrayList;

import static edu.byu.ece.rapidSmith.util.Exceptions.ParseException;

/**
 * This class is to represent a constraint within a UCF file.
 * Created on: May 5, 2011
 */
public class Constraint{
	/** Statement type (starting keyword of the constraint) */
	private StatementType statementType;
	/** Name of the resource */
	private String name;
	/** Type of constraint */
	private ConstraintType constraintType;
	/** Values of the constraint */
	private ArrayList<String> values;
	/** Keeps the original string of the constraint */
	private String constraint;
	
	/* Area group ranges are only set for instances with area groups */
	/* NOTE!  WE ASSUME THAT THE AREA_GROUP IS DECLARED: 	e.g. "INST "inst_name" AREA_GROUP = "area_group_name";"
	 * BEFORE AREA_GROUP CONSTRAINTS ARE DECLARED: 			e.g. "AREA_GROUP "area_group_name" RANGE=SLICE_X0Y0:SLICE_X43Y59;"
	 */
	private ArrayList<AreaGroupRange> areaGroupRanges;
	
	/**
	 * Empty constructor
	 */
	public Constraint(){
	}
	
	/**
	 * Creates a new constraint object and populates its members based on the
	 * string provided.
	 * @param constraint The constraint text as would be found in a UCF file.
	 */
	public Constraint(String constraint){
		setConstraintString(constraint);
	}

	/**
	 * This will parse the current constraint string into the various members of
	 * the constraint object.
	 */
	private void parseConstraint(){
		ArrayList<String> tokens = getConstraintTokens();
		values = new ArrayList<>();
		// Populate StatementType
		try{
			statementType = StatementType.valueOf(tokens.get(0).toUpperCase());
		}
		catch(IllegalArgumentException e){
			throw new ParseException("<StatementType>", tokens.get(0));
		}
		String token;
		switch(statementType){
			case INST:
			case NET:
			case PIN:
				// Populate Name
				name = tokens.get(1);
				// Populate the ConstraintType
				token = tokens.get(2).toUpperCase();
				if(token.equals("OFFSET")){
					token = token + "_" + tokens.get(4).toUpperCase();
				}
				try{
					constraintType = ConstraintType.valueOf(token);
				}
				catch(IllegalArgumentException e){
					throw new ParseException("<ConstraintType>", token);
				}
				// Check for the equals sign
				if(tokens.size() >= 4){
					token = tokens.get(3).toUpperCase();
					if(!token.equals("="))
						throw new ParseException("'='", tokens.get(3));

					for(int i = 4; i < tokens.size(); i++){
						values.add(tokens.get(i));
					}
				}
				break;
			case TIMEGRP:
				// Populate Name
				name = tokens.get(1);
				// TODO
				break;
			case TIMESPEC:
				// Populate Name
				name = tokens.get(1);
				// Check that name starts with TS
				if(!name.toUpperCase().startsWith("TS"))
					throw new ParseException("'TS'", tokens.get(1));

				// Check for the equals sign
				token = tokens.get(2).toUpperCase();
				if(!token.equals("="))
					throw new ParseException("'='", tokens.get(2));

				// Populate ConstraintType
				token = tokens.get(3).toUpperCase();
				if(token.equals("FROM")){
					if(constraint.toUpperCase().contains("THRU"))
						token = token + "_THRU_TO";
					else
						token = token + "_TO";
				}
				try{
					constraintType = ConstraintType.valueOf(token);
				}
				catch(IllegalArgumentException e){
					throw new ParseException("<ConstraintType>", token);
				}

				for(int i = 4; i < tokens.size(); i++){
					if(!tokens.get(i).equals("FROM") && !tokens.get(i).equals("THRU") && !tokens.get(i).equals("TO")){
						values.add(tokens.get(i));
					}
				}

				break;
			case AREA_GROUP:
				name = tokens.get(2);

				try{
					constraintType = ConstraintType.valueOf(tokens.get(0));
				}
				catch(IllegalArgumentException e){
					throw new ParseException("<ConstraintType>", tokens.get(2));
				}
				break;
		}
	}

	/**
	 * This will separate a constraint string into is various parts for easier
	 * parsing. 
	 * @return A list of tokens that were found in the constraint string.
	 */
	private ArrayList<String> getConstraintTokens(){
		ArrayList<String> matchList = new ArrayList<>();
		int i = 0;
		char[] buffer = constraint.toCharArray();
		char[] token = new char[constraint.length()];
		boolean inQuotedString = false;
		for(int j = 0; j < buffer.length; j++){
			switch(buffer[j]){
				case ' ':
				case '\t':
				case '\r':
				case '\n':
					if(inQuotedString){
						token[i++] = buffer[j];
					}
					else if(i > 0){
						matchList.add(new String(token, 0, i));
						i=0;
					}
					break;
				case '\"':
					if(inQuotedString && i > 0){
						matchList.add(new String(token, 0, i));
						i=0;
					}
					inQuotedString = !inQuotedString;
					break;
				case '=':
					if(i > 0){
						matchList.add(new String(token, 0, i));
						i=0;
					}
					matchList.add("=");
					break;
				default:
					token[i++] = buffer[j];
			}
		}
		if(i > 0){
			matchList.add(new String(token, 0, i));
		}
		return matchList;
	}

	/**
	 * @return the statementType
	 */
	public StatementType getStatementType() {
		return statementType;
	}
	/**
	 * @param statementType the statementType to set
	 */
	public void setStatementType(StatementType statementType) {
		this.statementType = statementType;
	}
	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}
	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}
	/**
	 * @return the constraintType
	 */
	public ConstraintType getConstraintType() {
		return constraintType;
	}
	/**7
	 * @param constraintType the constraintType to set
	 */
	public void setConstraintType(ConstraintType constraintType) {
		this.constraintType = constraintType;
	}
	/**
	 * @return the constraintString
	 */
	public String getConstraintString() {
		return constraint;
	}
	/**
	 * @param constraintString the constraintString to set
	 */
	public void setConstraintString(String constraintString){
		this.constraint = constraintString;
		parseConstraint();
	}

	/**
	 * @return the values
	 */
	public ArrayList<String> getValues() {
		return values;
	}

	/**
	 * @param values the values to set
	 */
	public void setValues(ArrayList<String> values) {
		this.values = values;
	}
	
	public void addAreaGroupRange(AreaGroupRange agr) {
		if(areaGroupRanges == null)
			areaGroupRanges = new ArrayList<>();
		areaGroupRanges.add(agr);
	}
	
	public ArrayList<AreaGroupRange> getAreaGroupRanges() {
		return areaGroupRanges;
	}

	@Override
	public String toString(){
		return statementType + " \"" + name + "\" " + constraintType + " " + values.toString() + " <" + constraint + ">"; 
	}
}
