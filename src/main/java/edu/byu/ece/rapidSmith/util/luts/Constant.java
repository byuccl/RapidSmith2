package edu.byu.ece.rapidSmith.util.luts;

/**
 * Created by Haroldsen on 3/16/2015.
 */
public final class Constant extends EquationTree {
    public static final Constant ONE = new Constant("1");
	public static final Constant ZERO = new Constant("0");
	private final String strValue;


	private Constant(String strValue) {
		this.strValue = strValue;
	}

	@Override
	public String toString() {
		return strValue;
	}

	@Override
	protected EquationTree deepCopy() {
		return this;
	}
}
