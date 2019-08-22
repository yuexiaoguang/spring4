package org.springframework.web.socket.server;

import org.springframework.core.NestedRuntimeException;

/**
 * 由于内部不可恢复的错误, 握手处理未能完成时抛出.
 * 这意味着服务器错误 (HTTP状态码500), 而不是握手协商中的失败.
 *
 * <p>相反, 当握手协商失败时, 响应状态码将为200, 响应头和主体将更新以反映失败的原因.
 * {@link HandshakeHandler}实现将具有受保护的方法, 以在这些情况下自定义对响应的更新.
 */
@SuppressWarnings("serial")
public class HandshakeFailureException extends NestedRuntimeException {

	public HandshakeFailureException(String message) {
		super(message);
	}

	public HandshakeFailureException(String message, Throwable cause) {
		super(message, cause);
	}

}
