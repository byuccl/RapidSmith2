package edu.byu.ece.rapidSmith.device;

import edu.byu.ece.rapidSmith.design.subsite.Cell;

/**
 * Enumeration of the possible directions for a top-level port.
 * @author Thomas Townsend
 *
 */
public enum PortDirection {
	IN, OUT, INOUT;
	
	public static PortDirection getPortDirectionForImport(Cell portCell) {
		checkIsValidPort(portCell);
		
		PinDirection dir = portCell.getPin("PAD").getDirection();
	
		switch (dir) {
			case IN: return PortDirection.OUT;
			case OUT: return PortDirection.IN;
			case INOUT: return PortDirection.INOUT;
			default : throw new UnsupportedOperationException("Undefined PinDirection");
		}
	}
	
	public static PortDirection getPortDirection(Cell portCell) {
		
		checkIsValidPort(portCell);
		return (PortDirection)portCell.getProperty("Dir").getValue();
	}
	
	public static boolean isInputPort(Cell portCell) {
		
		checkIsValidPort(portCell);
		return ((PortDirection)portCell.getProperty("Dir").getValue()) == PortDirection.IN;
	}
	
	public static boolean isOutputPort(Cell portCell) {

		checkIsValidPort(portCell);
		return ((PortDirection)portCell.getProperty("Dir").getValue()) == PortDirection.OUT;
	}
	
	public static boolean isInoutPort(Cell portCell) {

		checkIsValidPort(portCell);
		return ((PortDirection)portCell.getProperty("Dir").getValue()) == PortDirection.INOUT;
	}
	
	public static void checkIsValidPort(Cell portCell) {
		
		if (!portCell.isPort()) {
			throw new UnsupportedOperationException("Attempting to get a Port direction for a cell that is not a port!");
		}
	}
}
