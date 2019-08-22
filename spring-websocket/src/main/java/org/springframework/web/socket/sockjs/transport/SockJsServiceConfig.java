package org.springframework.web.socket.sockjs.transport;

import org.springframework.scheduling.TaskScheduler;
import org.springframework.web.socket.sockjs.SockJsService;
import org.springframework.web.socket.sockjs.frame.SockJsMessageCodec;

/**
 * 提供传输处理代码, 可访问他们需要访问的{@link SockJsService}配置选项.
 * 主要供内部使用.
 */
public interface SockJsServiceConfig {

	/**
	 * 用于调度心跳消息的调度器实例.
	 */
	TaskScheduler getTaskScheduler();

	/**
	 * 流传输在客户端保存响应, 不释放传递的消息使用的内存.
	 * 这种传输需要偶尔回收连接.
	 * 此属性设置在单个HTTP流式传输请求关闭之前发送的最小字节数.
	 * 之后客户端将打开一个新请求.
	 * 将此值设置为1可以有效地禁用流式传输, 并使流式传输的行为类似于轮询传输.
	 * <p>默认为 128K (i.e. 128 * 1024).
	 */
	int getStreamBytesLimit();

	/**
	 * 服务器未发送任何消息的时间量 (以毫秒为单位), 之后服务器应将心跳帧发送到客户端以防止连接中断.
	 * <p>默认为 25,000 (25 seconds).
	 */
	long getHeartbeatTime();

	/**
	 * 在等待来自客户端的下一个HTTP轮询请求时, 会话可以缓存的服务器到客户端消息的数量.
	 * 所有HTTP传输都使用此属性, 因为即使是流传输也会定期回收HTTP请求.
	 * <p>HTTP请求之间的时间量应该相对较短, 并且不会超过允许的断开连接的延迟
	 * (see {@link org.springframework.web.socket.sockjs.support.AbstractSockJsService#setDisconnectDelay(long)}),
	 * 默认为5秒.
	 * <p>默认为 100.
	 */
	int getHttpMessageCacheSize();

	/**
	 * 用于编码和解码SockJS消息的编解码器.
	 * 
	 * @throws IllegalStateException 如果没有{@link SockJsMessageCodec}可用
	 */
	SockJsMessageCodec getMessageCodec();

}
