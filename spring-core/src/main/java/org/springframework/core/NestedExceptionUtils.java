package org.springframework.core;

/**
 * Helper类, 用于实现能够保存嵌套异常的异常类.
 * 必要, 因为不能在不同的异常类型之间共享基类.
 *
 * <p>主要用于框架内.
 */
public abstract class NestedExceptionUtils {

	public static String buildMessage(String message, Throwable cause) {
		if (cause == null) {
			return message;
		}
		StringBuilder sb = new StringBuilder(64);
		if (message != null) {
			sb.append(message).append("; ");
		}
		sb.append("nested exception is ").append(cause);
		return sb.toString();
	}

	public static Throwable getRootCause(Throwable original) {
		if (original == null) {
			return null;
		}
		Throwable rootCause = null;
		Throwable cause = original.getCause();
		while (cause != null && cause != rootCause) {
			rootCause = cause;
			cause = cause.getCause();
		}
		return rootCause;
	}

	/**
	 * 检索给定异常的最具体原因, 即最内层原因 (根本原因)或异常本身.
	 * <p>与{@link #getRootCause}不同之处在于, 如果没有根本原因, 它会回退到原始异常.
	 * 
	 * @param original 要内省的原始异常
	 * 
	 * @return 最具体原因 (never {@code null})
	 */
	public static Throwable getMostSpecificCause(Throwable original) {
		Throwable rootCause = getRootCause(original);
		return (rootCause != null ? rootCause : original);
	}

}
