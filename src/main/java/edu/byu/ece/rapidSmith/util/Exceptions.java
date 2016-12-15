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
		private static final long serialVersionUID = 9155970893540903136L;

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
		private static final long serialVersionUID = -7823708332406385920L;

		public EnvironmentException(String message) {
			super(message);
		}

		public EnvironmentException(String message, Throwable cause) {
			super(message, cause);
		}

		public EnvironmentException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
			super(message, cause, enableSuppression, writableStackTrace);
		}

		public EnvironmentException(Throwable cause) {
			super(cause);
		}
	}

	/**
	 *
	 */
	public static class DesignAssemblyException extends RuntimeException {
		private static final long serialVersionUID = -5103058575363941027L;

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
