package edu.byu.ece.rapidSmith.util.luts;

import org.antlr.v4.runtime.*;

import java.util.*;

/**
 * A tree-structured representation of an XDL LUT equation.
 */
public abstract class LutEquation {
	private static final List<Long> inputValues = Arrays.asList(
			0xAAAAAAAAAAAAAAAAL,
			0xCCCCCCCCCCCCCCCCL,
			0xF0F0F0F0F0F0F0F0L,
			0xFF00FF00FF00FF00L,
			0xFFFF0000FFFF0000L,
			0xFFFFFFFF00000000L
	);

	// Prevent users from creating their own subclasses.
	LutEquation() { }

	/**
	 * Returns a deep copy of the equation.  Any changes made to the returned equation
	 * will not be reflected in this equation and vice versa.  Some immutable objects
	 * may not be updated.
	 *
	 * @return a deep copy of this equation
	 */
	public abstract LutEquation deepCopy();

	/**
	 * Returns the string representation of this equation.  The string should be such that
	 * calling {@link #parse(String)} on the returned string should yield an identical
	 * equation.
	 *
	 * @return the string representation of this equation
	 */
	public abstract String toString();

	/**
	 * Tests for equality of the equations.  Equations are equal if the trees are
	 * identical.  Equal LutEquations will be functionally equivalent but functionally
	 * equivalent LutEquations may not be equal.
	 *
	 * @return true if this LutEquation is equal to {@code other}
	 */
	public abstract boolean equals(Object other);

	/**
	 * Remaps the pins with the index in the keys of mapping to their values.
	 * @param mapping map of the index of the pins to the indexes to change them to
	 */
	public abstract void remapPins(Map<Integer, Integer> mapping);

	/**
	 * @return the inputs used in this equation
	 */
	public final Set<Integer> getUsedInputs() {
		HashSet<Integer> usedInputs = new HashSet<>();
		getUsedInputs(usedInputs);
		return usedInputs;
	}

	protected abstract void getUsedInputs(Set<Integer> usedInputs);

	/**
	 * Parses an XDL LUT equation into a LutEquation tree.
	 *
	 * @param string string representation of an equation to parse
	 * @return the equivalent LutEquation object
	 * @throws LutParseException if equation is improperly formatted
	 */
	public static LutEquation parse(String string) {
		ANTLRInputStream input = new ANTLRInputStream(string);
		LutEquationLexer lexer = new LutEquationLexer(input);
		CommonTokenStream tokens = new CommonTokenStream(lexer);
		LutEquationParser parser = new LutEquationParser(tokens);
		// Replace the default error listener with my fail hard listener
		parser.removeErrorListeners();
		parser.addErrorListener(new LutParseErrorListener());
		LutEquationParser.Equation_onlyContext tree = parser.equation_only();
		return new EqnParserVisitor().visitEquation_only(tree);
	}

	/* Convert the init string form to tree form */

	/**
	 * Converts the init string to a minimized sum of products LutEquation..
	 *
	 * @param initString the init string to convert
	 * @return the minimized equivalent LutEquation
	 */
	public static LutEquation convertToLutEquation(InitString initString) {
		Set<MatchProduct> products = formProducts(initString);
		products = reduceProducts(products);
		return convertToTree(products);
	}

	private static Set<MatchProduct> formProducts(InitString initString) {
		// organize into set of unsimplified sum of products
		Set<MatchProduct> products = new HashSet<>();
		int numInputs = initString.getNumInputs();
		int twoToTheInputs = twoToThe(numInputs);
		for (int i = 0; i < twoToTheInputs; i++) {
			long mask = 1L << i;
			// check if this product yields a 1
			if ((initString.getCfgValue() & mask) != 0) {
				MatchProduct product = new MatchProduct(numInputs);
				for (int j = 0; j < numInputs; j++) {
					if ((mask & inputValues.get(j)) != 0)
						product.value[j] = MatchValue.ONE;
					else
						product.value[j] = MatchValue.ZERO;
				}
				products.add(product);
			}
		}
		return products;
	}

