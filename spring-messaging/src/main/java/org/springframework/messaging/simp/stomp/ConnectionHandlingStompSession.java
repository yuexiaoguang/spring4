package org.springframework.messaging.simp.stomp;

import org.springframework.messaging.tcp.TcpConnectionHandler;
import org.springframework.util.concurrent.ListenableFuture;

/**
 * {@link StompSession}, 实现{@link org.springframework.messaging.tcp.TcpConnectionHandler TcpConnectionHandler},
 * 以发送和接收消息.
 *
 * <p>ConnectionHandlingStompSession可以与适用于{@code TcpConnectionHandler}约定的任何TCP或WebSocket库一起使用.
 */
public interface ConnectionHandlingStompSession extends StompSession, TcpConnectionHandler<byte[]> {

	/**
	 * 返回将在会话准备就绪时完成的Future.
	 */
	ListenableFuture<StompSession> getSessionFuture();

}
