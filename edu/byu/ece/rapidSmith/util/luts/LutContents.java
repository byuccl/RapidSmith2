package edu.byu.ece.rapidSmith.util.luts;

import java.util.*;

/**
 * Created by Haroldsen on 3/16/2015.
 */
public final class LutContents {
	static final List<Long> inputValues = Arrays.asList(
			0xAAAAAAAAAAAAAAAAL,
			0xCCCCCCCCCCCCCCCCL,
			0xF0F0F0F0F0F0F0F0L,
			0xFF00FF00FF00FF00L,
			0xFFFF0000FFFF0000L,
			0xFFFFFFFF00000000L
	);

	private EquationTree eqn;
	private InitString initString;

	public LutContents(EquationTree eqn) {
		this.eqn = eqn;
	}

	public LutContents(InitString initString) {
		this.initString = initString;
	}

	public static LutContents parseEquation(String eqn) {
		return new LutContents(EquationTree.parse(eqn));
	}

	public static LutContents parseInitString(String initString) {
		return new LutContents(InitString.parse(initString));
	}

	private void computeInitString() {
		if (initString == null)
			initString = InitString.convertToInitString(eqn);
	}

	private void computeEquation() {
		if (eqn == null) {
			eqn = EquationTree.convertToEquationTree(initString);
		}
	}

	public Set<Integer> getUsedInputs() {
		computeEquation();
		return getUsedInputs(new HashSet<>(), eqn);
	}

	private Set<Integer> getUsedInputs(Set<Integer> usedInputs, EquationTree node) {
		// Is null, means either static source or constant output
		if (node == null)
			return usedInputs;

		if (node.getClass() == LutInput.class) {
			LutInput lutInput = ((LutInput) node);
			usedInputs.add(lutInput.getIndex());
		} else if (node.getClass() == BinaryOperation.class) {
			BinaryOperation op = ((BinaryOperation) node);
			getUsedInputs(usedInputs, op.getLeft());
			getUsedInputs(usedInputs, op.getRight());
		} else {
			assert node.getClass() == Constant.class;
			// No inputs to add
		}
		return usedInputs;
	}

	/**
	 * Computes and returns the minimum number of inputs required by this LUT's
	 * configuration once the equation is optimized.  For example, ((A6 * ~A6) + A5)
	 * returns 1.
	 *
	 * @return the number of inputs required by this LUT's configuration
	 */
	public int getMinNumOfInputs() {
		// We'll convert to initString, minimize it, and convert it back to a
		// minimized equation
		EquationTree tree = getReducedForm().eqn;
		return getUsedInputs(new HashSet<>(), tree).size();
	}

	public LutContents getReducedForm() {
		computeInitString();
		return new LutContents(EquationTree.convertToEquationTree(initString));
	}

	public EquationTree getCopyOfEquation() {
		computeEquation();
		return eqn.deepCopy();
	}

	public InitString getCopyOfInitString() {
		computeInitString();
		return new InitString(initString.getValue());
	}

	public LutContents deepCopy() {
		return new LutContents(eqn.deepCopy());
	}

	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null)
			return false;
		if (this.getClass() != o.getClass())
			return false;
		LutContents other = ((LutContents) o);
		if (initString == null)
			computeInitString();
		if (other.initString == null)
			other.computeInitString();
		return initString.equals(other.initString);
	}

	@Override
	public int hashCode() {
		if (initString == null)
			computeInitString();
		return initString.hashCode();
	}

	@Override
	public String toString() {
		computeEquation();
		return eqn.toString();
	}
}
