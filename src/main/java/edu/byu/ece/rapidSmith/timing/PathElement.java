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
package edu.byu.ece.rapidSmith.timing;

import edu.byu.ece.rapidSmith.design.xdl.XdlPin;

public abstract class PathElement {
	/** Delay of the path element */
	private float delay;
	/** Type of the delay element */
	private String type;
	/** Primitive Site and Pin where the path elements resides */
	private XdlPin pin;
	
	/**
	 * @return the delay
	 */
	public float getDelay() {
		return delay;
	}
	/**
	 * @param delay the delay to set
	 */
	public void setDelay(float delay) {
		this.delay = delay;
	}
	/**
	 * @return the type
	 */
	public String getType() {
		return type;
	}
	/**
	 * @param type the type to set
	 */
	public void setType(String type) {
		this.type = type;
	}
	/**
	 * @return the pin
	 */
	public XdlPin getPin() {
		return pin;
	}
	/**
	 * @param pin the pin to set
	 */
	public void setPin(XdlPin pin) {
		this.pin = pin;
	}
	
	
}
