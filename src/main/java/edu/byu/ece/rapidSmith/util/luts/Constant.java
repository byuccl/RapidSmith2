package edu.byu.ece.rapidSmith.util.luts;

import java.util.Map;
import java.util.Set;

/**
 * A LutEquation constant value.  Only options are ZERO and ONE.
 */
public final class Constant extends LutEquation {
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
	public LutEquation deepCopy() {
		return this;
	}

	@Override
	protected void getUsedInputs(Set<Integer> usedInputs) {
		// nothing to add
	}

	@Override
	public void remapPins(Map<Integer, Integer> mapping) {
		// nothing to do here
	}

	@Override
	public boolean equals(Object o) {
		return this == o;
	}

	@Override
	public int hashCode() {
		return this == ONE ? 1 : 0;
	}
}
