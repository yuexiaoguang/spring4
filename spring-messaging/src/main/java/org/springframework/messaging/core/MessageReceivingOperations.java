package org.springframework.messaging.core;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;

/**
 * 从目标接收消息的操作.
 *
 * @param <D> 从中接收消息的目标类型
 */
public interface MessageReceivingOperations<D> {

	/**
	 * 从默认目标接收消息.
	 * 
	 * @return 收到的消息, 或{@code null} 如果无法接收消息, 例如由于超时
	 */
	Message<?> receive() throws MessagingException;

	/**
	 * 从给定目标接收消息.
	 * 
	 * @param destination 目标
	 * 
	 * @return 收到的消息, 或{@code null} 如果无法接收消息, 例如由于超时
	 */
	Message<?> receive(D destination) throws MessagingException;

	/**
	 * 从默认目标接收消息, 并将其有效负载转换为指定的目标类.
	 * 
	 * @param targetClass 要将有效负载转换为的目标类
	 * 
	 * @return 回复消息转换后的有效负载, 或{@code null} 如果无法接收消息, 例如由于超时
	 */
	<T> T receiveAndConvert(Class<T> targetClass) throws MessagingException;

	/**
	 * 从给定目标接收消息, 并将其有效负载转换为指定的目标类.
	 * 
	 * @param destination 目标
	 * @param targetClass 要将有效负载转换为的目标类
	 * 
	 * @return 回复消息转换后的有效负载, 或{@code null} 如果无法接收消息, 例如由于超时
	 */
	<T> T receiveAndConvert(D destination, Class<T> targetClass) throws MessagingException;

}
