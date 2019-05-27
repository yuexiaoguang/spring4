package org.springframework.jms.support.destination;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Session;

/**
 * 用于解析JMS目标的策略接口.
 *
 * <p>由{@link org.springframework.jms.core.JmsTemplate}使用,
 * 用于解析从简单的{@link String Strings}到实际的{@link Destination}实现实例的目标名称.
 *
 * <p>{@link org.springframework.jms.core.JmsTemplate}实例
 * 使用的默认{@link DestinationResolver}实现是{@link DynamicDestinationResolver}类.
 * 考虑使用{@link JndiDestinationResolver}用于更多高级场景.
 */
public interface DestinationResolver {

	/**
	 * 解析给定的目标名称, 可以是已定位资源, 也可以是动态目标.
	 * 
	 * @param session 当前的JMS会话 (如果解析器实现能够在没有它的情况下工作, 则可能是{@code null})
	 * @param destinationName 目标的名称
	 * @param pubSubDomain {@code true}如果域是pub-sub, {@code false}如果是P2P
	 * 
	 * @return JMS目标 (主题或队列)
	 * @throws javax.jms.JMSException 如果JMS会话无法解析目标
	 * @throws DestinationResolutionException 如果一般的目标解析失败
	 */
	Destination resolveDestinationName(Session session, String destinationName, boolean pubSubDomain)
			throws JMSException;

}
