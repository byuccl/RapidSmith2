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
package edu.byu.ece.rapidSmith.device;

import java.io.Serializable;
import java.util.Objects;

/**
 * This class is used to represent the Route-Through constructs of XDLRC/XDL
 * in association with PIPs.  Certain PIPs are defined as special configurations
 * of logic blocks which allow routing to pass through them.  This class defines
 * these kinds of routing constructs.
 * @author Chris Lavin
 */
public class PIPRouteThrough implements Serializable{
	/** The type of primitive where a route through exists */
	private SiteType type;
	/** The input pin of the route through */
	private String inPin;
	/** The output pin of the route through */
	private String outPin;
	
	/**
	 * Constructor which creates a new PIPRouteThrough.
	 * @param type The type of primitive involved in the route through.
	 * @param inPin The input wire.
	 * @param outPin The output wire.
	 */
	public PIPRouteThrough(SiteType type, String inPin, String outPin){
		this.type = type;
		this.inPin = inPin;
		this.outPin = outPin;
	}

	@Override
	public int hashCode(){
		final int prime = 31;
		int result = 1;
		result = prime * result + inPin.hashCode();
		result = prime * result + outPin.hashCode();
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		PIPRouteThrough other = (PIPRouteThrough) obj;
		return Objects.equals(inPin, other.inPin) &&
				Objects.equals(outPin, other.outPin) &&
				Objects.equals(type, other.type);
	}

	/**
	 * Returns the primitive type where this route through is found.
	 * @return the primitive type of this route through.
	 */
	public SiteType getType(){
		return type;
	}

	/**
	 * Sets the type of primitive for this route through.
	 * @param type the type to set.
	 */
	public void setType(SiteType type){
		this.type = type;
	}

	/**
	 * Returns the input wire for this route through.
	 * @return the input wire for this route through
	 */
	public String getInPin(){
		return inPin;
	}

	/**
	 * Sets the input wire for this route through.
	 * @param inPin the inPin to set.
	 */
	public void setInPin(String inPin){
		this.inPin = inPin;
	}

	/**
	 * Returns the output wire for this route through.
	 * @return the output wire for this route through.
	 */
	public String getOutPin(){
		return outPin;
	}

	/**
	 * Sets the output wire for this route through.
	 * @param outPin the outPin to set
	 */
	public void setOutPin(String outPin){
		this.outPin = outPin;
	}
	
	/**
	 * Creates an XDLRC compatible string of this route through. 
	 * @return The XDLRC compatible string of this route through.
	 */
	public String toString(){
		return this.type.toString() + "-" + inPin + "-" + outPin;
 	}
}
