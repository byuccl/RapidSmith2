package edu.byu.ece.rapidSmith.util.luts;

/**
*
*/
public final class LutInput extends EquationTree {
	private int index;
	private boolean inverted;

	public LutInput(int index) {
		this.index = index;
		this.inverted = false;
	}

	public LutInput(int index, boolean inverted) {
		this.index = index;
		this.inverted = inverted;
	}

	public int getIndex() {
		return index;
	}

	public void setIndex(int index) {
		if (index < 1)
			throw new IllegalArgumentException("LUT indices start at 1");
		this.index = index;
	}

	public boolean isInverted() {
		return inverted;
	}

	public void setInverted(boolean inverted) {
		this.inverted = inverted;
	}

	@Override
	public String toString() {
		return (inverted ? "~" : "") + "A" + index;
	}

	@Override
	protected EquationTree deepCopy() {
		return new LutInput(index, inverted);
	}
}
