package org.springframework.messaging;

/**
 * 一个{@link MessageChannel}, 可以通过轮询主动接收消息.
 */
public interface PollableChannel extends MessageChannel {

	/**
	 * 从此频道接收消息, 必要时无限期阻塞.
	 * 
	 * @return 下一个可用的{@link Message}或{@code null}如果被中断
	 */
	Message<?> receive();

	/**
	 * 从此频道接收消息, 阻塞直到消息可用或指定的超时时间过去.
	 * 
	 * @param timeout 超时时间(以毫秒为单位)或 {@link MessageChannel#INDEFINITE_TIMEOUT}.
	 * 
	 * @return 下一个可用的{@link Message}或{@code null}, 如果超过指定的超时时间或消息接收中断
	 */
	Message<?> receive(long timeout);

}
