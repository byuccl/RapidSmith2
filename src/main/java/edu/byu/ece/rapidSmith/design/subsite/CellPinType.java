package edu.byu.ece.rapidSmith.design.subsite;

/**
 * Each cell pin in Vivado has an associated type. {@code CellPinType)
 * represents the possible cell pin types available in Vivado. This is useful, for example,
 * to determine if a pin is a clock pin. When changing versions of Vivado, 
 * run the command {@code report_property -class pin} in an open Vivado TCL
 * command prompt, and look at the IS_* properties. NOTE: not all of these properties 
 * will correspond to a pin type.  
 * 
 * @author Thomas Townsend
 *
 */
public enum CellPinType {
	CLEAR,
	CLOCK,
	ENABLE,
	PRESET,
	RESET,
	REUSED,
	SET,
	SETRESET,
	WRITE_ENABLE,
	DATA, 
	PSEUDO
}
