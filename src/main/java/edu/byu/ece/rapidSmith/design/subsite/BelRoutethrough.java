package edu.byu.ece.rapidSmith.design.subsite;

import edu.byu.ece.rapidSmith.device.Bel;
import edu.byu.ece.rapidSmith.device.BelPin;
import edu.byu.ece.rapidSmith.device.Wire;

/**
 * Represents a BelRoutethrough
 *  
 * TODO: Update this to include the sink cell pin that it leads to? 
 *  
 * @author Thomas Townsend
 */
public class BelRoutethrough {

	private final Bel bel;
	private final BelPin inputPin;
	private final BelPin outputPin;
	//private final CellPin sinkCellPin;
	
	public BelRoutethrough(Bel bel, BelPin inputPin, BelPin outputPin) {
		this.bel = bel;
		this.inputPin = inputPin;
		this.outputPin = outputPin;
	}
	
	public Bel getBel() {
		return this.bel;
	}
	
	public BelPin getInputPin() {
		return this.inputPin;
	}
	
	public BelPin getOutputPin() {
		return this.outputPin;
	}
	public Wire getOutputWire() {
		return outputPin.getWire();
	}
}
