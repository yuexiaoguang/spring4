package org.aopalliance.aop;

/**
 * 所有AOP基础架构异常的超类.
 * 非受检异常, 因为这样的异常是致命的，不应强迫最终用户代码捕获它们.
 */
@SuppressWarnings("serial")
public class AspectException extends RuntimeException {

	public AspectException(String message) {
		super(message);
	}

	public AspectException(String message, Throwable cause) {
		super(message, cause);
	}
}
