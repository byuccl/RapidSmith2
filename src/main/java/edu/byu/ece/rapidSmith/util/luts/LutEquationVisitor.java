// Generated from C:/work/rapidsmith2/edu/byu/ece/rapidSmith/util/luts\LutEquation.g4 by ANTLR 4.5.1
package edu.byu.ece.rapidSmith.util.luts;
import org.antlr.v4.runtime.tree.ParseTreeVisitor;

/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link LutEquationParser}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 * operations with no return type.
 */
public interface LutEquationVisitor<T> extends ParseTreeVisitor<T> {
	/**
	 * Visit a parse tree produced by {@link LutEquationParser#config_string}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConfig_string(LutEquationParser.Config_stringContext ctx);
	/**
	 * Visit a parse tree produced by {@link LutEquationParser#equation_only}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEquation_only(LutEquationParser.Equation_onlyContext ctx);
	/**
	 * Visit a parse tree produced by {@link LutEquationParser#op_mode}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOp_mode(LutEquationParser.Op_modeContext ctx);
	/**
	 * Visit a parse tree produced by {@link LutEquationParser#output_pin}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOutput_pin(LutEquationParser.Output_pinContext ctx);
	/**
	 * Visit a parse tree produced by {@link LutEquationParser#lut_value}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLut_value(LutEquationParser.Lut_valueContext ctx);
	/**
	 * Visit a parse tree produced by {@link LutEquationParser#static_value}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStatic_value(LutEquationParser.Static_valueContext ctx);
	/**
	 * Visit a parse tree produced by {@link LutEquationParser#init_string}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInit_string(LutEquationParser.Init_stringContext ctx);
	/**
	 * Visit a parse tree produced by {@link LutEquationParser#equation_value}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEquation_value(LutEquationParser.Equation_valueContext ctx);
	/**
	 * Visit a parse tree produced by {@link LutEquationParser#equation}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEquation(LutEquationParser.EquationContext ctx);
	/**
	 * Visit a parse tree produced by {@link LutEquationParser#binary_eqn}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBinary_eqn(LutEquationParser.Binary_eqnContext ctx);
	/**
	 * Visit a parse tree produced by {@link LutEquationParser#binary_op}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBinary_op(LutEquationParser.Binary_opContext ctx);
	/**
	 * Visit a parse tree produced by {@link LutEquationParser#input}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInput(LutEquationParser.InputContext ctx);
}