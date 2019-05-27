package org.springframework.jms.listener;

import org.springframework.context.SmartLifecycle;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.destination.DestinationResolver;

/**
 * 表示消息监听器容器的框架使用的内部抽象.
 * 不应在外部实现, 同时支持JMS和JCA样式容器.
 */
public interface MessageListenerContainer extends SmartLifecycle {

	/**
	 * 设置要使用的消息监听器.
	 * 如果不支持该消息监听器类型, 则抛出{@link IllegalArgumentException}.
	 */
	void setupMessageListener(Object messageListener);

	/**
	 * 返回可用于转换{@link javax.jms.Message}的{@link MessageConverter}.
	 */
	MessageConverter getMessageConverter();

	/**
	 * 返回用于按名称解析目标的{@link DestinationResolver}.
	 */
	DestinationResolver getDestinationResolver();

	/**
	 * 返回是否使用Publish/Subscribe 域 ({@link javax.jms.Topic Topics}).
	 * 否则, 使用点对点域 ({@link javax.jms.Queue Queues}).
	 */
	boolean isPubSubDomain();

	/**
	 * 返回回复目标是否使用 Publish/Subscribe域 ({@link javax.jms.Topic Topics}).
	 * 否则, 使用点对点域 ({@link javax.jms.Queue Queues}).
	 * <p>默认情况下, 该值与{@link #isPubSubDomain()}相同.
	 */
	boolean isReplyPubSubDomain();

}
