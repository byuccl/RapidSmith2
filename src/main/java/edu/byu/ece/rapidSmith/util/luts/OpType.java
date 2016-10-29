package edu.byu.ece.rapidSmith.util.luts;

/**
 * Binary operations for a LutEquation.
 */
public enum OpType {
	AND('*'), OR('+'), XOR('@');

	/** The XDL equation symbol for the operation. */
	public final char xdlSymbol;

	OpType(char xdlSymbol) {
		this.xdlSymbol = xdlSymbol;
	}

	/**
	 * @param symbol the XDL symbol
	 * @return the operation correlating to the symbol {@code symbol} in an XDL equation
	 */
	public static OpType fromXdlSymbol(char symbol) {
		switch (symbol) {
			case '*': return AND;
			case '+': return OR;
			case '@': return XOR;
			default: throw new IllegalArgumentException("unsupported symbol");
		}
	}

	public String toString() {
		switch (this) {
			case AND: return "*";
			case OR: return "+";
			case XOR: return "@";
		}
		throw new AssertionError("Unknown operation");
	}
}
