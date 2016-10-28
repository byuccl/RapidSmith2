package edu.byu.ece.rapidSmith.util.luts;

public enum OpType {
	AND('*'), OR('+'), XOR('@');

	public final char xdlSymbol;

	OpType(char xdlSymbol) {
		this.xdlSymbol = xdlSymbol;
	}

	public static OpType fromXdlSymbol(char symbol) {
		switch (symbol) {
			case '*': return AND;
			case '+': return OR;
			case '@': return XOR;
			default: throw new IllegalArgumentException("unsupported symbol");
		}
	}

	public String toXdlString() {
		switch (this) {
			case AND: return "*";
			case OR: return "+";
			case XOR: return "@";
		}
		throw new AssertionError("Unknown operation");
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
