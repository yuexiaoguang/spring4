package org.springframework.expression;

/**
 * 如果访问器出现意外问题, 则抛出AccessException.
 */
@SuppressWarnings("serial")
public class AccessException extends Exception {

	public AccessException(String message) {
		super(message);
	}

	public AccessException(String message, Exception cause) {
		super(message, cause);
	}

}
