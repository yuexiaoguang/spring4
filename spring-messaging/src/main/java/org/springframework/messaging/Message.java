package org.springframework.messaging;

/**
 * 带header和正文的通用消息表示.
 */
public interface Message<T> {

	/**
	 * 返回消息有效负载.
	 */
	T getPayload();

	/**
	 * 返回消息的header (never {@code null} but may be empty).
	 */
	MessageHeaders getHeaders();

}
