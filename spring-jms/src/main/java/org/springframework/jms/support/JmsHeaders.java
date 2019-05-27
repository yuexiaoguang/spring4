package org.springframework.jms.support;

/**
 * 用于从/向通用消息header设置和/或检索JMS属性的预定义名称和前缀.
 */
public interface JmsHeaders {

	/**
	 * 用于JMS API相关header的前缀, 以区别于用户定义的header和其他内部header (e.g. correlationId).
	 */
	String PREFIX = "jms_";

	/**
	 * 消息的关联ID. 这可能是此消息回复的消息的{@link #MESSAGE_ID}.
	 * 它也可以是特定于应用程序的标识符.
	 */
	String CORRELATION_ID = PREFIX + "correlationId";

	/**
	 * 消息的目标 (topic or queue)的名称.
	 * <p>只读值.
	 */
	String DESTINATION = PREFIX + "destination";

	/**
	 * 传递模式.
	 * <p>只读值.
	 */
	String DELIVERY_MODE = PREFIX + "deliveryMode";

	/**
	 * 消息到期日期和时间.
	 * <p>只读值.
	 */
	String EXPIRATION = PREFIX + "expiration";

	/**
	 * 消息的唯一标识符.
	 * <p>只读值.
	 */
	String MESSAGE_ID = PREFIX + "messageId";

	/**
	 * 消息优先级.
	 * <p>只读值.
	 */
	String PRIORITY = PREFIX + "priority";

	/**
	 * 应将消息回复发送到的目标 (topic or queue)的名称.
	 */
	String REPLY_TO = PREFIX + "replyTo";

	/**
	 * 指定是否重新发送消息.
	 * 当消息消费者未能确认消息接收时, 会发生这种情况.
	 * <p>只读值.
	 */
	String REDELIVERED = PREFIX + "redelivered";

	/**
	 * 消息类型标签.
	 * 此类型是以函数方式描述消息的字符串值.
	 */
	String TYPE = PREFIX + "type";

	/**
	 * 消息发送操作的日期和时间.
	 * <p>只读值.
	 */
	String TIMESTAMP = PREFIX + "timestamp";

}
