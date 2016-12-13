
package edu.byu.ece.rapidSmith.util;

/**
 * This class holds general runtime exceptions that may be useful for RapidSmith 
 *  
 * @author Thomas Townsend
 *
 */
public class Exceptions {

	/**
	 * Exception thrown if an unexpected state is encountered while parsing a file 
	 * 
	 * @author Thomas Townsend
	 */
	public static class ParseException extends RuntimeException {
		public ParseException() {
			super();
		}
		
		public ParseException(String message) {
			super(message);
		}
		
		public ParseException(String message, Throwable cause) {
			super(message, cause);
		}
		
		public ParseException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
			super(message, cause, enableSuppression, writableStackTrace);
		}
		
		public ParseException(Throwable cause) {
			super(cause);
		}
	}

	/**
	 * Exception caused by an improperly initialized RapidSmith environment.
	 */
	public static class EnvironmentException extends RuntimeException {
		public EnvironmentException(String message) {
			super(message);
		}

	}

	/**
	 *
	 */
	public static class DesignAssemblyException extends RuntimeException {
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
}
