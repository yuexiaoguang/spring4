package org.springframework.messaging.converter;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;

/**
 * 带有转换提示支持的{@link MessageConverter} SPI扩展.
 *
 * <p>如果提供转换提示, 而且转换器实现了此接口, 框架将调用这些扩展方法, 而不是调用常规{@code fromMessage} / {@code toMessage}变体.
 */
public interface SmartMessageConverter extends MessageConverter {

	/**
	 * {@link #fromMessage(Message, Class)}的变体, 它将额外的转换上下文作为参数, 允许考虑负载参数上的注解.
	 * 
	 * @param message 输入消息
	 * @param targetClass 转换的目标类
	 * @param conversionHint 传递给{@link MessageConverter}的额外对象, e.g. 相关的{@code MethodParameter} (may be {@code null}}
	 * 
	 * @return 转换的结果, 或{@code null}如果转换器无法执行转换
	 */
	Object fromMessage(Message<?> message, Class<?> targetClass, Object conversionHint);

	/**
	 * {@link #toMessage(Object, MessageHeaders)}的变体, 它将额外的转换上下文作为参数, 允许考虑负载参数上的注解.
	 * 
	 * @param payload 要转换的Object
	 * @param headers 消息可选的header (may be {@code null})
	 * @param conversionHint 传递给{@link MessageConverter}的额外对象, e.g. 相关的{@code MethodParameter} (may be {@code null}}
	 * 
	 * @return 新消息, 或{@code null} 如果转换器不支持对象类型或目标媒体类型
	 */
	Message<?> toMessage(Object payload, MessageHeaders headers, Object conversionHint);

}
