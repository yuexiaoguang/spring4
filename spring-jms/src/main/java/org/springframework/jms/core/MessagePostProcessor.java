package org.springframework.jms.core;

import javax.jms.JMSException;
import javax.jms.Message;

/**
 * 与JmsTemplate的将对象转换为消息的send方法一起使用.
 * 它允许在转换器处理完消息后进一步修改消息.
 * 这对于设置JMS Header和属性很有用.
 *
 * <p>这通常作为方法实现中的匿名类.
 */
public interface MessagePostProcessor {

	/**
	 * 将MessagePostProcessor应用于消息. 返回的消息通常是原始的修改版本.
	 * 
	 * @param message 来自MessageConverter的JMS消息
	 * 
	 * @return Message的修改版本
	 * @throws javax.jms.JMSException 如果由JMS API方法抛出
	 */
	Message postProcessMessage(Message message) throws JMSException;

}
