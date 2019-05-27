package org.springframework.jms.listener;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;

/**
 * 标准JMS {@link javax.jms.MessageListener}接口的变体, 不仅提供接收的Message, 还提供底层的JMS Session对象.
 * 后者可用于发送回复消息, 而无需访问外部Connection/Session, i.e. 无需访问底层ConnectionFactory.
 *
 * <p>由Spring的{@link DefaultMessageListenerContainer}和{@link SimpleMessageListenerContainer}提供支持,
 * 作为标准JMS MessageListener接口的直接替代方法.
 * 通常<i>不</i>由基于JCA的监听器容器支持:
 * 为获得最大兼容性, 请改为实现标准JMS MessageListener.
 */
public interface SessionAwareMessageListener<M extends Message> {

	/**
	 * 处理收到的JMS消息的回调.
	 * <p>实现者应该处理给定的Message, 通常通过给定的Session发送回复消息.
	 * 
	 * @param message 收到的JMS消息 (never {@code null})
	 * @param session 底层JMS Session (never {@code null})
	 * 
	 * @throws JMSException 如果由JMS方法抛出
	 */
	void onMessage(M message, Session session) throws JMSException;

}
