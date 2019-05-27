package org.springframework.messaging.tcp;

import org.springframework.util.concurrent.ListenableFuture;

/**
 * 建立TCP连接的约定.
 *
 * @param <P> 入站和出站消息的有效负载类型
 */
public interface TcpOperations<P> {

	/**
	 * 打开一个新连接.
	 * 
	 * @param connectionHandler 管理连接的处理器
	 * 
	 * @return 一个ListenableFuture, 可用于确定何时以及是否成功建立连接
	 */
	ListenableFuture<Void> connect(TcpConnectionHandler<P> connectionHandler);

	/**
	 * 打开新连接, 如果连接失败, 重新连接的策略.
	 * 
	 * @param connectionHandler 管理连接的处理器
	 * @param reconnectStrategy 重新连接的策略
	 * 
	 * @return 一个ListenableFuture, 可用于确定何时以及是否成功建立初始连接
	 */
	ListenableFuture<Void> connect(TcpConnectionHandler<P> connectionHandler, ReconnectStrategy reconnectStrategy);

	/**
	 * 关闭所有打开的连接.
	 * 
	 * @return 一个ListenableFuture, 可用于确定连接何时以及是否成功关闭
	 */
	ListenableFuture<Void> shutdown();

}
