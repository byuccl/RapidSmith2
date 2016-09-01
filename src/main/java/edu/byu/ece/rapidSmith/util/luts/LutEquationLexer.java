// Generated from C:/work/rapidsmith2/edu/byu/ece/rapidSmith/util/luts\LutEquation.g4 by ANTLR 4.5.1
package edu.byu.ece.rapidSmith.util.luts;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.misc.*;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class LutEquationLexer extends Lexer {
	static { RuntimeMetaData.checkVersion("4.5.1", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		T__0=1, T__1=2, T__2=3, T__3=4, T__4=5, T__5=6, T__6=7, T__7=8, T__8=9, 
		T__9=10, INV=11, AND=12, OR=13, XOR=14, INPUT=15, CONST_VALUE=16, OUTPUT_PIN=17;
	public static String[] modeNames = {
		"DEFAULT_MODE"
	};

	public static final String[] ruleNames = {
		"T__0", "T__1", "T__2", "T__3", "T__4", "T__5", "T__6", "T__7", "T__8", 
		"T__9", "INV", "AND", "OR", "XOR", "INPUT", "CONST_VALUE", "OUTPUT_PIN"
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


	public LutEquationLexer(CharStream input) {
		super(input);
		_interp = new LexerATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	@Override
	public String getGrammarFileName() { return "LutEquation.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public String[] getModeNames() { return modeNames; }

	@Override
	public ATN getATN() { return _ATN; }

	public static final String _serializedATN =
		"\3\u0430\ud6d1\u8206\uad2d\u4417\uaef1\u8d80\uaadd\2\23W\b\1\4\2\t\2\4"+
		"\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4\13\t"+
		"\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t\20\4\21\t\21\4\22\t\22"+
		"\3\2\3\2\3\3\3\3\3\4\3\4\3\5\3\5\3\5\3\5\3\6\3\6\3\6\3\6\3\7\3\7\3\7\3"+
		"\7\3\b\3\b\3\t\3\t\3\n\3\n\3\13\3\13\3\f\3\f\3\r\3\r\3\16\3\16\3\17\3"+
		"\17\3\20\3\20\3\20\3\21\3\21\3\21\3\21\6\21O\n\21\r\21\16\21P\3\22\6\22"+
		"T\n\22\r\22\16\22U\2\2\23\3\3\5\4\7\5\t\6\13\7\r\b\17\t\21\n\23\13\25"+
		"\f\27\r\31\16\33\17\35\20\37\21!\22#\23\3\2\6\4\2C\\c|\3\2\62;\5\2\62"+
		";CHch\5\2\62;C\\c|X\2\3\3\2\2\2\2\5\3\2\2\2\2\7\3\2\2\2\2\t\3\2\2\2\2"+
		"\13\3\2\2\2\2\r\3\2\2\2\2\17\3\2\2\2\2\21\3\2\2\2\2\23\3\2\2\2\2\25\3"+
		"\2\2\2\2\27\3\2\2\2\2\31\3\2\2\2\2\33\3\2\2\2\2\35\3\2\2\2\2\37\3\2\2"+
		"\2\2!\3\2\2\2\2#\3\2\2\2\3%\3\2\2\2\5\'\3\2\2\2\7)\3\2\2\2\t+\3\2\2\2"+
		"\13/\3\2\2\2\r\63\3\2\2\2\17\67\3\2\2\2\219\3\2\2\2\23;\3\2\2\2\25=\3"+
		"\2\2\2\27?\3\2\2\2\31A\3\2\2\2\33C\3\2\2\2\35E\3\2\2\2\37G\3\2\2\2!J\3"+
		"\2\2\2#S\3\2\2\2%&\7%\2\2&\4\3\2\2\2\'(\7<\2\2(\6\3\2\2\2)*\7?\2\2*\b"+
		"\3\2\2\2+,\7N\2\2,-\7W\2\2-.\7V\2\2.\n\3\2\2\2/\60\7T\2\2\60\61\7C\2\2"+
		"\61\62\7O\2\2\62\f\3\2\2\2\63\64\7T\2\2\64\65\7Q\2\2\65\66\7O\2\2\66\16"+
		"\3\2\2\2\678\7\62\2\28\20\3\2\2\29:\7\63\2\2:\22\3\2\2\2;<\7*\2\2<\24"+
		"\3\2\2\2=>\7+\2\2>\26\3\2\2\2?@\7\u0080\2\2@\30\3\2\2\2AB\7,\2\2B\32\3"+
		"\2\2\2CD\7-\2\2D\34\3\2\2\2EF\7B\2\2F\36\3\2\2\2GH\t\2\2\2HI\t\3\2\2I"+
		" \3\2\2\2JK\7\62\2\2KL\7z\2\2LN\3\2\2\2MO\t\4\2\2NM\3\2\2\2OP\3\2\2\2"+
		"PN\3\2\2\2PQ\3\2\2\2Q\"\3\2\2\2RT\t\5\2\2SR\3\2\2\2TU\3\2\2\2US\3\2\2"+
		"\2UV\3\2\2\2V$\3\2\2\2\5\2PU\2";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}