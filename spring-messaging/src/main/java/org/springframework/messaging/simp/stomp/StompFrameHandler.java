package org.springframework.messaging.simp.stomp;

import java.lang.reflect.Type;

/**
 * 处理STOMP帧的约定.
 */
public interface StompFrameHandler {

	/**
	 * 在{@link #handleFrame(StompHeaders, Object)}之前调用, 以确定有效负载应转换为的Object类型.
	 * 
	 * @param headers 消息的header
	 */
	Type getPayloadType(StompHeaders headers);

	/**
	 * 处理STOMP帧, 并将有效负载转换为从{@link #getPayloadType(StompHeaders)}返回的目标类型.
	 * 
	 * @param headers 帧的header
	 * @param payload 有效载荷或{@code null}
	 */
	void handleFrame(StompHeaders headers, Object payload);

}
