package edu.byu.ece.rapidSmith.util.luts;

import java.util.Arrays;
import java.util.List;

/**
 * String of 0s and 1s representing a lut configuration.  Each bit represents the result
 * of the LUT for a different set of input values.  The corresponding input for the bit in
 * the string is obtained from computing the unsigned value of the inputs bits arranged
 * from highest bit to lowest.  For example, if A6 and A4 are 1 and the other bits are 0,
 * then the index into the string would be 0b101000 = 0d40.
 *
 * InitStrings provide for simple means of comparing functionally equivalent LUT
 * configurations as each possible InitString represents a unique LUT function as opposed
 * to equations in which multiple equations may be functionally identical.
 */
public final class InitString {
	private static final List<Long> inputValues = Arrays.asList(
			0xAAAAAAAAAAAAAAAAL,
			0xCCCCCCCCCCCCCCCCL,
			0xF0F0F0F0F0F0F0F0L,
			0xFF00FF00FF00FF00L,
			0xFFFF0000FFFF0000L,
			0xFFFFFFFF00000000L
	);

	private long cfgValue;
	private int numInputs;

	/**
	 * Creates a new InitString from the long value.  The InitString is configured to use
	 * {@code numInputs} number of inputs.  The configuration value is truncated to contain
	 * only as many bits as required for the specified number of inputs.  Any higher order
	 * bits are discarded.  {@code numInputs} must be > 1 and <=6.
	 *
	 * @param configuration the configuration provided as a long
	 * @param numInputs number of inputs in this init string
	 * @throws IllegalArgumentException if {@code numInputs} > 6 or < 1
	 */
	public InitString(long configuration, int numInputs) {
		if (numInputs > 6)
			throw new IllegalArgumentException("cannot support more than 6 inputs");
		if (numInputs < 1)
			throw new IllegalArgumentException("init string must have at least 1 input");

		long mask = getMask(numInputs);
		this.cfgValue = mask & configuration;
		this.numInputs = numInputs;
	}

	/**
	 * Constructs a copy of other.
	 * @param other InitString object to copy
	 */
	public InitString(InitString other) {
		this.cfgValue = other.cfgValue;
		this.numInputs = other.numInputs;
	}

	/**
	 * @return the configuration contained in this init string
	 */
	public long getCfgValue() {
		return cfgValue;
	}

	/**
	 * @return the number of inputs used by this init string
	 */
	public int getNumInputs() {
		return numInputs;
	}

	/**
	 * Resizes the init string to use the new specified number of inputs.  Downsizing will
	 * truncate the value to the number of required bits while upsizing will replicate the
	 * bits treating the new higher order inputs as don't cares.  {@code numInputs} must be
	 * > 1 and <=6.
	 *
	 * @param numInputs the new number of inputs
	 * @throws IllegalArgumentException if {@code numInputs} > 6 or < 1
	 */
	public void resize(int numInputs) {
		if (numInputs > 6)
			throw new IllegalArgumentException("cannot support more than 6 inputs");
		if (numInputs < 1)
			throw new IllegalArgumentException("init string must have at least 1 input");

		if (numInputs > this.numInputs) {
			for (int i = this.numInputs; i <= numInputs; i++) {
				int shift = twoToThe(i);
				cfgValue |= cfgValue << shift;
			}
		} else if (numInputs < this.numInputs) {
			cfgValue &= getMask(numInputs);
		}
		this.numInputs = numInputs;
	}

	/**
	 * Returns the init string in hex form prepended with 0x.
	 */
	@Override
	public String toString() {
		int numDigits = twoToThe(this.numInputs) / 4;
		return String.format("0x%0" + numDigits + "X", cfgValue);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null)
			return false;
		if (o.getClass() != InitString.class)
			return false;
		InitString that = (InitString) o;
		return cfgValue == that.cfgValue;
	}

	@Override
	public int hashCode() {
		return (int) (cfgValue ^ (cfgValue >>> 32));
	}

	/* Parse String methods */

	/**
	 * Parses the string and returns a new InitString of the configuration.  This method
	 * supports init strings in the form "0x&lt;cfg&gt;" for value in hexadecimal format
	 * and "0b&lt;cfg&gt;" for values in binary format.
	 *
	 * @param configuration string representation of the init string
	 * @param numInputs number of inputs for this init string
	 * @return a new InitString containing the provided configuration
	 */
	public static InitString parse(String configuration, int numInputs) {
		long value = 0;
		if (configuration.startsWith("0x")) {
			for (char ch : configuration.substring(2).toCharArray()) {
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
		} else if (configuration.startsWith("0b")) {
			for (char ch : configuration.substring(2).toCharArray()) {
				if (ch >= '0' && ch <= '1') {
					value <<= 1;
					value += ch - '0';
				} else {
					throw new LutParseException("Unrecognized character in init string");
				}
			}
		}

		return new InitString(value, numInputs);
	}

	/* convert to tree form to init string form */
	/**
	 * Converts the equation tree to a init string.
	 *
	 * @param lutEquation the tree to convert
	 * @return the equation represented in init string format
	 */
	public static InitString convertToInitString(LutEquation lutEquation, int numInputs) {
		return new InitString(buildInitString_recursive(lutEquation), numInputs);
	}

	private static long buildInitString_recursive(LutEquation tree) {
		if (tree instanceof LutInput) {
			LutInput lutInput = (LutInput) tree;
			long inputValue = inputValues.get(lutInput.getIndex()-1);
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

	private static int twoToThe(int power) {
		switch (power) {
			case 1: return 2;
			case 2: return 4;
			case 3: return 8;
			case 4: return 16;
			case 5: return 32;
			case 6: return 64;
			default: throw new AssertionError("unsupported power");
		}
	}

	private static long getMask(int numInputs) {
		switch (numInputs) {
			case 1: return 0x3L;
			case 2: return 0xFL;
			case 3: return 0xFFL;
			case 4: return 0xFFFFL;
			case 5: return 0xFFFFFFFFL;
			case 6: return 0xFFFFFFFFFFFFFFFFL;
			default: throw new AssertionError("unsupported power");
		}
	}
}
