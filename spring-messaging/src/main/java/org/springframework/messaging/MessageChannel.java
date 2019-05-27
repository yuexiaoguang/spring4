package org.springframework.messaging;

/**
 * 定义发送消息的方法.
 */
public interface MessageChannel {

	/**
	 * 在没有规定超时的情况下发送消息的常量.
	 */
	long INDEFINITE_TIMEOUT = -1;


	/**
	 * 发送{@link Message}到此频道.
	 * 如果消息发送成功, 则该方法返回{@code true}.
	 * 如果由于非致命原因而无法发送消息, 则该方法返回{@code false}.
	 * 如果出现不可恢复的错误, 该方法也可能抛出RuntimeException.
	 * <p>取决于实现, 该方法可以无限期地阻塞.
	 * 要提供最长等待时间, 使用{@link #send(Message, long)}.
	 * 
	 * @param message 要发送的消息
	 * 
	 * @return 消息是否已发送
	 */
	boolean send(Message<?> message);

	/**
	 * 发送消息, 阻塞, 直到接受消息或超过指定的超时时间.
	 * 
	 * @param message 要发送的消息
	 * @param timeout 超时时间(毫秒)或{@link #INDEFINITE_TIMEOUT}
	 * 
	 * @return {@code true}如果消息被发送, {@code false}如果不包括发送中断的超时
	 */
	boolean send(Message<?> message, long timeout);

}
