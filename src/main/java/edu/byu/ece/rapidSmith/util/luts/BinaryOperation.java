package edu.byu.ece.rapidSmith.util.luts;

/**
*
*/
public final class BinaryOperation extends EquationTree {
	private OpType op;
	private EquationTree left;
	private EquationTree right;

	public BinaryOperation(OpType op, EquationTree left, EquationTree right) {
		this.op = op;
		this.left = left;
		this.right = right;
	}

	public OpType getOp() {
		return op;
	}

	public void setOp(OpType op) {
		this.op = op;
	}

	public EquationTree getLeft() {
		return left;
	}

	public void setLeft(EquationTree left) {
		this.left = left;
	}

	public EquationTree getRight() {
		return right;
	}

	public void setRight(EquationTree right) {
		this.right = right;
	}

	@Override
	protected EquationTree deepCopy() {
		return new BinaryOperation(op, left.deepCopy(), right.deepCopy());
	}

	@Override
	public String toString() {
		return "(" + left + op + right + ")";
	}
}
