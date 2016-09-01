package edu.byu.ece.rapidSmith.util.luts;

import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.tree.TerminalNode;

/**
* Created by Haroldsen on 3/16/2015.
*/ /* Parse a lut configuration attribute value to a LUTConfiguration */
public final class EqnParserVisitor extends LutEquationBaseVisitor<Object> {
	@Override
	public EquationTree visitEquation_only(@NotNull LutEquationParser.Equation_onlyContext ctx) {
		return visitEquation(ctx.equation());
	}

	@Override
	public EquationTree visitEquation(@NotNull LutEquationParser.EquationContext ctx) {
		if (ctx.binary_eqn() != null)
			return visitBinary_eqn(ctx.binary_eqn());
		throw new AssertionError("Illegal tree structure");
	}

	@Override
	public EquationTree visitBinary_eqn(@NotNull LutEquationParser.Binary_eqnContext ctx) {
		EquationTree left;
		if (ctx.input() != null)
			left = visitInput(ctx.input());
		else if (ctx.static_value() != null)
			left = visitStatic_value(ctx.static_value());
		else if (ctx.left_eqn != null)
		left = visitBinary_eqn(ctx.binary_eqn(0));
		else
			throw new AssertionError("Expecting input or binary_eqn on left");

		EquationTree tree = left;

		if (ctx.binary_op() != null) {
			OpType op = visitBinary_op(ctx.binary_op());
			EquationTree right = visitBinary_eqn(ctx.right_eqn);
			tree = new BinaryOperation(op,
				left, right);

		}

		return tree;
	}

	@Override
	public OpType visitBinary_op(@NotNull LutEquationParser.Binary_opContext ctx) {
		switch (((TerminalNode) ctx.getChild(0)).getSymbol().getType()) {
			case LutEquationParser.AND:
				return OpType.AND;
			case LutEquationParser.OR:
				return OpType.OR;
			case LutEquationParser.XOR:
				return OpType.XOR;
		}
		throw new AssertionError("Unrecognized operation");
	}

	@Override
	public LutInput visitInput(@NotNull LutEquationParser.InputContext ctx) {
		LutInput lutInput = new LutInput(Integer.parseInt("" + ctx.INPUT().getText().charAt(1)));
		lutInput.setInverted(ctx.INV() != null);
		return lutInput;
	}

	@Override
	public Constant visitStatic_value(LutEquationParser.Static_valueContext ctx) {
		if (ctx.getText().equals("0"))
			return Constant.ZERO;
		else if (ctx.getText().equals("1"))
			return Constant.ONE;
		else
			throw new AssertionError("Invalid parse");
	}
}
