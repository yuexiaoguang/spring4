package org.springframework.messaging.core;

import org.springframework.messaging.Message;

/**
 * 创建{@link Message}之后处理的约定, 要么返回修改后的(有效新消息)消息, 要么返回相同的消息.
 */
public interface MessagePostProcessor {

	/**
	 * 处理给定的消息.
	 * 
	 * @param message 要处理的消息
	 * 
	 * @return 消息后处理后的变体, 或原始的传入消息; never {@code null}
	 */
	Message<?> postProcessMessage(Message<?> message);

}
