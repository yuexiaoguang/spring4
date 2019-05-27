package org.springframework.expression;

/**
 * 表示在表达式求值期间发生的异常.
 */
@SuppressWarnings("serial")
public class EvaluationException extends ExpressionException {

	public EvaluationException(String message) {
		super(message);
	}

	public EvaluationException(String message, Throwable cause) {
		super(message,cause);
	}

	/**
	 * @param position 表达式中出现问题的位置
	 * @param message 发生问题的描述
	 */
	public EvaluationException(int position, String message) {
		super(position, message);
	}

	/**
	 * @param expressionString 无法评估的表达式
	 * @param message 发生问题的描述
	 */
	public EvaluationException(String expressionString, String message) {
		super(expressionString, message);
	}

	/**
	 * @param position 表达式中出现问题的位置
	 * @param message 发生问题的描述
	 * @param cause 此异常的根本原因
	 */
	public EvaluationException(int position, String message, Throwable cause) {
		super(position, message, cause);
	}

}
