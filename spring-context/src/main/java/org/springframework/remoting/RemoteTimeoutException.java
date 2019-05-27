package org.springframework.remoting;

/**
 * 在配置的超时之前, 未完成目标方法的执行时, 抛出的RemoteAccessException子类, 例如未收到回复消息时.
 */
@SuppressWarnings("serial")
public class RemoteTimeoutException extends RemoteAccessException {

	public RemoteTimeoutException(String msg) {
		super(msg);
	}

	public RemoteTimeoutException(String msg, Throwable cause) {
		super(msg, cause);
	}
}
