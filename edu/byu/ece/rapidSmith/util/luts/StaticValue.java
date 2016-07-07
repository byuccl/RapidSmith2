package edu.byu.ece.rapidSmith.util.luts;

/**
*
*/
public final class StaticValue extends EquationTree {
	private int value;

	public StaticValue(int value) {
		this.value = value;
	}

	public int getValue() {
		return value;
	}

	public void setValue(int value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return Integer.toString(value);
	}

	@Override
	protected EquationTree deepCopy() {
		return new StaticValue(value);
	}
}
