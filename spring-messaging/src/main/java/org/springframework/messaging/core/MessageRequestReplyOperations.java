package org.springframework.messaging.core;

import java.util.Map;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;

/**
 * 向目标发送消息和从目标接收回复的操作.
 *
 * @param <D> 目标的类型
 */
public interface MessageRequestReplyOperations<D> {

	/**
	 * 发送请求消息并从默认目标接收回复.
	 * 
	 * @param requestMessage 要发送的消息
	 * 
	 * @return 回复, 或{@code null} 如果无法接收消息, 例如由于超时
	 */
	Message<?> sendAndReceive(Message<?> requestMessage) throws MessagingException;

	/**
	 * 发送请求消息并接收来自给定目标的回复.
	 * 
	 * @param destination 目标
	 * @param requestMessage 要发送的消息
	 * 
	 * @return 回复, 或{@code null} 如果无法接收消息, 例如由于超时
	 */
	Message<?> sendAndReceive(D destination, Message<?> requestMessage) throws MessagingException;

	/**
	 * 将给定的请求对象转换为序列化形式, 可能使用{@link org.springframework.messaging.converter.MessageConverter},
	 * 将其作为{@link Message}发送到默认目标, 接收回复并将其主体转换为指定的目标类.
	 * 
	 * @param request 要发送的请求消息的有效负载
	 * @param targetClass 要将回复的有效负载转换为的目标类型
	 * 
	 * @return 回复消息的有效负载, 或{@code null} 如果无法接收消息, 例如由于超时
	 */
	<T> T convertSendAndReceive(Object request, Class<T> targetClass) throws MessagingException;

	/**
	 * 将给定的请求对象转换为序列化形式, 可能使用{@link org.springframework.messaging.converter.MessageConverter},
	 * 将其作为{@link Message}发送到给定目标, 接收回复并将其主体转换为指定的目标类.
	 * 
	 * @param destination 目标
	 * @param request 要发送的请求消息的有效负载
	 * @param targetClass 要将回复的有效负载转换为的目标类型
	 * 
	 * @return 回复消息的有效负载, 或{@code null} 如果无法接收消息, 例如由于超时
	 */
	<T> T convertSendAndReceive(D destination, Object request, Class<T> targetClass) throws MessagingException;

	/**
	 * 将给定的请求对象转换为序列化形式, 可能使用{@link org.springframework.messaging.converter.MessageConverter},
	 * 将其作为具有给定标头的{@link Message}发送到指定目标, 接收回复并将其主体转换为指定的目标类.
	 * 
	 * @param destination 目标
	 * @param request 要发送的请求消息的有效负载
	 * @param headers 要发送的请求消息的header
	 * @param targetClass 要将回复的有效负载转换为的目标类型
	 * 
	 * @return 回复消息的有效负载, 或{@code null} 如果无法接收消息, 例如由于超时
	 */
	<T> T convertSendAndReceive(D destination, Object request, Map<String, Object> headers, Class<T> targetClass)
			throws MessagingException;

	/**
	 * 将给定的请求对象转换为序列化形式, 可能使用{@link org.springframework.messaging.converter.MessageConverter},
	 * 应用给定的后处理器并将生成的{@link Message}发送到默认目标, 接收回复并将其主体转换为指定的目标类.
	 * 
	 * @param request 要发送的请求消息的有效负载
	 * @param targetClass 要将回复的有效负载转换为的目标类型
	 * @param requestPostProcessor 要应用于请求消息的后处理
	 * 
	 * @return 回复消息的有效负载, 或{@code null} 如果无法接收消息, 例如由于超时
	 */
	<T> T convertSendAndReceive(Object request, Class<T> targetClass, MessagePostProcessor requestPostProcessor)
			throws MessagingException;

	/**
	 * 将给定的请求对象转换为序列化形式, 可能使用{@link org.springframework.messaging.converter.MessageConverter},
	 * 应用给定的后处理器并将生成的{@link Message}发送到给定目标, 接收回复并将其主体转换为指定的目标类.
	 * 
	 * @param destination 目标
	 * @param request 要发送的请求消息的有效负载
	 * @param targetClass 要将回复的有效负载转换为的目标类型
	 * @param requestPostProcessor 要应用于请求消息的后处理
	 * 
	 * @return 回复消息的有效负载, 或{@code null} 如果无法接收消息, 例如由于超时
	 */
	<T> T convertSendAndReceive(D destination, Object request, Class<T> targetClass,
			MessagePostProcessor requestPostProcessor) throws MessagingException;

	/**
	 * 将给定的请求对象转换为序列化形式, 可能使用 {@link org.springframework.messaging.converter.MessageConverter},
	 * 将其包装为具有给定header的消息, 应用给定的后处理器, 并将生成的{@link Message}发送到指定的目标,
	 * 接收回复并将其主体转换为指定的目标类.
	 * 
	 * @param destination 目标
	 * @param request 要发送的请求消息的有效负载
	 * @param targetClass 要将回复的有效负载转换为的目标类型
	 * @param requestPostProcessor 要应用于请求消息的后处理
	 * 
	 * @return 回复消息的有效负载, 或{@code null} 如果无法接收消息, 例如由于超时
	 */
	<T> T convertSendAndReceive(D destination, Object request, Map<String, Object> headers,
			Class<T> targetClass, MessagePostProcessor requestPostProcessor) throws MessagingException;

}
