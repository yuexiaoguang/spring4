package org.springframework.remoting;

/**
 * RemoteAccessException子类, 当无法与远程服务建立连接时抛出.
 */
@SuppressWarnings("serial")
public class RemoteConnectFailureException extends RemoteAccessException {

	public RemoteConnectFailureException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
