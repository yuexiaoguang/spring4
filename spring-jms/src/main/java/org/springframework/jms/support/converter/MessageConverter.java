package org.springframework.jms.support.converter;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;

/**
 * 策略接口, 指定Java对象和JMS消息之间的转换器.
 *
 * <p>查看{@link SimpleMessageConverter}以获取默认实现, 在'标准'消息有效负载和JMS消息类型之间进行转换.
 */
public interface MessageConverter {

	/**
	 * 使用提供的会话将Java对象转换为JMS Message以创建消息对象.
	 * 
	 * @param object 要转换的对象
	 * @param session 用于创建JMS消息的会话
	 * 
	 * @return the JMS Message
	 * @throws javax.jms.JMSException 如果由JMS API方法抛出
	 * @throws MessageConversionException 如果转换失败
	 */
	Message toMessage(Object object, Session session) throws JMSException, MessageConversionException;

	/**
	 * 从JMS Message转换为Java对象.
	 * 
	 * @param message 要转换的消息
	 * 
	 * @return 转换后的Java对象
	 * @throws javax.jms.JMSException 如果由JMS API方法抛出
	 * @throws MessageConversionException 如果转换失败
	 */
	Object fromMessage(Message message) throws JMSException, MessageConversionException;

}
