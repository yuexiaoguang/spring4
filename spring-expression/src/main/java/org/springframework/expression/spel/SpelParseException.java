package org.springframework.expression.spel;

import org.springframework.expression.ParseException;

/**
 * Spring EL相关异常的根异常.
 * 它不是保存指示问题的硬编码字符串, 而是记录消息key和消息的插入.
 * 有关可能发生的所有可能消息的列表, 请参阅{@link SpelMessage}.
 */
@SuppressWarnings("serial")
public class SpelParseException extends ParseException {

	private final SpelMessage message;

	private final Object[] inserts;


	public SpelParseException(String expressionString, int position, SpelMessage message, Object... inserts) {
		super(expressionString, position, message.formatMessage(inserts));
		this.message = message;
		this.inserts = inserts;
	}

	public SpelParseException(int position, SpelMessage message, Object... inserts) {
		super(position, message.formatMessage(inserts));
		this.message = message;
		this.inserts = inserts;
	}

	public SpelParseException(int position, Throwable cause, SpelMessage message, Object... inserts) {
		super(position, message.formatMessage(inserts), cause);
		this.message = message;
		this.inserts = inserts;
	}


	/**
	 * 返回消息代码.
	 */
	public SpelMessage getMessageCode() {
		return this.message;
	}

	/**
	 * 返回消息插入.
	 */
	public Object[] getInserts() {
		return this.inserts;
	}

}
