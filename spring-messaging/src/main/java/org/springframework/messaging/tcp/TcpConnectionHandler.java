package org.springframework.messaging.tcp;

import org.springframework.messaging.Message;

/**
 * 管理TCP连接的生命周期事件, 包括传入消息的处理的约定.
 *
 * @param <P> 入站和出站消息的有效负载类型
 */
public interface TcpConnectionHandler<P> {

	/**
	 * 成功建立连接后调用.
	 * 
	 * @param connection 连接
	 */
	void afterConnected(TcpConnection<P> connection);

	/**
	 * 无法连接时调用.
	 * 
	 * @param ex 异常
	 */
	void afterConnectFailure(Throwable ex);

	/**
	 * 处理从远程主机收到的消息.
	 * 
	 * @param message 消息
	 */
	void handleMessage(Message<P> message);

	/**
	 * 处理连接失败.
	 * 
	 * @param ex 异常
	 */
	void handleFailure(Throwable ex);

	/**
	 * 关闭连接后调用.
	 */
	void afterConnectionClosed();

}
