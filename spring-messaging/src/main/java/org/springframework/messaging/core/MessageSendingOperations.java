package org.springframework.messaging.core;

import java.util.Map;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;

/**
 * 将消息发送到目标的操作.
 *
 * @param <D> 要向其发送消息的目标的类型
 */
public interface MessageSendingOperations<D> {

	/**
	 * 将消息发送到默认目标.
	 * 
	 * @param message 要发送的消息
	 */
	void send(Message<?> message) throws MessagingException;

	/**
	 * 发送消息到指定目标.
	 * 
	 * @param destination 目标
	 * @param message 要发送的消息
	 */
	void send(D destination, Message<?> message) throws MessagingException;

	/**
	 * 将给定的对象转换为序列化形式, 可能使用{@link org.springframework.messaging.converter.MessageConverter},
	 * 将其包装为消息并将其发送到默认目标.
	 * 
	 * @param payload 要用作有效负载的Object
	 */
	void convertAndSend(Object payload) throws MessagingException;

	/**
	 * 将给定的对象转换为序列化形式, 可能使用{@link org.springframework.messaging.converter.MessageConverter},
	 * 将其包装为消息并将其发送到给定目标.
	 * 
	 * @param destination 目标
	 * @param payload 要用作有效负载的Object
	 */
	void convertAndSend(D destination, Object payload) throws MessagingException;

	/**
	 * 将给定的对象转换为序列化形式, 可能使用{@link org.springframework.messaging.converter.MessageConverter},
	 * 将其包装为带有给定header的消息, 并将其发送到给定目标.
	 * 
	 * @param destination 目标
	 * @param payload 要用作有效负载的Object
	 * @param headers 要发送的消息的header
	 */
	void convertAndSend(D destination, Object payload, Map<String, Object> headers) throws MessagingException;

	/**
	 * 将给定的对象转换为序列化形式, 可能使用{@link org.springframework.messaging.converter.MessageConverter},
	 * 将其包装为消息, 应用给定的后处理器, 并将生成的消息发送到默认目标.
	 * 
	 * @param payload 要用作有效负载的Object
	 * @param postProcessor 要应用于消息的后处理器
	 */
	void convertAndSend(Object payload, MessagePostProcessor postProcessor) throws MessagingException;

	/**
	 * 将给定的对象转换为序列化形式, 可能使用{@link org.springframework.messaging.converter.MessageConverter},
	 * 将其包装为消息, 应用给定的后处理器, 并将结果消息发送到给定目标.
	 * 
	 * @param destination 目标
	 * @param payload 要用作有效负载的Object
	 * @param postProcessor 要应用于消息的后处理器
	 */
	void convertAndSend(D destination, Object payload, MessagePostProcessor postProcessor) throws MessagingException;

	/**
	 * 将给定的对象转换为序列化形式, 可能使用{@link org.springframework.messaging.converter.MessageConverter},
	 * 将其包装为具有给定header的消息, 应用给定的后处理器, 并将结果消息发送到给定目标.
	 * 
	 * @param destination 目标
	 * @param payload 要用作有效负载的Object
	 * @param headers 要发送的消息的header
	 * @param postProcessor 要应用于消息的后处理器
	 */
	void convertAndSend(D destination, Object payload, Map<String, Object> headers, MessagePostProcessor postProcessor)
			throws MessagingException;

}
