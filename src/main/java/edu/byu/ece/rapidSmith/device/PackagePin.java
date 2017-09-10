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
package edu.byu.ece.rapidSmith.device;

import java.io.Serializable;

/**
 * This class represents a Vivado PackagePin that can be used
 * for signal input output on the FPGA. They are typically named
 * A15 or something similar. 
 */
public class PackagePin implements Serializable {
	
	/** For serialization */
	private static final long serialVersionUID = 8489335358800911167L;
	private final String name;
	private final String siteName;
	private final String belName;
	private final boolean isClock;
	
	/**
	 * Creates a new PackagePin object
	 * 
	 * @param name Name of the pin (i.e. M17)
	 * @param site Name of the site of the package pin
	 * @param bel Name of the bel of the package pin 
	 */
	public PackagePin(String name, String site, String bel, boolean isClock) {
		this.name = name;
		this.siteName = site;
		this.belName = bel;
		this.isClock = isClock;
	}
	
	/**
	 * Creates a new PackagePin object
	 * 
	 * @param name Name of the pin (i.e. M17)
	 * @param fullBelName Name of a pad bel in the form "sitename/belname". 
	 * 	An example is "IOB_X0Y133/PAD". 
	 */
	public PackagePin(String name, String fullBelName, boolean isClock) {
		this.name = name; 
		String[] toks = fullBelName.split("/");
		assert toks.length==2;
		this.siteName = toks[0];
		this.belName = toks[1];
		this.isClock = isClock;
	}
	
	/**
	 * Returns the name of the package pin 
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * Returns the site name of the package pin
	 */
	public String getSite() {
		return siteName;
	}

	/**
	 * Returns the bel name of the package pin
	 */
	public String getBel() {
		return belName;
	}
	
	/**
	 * Returns {@code true} if the package pin is also a clock pad, {@code false} otherwise.
	 */
	public boolean isClockPad() {
		return this.isClock;
	}
}
