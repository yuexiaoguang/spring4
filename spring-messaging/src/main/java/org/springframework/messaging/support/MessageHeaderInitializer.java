package org.springframework.messaging.support;

/**
 * 用于初始化{@link MessageHeaderAccessor}的回调接口.
 */
public interface MessageHeaderInitializer {

	/**
	 * 初始化给定的{@code MessageHeaderAccessor}.
	 * 
	 * @param headerAccessor 要初始化的MessageHeaderAccessor
	 */
	void initHeaders(MessageHeaderAccessor headerAccessor);

}
