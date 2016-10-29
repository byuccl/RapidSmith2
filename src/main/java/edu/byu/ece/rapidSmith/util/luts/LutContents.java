package edu.byu.ece.rapidSmith.util.luts;

import java.util.*;

/**
 * Maintains the programming of a LUT in both equation mode and init string mode.
 * This class makes it easy to modify the contents of a LUT.
 */
public final class LutContents {
	private LutEquation equation;
	private InitString initString;
	private int numInputs;

	/**
	 * Creates a new LutContent configured to the provided equation.  {@code numInputs}
	 * should be the number of pins used by this LUT regardless of whether they are
	 * used in the equation or not.  LutContent maintains its own copy of the equation.
	 *
	 * @param equation equation for the LUT
	 * @param numInputs the number of inputs for this LUT
	 */
	public LutContents(LutEquation equation, int numInputs) {
		this.numInputs = numInputs;
		updateConfiguration(equation);
	}

	/**
	 * Creates a new LutContent configured to the provided init string.  {@code numInputs}
	 * should be the number of pins used by this LUT and the init string will be resized
	 * to {@code numInputs} possibly truncating the init string.  LutContent maintains its
	 * own copy of the init string.
	 *
	 * @param initString the init string for the LUT
	 * @param numInputs the number of inputs for this LUT
	 */
	public LutContents(InitString initString, int numInputs) {
		this.numInputs = numInputs;
		updateConfiguration(initString);
	}

	private LutContents(LutContents other) {
		this.numInputs = other.numInputs;
		if (other.equation != null)
			this.equation = other.equation.deepCopy();
		if (other.initString != null)
			this.initString = new InitString(other.initString);
	}

	/**
	 * Computes if necessary the equation form for the contents of the LUT and returns
	 * a copy of the equation.
	 *
	 * @return copy of the contents of the LUT in equation form
	 */
	public LutEquation getEquation() {
		computeEquation();
		return equation.deepCopy();
	}

	/**
	 * Computes if necessary the init string form for the contents of the LUT and returns
	 * a copy of the init string.
	 *
	 * @return copy of the contents of the LUT in init string form
	 */
	public InitString getInitString() {
		computeInitString();
		return new InitString(initString);
	}

	/**
	 * Updates the contents of the LUT with the new equation.  LutContent maintains its
	 * own copy of the equation.
	 *
	 * @param equation new equation for the LUT
	 */
	public void updateConfiguration(LutEquation equation) {
		this.initString = null;
		this.equation = equation.deepCopy();
	}

	/**
	 * Updates the contents of the LUT with the new init string.  LutContent maintains its
	 * own copy of the init string.
	 *
	 * @param initString new init string for the LUT
	 */
	public void updateConfiguration(InitString initString) {
		this.equation = null;
		initString.resize(numInputs);
		this.initString = new InitString(initString);
	}

	/**
	 * Returns the number of inputs used by the LUT.  For a LUT5, this will be 5 regardless
	 * of how many pins are actually used in the computation.
	 *
	 * @return the number of inputs used by the LUT
	 */
	public int getNumInputs() {
		return numInputs;
	}

	/**
	 * Updates the number of inputs used by the LUT.  If this method increases the number
	 * of inputs, the contents will be updated to reflect the added inputs are "don't
	 * cares".  If decreasing, the top pins will be removed possibly altering the
	 * functionality of the LUT.
	 *
	 * @param numInputs the new number of inputs for the LUT
	 */
	public void updateNumInputs(int numInputs) {
		if (numInputs == this.numInputs)
			return;  // nothing needs to be changed

		computeInitString();
		this.numInputs = numInputs;
		initString.resize(numInputs);
		this.equation = null;
	}

	/**
	 * Returns the inputs that are used in the equation form of this LUT.  This will not
	 * filter out inputs that are configured but ultimately do affect the operation of the
	 * LUT.  For example, the equation (A6+~A6)*A5 will return the set {A5, A6}.
	 *
	 * @return the inputs that are used in the equation form of this LUT
	 */
	public Set<Integer> getUsedInputs() {
		computeEquation();
		return equation.getUsedInputs();
	}

	/**
	 * Computes and returns the inputs affect the functionality of this LUT.  For example,
	 * ((A6+~A6)*A5) returns {A5} since A6 is effectively a "don't care" value.
	 *
	 * @return the set of inputs that affect the functionality of this LUT
	 */
	public Set<Integer> getRequiredInputs() {
		// We'll convert to initString, minimize it, and convert it back to a
		// minimized equation
		LutEquation reduced = getReducedForm();
		return reduced.getUsedInputs();
	}

	private LutEquation getReducedForm() {
		computeInitString();
		return LutEquation.convertToLutEquation(initString);
	}

	/**
	 * Removes any unneeded inputs from the LUT and reduces the number of inputs to the
	 * minimum required number.  Inputs to the LUT are shifted down as far as possible
	 * while maintaining incremental order.  For example, (((A6+~A6)*A5)*(A2+A3)) will be
	 * updated to the equation (A3*(A1+A2)).
	 */
	public void reduceToMinSize() {
		LutEquation reducedForm = getReducedForm();
		SortedSet<Integer> requiredInputs = new TreeSet<>(reducedForm.getUsedInputs());
		if (requiredInputs.size() == numInputs)
			return; // no need to minimize

		// create pin mapping, shifting all pins down
		int nextAvailablePin = 1;
		Map<Integer, Integer> mapping = new HashMap<>();
		for (Integer pinIndex : requiredInputs) {
			mapping.put(pinIndex, nextAvailablePin++);
		}

		// remap pins
		reducedForm.remapPins(mapping);
		this.numInputs = requiredInputs.size();
		updateConfiguration(reducedForm);
	}

	/**
	 * @return a deep copy of this LutContent.
	 */
	public LutContents deepCopy() {
		return new LutContents(this);
	}

	/**
	 * Tests whether this LutContents is functionally equivalent to o.
	 *
	 * @param o object to test against
	 * @return true if the LutContents is functionally equivalent to o.
	 */
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
		return equation.toString();
	}

	private void computeInitString() {
		if (initString == null)
			initString = InitString.convertToInitString(equation, numInputs);
	}

	private void computeEquation() {
		if (equation == null) {
			equation = LutEquation.convertToLutEquation(initString);
		}
	}
}
