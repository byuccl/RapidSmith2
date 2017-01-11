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
package edu.byu.ece.rapidSmith.device.vsrt.primitiveDefs;

import java.io.Serializable;

public class PrimitiveDefPin implements Serializable {

	private static final long serialVersionUID = -5733624692261327342L;

	private String externalName;
	private String internalName;
	//private boolean isOutput;
	private PrimitiveDefPinDirection dir;
	private boolean isConnected;
	
	public PrimitiveDefPin(){
		externalName = null;
		internalName = null;
		//isOutput = false;
		dir = null;
		isConnected = true;
	}
	
	public String getExternalName() {
		return externalName;
	}
	public void setExternalName(String externalName) {
		this.externalName = externalName;
	}
	public String getInternalName() {
		return internalName;
	}

	public void setInternalName(String internalName) {
		this.internalName = internalName;
	}
	public PrimitiveDefPinDirection getDirection(){
		return this.dir;
	}
	public void setDirection( PrimitiveDefPinDirection dir){
		this.dir = dir; 
	}

	public void setConnected(boolean connected){
		this.isConnected = connected;
	}
	public boolean isConnected(){
		return this.isConnected;
	}

//	@Override
//	public String toString(){
//		if(externalName == null){
//			return "(pin " + internalName + (dir == PrimitiveDefPinDirection.OUTPUT ? " output)" : " input)");
//		}
//		return "(pin " + externalName + " " + internalName + (dir == PrimitiveDefPinDirection.OUTPUT ? " output)" : " input)");
//	}
	
	public String toString(boolean printInout) {
		if(externalName == null){
			if (printInout){
				return "(pin " + internalName + (dir == PrimitiveDefPinDirection.OUTPUT ? " output)" : (dir == PrimitiveDefPinDirection.INPUT ?" input)" : " inout)"));
				
			}else {
				return "(pin " + internalName + (dir == PrimitiveDefPinDirection.OUTPUT ? " output)" : " input)");
			}
		}
		
		if (printInout){
			return "(pin " + externalName + " " + internalName + (dir == PrimitiveDefPinDirection.OUTPUT ? " output)" : (dir == PrimitiveDefPinDirection.INPUT ?" input)" : " inout)"));
		}else {
			return "(pin " + externalName + " " + internalName + (dir == PrimitiveDefPinDirection.OUTPUT ? " output)" : " input)");
		}
	}

	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		PrimitiveDefPin other = (PrimitiveDefPin) obj;
		if (dir != other.dir)
			return false;
		if (externalName == null) {
			if (other.externalName != null)
				return false;
		} else if (!externalName.equals(other.externalName))
			return false;
		if (internalName == null) {
			if (other.internalName != null)
				return false;
		} else if (!internalName.equals(other.internalName))
			return false;
		if (isConnected != other.isConnected)
			return false;
		return true;
	}
}
