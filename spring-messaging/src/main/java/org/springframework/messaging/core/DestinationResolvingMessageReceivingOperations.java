package org.springframework.messaging.core;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;

/**
 * 扩展{@link MessageReceivingOperations}, 并添加用于从指定为(可解析的)字符串名称的目标接收消息的操作.
 */
public interface DestinationResolvingMessageReceivingOperations<D> extends MessageReceivingOperations<D> {

	/**
	 * 解析给定的目标名称并从中接收消息.
	 * 
	 * @param destinationName 要解析的目标名称
	 */
	Message<?> receive(String destinationName) throws MessagingException;

	/**
	 * 解析给定的目标名称, 从中接收消息, 将有效负载转换为指定的目标类型.
	 * 
	 * @param destinationName 要解析的目标名称
	 * @param targetClass 要转换为的类型
	 */
	<T> T receiveAndConvert(String destinationName, Class<T> targetClass) throws MessagingException;

}
