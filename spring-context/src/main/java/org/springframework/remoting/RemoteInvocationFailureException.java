package org.springframework.remoting;

/**
 * RemoteAccessException子类, 在服务器端执行目标方法失败时抛出, 例如在目标对象上找不到方法时.
 */
@SuppressWarnings("serial")
public class RemoteInvocationFailureException extends RemoteAccessException {

	public RemoteInvocationFailureException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
