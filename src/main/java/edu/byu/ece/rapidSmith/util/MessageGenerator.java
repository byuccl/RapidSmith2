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

import java.io.IOException;

public class MessageGenerator{
	/**
	 * Prompts the user to press any key to continue execution.
	 */
	public static void waitOnAnyKey(){
		System.out.print("Press Enter to continue...");
		waitOnAnyKeySilent();
	}
	
	/**
	 * Pauses execution until a key is pressed.
	 */
	public static void waitOnAnyKeySilent(){
		try{
			System.in.read();
		}
		catch(IOException e){
			e.printStackTrace();
		}
	}
	
	/**
	 * Creates a simple string from an array of objects.  This method
	 * calls the toString() on each object in the array.
	 * @param objs The object to create a string from. 
	 * @return A string of all the objects toStrings() concatenated 
	 * with spaces in between.
	 */
	public static String createStringFromArray(Object[] objs){
		StringBuilder sb = new StringBuilder();
		for(Object o : objs){
			sb.append(o.toString() + " ");
		}
		return sb.toString();
	}

	/**
	 * Prints a generic header to standard out to separate operations.
	 * @param s
	 */
	public static void printHeader(String s){
		String bar = "==============================================================================";
		String left;
		String right;
		double whiteSpace = (72 - s.length())/2.0;
		left = MessageGenerator.makeWhiteSpace((int)(whiteSpace));
		right = MessageGenerator.makeWhiteSpace((int)(whiteSpace+0.5));
		System.out.println(bar);
		System.out.println("== "+ left + s + right +" ==");
		System.out.println(bar);
	}

	/**
	 * Creates a whitespace string with length number of spaces.
	 * @param length Number of spaces in the string.
	 * @return The newly created whitespace string.
	 */
	public static String makeWhiteSpace(int length){
		if (length < 1)
			return "";
		StringBuilder sb = new StringBuilder(length);
		for(int i=0; i<length; i++){
			sb.append(" ");
		}
		return sb.toString();
	}
	
	/**
	 * This will prompt the user to type y or n to either continue
	 * with a process or to exit.
	 */
	public static void promptToContinue() throws IOException {
		System.out.print("Would you like to continue(y/n)? ");
		int ch = System.in.read();
		while(ch != 'y' && ch != 'n' && ch != 'Y' && ch != 'N'){
			while(System.in.read() != '\n');
			System.out.print("Would you like to continue(y/n)? ");
			ch = System.in.read();
		}
		if (ch != 'y' && ch != 'Y') {
			System.exit(1);
		}
	}
	
	/**
	 * This will prompt the user to type y or n to either continue
	 * with a process or to exit.
	 */
	public static void agreeToContinue() throws IOException {
		System.out.print("(y/n)? ");
		int ch = System.in.read();
		while(ch != 'y' && ch != 'n' && ch != 'Y' && ch != 'N'){
			while(System.in.read() != '\n');
			System.out.print("Would you like to continue(y/n)? ");
			ch = System.in.read();
		}
		if (ch != 'y' && ch != 'Y') {
			System.exit(1);
		}
	}
}
