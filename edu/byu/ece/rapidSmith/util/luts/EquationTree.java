package edu.byu.ece.rapidSmith.util.luts;

import org.antlr.v4.runtime.*;

import java.util.*;

/**
 *
 */
public abstract class EquationTree {
	protected abstract EquationTree deepCopy();

	public static EquationTree parse(String eqn) {
		ANTLRInputStream input = new ANTLRInputStream(eqn);
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
	 * Converts the init string to a minimized equation tree in sum of products
	 * form.  If the contents are constant value, returns null.
	 *
	 * @param initString the init string to convert
	 * @return the minimized equation tree representing the init string
	 */
	public static EquationTree convertToEquationTree(InitString initString) {
		Set<MatchProduct> products = formProducts(initString);
		products = reduceProducts(products);
		return convertToTree(products);
	}

	private static Set<MatchProduct> formProducts(InitString initString) {
		// organize into set of unsimplified sum of products
		Set<MatchProduct> products = new HashSet<>();
		for (int i = 0; i < 64; i++) {
			long mask = 1L << i;
			// check if this product yields a 1
			if ((initString.getValue() & mask) != 0) {
				MatchProduct product = new MatchProduct();
				for (int j = 0; j < 6; j++) {
					if ((mask & LutContents.inputValues.get(j)) != 0)
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
	private static EquationTree convertToTree(Set<MatchProduct> products) {
		EquationTree equationTree = null;
		for (MatchProduct mp : products) {
			EquationTree productTree = makeProductTree(mp);
			if (equationTree == null) // only for first product
				equationTree = productTree;
			else // make an or chain
				equationTree = new BinaryOperation(OpType.OR, equationTree, productTree);
		}
		// handle potential constant outputs
		if (equationTree == null) {
			if (products.size() == 0)
				equationTree = Constant.ZERO;
			else if (products.size() == 1)
				equationTree = Constant.ONE;
		}
		return equationTree;
	}

	private static EquationTree makeProductTree(MatchProduct mp) {
		EquationTree productTree = null;
		for (int i = 0; i < 6; i++) {
			EquationTree inputTree = null;
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
		MatchValue[] value = new MatchValue[6];

		// checks if this product is one bit different from the other.  This
		// signifies a don't care
		public boolean offByOne(MatchProduct other) {
			int offCount = 0;
			for (int i = 0; i < 6; i++) {
				if (value[i] != other.value[i]) {
					offCount++;
					if (offCount > 1)
						return false;
				}
			}
			return offCount == 1;
		}

		// merges two products which are off by one
		public static MatchProduct merge(MatchProduct mp1, MatchProduct mp2) {
			assert mp1.offByOne(mp2);
			MatchProduct ret = new MatchProduct();
			for (int i = 0; i < 6; i++) {
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
			for (int i = 5; i >= 0; i--) {
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
}
