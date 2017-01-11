package edu.byu.ece.rapidSmith.device.vsrt.gui.shapes;

/**
 * This enumerated type is used to represent the different types of bels <br>
 * that can be found in a primitive site.  Currently, this is not used, but it <br>
 * it may be used in the future 
 * @author Thomas Townsend
 * Created on: June 3, 2014
 */
public enum BelType {

	VCC,
	GND,
	MUX,
	XOR,
	INVERTER,
	BUFFER,
	REGULAR;
}