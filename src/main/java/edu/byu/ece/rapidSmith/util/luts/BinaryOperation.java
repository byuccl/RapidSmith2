package edu.byu.ece.rapidSmith.util.luts;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
* A binary operation in a LutEquation.
*/
public final class BinaryOperation extends LutEquation {
	private OpType op;
	private LutEquation left;
	private LutEquation right;

	/**
	 * Constructs a new binary equation.
	 * @param op operator of the equation
	 * @param left left sub-equation of the equation
	 * @param right right sub-equation of the equation
	 */
	public BinaryOperation(OpType op, LutEquation left, LutEquation right) {
		setOp(op);
		setLeft(left);
		setRight(right);
	}

	/**
	 * @return the operator of this operation
	 */
	public OpType getOp() {
		return op;
	}

	/**
	 * Sets the operator of this equation.
	 * 
	 * @param op the new operator for this equation
	 */
	public void setOp(OpType op) {
		Objects.requireNonNull(op);
		this.op = op;
	}

	/**
	 * @return the left sub-equation of this operation
	 */
	public LutEquation getLeft() {
		return left;
	}

	/**
	 * Sets the left sub-equation of this operation.
	 * 
	 * @param left the new left sub-equation of this operation
	 */
	public void setLeft(LutEquation left) {
		Objects.requireNonNull(left);
		this.left = left;
	}

	/**
	 * @return the right sub-equation of this operation
	 */
	public LutEquation getRight() {
		return right;
	}

	/**
	 * Sets the right sub-equation of this operation.
	 *
	 * @param right the new right sub-equation of this operation
	 */
	public void setRight(LutEquation right) {
		Objects.requireNonNull(right);
		this.right = right;
	}

	@Override
	public LutEquation deepCopy() {
		return new BinaryOperation(op, left.deepCopy(), right.deepCopy());
	}

	@Override
	protected void getUsedInputs(Set<Integer> usedInputs) {
		getLeft().getUsedInputs(usedInputs);
		getRight().getUsedInputs(usedInputs);
	}

	@Override
	public void remapPins(Map<Integer, Integer> mapping) {
		getLeft().remapPins(mapping);
		getRight().remapPins(mapping);
	}

	@Override
	public String toString() {
		return "(" + left + op + right + ")";
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof BinaryOperation)) return false;
		BinaryOperation that = (BinaryOperation) o;
		return getOp() == that.getOp() &&
				Objects.equals(getLeft(), that.getLeft()) &&
				Objects.equals(getRight(), that.getRight());
	}

	@Override
	public int hashCode() {
		return Objects.hash(getOp(), getLeft(), getRight());
	}
}
