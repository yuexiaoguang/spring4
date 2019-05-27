package org.springframework.expression;

/**
 * 处理表达式时可能发生的异常的超类.
 */
@SuppressWarnings("serial")
public class ExpressionException extends RuntimeException {

	protected String expressionString;

	protected int position;  // -1 未知; 应该在所有合理的情况下都知道


	public ExpressionException(String message) {
		super(message);
	}

	public ExpressionException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * @param expressionString 表达式字符串
	 * @param message 描述性消息
	 */
	public ExpressionException(String expressionString, String message) {
		super(message);
		this.expressionString = expressionString;
		this.position = -1;
	}

	/**
	 * @param expressionString 表达式字符串
	 * @param position 表达式字符串中发生问题的位置
	 * @param message 描述性消息
	 */
	public ExpressionException(String expressionString, int position, String message) {
		super(message);
		this.expressionString = expressionString;
		this.position = position;
	}

	/**
	 * @param position 表达式字符串中发生问题的位置
	 * @param message 描述性消息
	 */
	public ExpressionException(int position, String message) {
		super(message);
		this.position = position;
	}

	/**
	 * @param position 表达式字符串中发生问题的位置
	 * @param message 描述性消息
	 * @param cause 此异常的根本原因
	 */
	public ExpressionException(int position, String message, Throwable cause) {
		super(message, cause);
		this.position = position;
	}


	/**
	 * 返回表达式字符串.
	 */
	public final String getExpressionString() {
		return this.expressionString;
	}

	/**
	 * 返回发生问题的表达式字符串中的位置.
	 */
	public final int getPosition() {
		return this.position;
	}

	/**
	 * 返回异常消息.
	 * 从Spring 4.0开始, 此方法返回与{@link #toDetailedString()}相同的结果.
	 */
	@Override
	public String getMessage() {
		return toDetailedString();
	}

	/**
	 * 返回此异常的详细描述, 包括表达式String和position, 以及实际异常消息.
	 */
	public String toDetailedString() {
		if (this.expressionString != null) {
			StringBuilder output = new StringBuilder();
			output.append("Expression [");
			output.append(this.expressionString);
			output.append("]");
			if (this.position >= 0) {
				output.append(" @");
				output.append(this.position);
			}
			output.append(": ");
			output.append(getSimpleMessage());
			return output.toString();
		}
		else {
			return getSimpleMessage();
		}
	}

	/**
	 * 返回异常简单消息, 不包括导致失败的表达式.
	 */
	public String getSimpleMessage() {
		return super.getMessage();
	}
}
