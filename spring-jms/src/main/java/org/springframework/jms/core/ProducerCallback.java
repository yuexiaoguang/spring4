package org.springframework.jms.core;

import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Session;

/**
 * 用于将消息发送到JMS目标的回调.
 *
 * <p>与{@link JmsTemplate}的回调方法一起使用, 该方法采用{@link ProducerCallback}参数, 通常实现为匿名内部类或lambda表达式.
 *
 * <p>典型的实现将在提供的JMS {@link Session}和{@link MessageProducer}上执行多个操作.
 */
public interface ProducerCallback<T> {

	/**
	 * 在给定的{@link Session}和{@link MessageProducer}上执行操作.
	 * <p>除非在JmsTemplate调用中指定, 否则消息生产者不与任何目标关联.
	 * 
	 * @param session 要使用的JMS {@code Session}对象
	 * @param producer 要使用的JMS {@code MessageProducer}对象
	 * 
	 * @return 使用{@code Session}的结果对象 (或{@code null})
	 * @throws javax.jms.JMSException 如果由JMS API方法抛出
	 */
	T doInJms(Session session, MessageProducer producer) throws JMSException;

}
