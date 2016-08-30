
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

		private static final long serialVersionUID = 1L;

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
}
