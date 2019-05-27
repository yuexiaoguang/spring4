package org.springframework.remoting;

/**
 * RemoteAccessException子类, 在远程服务的客户端代理中发生故障时抛出, 例如在底层RMI stub上找不到方法时.
 */
@SuppressWarnings("serial")
public class RemoteProxyFailureException extends RemoteAccessException {

	public RemoteProxyFailureException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
