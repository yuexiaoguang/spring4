package org.springframework.expression.spel;

/**
 * 包装一个真正的解析异常.
 * 此异常流向顶部解析方法, 然后将包装的异常作为真正的问题抛出.
 */
@SuppressWarnings("serial")
public class InternalParseException extends RuntimeException {

	public InternalParseException(SpelParseException cause) {
		super(cause);
	}

	@Override
	public SpelParseException getCause() {
		return (SpelParseException) super.getCause();
	}

}
