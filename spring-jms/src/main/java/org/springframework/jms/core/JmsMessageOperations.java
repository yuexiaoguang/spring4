package org.springframework.jms.core;

import java.util.Map;
import javax.jms.Destination;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.core.MessagePostProcessor;
import org.springframework.messaging.core.MessageReceivingOperations;
import org.springframework.messaging.core.MessageRequestReplyOperations;
import org.springframework.messaging.core.MessageSendingOperations;

/**
 * {@link MessageSendingOperations}, {@link MessageReceivingOperations}
 * 和{@link MessageRequestReplyOperations}的专门化, 用于JMS相关操作,
 * 允许指定目标名称而不是实际的{@link javax.jms.Destination}
 */
public interface JmsMessageOperations extends MessageSendingOperations<Destination>,
		MessageReceivingOperations<Destination>, MessageRequestReplyOperations<Destination> {

	/**
	 * 发送消息到指定目标.
	 * 
	 * @param destinationName 目标的名称
	 * @param message 要发送的消息
	 */
	void send(String destinationName, Message<?> message) throws MessagingException;

	/**
	 * 将给定的Object转换为序列化形式, 可能使用{@link org.springframework.messaging.converter.MessageConverter},
	 * 将其包装为消息并将其发送到给定目标.
	 * 
	 * @param destinationName 目标的名称
	 * @param payload 要用作有效负载的Object
	 */
	void convertAndSend(String destinationName, Object payload) throws MessagingException;

	/**
	 * 将给定的Object转换为序列化形式, 可能使用{@link org.springframework.messaging.converter.MessageConverter},
	 * 将其包装为带有给定header的消息, 并将其发送到给定目标.
	 * 
	 * @param destinationName 目标的名称
	 * @param payload 要用作有效负载的Object
	 * @param headers 要发送的消息的header
	 */
	void convertAndSend(String destinationName, Object payload, Map<String, Object> headers)
			throws MessagingException;

	/**
	 * 将给定的Object转换为序列化形式, 可能使用{@link org.springframework.messaging.converter.MessageConverter},
	 * 将其包装为消息, 应用给定的后处理器, 并将结果消息发送到给定目标.
	 * 
	 * @param destinationName 目标的名称
	 * @param payload 要用作有效负载的Object
	 * @param postProcessor 要应用于消息的后处理器
	 */
	void convertAndSend(String destinationName, Object payload, MessagePostProcessor postProcessor)
			throws MessagingException;

	/**
	 * 将给定的Object转换为序列化形式, 可能使用{@link org.springframework.messaging.converter.MessageConverter},
	 * 将其包装为具有给定header的消息, 应用给定的后处理器, 并将结果消息发送到给定目标.
	 * 
	 * @param destinationName 目标的名称
	 * @param payload 要用作有效负载的Object
	 * @param headers 要发送的消息的header
	 * @param postProcessor 要应用于消息的后处理器
	 */
	void convertAndSend(String destinationName, Object payload, Map<String,
			Object> headers, MessagePostProcessor postProcessor) throws MessagingException;

	/**
	 * 从给定目标接收消息.
	 * 
	 * @param destinationName 目标的名称
	 * 
	 * @return 收到的消息, 或{@code null} 如果无法接收消息, 例如由于超时
	 */
	Message<?> receive(String destinationName) throws MessagingException;

	/**
	 * 从给定目标接收消息, 并将其有效负载转换为指定的目标类.
	 * 
	 * @param destinationName 目标的名称
	 * @param targetClass 要将有效负载转换为的目标类
	 * 
	 * @return 回复的消息转换后的有效负载, 或{@code null} 如果无法接收消息, 例如由于超时
	 */
	<T> T receiveAndConvert(String destinationName, Class<T> targetClass) throws MessagingException;

	/**
	 * 发送请求消息并接收来自给定目标的回复.
	 * 
	 * @param destinationName 目标的名称
	 * @param requestMessage 要发送的消息
	 * 
	 * @return 回复, 或{@code null} 如果无法接收消息, 例如由于超时
	 */
	Message<?> sendAndReceive(String destinationName, Message<?> requestMessage) throws MessagingException;

	/**
	 * 将给定的请求对象转换为序列化形式, 可能使用{@link org.springframework.messaging.converter.MessageConverter},
	 * 将其作为{@link Message}发送到给定目标, 接收回复并将其正文转换为指定的目标类.
	 * 
	 * @param destinationName 目标的名称
	 * @param request 要发送的请求消息的有效负载
	 * @param targetClass 要将回复的有效负载转换为的目标类型
	 *
	 * @return 回复消息的有效负载, 或{@code null} 如果无法接收消息, 例如由于超时
	 */
	<T> T convertSendAndReceive(String destinationName, Object request, Class<T> targetClass) throws MessagingException;

	/**
	 * 将给定的请求对象转换为序列化形式, 可能使用{@link org.springframework.messaging.converter.MessageConverter},
	 * 将其作为具有给定header的{@link Message}发送到指定目标, 接收回复并将其正文转换为指定的目标类.
	 * 
	 * @param destinationName 目标的名称
	 * @param request 要发送的请求消息的有效负载
	 * @param headers 要发送的请求消息的header
	 * @param targetClass 要将回复的有效负载转换为的目标类型
	 * 
	 * @return 回复消息的有效负载, 或{@code null} 如果无法接收消息, 例如由于超时
	 */
	<T> T convertSendAndReceive(String destinationName, Object request, Map<String, Object> headers, Class<T> targetClass)
			throws MessagingException;

	/**
	 * 将给定的请求对象转换为序列化形式, 可能使用{@link org.springframework.messaging.converter.MessageConverter},
	 * 应用给定的后处理器并将生成的{@link Message}发送到给定目标, 接收回复并将其正文转换为指定的目标类.
	 * 
	 * @param destinationName 目标的名称
	 * @param request 要发送的请求消息的有效负载
	 * @param targetClass 要将回复的有效负载转换为的目标类型
	 * @param requestPostProcessor 要应用到请求消息的后处理器
	 * 
	 * @return 回复消息的有效负载, 或{@code null} 如果无法接收消息, 例如由于超时
	 */
	<T> T convertSendAndReceive(String destinationName, Object request, Class<T> targetClass,
			MessagePostProcessor requestPostProcessor) throws MessagingException;

	/**
	 * 将给定的请求对象转换为序列化形式, 可能使用{@link org.springframework.messaging.converter.MessageConverter},
	 * 将其包装为具有给定header的消息, 应用给定的后处理器并将生成的{@link Message}发送到指定的目标, 接收回复并将其正文转换为指定的目标类.
	 * 
	 * @param destinationName 目标的名称
	 * @param request 要发送的请求消息的有效负载
	 * @param targetClass 要将回复的有效负载转换为的目标类型
	 * @param requestPostProcessor 要应用到请求消息的后处理器
	 * 
	 * @return 回复消息的有效负载, 或{@code null} 如果无法接收消息, 例如由于超时
	 */
	<T> T convertSendAndReceive(String destinationName, Object request, Map<String, Object> headers,
			Class<T> targetClass, MessagePostProcessor requestPostProcessor) throws MessagingException;

}
