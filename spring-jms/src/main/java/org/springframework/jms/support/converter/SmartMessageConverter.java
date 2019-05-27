package org.springframework.jms.support.converter;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;

/**
 * 带有转换提示支持的扩展{@link MessageConverter} SPI.
 *
 * <p>在提供转换提示的情况下, 如果转换器实现此接口, 框架将调用扩展方法, 而不是调用常规{@code toMessage}变体.
 */
public interface SmartMessageConverter extends MessageConverter {

	/**
	 * {@link #toMessage(Object, Session)}的变体, 它将额外的转换上下文作为参数, 允许考虑有效载荷参数上的注解.
	 * 
	 * @param object 要转换的对象
	 * @param session 用于创建JMS消息的会话
	 * @param conversionHint 传递给{@link MessageConverter}的额外对象, e.g. 关联的{@code MethodParameter} (may be {@code null}}
	 * 
	 * @return the JMS Message
	 * @throws javax.jms.JMSException 如果由JMS API方法抛出
	 * @throws MessageConversionException 转换失败
	 */
	Message toMessage(Object object, Session session, Object conversionHint)
			throws JMSException, MessageConversionException;

}
