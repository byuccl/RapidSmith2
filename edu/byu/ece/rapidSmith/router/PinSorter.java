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
package edu.byu.ece.rapidSmith.router;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;

import edu.byu.ece.rapidSmith.design.NetType;
import edu.byu.ece.rapidSmith.design.Pin;
import edu.byu.ece.rapidSmith.device.Device;
import edu.byu.ece.rapidSmith.device.WireEnumerator;
import edu.byu.ece.rapidSmith.util.FamilyType;
import edu.byu.ece.rapidSmith.util.MessageGenerator;


/**
 * This class is used by the StaticSourceHandler to sort pins based on
 * how they should be allocated for TIEOFFs.
 * @author Chris Lavin
 * Created on: Jul 13, 2010
 */
public abstract class PinSorter{
	/** These are the pins that are the most needy of the TIEOFF source pins */
	LinkedList<StaticSink> useTIEOFF;
	/** These are pins that could be source by the TIEOFF, but not as necessary */
	ArrayList<StaticSink> attemptTIEOFF;
	/** These are pins that will be sourced by a slice */
	ArrayList<StaticSink> useSLICE;
	/** This is a list of pin names that require a HARD1 if being driven by a TIEOFF */
	static HashSet<String> needHARD1;
	/** This is a list of pins that require a SLICE to source them */
	static HashSet<String> needSLICESource;
	/** Reference to the wire enumerator class */
	static WireEnumerator we;
	/**
	 * Initializes data structures
	 */
	public PinSorter(){
		useTIEOFF = new LinkedList<StaticSink>();
		attemptTIEOFF = new ArrayList<StaticSink>();
		useSLICE = new ArrayList<StaticSink>();
	}
	
	/**
	 * Creates the proper PinSorter subclass based on the family type provided
	 * @param dev The device to create a pin sorter for
	 * @return A new PinSorter subclass, or null if unsupported.
	 */
	public static PinSorter createPinSorter(Device dev){
		FamilyType type = dev.getFamilyType();
		switch(type){
			case VIRTEX4:
				return new V4PinSorter(dev);
			case VIRTEX5:
				return new V5PinSorter(dev);
			default:
				MessageGenerator.briefError("Sorry, " +	type.name() + 
					" is unsupported by the PinSorter class.");
				return null;
		}
	}
	
	/**
	 * This methods sorts the pins as they are added.
	 * @param switchMatrixSink The switch matrix sink node.
	 * @param pin The current sink pin being sorted.
	 */
	public void addPin(Node switchMatrixSink, Pin pin){
		StaticSink ss = new StaticSink(switchMatrixSink, pin);
		String wireName = we.getWireName(switchMatrixSink.wire);
		
		if(needHARD1.contains(wireName)){
			useTIEOFF.addFirst(ss);
		}
		else if(needSLICESource.contains(wireName)){
			useSLICE.add(ss);
		}
		else if(pin.getNet().getType().equals(NetType.GND)){
			useTIEOFF.addLast(ss);
		}
		else {
			attemptTIEOFF.add(ss);
		}
	}
	
	public void addPinToSliceList(Node node, Pin pin){
		StaticSink ss = new StaticSink(node, pin);
		useSLICE.add(ss);
	}
	
	/**
	 * This is just a small class to help the PinSorter class keep track of things.
	 * @author Chris Lavin
	 */
	public class StaticSink{
		public Node switchMatrixSink;
		public Pin pin;
		public StaticSink(Node switchMatrixSink, Pin pin){
			this.switchMatrixSink = switchMatrixSink;
			this.pin = pin;
		}
		public String toString(){
			return "["+switchMatrixSink.toString() + ", pin=" + pin.getName() + ", instance=" + pin.getInstanceName() +"]";
		}
	}
}
