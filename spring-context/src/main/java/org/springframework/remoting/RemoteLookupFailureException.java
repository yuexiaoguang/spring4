package org.springframework.remoting;

/**
 * RemoteAccessException子类, 在查找失败的情况下抛出, 通常是在每次方法调用时根据需要进行查找.
 */
@SuppressWarnings("serial")
public class RemoteLookupFailureException extends RemoteAccessException {

	public RemoteLookupFailureException(String msg) {
		super(msg);
	}

	public RemoteLookupFailureException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
