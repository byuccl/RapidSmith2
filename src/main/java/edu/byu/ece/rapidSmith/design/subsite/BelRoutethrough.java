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

package edu.byu.ece.rapidSmith.design.subsite;

import edu.byu.ece.rapidSmith.device.Bel;
import edu.byu.ece.rapidSmith.device.BelPin;
import edu.byu.ece.rapidSmith.device.Wire;
import static edu.byu.ece.rapidSmith.util.Exceptions.DesignAssemblyException;

import java.util.Objects;

/**
 * Represents a LUT Routethrough. A LUT is used  
 *  
 * TODO: Update this to include the sink cell pin that it leads to? 
 *  
 * @author Thomas Townsend
 */
public class BelRoutethrough {
	
	/**Input pin of the BelRoutethrough (A1, A2, ... , A6) */
	private final BelPin inputPin;
	/**Input pin of the BelRoutethrough (O5, O6) */
	private final BelPin outputPin;
	
	/**
	 * Create a new BelRoutethrough object. 
	 * 
	 * @param inputPin Input {@link BelPin} object (A1 - A6 pins on a LUT typically)
	 * @param outputPin Output {@link BelPin} object (O5 or O6 pin on a LUT typically)
	 */
	public BelRoutethrough(BelPin inputPin, BelPin outputPin) {
		
		// Reject null objects
		Objects.requireNonNull(inputPin);
		Objects.requireNonNull(outputPin);
		
		// Can we assume that getBel never returns null?
		if (inputPin.getBel().equals(outputPin.getBel())) {
			throw new DesignAssemblyException("BelPins are not on the same Bel object!");
		}
		
		this.inputPin = inputPin;
		this.outputPin = outputPin;
	}
	
	/**
	 * Returns the {@link Bel} object of this routethrough 
	 */
	public Bel getBel() {
		return this.inputPin.getBel();
	}
	
	/**
	 * Returns the input {@link BelPin} of this routethrough
	 */
	public BelPin getInputPin() {
		return this.inputPin;
	}
	
	/**
	 * Returns the output {@link BelPin} of this routethrough
	 */
	public BelPin getOutputPin() {
		return this.outputPin;
	}
	
	/**
	 * Returns the {@link Wire} object connected to the output {@link BelPin} of this routethrough
	 */
	public Wire getOutputWire() {
		return outputPin.getWire();
	}
}