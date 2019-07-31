package org.springframework.http.server;

/**
 * 一种控件, 可以将HTTP请求的处理置于异步模式, 在此期间响应保持打开状态直到显式关闭.
 */
public interface ServerHttpAsyncRequestControl {

	/**
	 * 启用异步处理, 之后响应保持打开状态, 直到调用{@link #complete()}或服务器超时请求.
	 * 启用后, 将忽略对此方法的其他调用.
	 */
	void start();

	/**
	 * {@link #start()}的变体, 允许指定用于异步处理的超时值.
	 * 如果未在指定值内调用{@link #complete()}, 则请求超时.
	 */
	void start(long timeout);

	/**
	 * 返回是否已启动异步请求处理.
	 */
	boolean isStarted();

	/**
	 * 将异步请求处理标记为已完成.
	 */
	void complete();

	/**
	 * 返回异步请求处理是否已完成.
	 */
	boolean isCompleted();

}
