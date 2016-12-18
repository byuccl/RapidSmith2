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
package edu.byu.ece.rapidSmith.primitiveDefs;

import java.io.Serializable;

public class PrimitiveConnection implements Serializable {
	private static final long serialVersionUID = 6690045406914379740L;
	private String element0;
	private String pin0;
	private boolean forwardConnection;
	private String element1;
	private String pin1;
	
	public String getElement0() {
		return element0;
	}

	public void setElement0(String element0) {
		this.element0 = element0;
	}

	public String getPin0() {
		return pin0;
	}

	public void setPin0(String pin0) {
		this.pin0 = pin0;
	}

	public String getElement1() {
		return element1;
	}

	public void setElement1(String element1) {
		this.element1 = element1;
	}

	public String getPin1() {
		return pin1;
	}

	public void setPin1(String pin1) {
		this.pin1 = pin1;
	}

	public PrimitiveConnection(){
		element0 = null;
		pin0 = null;
		forwardConnection = false;
		element1 = null;
		pin1 = null;
	}
	
	public boolean isForwardConnection() {
		return forwardConnection;
	}
	public void setForwardConnection(boolean forwardConnection) {
		this.forwardConnection = forwardConnection;
	}

	@Override
	public String toString(){
		return "(conn " + element0 + " " + pin0 +
		(forwardConnection ? " ==> " : " <== ") + element1 + " " + 
		pin1 + ")";
	}
}
