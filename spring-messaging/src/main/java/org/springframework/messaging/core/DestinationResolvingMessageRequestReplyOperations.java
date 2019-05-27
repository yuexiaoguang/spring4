package org.springframework.messaging.core;

import java.util.Map;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;

/**
 * 扩展{@link MessageRequestReplyOperations}, 并添加用于向指定为(可解析的)字符串名称的目标发送和接收消息的操作.
 */
public interface DestinationResolvingMessageRequestReplyOperations<D> extends MessageRequestReplyOperations<D> {

	/**
	 * 将给定目标名称解析为目标并发送给定消息, 接收回复并将其返回.
	 * 
	 * @param destinationName 目标的名称
	 * @param requestMessage 要发送的消息
	 * 
	 * @return 收到的消息, 或{@code null} 如果无法接收消息, 例如由于超时
	 */
	Message<?> sendAndReceive(String destinationName, Message<?> requestMessage) throws MessagingException;

	/**
	 * 解析给定的目标名称, 将有效负载请求对象转换为序列化形式,
	 * 可能使用{@link org.springframework.messaging.converter.MessageConverter},
	 * 将其包装为消息并将其发送到已解析的目标, 接收回复并将其主体转换为指定的目标类.
	 * 
	 * @param destinationName 目标的名称
	 * @param request 要发送的请求消息的有效负载
	 * @param targetClass 要将回复的有效负载转换为的目标类
	 *
	 * @return 回复消息转换后的有效负载, 或{@code null} 如果无法接收消息, 例如由于超时
	 */
	<T> T convertSendAndReceive(String destinationName, Object request, Class<T> targetClass)
			throws MessagingException;

	/**
	 * 解析给定的目标名称, 将有效负载请求对象转换为序列化形式,
	 * 可能使用{@link org.springframework.messaging.converter.MessageConverter},
	 * 将其包装为具有给定header的消息并将其发送到已解析的目标, 接收回复并将其主体转换为指定的目标类.
	 * 
	 * @param destinationName 目标的名称
	 * @param request 要发送的请求消息的有效负载
	 * @param headers 要发送的请求消息的header
	 * @param targetClass 要将回复的有效负载转换为的目标类
	 * 
	 * @return 回复消息转换后的有效负载, 或{@code null} 如果无法接收消息, 例如由于超时
	 */
	<T> T convertSendAndReceive(String destinationName, Object request, Map<String, Object> headers,
			Class<T> targetClass) throws MessagingException;

	/**
	 * 解析给定的目标名称, 将有效负载请求对象转换为序列化形式,
	 * 可能使用{@link org.springframework.messaging.converter.MessageConverter},
	 * 将其包装为消息, 应用给定的后期处理, 并将生成的消息发送到已解析的目标, 然后接收回复并将其主体转换为指定的目标类.
	 * 
	 * @param destinationName 目标的名称
	 * @param request 要发送的请求消息的有效负载
	 * @param targetClass 要将回复的有效负载转换为的目标类
	 * @param requestPostProcessor 请求消息的后处理
	 * 
	 * @return 回复消息转换后的有效负载, 或{@code null} 如果无法接收消息, 例如由于超时
	 */
	<T> T convertSendAndReceive(String destinationName, Object request,
			Class<T> targetClass, MessagePostProcessor requestPostProcessor) throws MessagingException;

	/**
	 * 解析给定的目标名称, 将有效负载请求对象转换为序列化形式,
	 * 可能使用{@link org.springframework.messaging.converter.MessageConverter},
	 * 将其包装为带有给定header的消息, 应用给定的后期处理, 并将生成的消息发送到已解析的目标, 然后接收回复并将其主体转换为指定的目标类.
	 * 
	 * @param destinationName 目标的名称
	 * @param request 要发送的请求消息的有效负载
	 * @param headers 要发送的请求消息的header
	 * @param targetClass 要将回复的有效负载转换为的目标类
	 * @param requestPostProcessor 请求消息的后处理
	 * 
	 * @return 回复消息转换后的有效负载, 或{@code null} 如果无法接收消息, 例如由于超时
	 */
	<T> T convertSendAndReceive(String destinationName, Object request, Map<String, Object> headers,
			Class<T> targetClass, MessagePostProcessor requestPostProcessor) throws MessagingException;

}
