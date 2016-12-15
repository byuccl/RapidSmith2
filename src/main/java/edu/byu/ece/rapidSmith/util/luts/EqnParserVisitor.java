/*
 * Copyright (c) 2016 Brigham Young University
 *
 * This file is part of the BYU RapidSmith Tools.
 *
 * BYU RapidSmith Tools is free software: you may redistribute it
 * and/or modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 *
 * BYU RapidSmith Tools is distributed in the hope that it will be
 * useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * A copy of the GNU General Public License is included with the BYU
 * RapidSmith Tools. It can be found at doc/LICENSE.GPL3.TXT. You may
 * also get a copy of the license at <http://www.gnu.org/licenses/>.
 */

package edu.byu.ece.rapidSmith.util.luts;

import org.antlr.v4.runtime.tree.TerminalNode;

/**
* Created by Haroldsen on 3/16/2015.
*/ /* Parse a lut configuration attribute value to a LUTConfiguration */
final class EqnParserVisitor {
	LutEquation visitEquation_only(LutEquationParser.Equation_onlyContext ctx) {
		return visitEquation(ctx.equation());
	}

	LutEquation visitEquation(LutEquationParser.EquationContext ctx) {
		if (ctx.binary_eqn() != null)
			return visitBinary_eqn(ctx.binary_eqn());
		throw new AssertionError("Illegal tree structure");
	}

	private LutEquation visitBinary_eqn(LutEquationParser.Binary_eqnContext ctx) {
		LutEquation left;
		if (ctx.input() != null)
			left = visitInput(ctx.input());
		else if (ctx.static_value() != null)
			left = visitStatic_value(ctx.static_value());
		else if (ctx.left_eqn != null)
			left = visitBinary_eqn(ctx.binary_eqn(0));
		else
			throw new AssertionError("Expecting input or binary_eqn on left");

		LutEquation tree;

		if (ctx.binary_op() != null) {
			OpType op = visitBinary_op(ctx.binary_op());
			LutEquation right = visitBinary_eqn(ctx.right_eqn);
			tree = new BinaryOperation(op, left, right);
		} else {
			tree = left;
		}

		return tree;
	}

	private OpType visitBinary_op(LutEquationParser.Binary_opContext ctx) {
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

	private LutInput visitInput(LutEquationParser.InputContext ctx) {
		LutInput lutInput = new LutInput(Integer.parseInt("" + ctx.INPUT().getText().charAt(1)));
		lutInput.setInverted(ctx.INV() != null);
		return lutInput;
	}

	private Constant visitStatic_value(LutEquationParser.Static_valueContext ctx) {
		if (ctx.getText().equals("0"))
			return Constant.ZERO;
		else if (ctx.getText().equals("1"))
			return Constant.ONE;
		else
			throw new AssertionError("Invalid parse");
	}
}
