package org.springframework.messaging;

/**
 * {@link MessageChannel}, 用于维护订阅者注册表, 并调用它们来处理通过此通道发送的消息.
 */
public interface SubscribableChannel extends MessageChannel {

	/**
	 * 注册消息处理器.
	 * 
	 * @return {@code true}如果处理器被订阅, 或{@code false}如果其已订阅.
	 */
	boolean subscribe(MessageHandler handler);

	/**
	 * 取消注册消息处理器.
	 * 
	 * @return {@code true} 如果处理器未被订阅, 或{@code false}如果未注册.
	 */
	boolean unsubscribe(MessageHandler handler);

}
