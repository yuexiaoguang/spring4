package org.springframework.messaging.core;

import java.util.Map;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;

/**
 * 扩展{@link MessageSendingOperations}, 并添加将消息发送到指定为(可解析的)字符串名称的目标的操作.
 */
public interface DestinationResolvingMessageSendingOperations<D> extends MessageSendingOperations<D> {

	/**
	 * 将给定目标名称解析为目标, 并向其发送消息.
	 * 
	 * @param destinationName 要解析的目标名称
	 * @param message 要发送的消息
	 */
	void send(String destinationName, Message<?> message) throws MessagingException;

	/**
	 * 将给定目标名称解析为目标, 将有效负载对象转换为序列化形式,
	 * 可能使用{@link org.springframework.messaging.converter.MessageConverter},
	 * 将其包装为消息并将其发送到已解析的目标.
	 * 
	 * @param destinationName 要解析的目标名称
   	 * @param payload 要用作有效负载的Object
	 */
	<T> void convertAndSend(String destinationName, T payload) throws MessagingException;

	/**
	 * 将给定目标名称解析为目标, 将有效负载对象转换为序列化形式,
	 * 可能使用{@link org.springframework.messaging.converter.MessageConverter},
	 * 将其包装为带有给定header的消息并将其发送到已解析的目标.
	 * 
	 * @param destinationName 要解析的目标名称
	 * @param payload 要用作有效负载的Object
 	 * @param headers 要发送的消息的header
	 */
	<T> void convertAndSend(String destinationName, T payload, Map<String, Object> headers)
			throws MessagingException;

	/**
	 * 将给定目标名称解析为目标, 将有效负载对象转换为序列化形式,
	 * 可能使用{@link org.springframework.messaging.converter.MessageConverter},
	 * 将其包装为消息, 应用给定的后处理器, 并将生成的消息发送到已解析的目标.
	 * 
	 * @param destinationName 要解析的目标名称
	 * @param payload 要用作有效负载的Object
	 * @param postProcessor 要应用于消息的后处理器
	 */
	<T> void convertAndSend(String destinationName, T payload, MessagePostProcessor postProcessor)
			throws MessagingException;

	/**
	 * 将给定目标名称解析为目标, 将有效负载对象转换为序列化形式,
	 * 可能使用{@link org.springframework.messaging.converter.MessageConverter},
	 * 将其包装为具有给定header的消息, 应用给定的后处理器, 并将结果消息发送到已解析的目标.
	 * 
	 * @param destinationName 要解析的目标名称
	 * @param payload 要用作有效负载的Object
	 * @param headers 要发送的消息的header
	 * @param postProcessor 要应用于消息的后处理器
	 */
	<T> void convertAndSend(String destinationName, T payload, Map<String, Object> headers,
			MessagePostProcessor postProcessor) throws MessagingException;

}
