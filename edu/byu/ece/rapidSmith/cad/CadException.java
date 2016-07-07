package edu.byu.ece.rapidSmith.cad;

/**
 *
 */
public class CadException extends RuntimeException {
	public CadException() {
		super();
	}

	public CadException(String message) {
		super(message);
	}

	public CadException(String message, Throwable cause) {
		super(message, cause);
	}

	public CadException(Throwable cause) {
		super(cause);
	}

	protected CadException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}
