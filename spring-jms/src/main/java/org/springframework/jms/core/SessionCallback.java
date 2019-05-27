package org.springframework.jms.core;

import javax.jms.JMSException;
import javax.jms.Session;

/**
 * 在提供的{@link Session}上执行任意数量的操作的回调.
 *
 * <p>与{@link JmsTemplate#execute(SessionCallback)}方法一起使用, 通常实现为匿名内部类或lambda表达式.
 */
public interface SessionCallback<T> {

	/**
	 * 对提供的JMS {@link Session}执行任意数量的操作, 可能返回结果.
	 * 
	 * @param session the JMS {@code Session}
	 * 
	 * @return 使用{@code Session}的结果对象 (或{@code null})
	 * @throws javax.jms.JMSException 如果由JMS API方法抛出
	 */
	T doInJms(Session session) throws JMSException;

}
