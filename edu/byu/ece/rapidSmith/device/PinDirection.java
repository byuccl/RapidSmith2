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
	
	public static PinDirection reverse(PinDirection dir) {
		
		switch (dir) {
			case IN: return PinDirection.OUT;
			case OUT: return PinDirection.IN;
			case INOUT: return PinDirection.INOUT;
			default : throw new AssertionError("Invalid Pin Direction");
		}
	}
	
	public static boolean isInput(PinDirection dir) {
		return dir == PinDirection.IN;
	}
	
	public static boolean isOutput(PinDirection dir) {
		return dir == PinDirection.OUT;
	}
	
	public static boolean isInout(PinDirection dir) {
		return dir == PinDirection.INOUT;
	}
}
