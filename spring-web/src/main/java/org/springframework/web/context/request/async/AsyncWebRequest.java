package org.springframework.web.context.request.async;

import org.springframework.web.context.request.NativeWebRequest;

/**
 * 使用异步请求处理方法扩展{@link NativeWebRequest}.
 */
public interface AsyncWebRequest extends NativeWebRequest {

	/**
	 * 设置完成并发处理所需的时间.
	 * 当并发处理正在进行时, i.e. {@link #isAsyncStarted()}为{@code true}时, 不应设置此属性.
	 * 
	 * @param timeout 以毫秒为单位的时间量; {@code null}表示没有超时, i.e. 依赖于容器的默认超时.
	 */
	void setTimeout(Long timeout);

	/**
	 * 添加处理器以在并发处理超时时调用.
	 */
	void addTimeoutHandler(Runnable runnable);

	/**
	 * 添加句柄以在请求处理完成时调用.
	 */
	void addCompletionHandler(Runnable runnable);

	/**
	 * 标记异步请求处理的开始, 以便在主处理线程退出时, 响应保持打开状态, 以便在另一个线程中进一步处理.
	 * 
	 * @throws IllegalStateException 如果异步处理已完成或不受支持
	 */
	void startAsync();

	/**
	 * 调用{@link #startAsync()}后请求是否处于异步模式.
	 * 如果异步处理从未开始, 已完成, 或者已分派请求以进行进一步处理, 则返回"false".
	 */
	boolean isAsyncStarted();

	/**
	 * 将请求分派给容器, 以便在应用程序线程中并发执行后恢复处理.
	 */
	void dispatch();

	/**
	 * 异步处理是否已完成.
	 */
	boolean isAsyncComplete();

}
