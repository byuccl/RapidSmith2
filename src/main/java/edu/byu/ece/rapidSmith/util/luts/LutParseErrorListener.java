package edu.byu.ece.rapidSmith.util.luts;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;

/**
* Created by Haroldsen on 3/16/2015.
*/ // For the parser
public class LutParseErrorListener extends BaseErrorListener {
	@Override
	public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e) {
		throw new LutParseException("line " + line + ":" + charPositionInLine + " " + msg);
	}
}
