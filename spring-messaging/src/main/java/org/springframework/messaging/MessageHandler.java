package org.springframework.messaging;

/**
 * 处理{@link Message}的约定.
 */
public interface MessageHandler {

	/**
	 * 处理给定的消息.
	 * 
	 * @param message 要处理的消息
	 * 
	 * @throws MessagingException 如果处理器无法处理消息
	 */
	void handleMessage(Message<?> message) throws MessagingException;

}
