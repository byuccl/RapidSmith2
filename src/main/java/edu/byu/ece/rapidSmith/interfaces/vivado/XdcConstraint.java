package edu.byu.ece.rapidSmith.interfaces.vivado;

/**
 * This class holds design constraints found in the constraints.xdc file.
 * TODO: Make these constraints easier to work with
 * TODO: Provide an example XDC constraints file.
 *
 */
public final class XdcConstraint {

	private final String command;
	private final String options;
	
	public XdcConstraint(String command, String options){
		this.command = command;
		this.options = options;
	}
	
	/**
	 * Formats the XDC constraint and returns it as a string.
	 */
	@Override
	public String toString(){
		return command + " " + options;
	}
}
