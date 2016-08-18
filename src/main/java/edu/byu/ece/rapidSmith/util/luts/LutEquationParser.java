// Generated from C:/work/rapidsmith2/edu/byu/ece/rapidSmith/util/luts\LutEquation.g4 by ANTLR 4.5.1
package edu.byu.ece.rapidSmith.util.luts;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.*;
import org.antlr.v4.runtime.tree.*;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class LutEquationParser extends Parser {
	static { RuntimeMetaData.checkVersion("4.5.1", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		T__0=1, T__1=2, T__2=3, T__3=4, T__4=5, T__5=6, T__6=7, T__7=8, T__8=9, 
		T__9=10, INV=11, AND=12, OR=13, XOR=14, INPUT=15, CONST_VALUE=16, OUTPUT_PIN=17;
	public static final int
		RULE_config_string = 0, RULE_equation_only = 1, RULE_op_mode = 2, RULE_output_pin = 3, 
		RULE_lut_value = 4, RULE_static_value = 5, RULE_init_string = 6, RULE_equation_value = 7, 
		RULE_equation = 8, RULE_binary_eqn = 9, RULE_binary_op = 10, RULE_input = 11;
	public static final String[] ruleNames = {
		"config_string", "equation_only", "op_mode", "output_pin", "lut_value", 
		"static_value", "init_string", "equation_value", "equation", "binary_eqn", 
		"binary_op", "input"
	};

	private static final String[] _LITERAL_NAMES = {
		null, "'#'", "':'", "'='", "'LUT'", "'RAM'", "'ROM'", "'0'", "'1'", "'('", 
		"')'", "'~'", "'*'", "'+'", "'@'"
	};
	private static final String[] _SYMBOLIC_NAMES = {
		null, null, null, null, null, null, null, null, null, null, null, "INV", 
		"AND", "OR", "XOR", "INPUT", "CONST_VALUE", "OUTPUT_PIN"
	};
	public static final Vocabulary VOCABULARY = new VocabularyImpl(_LITERAL_NAMES, _SYMBOLIC_NAMES);

	/**
	 * @deprecated Use {@link #VOCABULARY} instead.
	 */
	@Deprecated
	public static final String[] tokenNames;
	static {
		tokenNames = new String[_SYMBOLIC_NAMES.length];
		for (int i = 0; i < tokenNames.length; i++) {
			tokenNames[i] = VOCABULARY.getLiteralName(i);
			if (tokenNames[i] == null) {
				tokenNames[i] = VOCABULARY.getSymbolicName(i);
			}

			if (tokenNames[i] == null) {
				tokenNames[i] = "<INVALID>";
			}
		}
	}

	@Override
	@Deprecated
	public String[] getTokenNames() {
		return tokenNames;
	}

	@Override

	public Vocabulary getVocabulary() {
		return VOCABULARY;
	}

	@Override
	public String getGrammarFileName() { return "LutEquation.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public ATN getATN() { return _ATN; }

	public LutEquationParser(TokenStream input) {
		super(input);
		_interp = new ParserATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}
	public static class Config_stringContext extends ParserRuleContext {
		public Op_modeContext op_mode() {
			return getRuleContext(Op_modeContext.class,0);
		}
		public Output_pinContext output_pin() {
			return getRuleContext(Output_pinContext.class,0);
		}
		public Lut_valueContext lut_value() {
			return getRuleContext(Lut_valueContext.class,0);
		}
		public TerminalNode EOF() { return getToken(LutEquationParser.EOF, 0); }
		public Config_stringContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_config_string; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof LutEquationVisitor ) return ((LutEquationVisitor<? extends T>)visitor).visitConfig_string(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Config_stringContext config_string() throws RecognitionException {
		Config_stringContext _localctx = new Config_stringContext(_ctx, getState());
		enterRule(_localctx, 0, RULE_config_string);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(24);
			match(T__0);
			setState(25);
			op_mode();
			setState(26);
			match(T__1);
			setState(27);
			output_pin();
			setState(28);
			match(T__2);
			setState(29);
			lut_value();
			setState(30);
			match(EOF);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Equation_onlyContext extends ParserRuleContext {
		public EquationContext equation() {
			return getRuleContext(EquationContext.class,0);
		}
		public TerminalNode EOF() { return getToken(LutEquationParser.EOF, 0); }
		public Equation_onlyContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_equation_only; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof LutEquationVisitor ) return ((LutEquationVisitor<? extends T>)visitor).visitEquation_only(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Equation_onlyContext equation_only() throws RecognitionException {
		Equation_onlyContext _localctx = new Equation_onlyContext(_ctx, getState());
		enterRule(_localctx, 2, RULE_equation_only);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(32);
			equation();
			setState(33);
			match(EOF);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Op_modeContext extends ParserRuleContext {
		public Op_modeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_op_mode; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof LutEquationVisitor ) return ((LutEquationVisitor<? extends T>)visitor).visitOp_mode(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Op_modeContext op_mode() throws RecognitionException {
		Op_modeContext _localctx = new Op_modeContext(_ctx, getState());
		enterRule(_localctx, 4, RULE_op_mode);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(35);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << T__3) | (1L << T__4) | (1L << T__5))) != 0)) ) {
			_errHandler.recoverInline(this);
			} else {
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Output_pinContext extends ParserRuleContext {
		public TerminalNode INPUT() { return getToken(LutEquationParser.INPUT, 0); }
		public TerminalNode OUTPUT_PIN() { return getToken(LutEquationParser.OUTPUT_PIN, 0); }
		public TerminalNode CONST_VALUE() { return getToken(LutEquationParser.CONST_VALUE, 0); }
		public Output_pinContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_output_pin; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof LutEquationVisitor ) return ((LutEquationVisitor<? extends T>)visitor).visitOutput_pin(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Output_pinContext output_pin() throws RecognitionException {
		Output_pinContext _localctx = new Output_pinContext(_ctx, getState());
		enterRule(_localctx, 6, RULE_output_pin);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(37);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << INPUT) | (1L << CONST_VALUE) | (1L << OUTPUT_PIN))) != 0)) ) {
			_errHandler.recoverInline(this);
			} else {
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Lut_valueContext extends ParserRuleContext {
		public Init_stringContext init_string() {
			return getRuleContext(Init_stringContext.class,0);
		}
		public Static_valueContext static_value() {
			return getRuleContext(Static_valueContext.class,0);
		}
		public Equation_valueContext equation_value() {
			return getRuleContext(Equation_valueContext.class,0);
		}
		public Lut_valueContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_lut_value; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof LutEquationVisitor ) return ((LutEquationVisitor<? extends T>)visitor).visitLut_value(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Lut_valueContext lut_value() throws RecognitionException {
		Lut_valueContext _localctx = new Lut_valueContext(_ctx, getState());
		enterRule(_localctx, 8, RULE_lut_value);
		try {
			setState(42);
			switch ( getInterpreter().adaptivePredict(_input,0,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(39);
				init_string();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(40);
				static_value();
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(41);
				equation_value();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Static_valueContext extends ParserRuleContext {
		public Static_valueContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_static_value; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof LutEquationVisitor ) return ((LutEquationVisitor<? extends T>)visitor).visitStatic_value(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Static_valueContext static_value() throws RecognitionException {
		Static_valueContext _localctx = new Static_valueContext(_ctx, getState());
		enterRule(_localctx, 10, RULE_static_value);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(44);
			_la = _input.LA(1);
			if ( !(_la==T__6 || _la==T__7) ) {
			_errHandler.recoverInline(this);
			} else {
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Init_stringContext extends ParserRuleContext {
		public TerminalNode CONST_VALUE() { return getToken(LutEquationParser.CONST_VALUE, 0); }
		public Init_stringContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_init_string; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof LutEquationVisitor ) return ((LutEquationVisitor<? extends T>)visitor).visitInit_string(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Init_stringContext init_string() throws RecognitionException {
		Init_stringContext _localctx = new Init_stringContext(_ctx, getState());
		enterRule(_localctx, 12, RULE_init_string);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(46);
			match(CONST_VALUE);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Equation_valueContext extends ParserRuleContext {
		public EquationContext equation() {
			return getRuleContext(EquationContext.class,0);
		}
		public Equation_valueContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_equation_value; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof LutEquationVisitor ) return ((LutEquationVisitor<? extends T>)visitor).visitEquation_value(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Equation_valueContext equation_value() throws RecognitionException {
		Equation_valueContext _localctx = new Equation_valueContext(_ctx, getState());
		enterRule(_localctx, 14, RULE_equation_value);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(48);
			equation();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class EquationContext extends ParserRuleContext {
		public Binary_eqnContext binary_eqn() {
			return getRuleContext(Binary_eqnContext.class,0);
		}
		public EquationContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_equation; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof LutEquationVisitor ) return ((LutEquationVisitor<? extends T>)visitor).visitEquation(this);
			else return visitor.visitChildren(this);
		}
	}

	public final EquationContext equation() throws RecognitionException {
		EquationContext _localctx = new EquationContext(_ctx, getState());
		enterRule(_localctx, 16, RULE_equation);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(50);
			binary_eqn();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Binary_eqnContext extends ParserRuleContext {
		public Binary_eqnContext left_eqn;
		public Binary_eqnContext right_eqn;
		public InputContext input() {
			return getRuleContext(InputContext.class,0);
		}
		public Static_valueContext static_value() {
			return getRuleContext(Static_valueContext.class,0);
		}
		public List<Binary_eqnContext> binary_eqn() {
			return getRuleContexts(Binary_eqnContext.class);
		}
		public Binary_eqnContext binary_eqn(int i) {
			return getRuleContext(Binary_eqnContext.class,i);
		}
		public Binary_opContext binary_op() {
			return getRuleContext(Binary_opContext.class,0);
		}
		public Binary_eqnContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_binary_eqn; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof LutEquationVisitor ) return ((LutEquationVisitor<? extends T>)visitor).visitBinary_eqn(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Binary_eqnContext binary_eqn() throws RecognitionException {
		Binary_eqnContext _localctx = new Binary_eqnContext(_ctx, getState());
		enterRule(_localctx, 18, RULE_binary_eqn);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(58);
			switch (_input.LA(1)) {
			case INV:
			case INPUT:
				{
				setState(52);
				input();
				}
				break;
			case T__6:
			case T__7:
				{
				setState(53);
				static_value();
				}
				break;
			case T__8:
				{
				setState(54);
				match(T__8);
				setState(55);
				((Binary_eqnContext)_localctx).left_eqn = binary_eqn();
				setState(56);
				match(T__9);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			setState(63);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << AND) | (1L << OR) | (1L << XOR))) != 0)) {
				{
				setState(60);
				binary_op();
				setState(61);
				((Binary_eqnContext)_localctx).right_eqn = binary_eqn();
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Binary_opContext extends ParserRuleContext {
		public TerminalNode AND() { return getToken(LutEquationParser.AND, 0); }
		public TerminalNode OR() { return getToken(LutEquationParser.OR, 0); }
		public TerminalNode XOR() { return getToken(LutEquationParser.XOR, 0); }
		public Binary_opContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_binary_op; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof LutEquationVisitor ) return ((LutEquationVisitor<? extends T>)visitor).visitBinary_op(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Binary_opContext binary_op() throws RecognitionException {
		Binary_opContext _localctx = new Binary_opContext(_ctx, getState());
		enterRule(_localctx, 20, RULE_binary_op);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(65);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << AND) | (1L << OR) | (1L << XOR))) != 0)) ) {
			_errHandler.recoverInline(this);
			} else {
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class InputContext extends ParserRuleContext {
		public TerminalNode INPUT() { return getToken(LutEquationParser.INPUT, 0); }
		public TerminalNode INV() { return getToken(LutEquationParser.INV, 0); }
		public InputContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_input; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof LutEquationVisitor ) return ((LutEquationVisitor<? extends T>)visitor).visitInput(this);
			else return visitor.visitChildren(this);
		}
	}

	public final InputContext input() throws RecognitionException {
		InputContext _localctx = new InputContext(_ctx, getState());
		enterRule(_localctx, 22, RULE_input);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(68);
			_la = _input.LA(1);
			if (_la==INV) {
				{
				setState(67);
				match(INV);
				}
			}

			setState(70);
			match(INPUT);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static final String _serializedATN =
		"\3\u0430\ud6d1\u8206\uad2d\u4417\uaef1\u8d80\uaadd\3\23K\4\2\t\2\4\3\t"+
		"\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4\13\t\13\4"+
		"\f\t\f\4\r\t\r\3\2\3\2\3\2\3\2\3\2\3\2\3\2\3\2\3\3\3\3\3\3\3\4\3\4\3\5"+
		"\3\5\3\6\3\6\3\6\5\6-\n\6\3\7\3\7\3\b\3\b\3\t\3\t\3\n\3\n\3\13\3\13\3"+
		"\13\3\13\3\13\3\13\5\13=\n\13\3\13\3\13\3\13\5\13B\n\13\3\f\3\f\3\r\5"+
		"\rG\n\r\3\r\3\r\3\r\2\2\16\2\4\6\b\n\f\16\20\22\24\26\30\2\6\3\2\6\b\3"+
		"\2\21\23\3\2\t\n\3\2\16\20D\2\32\3\2\2\2\4\"\3\2\2\2\6%\3\2\2\2\b\'\3"+
		"\2\2\2\n,\3\2\2\2\f.\3\2\2\2\16\60\3\2\2\2\20\62\3\2\2\2\22\64\3\2\2\2"+
		"\24<\3\2\2\2\26C\3\2\2\2\30F\3\2\2\2\32\33\7\3\2\2\33\34\5\6\4\2\34\35"+
		"\7\4\2\2\35\36\5\b\5\2\36\37\7\5\2\2\37 \5\n\6\2 !\7\2\2\3!\3\3\2\2\2"+
		"\"#\5\22\n\2#$\7\2\2\3$\5\3\2\2\2%&\t\2\2\2&\7\3\2\2\2\'(\t\3\2\2(\t\3"+
		"\2\2\2)-\5\16\b\2*-\5\f\7\2+-\5\20\t\2,)\3\2\2\2,*\3\2\2\2,+\3\2\2\2-"+
		"\13\3\2\2\2./\t\4\2\2/\r\3\2\2\2\60\61\7\22\2\2\61\17\3\2\2\2\62\63\5"+
		"\22\n\2\63\21\3\2\2\2\64\65\5\24\13\2\65\23\3\2\2\2\66=\5\30\r\2\67=\5"+
		"\f\7\289\7\13\2\29:\5\24\13\2:;\7\f\2\2;=\3\2\2\2<\66\3\2\2\2<\67\3\2"+
		"\2\2<8\3\2\2\2=A\3\2\2\2>?\5\26\f\2?@\5\24\13\2@B\3\2\2\2A>\3\2\2\2AB"+
		"\3\2\2\2B\25\3\2\2\2CD\t\5\2\2D\27\3\2\2\2EG\7\r\2\2FE\3\2\2\2FG\3\2\2"+
		"\2GH\3\2\2\2HI\7\21\2\2I\31\3\2\2\2\6,<AF";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}