package org.springframework.messaging.support;

import java.util.Map;

import org.springframework.messaging.MessageHeaders;

/**
 * 带有{@link Throwable}有效负载的{@link GenericMessage}.
 */
public class ErrorMessage extends GenericMessage<Throwable> {

	private static final long serialVersionUID = -5470210965279837728L;


	/**
	 * @param payload 消息有效负载 (never {@code null})
	 */
	public ErrorMessage(Throwable payload) {
		super(payload);
	}

	/**
	 * 复制给定header Map的内容.
	 * 
	 * @param payload 消息有效负载 (never {@code null})
	 * @param headers 用于初始化的消息header
	 */
	public ErrorMessage(Throwable payload, Map<String, Object> headers) {
		super(payload, headers);
	}

	/**
	 * 要使用{@link MessageHeaders}实例的构造函数.
	 * <p><strong>Note:</strong> 给定的{@code MessageHeaders}实例直接在新消息中使用, i.e. 不复制它.
	 * 
	 * @param payload 消息有效负载 (never {@code null})
	 * @param headers 消息header
	 */
	public ErrorMessage(Throwable payload, MessageHeaders headers) {
		super(payload, headers);
	}
}
