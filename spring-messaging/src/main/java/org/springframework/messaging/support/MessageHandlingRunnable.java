package org.springframework.messaging.support;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;

/**
 * {@link Runnable}接口的扩展, 使用方法获取要处理的{@link MessageHandler}和{@link Message}.
 */
public interface MessageHandlingRunnable extends Runnable {

	/**
	 * 返回将要处理的消息.
	 */
	Message<?> getMessage();

	/**
	 * 返回将用于处理消息的MessageHandler.
	 */
	MessageHandler getMessageHandler();

}
