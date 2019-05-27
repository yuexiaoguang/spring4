package org.springframework.expression;

/**
 * 此异常包装 (作为原因) 由SpEL调用的某个方法抛出的受检异常.
 * 不同于SpelEvaluationException, 因为这表示被调用的方法被定义为抛出受检异常.
 * SpelEvaluationExceptions用于处理 (和包装) 意外异常.
 */
@SuppressWarnings("serial")
public class ExpressionInvocationTargetException extends EvaluationException {

	public ExpressionInvocationTargetException(int position, String message, Throwable cause) {
		super(position, message, cause);
	}

	public ExpressionInvocationTargetException(int position, String message) {
		super(position, message);
	}

	public ExpressionInvocationTargetException(String expressionString, String message) {
		super(expressionString, message);
	}

	public ExpressionInvocationTargetException(String message, Throwable cause) {
		super(message, cause);
	}

	public ExpressionInvocationTargetException(String message) {
		super(message);
	}

}
