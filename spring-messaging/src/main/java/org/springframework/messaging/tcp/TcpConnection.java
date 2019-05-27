package org.springframework.messaging.tcp;

import java.io.Closeable;

import org.springframework.messaging.Message;
import org.springframework.util.concurrent.ListenableFuture;

/**
 * 发送消息和管理TCP连接的约定.
 *
 * @param <P> 出站{@link Message}的有效负载类型
 */
public interface TcpConnection<P> extends Closeable {

	/**
	 * 发送给定的消息.
	 * 
	 * @param message 消息
	 * 
	 * @return 一个ListenableFuture, 可用于确定消息何时以及是否已成功发送
	 */
	ListenableFuture<Void> send(Message<P> message);

	/**
	 * 注册一个任务, 在读取不活动一段时间后调用.
	 * 
	 * @param runnable 要调用的任务
	 * @param duration 不活动的时间长度, 以毫秒为单位
	 */
	void onReadInactivity(Runnable runnable, long duration);

	/**
	 * 注册一个任务, 在写入不活动一段时间后调用.
	 * 
	 * @param runnable 要调用的任务
	 * @param duration 不活动的时间长度, 以毫秒为单位
	 */
	void onWriteInactivity(Runnable runnable, long duration);

	/**
	 * 关闭连接.
	 */
	@Override
	void close();

}
