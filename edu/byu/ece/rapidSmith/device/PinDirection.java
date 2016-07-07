package edu.byu.ece.rapidSmith.device;

/**
 *  Enumeration of the possible directions for a pin.
 */
public enum PinDirection {
	IN, OUT, INOUT;

	public static PinDirection convert(boolean isOutput) {
		if (isOutput)
			return OUT;
		else
			return IN;
	}
}
