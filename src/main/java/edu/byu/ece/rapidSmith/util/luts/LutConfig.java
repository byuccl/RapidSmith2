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

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import java.util.*;

/**
 * Class representing the configuration of a mux.
 */
public final class LutConfig {
	private OperatingMode operatingMode;
	private String outputPin;
	private LutContents contents;

	/**
	 * Constructs a new LutConfig.
	 *
	 * @param operatingMode the operating mode (LUT, RAM, ROM)
	 * @param outputPin the pin the lut leaves on (O5, O6)
	 * @param contents the configuration contents of the LUT
	 */
	public LutConfig(OperatingMode operatingMode, String outputPin, LutContents contents) {
		Objects.requireNonNull(operatingMode);
		Objects.requireNonNull(contents);

		this.operatingMode = operatingMode;
		this.outputPin = outputPin;
		this.contents = contents;
	}

	/**
	 * Constructs a new LutConfig.
	 *
	 * @param operatingMode the operating mode (LUT, RAM, ROM)
	 * @param contents the configuration contents of the LUT
	 */
	public LutConfig(OperatingMode operatingMode, LutContents contents) {
		this(operatingMode, null, contents);
	}

	/**
	 * Constructs a deep copy of LutConfig {@code other}.
	 *
	 * @param other the LutConfig to copy
	 */
	public LutConfig(LutConfig other) {
		this(other.operatingMode, other.outputPin, new LutContents(other.contents));
	}

	/**
	 * Parses an XDL LUT attribute string into a LutConfig.
	 *
	 * @param attr the attribute to parse
	 * @param numInputs the number of inputs to the LUT
	 * @return the parsed LutConfig
	 */
	public static LutConfig parseXdlLutAttribute(String attr, int numInputs) {
		// prep the parser
		ANTLRInputStream input = new ANTLRInputStream(attr);
		LutEquationLexer lexer = new LutEquationLexer(input);
		CommonTokenStream tokens = new CommonTokenStream(lexer);
		LutEquationParser parser = new LutEquationParser(tokens);
		// Replace the default error listener with my fail hard listener
		parser.removeErrorListeners();
		parser.addErrorListener(new LutParseErrorListener());

		// do the actual parsing
		ParseTree tree = parser.config_string();

		// traverse the tree
		LutParserListener listener = new LutParserListener(numInputs);
		ParseTreeWalker walker = new ParseTreeWalker();
		walker.walk(listener, tree);
		return listener.makeLutConfig();
	}

	/**
	 * Returns the operating mode of this LUT, eg LUT, RAM, ROM.
	 *
	 * @return the operating mode of this LUT
	 */
	public OperatingMode getOperatingMode() {
		return operatingMode;
	}

	/**
	 * Sets the operating mode of this LUT, eg LUT, RAM, ROM.
	 *
	 * @param operatingMode the new operating mode
	 */
	public void setOperatingMode(OperatingMode operatingMode) {
		Objects.requireNonNull(operatingMode);
		this.operatingMode = operatingMode;
	}

	/**
	 * Name of the output pin for this LUT, eg O5, O6.  Unused when targeting Vivado.
	 *
	 * @return the output pin for this LUT
	 */
	public String getOutputPinName() {
		return outputPin;
	}

	/**
	 * Sets the output pin for this LUT, eg O5, O6.  Unused when targeting Vivado.
	 *
	 * @param output the new output pin
	 */
	public void setOutputPinName(String output) {
		this.outputPin = output;
	}

	/**
	 * Returns true if this LUT is configured as a static source.  A LUT is
	 * considered a static source if it is configured in LUT operating mode and
	 * its equation is "0" or "1".
	 *
	 * @return true if this LUT is configured as a static source
	 */
	public boolean isStaticSource() {
		return isVccSource() || isGndSource();
	}

	/**
	 * Returns true if this LUT is configured as a VCC source.  A LUT is
	 * considered a VCC source if it is configured in LUT operating mode and its
	 * equations is "1".
	 *
	 * @return true if this LUT is configured as a VCC source
	 */
	public boolean isVccSource() {
		return operatingMode == OperatingMode.LUT &&
				contents.getEquation() == Constant.ONE;
	}

	/**
	 * Returns true if this LUT is configured as a ground source.  A LUT is
	 * considered a ground source if it is configured in LUT operating mode and its
	 * equations is "0".
	 *
	 * @return true if this LUT is configured as a ground source
	 */
	public boolean isGndSource() {
		return operatingMode == OperatingMode.LUT &&
				contents.getEquation() == Constant.ZERO;
	}

	/**
	 * Returns the configuration of this LUT in tree form.  If needed, the tree will
	 * be constructed from the initString.  Returns null if the LUT is a static
	 * source.
	 *
	 * @return the contents of this LUT
	 */
	public LutContents getContents() {
		return contents;
	}

	/**
	 * Sets the contents of this LUT.
	 *
	 * @param contents new contents for this LUT
	 */
	public void setContents(LutContents contents) {
		Objects.requireNonNull(contents);
		this.contents = contents;
	}

	/**
	 * Configures the LUT as a VCC source.
	 */
	public void configureAsVccSource() {
		this.operatingMode = OperatingMode.LUT;
		this.contents.updateConfiguration(Constant.ONE);
	}

	/**
	 * Configures the LUT as a GND source.
	 */
	public void configureAsGndSource() {
		this.operatingMode = OperatingMode.LUT;
		this.contents.updateConfiguration(Constant.ZERO);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		LutConfig lutConfig = (LutConfig) o;
		return operatingMode == lutConfig.operatingMode &&
				Objects.equals(outputPin, lutConfig.outputPin) &&
				Objects.equals(contents, lutConfig.contents);
	}

	@Override
	public int hashCode() {
		return Objects.hash(operatingMode, outputPin, contents);
	}

	/* Parse a lut configuration attribute value to a LUTConfiguration */
	private static class LutParserListener extends LutEquationBaseListener {
		private final int numInputs;
		private OperatingMode mode;
		private String outputPin;
		private LutContents contents;

		LutParserListener(int numInputs) {
			this.numInputs = numInputs;
		}

		@Override
		public void enterOp_mode(LutEquationParser.Op_modeContext ctx) {
			mode = OperatingMode.valueOf(ctx.getText());
		}

		@Override
		public void enterOutput_pin(LutEquationParser.Output_pinContext ctx) {
			outputPin = ctx.getText();
		}

		@Override
		public void enterInit_string(LutEquationParser.Init_stringContext ctx) {
			InitString initString = InitString.parse(ctx.getText(), numInputs);
			contents = new LutContents(initString, numInputs);
		}

		@Override
		public void enterEquation_value(LutEquationParser.Equation_valueContext ctx) {
			LutEquation eqn = new EqnParserVisitor().visitEquation(ctx.equation());
			contents = new LutContents(eqn, numInputs);
		}

		LutConfig makeLutConfig() {
			return new LutConfig(mode, outputPin, contents);
		}
	}

	@Override
	public String toString() {
		return "#" + operatingMode + ":" + getOutputPinName() + "=" + contents.toString();
	}

	public String toXDLAttributeValue() {
		StringBuilder sb = new StringBuilder();
		sb.append('#');
		sb.append(operatingMode.name());
		sb.append(':');
		sb.append(getOutputPinName());
		sb.append('=');

		if (isVccSource()) {
			sb.append('1');
		} else if (isGndSource()) {
			sb.append('0');
		} else if (operatingMode == OperatingMode.LUT) {
			sb.append(contents.getEquation().toString());
		} else {
			sb.append(contents.getInitString().toString());
		}
		return sb.toString();
	}
}
