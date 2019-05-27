package org.springframework.jms.core;

import javax.jms.JMSException;
import javax.jms.QueueBrowser;
import javax.jms.Session;

/**
 * 用于浏览JMS队列中的消息的回调.
 *
 * <p>与{@link JmsTemplate}的回调方法一起使用, 该方法采用{@link BrowserCallback}参数, 通常实现为匿名内部类或lambda表达式.
 */
public interface BrowserCallback<T> {

	/**
	 * 对给定的{@link javax.jms.Session}和{@link javax.jms.QueueBrowser}执行操作.
	 * 
	 * @param session 要使用的JMS {@code Session}对象
	 * @param browser 要使用的JMS {@code QueueBrowser}对象
	 * 
	 * @return 使用{@code Session}的结果对象 (或{@code null})
	 * @throws javax.jms.JMSException 如果由JMS API方法抛出
	 */
	T doInJms(Session session, QueueBrowser browser) throws JMSException;

}
