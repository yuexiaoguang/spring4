package org.springframework.jms.support.destination;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.Topic;

import org.springframework.util.Assert;

/**
 * 简单的{@link DestinationResolver}实现, 将目标名称解析为动态目标.
 */
public class DynamicDestinationResolver implements DestinationResolver {

	/**
	 * 将指定的目标名称解析为动态目标.
	 * 
	 * @param session 当前JMS Session
	 * @param destinationName 目标名称
	 * @param pubSubDomain {@code true}如果域是pub-sub, {@code false}如果是 P2P
	 * 
	 * @return JMS目标 (主题或队列)
	 * @throws javax.jms.JMSException 如果解析失败
	 */
	@Override
	public Destination resolveDestinationName(Session session, String destinationName, boolean pubSubDomain)
			throws JMSException {

		Assert.notNull(session, "Session must not be null");
		Assert.notNull(destinationName, "Destination name must not be null");
		if (pubSubDomain) {
			return resolveTopic(session, destinationName);
		}
		else {
			return resolveQueue(session, destinationName);
		}
	}


	/**
	 * 将给定的目标名称解析为{@link Topic}.
	 * 
	 * @param session 当前JMS Session
	 * @param topicName 所需{@link Topic}的名称
	 * 
	 * @return the JMS {@link Topic}
	 * @throws javax.jms.JMSException 如果解析失败
	 */
	protected Topic resolveTopic(Session session, String topicName) throws JMSException {
		return session.createTopic(topicName);
	}

	/**
	 * 将给定的目标名称解析为{@link Queue}.
	 * 
	 * @param session 当前JMS Session
	 * @param queueName 所需{@link Queue}的名称
	 * 
	 * @return the JMS {@link Queue}
	 * @throws javax.jms.JMSException 如果解析失败
	 */
	protected Queue resolveQueue(Session session, String queueName) throws JMSException {
		return session.createQueue(queueName);
	}

}