	// Perform boolean minimization to obtain a minimal SOP representation
	// Uses a list minimization technique, we only have a maximum of 6 inputs
	// so this should be reasonably quick
	private static Set<MatchProduct> reduceProducts(Set<MatchProduct> products) {
		boolean madeChanges;
		do {
			madeChanges = false;
			Set<MatchProduct> updated = new HashSet<>();
			for (MatchProduct product0 : products) {
				boolean productMerged = false;
				for (MatchProduct product1 : products) {
					// don't compare against self
					if (product0 == product1)
						continue;
					// A single bit difference indicates an input can be
					// changed to a don't care
					if (product0.offByOne(product1)) {
						updated.add(MatchProduct.merge(product0, product1));
						madeChanges = true;
						productMerged = true;
					}
				}
				if (!productMerged)
					updated.add(product0);
			}
			products = updated;
		} while (madeChanges); // continue until no more minimizations are possible
		return products;
	}

	// Convert the set of products into an equation in SOP form
	private static LutEquation convertToTree(Set<MatchProduct> products) {
		LutEquation lutEquation = null;
		for (MatchProduct mp : products) {
			LutEquation productTree = makeProductTree(mp);
			if (lutEquation == null) // only for first product
				lutEquation = productTree;
			else // make an or chain
				lutEquation = new BinaryOperation(OpType.OR, lutEquation, productTree);
		}
		// handle potential constant outputs
		if (lutEquation == null) {
			if (products.size() == 0)
				lutEquation = Constant.ZERO;
			else if (products.size() == 1)
				lutEquation = Constant.ONE;
		}
		return lutEquation;
	}

	private static LutEquation makeProductTree(MatchProduct mp) {
		LutEquation productTree = null;
		for (int i = 0; i < mp.getNumInputs(); i++) {
			LutEquation inputTree = null;
			switch (mp.value[i]) {
				case DONT_CARE:
					continue; // don't cares signal unused inputs
				case ONE:
					inputTree = new LutInput(i+1, false); // an uninverted input
					break;
				case ZERO:
					inputTree = new LutInput(i+1, true); // an inverted input
			}

			if (productTree == null) // only true for the first input
				productTree = inputTree;
			else // create a chain of ands
				productTree = new BinaryOperation(OpType.AND, productTree, inputTree);
		}
		return productTree;
	}

	private enum MatchValue {
		ZERO, ONE, DONT_CARE
	}

	// A product form of the equation
	private static class MatchProduct {
		final MatchValue[] value;

		MatchProduct(int numInputs) {
			value  = new MatchValue[numInputs];
		}

		int getNumInputs() {
			return value.length;
		}

		// checks if this product is one bit different from the other.  This
		// signifies a don't care
		boolean offByOne(MatchProduct other) {
			int offCount = 0;
			for (int i = 0; i < value.length; i++) {
				if (value[i] != other.value[i]) {
					offCount++;
					if (offCount > 1)
						return false;
				}
			}
			return offCount == 1;
		}

		// merges two products which are off by one
		static MatchProduct merge(MatchProduct mp1, MatchProduct mp2) {
			assert mp1.getNumInputs() == mp2.getNumInputs();
			assert mp1.offByOne(mp2);
			int numInputs = mp1.getNumInputs();
			MatchProduct ret = new MatchProduct(numInputs);
			for (int i = 0; i < numInputs; i++) {
				if (mp1.value[i] == mp2.value[i]) {
					ret.value[i] = mp1.value[i];
				} else {
					// I can get away with this since only 1 input should be different
					ret.value[i] = MatchValue.DONT_CARE;
				}
			}
			return ret;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null || getClass() != obj.getClass()) {
				return false;
			}
			final MatchProduct other = (MatchProduct) obj;
			return Objects.deepEquals(this.value, other.value);
		}

		@Override
		public int hashCode() {
			return Arrays.hashCode(value);
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			for (int i = getNumInputs()-1; i >= 0; i--) {
				switch (value[i]) {
					case ONE:
						sb.append("1");
						break;
					case ZERO:
						sb.append("0");
						break;
					case DONT_CARE:
						sb.append("-");
						break;
				}
			}
			return sb.toString();
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
}
