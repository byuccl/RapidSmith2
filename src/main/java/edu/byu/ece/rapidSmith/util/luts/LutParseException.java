package edu.byu.ece.rapidSmith.util.luts;

/**
 * Created by Haroldsen on 3/14/2015.
 */
public class LutParseException extends RuntimeException {
	private static final long serialVersionUID = 7630112317695417852L;

	public LutParseException() {
		super();
	}

	public LutParseException(String message) {
		super(message);
	}

	public LutParseException(String message, Throwable cause) {
		super(message, cause);
	}

	public LutParseException(Throwable cause) {
		super(cause);
	}

	protected LutParseException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}
