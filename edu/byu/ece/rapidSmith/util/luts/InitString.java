package edu.byu.ece.rapidSmith.util.luts;

/**
 *
 */
public final class InitString {
	private long value;

	public InitString(long value) {
		this.value = value;
	}

	public long getValue() {
		return value;
	}

	@Override
	public String toString() {
		return Long.toHexString(value);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null)
			return false;
		if (o.getClass() != InitString.class)
			return false;
		InitString that = (InitString) o;
		return value == that.value;
	}

	@Override
	public int hashCode() {
		return (int) (value ^ (value >>> 32));
	}

	/* Parse String methods */
	public static InitString parse(String initString) {
		long value = 0;
		int length = 0;
		if (initString.startsWith("0x")) {
			for (char ch : initString.substring(2).toCharArray()) {
				length += 4;
				if (ch >= '0' && ch <= '9') {
					value <<= 4;
					value += ch - '0';
				} else if (ch >= 'A' && ch <= 'F') {
					value <<= 4;
					value += ch - 'A' + 10;
				} else if (ch >= 'a' && ch <= 'f') {
					value <<= 4;
					value += ch - 'a' + 10;
				} else {
					throw new LutParseException("Unrecognized character in init string");
				}
			}
		} else if (initString.startsWith("0b")) {
			for (char ch : initString.substring(2).toCharArray()) {
				length += 1;
				if (ch >= '0' && ch <= '1') {
					value <<= 1;
					value += ch - '0';
				} else {
					throw new LutParseException("Unrecognized character in init string");
				}
			}
		}

		return convertTo6InputInitString(value, length);
	}

	// resize to standardize on 6 input luts
	private static InitString convertTo6InputInitString(long value, int length) {
		length = roundUpToNearestPowerOfTwo(length);
		while (length < 64) {
			value |= value << length;
			length *= 2;
		}
		return new InitString(value);
	}

	// finds the power of two greater than or equal to val
	private static int roundUpToNearestPowerOfTwo(int val) {
		int powerOf2 = 1;
		while (powerOf2 < val)
			powerOf2 *= 2;
		return powerOf2;
	}



	/* convert to tree form to init string form */
	/**
	 * Converts the equation tree to a init string.
	 *
	 * @param equationTree the tree to convert
	 * @return the equation represented in init string format
	 */
	public static InitString convertToInitString(EquationTree equationTree) {
		return new InitString(buildInitString_recursive(equationTree));
	}

	private static long buildInitString_recursive(EquationTree tree) {
		if (tree instanceof LutInput) {
			LutInput lutInput = (LutInput) tree;
			long inputValue = LutContents.inputValues.get(lutInput.getIndex()-1);
			if (lutInput.isInverted())
				inputValue = ~inputValue;
			return inputValue;
		} else if (tree instanceof BinaryOperation) {
			BinaryOperation op = (BinaryOperation) tree;
			long leftValue = buildInitString_recursive(op.getLeft());
			long rightValue = buildInitString_recursive(op.getRight());
			switch (op.getOp()) {
				case AND:
					return leftValue & rightValue;
				case OR:
					return leftValue | rightValue;
				case XOR:
					return leftValue ^ rightValue;
			}
			throw new AssertionError("Unrecognized operation");
		} else if (tree instanceof Constant) {
			if (tree == Constant.ONE)
				return 0xFFFFFFFFFFFFFFFFL;
			else if (tree == Constant.ZERO)
				return 0x0000000000000000L;
			throw new AssertionError("Unknown constant value");
		} else {
			throw new AssertionError("Unrecognized node");
		}
	}
}
