package org.springframework.web.context.request.async;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.servlet.AsyncContext;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.util.Assert;
import org.springframework.web.context.request.ServletWebRequest;

/**
 * {@link AsyncWebRequest}的Servlet 3.0实现.
 *
 * <p>异步请求中涉及的servlet和所有过滤器必须使用Servlet API启用异步支持,
 * 或者在{@code web.xml}中添加{@code <async-supported>true</async-supported>}元素到servlet和过滤器的声明.
 */
public class StandardServletAsyncWebRequest extends ServletWebRequest implements AsyncWebRequest, AsyncListener {

	private Long timeout;

	private AsyncContext asyncContext;

	private AtomicBoolean asyncCompleted = new AtomicBoolean(false);

	private final List<Runnable> timeoutHandlers = new ArrayList<Runnable>();

	private ErrorHandler errorHandler;

	private final List<Runnable> completionHandlers = new ArrayList<Runnable>();


	/**
	 * @param request 当前的HTTP请求
	 * @param response 当前的HTTP响应
	 */
	public StandardServletAsyncWebRequest(HttpServletRequest request, HttpServletResponse response) {
		super(request, response);
	}


	/**
	 * 在Servlet 3异步处理中, 超时期限在容器处理线程退出后开始.
	 */
	@Override
	public void setTimeout(Long timeout) {
		Assert.state(!isAsyncStarted(), "Cannot change the timeout with concurrent handling in progress");
		this.timeout = timeout;
	}

	@Override
	public void addTimeoutHandler(Runnable timeoutHandler) {
		this.timeoutHandlers.add(timeoutHandler);
	}

	void setErrorHandler(ErrorHandler errorHandler) {
		this.errorHandler = errorHandler;
	}

	@Override
	public void addCompletionHandler(Runnable runnable) {
		this.completionHandlers.add(runnable);
	}

	@Override
	public boolean isAsyncStarted() {
		return (this.asyncContext != null && getRequest().isAsyncStarted());
	}

	/**
	 * 异步请求处理是否已完成.
	 * <p>异步处理完成后, 避免使用请求和响应对象非常重要. Servlet容器经常重复使用它们.
	 */
	@Override
	public boolean isAsyncComplete() {
		return this.asyncCompleted.get();
	}

	@Override
	public void startAsync() {
		Assert.state(getRequest().isAsyncSupported(),
				"Async support must be enabled on a servlet and for all filters involved " +
				"in async request processing. This is done in Java code using the Servlet API " +
				"or by adding \"<async-supported>true</async-supported>\" to servlet and " +
				"filter declarations in web.xml.");
		Assert.state(!isAsyncComplete(), "Async processing has already completed");

		if (isAsyncStarted()) {
			return;
		}
		this.asyncContext = getRequest().startAsync(getRequest(), getResponse());
		this.asyncContext.addListener(this);
		if (this.timeout != null) {
			this.asyncContext.setTimeout(this.timeout);
		}
	}

	@Override
	public void dispatch() {
		Assert.notNull(this.asyncContext, "Cannot dispatch without an AsyncContext");
		this.asyncContext.dispatch();
	}


	// ---------------------------------------------------------------------
	// Implementation of AsyncListener methods
	// ---------------------------------------------------------------------

	@Override
	public void onStartAsync(AsyncEvent event) throws IOException {
	}

	@Override
	public void onError(AsyncEvent event) throws IOException {
		if (this.errorHandler != null) {
			this.errorHandler.handle(event.getThrowable());
		}
	}

	@Override
	public void onTimeout(AsyncEvent event) throws IOException {
		for (Runnable handler : this.timeoutHandlers) {
			handler.run();
		}
	}

	@Override
	public void onComplete(AsyncEvent event) throws IOException {
		for (Runnable handler : this.completionHandlers) {
			handler.run();
		}
		this.asyncContext = null;
		this.asyncCompleted.set(true);
	}


	interface ErrorHandler {

		void handle(Throwable ex);

	}

}
