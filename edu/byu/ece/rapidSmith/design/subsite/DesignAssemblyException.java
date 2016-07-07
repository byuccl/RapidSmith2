package edu.byu.ece.rapidSmith.design.subsite;

/**
 *
 */
public class DesignAssemblyException extends RuntimeException {
	public DesignAssemblyException() {
		super();
	}

	public DesignAssemblyException(String message) {
		super(message);
	}

	public DesignAssemblyException(String message, Throwable cause) {
		super(message, cause);
	}

	public DesignAssemblyException(Throwable cause) {
		super(cause);
	}

	protected DesignAssemblyException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}
