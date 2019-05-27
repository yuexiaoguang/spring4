package org.springframework.expression.spel;

import org.springframework.expression.EvaluationException;

/**
 * Spring EL相关异常的根异常.
 * 它不是保存指示问题的硬编码字符串, 而是记录消息key和消息的插入.
 * 有关可能发生的所有可能消息的列表, 请参阅{@link SpelMessage}.
 */
@SuppressWarnings("serial")
public class SpelEvaluationException extends EvaluationException {

	private final SpelMessage message;

	private final Object[] inserts;


	public SpelEvaluationException(SpelMessage message, Object... inserts) {
		super(message.formatMessage(inserts));
		this.message = message;
		this.inserts = inserts;
	}

	public SpelEvaluationException(int position, SpelMessage message, Object... inserts) {
		super(position, message.formatMessage(inserts));
		this.message = message;
		this.inserts = inserts;
	}

	public SpelEvaluationException(int position, Throwable cause, SpelMessage message, Object... inserts) {
		super(position, message.formatMessage(inserts), cause);
		this.message = message;
		this.inserts = inserts;
	}

	public SpelEvaluationException(Throwable cause, SpelMessage message, Object... inserts) {
		super(message.formatMessage(inserts), cause);
		this.message = message;
		this.inserts = inserts;
	}


	/**
	 * 在相关表达式中设置引起此异常的位置.
	 */
	public void setPosition(int position) {
		this.position = position;
	}

	/**
	 * 返回消息码.
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
