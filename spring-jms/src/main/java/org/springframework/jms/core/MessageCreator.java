package org.springframework.jms.core;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;

/**
 * 给定{@link Session}创建JMS消息.
 *
 * <p>{@code Session}通常由{@link JmsTemplate}类的实例提供.
 *
 * <p>实现<i>不</i>需要关注可能从他们尝试的操作中抛出的受检异常{@code JMSExceptions} (来自'{@code javax.jms}'包).
 * {@code JmsTemplate}将适当地捕获并处理这些{@code JMSExceptions}.
 */
public interface MessageCreator {

	/**
	 * 创建要发送的{@link Message}.
	 * 
	 * @param session 用于创建{@code Message}的JMS {@link Session} (never {@code null})
	 * 
	 * @return 要发送的{@code Message}
	 * @throws javax.jms.JMSException 如果由JMS API方法抛出
	 */
	Message createMessage(Session session) throws JMSException;

}
