package edu.byu.ece.rapidSmith.util;
public class EnvironmentException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public EnvironmentException(String message) {
		super(message);
	}

	public EnvironmentException(String message, Throwable cause) {
		super(message, cause);
	}
}
