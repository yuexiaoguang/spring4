package org.springframework.expression;

/**
 * 表示在表达式解析期间发生的异常.
 */
@SuppressWarnings("serial")
public class ParseException extends ExpressionException {

	/**
	 * @param expressionString 无法解析的表达式字符串
	 * @param position 表达式字符串中发生问题的位置
	 * @param message 发生的问题的描述
	 */
	public ParseException(String expressionString, int position, String message) {
		super(expressionString, position, message);
	}

	/**
	 * @param position 表达式字符串中发生问题的位置
	 * @param message 发生的问题的描述
	 * @param cause 这个异常的根本原因
	 */
	public ParseException(int position, String message, Throwable cause) {
		super(position, message, cause);
	}

	/**
	 * @param position 表达式字符串中发生问题的位置
	 * @param message 发生的问题的描述
	 */
	public ParseException(int position, String message) {
		super(position, message);
	}

}
