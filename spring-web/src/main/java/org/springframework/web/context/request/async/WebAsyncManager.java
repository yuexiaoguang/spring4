package org.springframework.web.context.request.async;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.util.Assert;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.async.DeferredResult.DeferredResultHandler;
import org.springframework.web.util.UrlPathHelper;

/**
 * 用于管理异步请求处理的中心类, 主要用作SPI, 通常不由应用程序类直接使用.
 *
 * <p>异步场景以线程 (T1)中的常规请求处理开始.
 * 可以通过调用
 * {@link #startCallableProcessing(Callable, Object...) startCallableProcessing}
 * 或{@link #startDeferredResultProcessing(DeferredResult, Object...) startDeferredResultProcessing}
 * 来启动并发请求处理, 这两者都在单独的线程 (T2)中生成结果.
 * 保存结果并将请求分派给容器, 以便在第三个线程 (T3)中恢复处理保存的结果.
 * 在调度线程 (T3)中, 可以通过{@link #getConcurrentResult()}访问保存的结果,
 * 或通过{@link #hasConcurrentResult()}检测其存在.
 */
public final class WebAsyncManager {

	private static final Object RESULT_NONE = new Object();

	private static final Log logger = LogFactory.getLog(WebAsyncManager.class);

	private static final UrlPathHelper urlPathHelper = new UrlPathHelper();

	private static final CallableProcessingInterceptor timeoutCallableInterceptor =
			new TimeoutCallableProcessingInterceptor();

	private static final DeferredResultProcessingInterceptor timeoutDeferredResultInterceptor =
			new TimeoutDeferredResultProcessingInterceptor();


	private AsyncWebRequest asyncWebRequest;

	private AsyncTaskExecutor taskExecutor = new SimpleAsyncTaskExecutor(this.getClass().getSimpleName());

	private volatile Object concurrentResult = RESULT_NONE;

	private volatile Object[] concurrentResultContext;

	private final Map<Object, CallableProcessingInterceptor> callableInterceptors =
			new LinkedHashMap<Object, CallableProcessingInterceptor>();

	private final Map<Object, DeferredResultProcessingInterceptor> deferredResultInterceptors =
			new LinkedHashMap<Object, DeferredResultProcessingInterceptor>();


	WebAsyncManager() {
	}


	/**
	 * 配置要使用的{@link AsyncWebRequest}.
	 * 在单个请求期间可以多次设置此属性以准确反映请求的当前状态 (e.g. 在转发, 请求/响应包装等之后).
	 * 但是, 在并发处理正在进行时不应设置它, i.e. {@link #isConcurrentHandlingStarted()}是{@code true}时.
	 * 
	 * @param asyncWebRequest 要使用的Web请求
	 */
	public void setAsyncWebRequest(final AsyncWebRequest asyncWebRequest) {
		Assert.notNull(asyncWebRequest, "AsyncWebRequest must not be null");
		this.asyncWebRequest = asyncWebRequest;
		this.asyncWebRequest.addCompletionHandler(new Runnable() {
			@Override
			public void run() {
				asyncWebRequest.removeAttribute(WebAsyncUtils.WEB_ASYNC_MANAGER_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);
			}
		});
	}

	/**
	 * 通过{@link #startCallableProcessing(Callable, Object...)}配置AsyncTaskExecutor以用于并发处理.
	 * <p>默认使用{@link SimpleAsyncTaskExecutor}实例.
	 */
	public void setTaskExecutor(AsyncTaskExecutor taskExecutor) {
		this.taskExecutor = taskExecutor;
	}

	/**
	 * 当前请求的所选处理器是否选择异步处理请求.
	 * 返回"true"表示正在进行并发处理, 响应将保持打开状态.
	 * 返回"false"表示并发处理未启动或可能已完成, 并且已分派请求以进一步处理并发结果.
	 */
	public boolean isConcurrentHandlingStarted() {
		return (this.asyncWebRequest != null && this.asyncWebRequest.isAsyncStarted());
	}

	/**
	 * 结果值是否因并发处理而存在.
	 */
	public boolean hasConcurrentResult() {
		return (this.concurrentResult != RESULT_NONE);
	}

	/**
	 * 提供对并发处理结果的访问.
	 * 
	 * @return Object, 可能是{@code Exception}或{@code Throwable}
	 */
	public Object getConcurrentResult() {
		return this.concurrentResult;
	}

	/**
	 * 提供对并发处理开始时保存的其他处理上下文的访问.
	 */
	public Object[] getConcurrentResultContext() {
		return this.concurrentResultContext;
	}

	/**
	 * 获取在给定键下注册的{@link CallableProcessingInterceptor}.
	 * 
	 * @param key 键
	 * 
	 * @return 在该键下注册的拦截器, 或{@code null}
	 */
	public CallableProcessingInterceptor getCallableInterceptor(Object key) {
		return this.callableInterceptors.get(key);
	}

	/**
	 * 获取在给定键下注册的{@link DeferredResultProcessingInterceptor}.
	 * 
	 * @param key 键
	 * 
	 * @return 在该键下注册的拦截器, 或{@code null}
	 */
	public DeferredResultProcessingInterceptor getDeferredResultInterceptor(Object key) {
		return this.deferredResultInterceptors.get(key);
	}

	/**
	 * 在给定键下注册{@link CallableProcessingInterceptor}.
	 * 
	 * @param key 键
	 * @param interceptor 要注册的拦截器
	 */
	public void registerCallableInterceptor(Object key, CallableProcessingInterceptor interceptor) {
		Assert.notNull(key, "Key is required");
		Assert.notNull(interceptor, "CallableProcessingInterceptor  is required");
		this.callableInterceptors.put(key, interceptor);
	}

	/**
	 * 在没有键的情况下注册{@link CallableProcessingInterceptor}.
	 * 键源自类名和哈希码.
	 * 
	 * @param interceptors 要注册的一个或多个拦截器
	 */
	public void registerCallableInterceptors(CallableProcessingInterceptor... interceptors) {
		Assert.notNull(interceptors, "A CallableProcessingInterceptor is required");
		for (CallableProcessingInterceptor interceptor : interceptors) {
			String key = interceptor.getClass().getName() + ":" + interceptor.hashCode();
			this.callableInterceptors.put(key, interceptor);
		}
	}

	/**
	 * 在给定的键下注册一个{@link DeferredResultProcessingInterceptor}.
	 * 
	 * @param key 键
	 * @param interceptor 要注册的拦截器
	 */
	public void registerDeferredResultInterceptor(Object key, DeferredResultProcessingInterceptor interceptor) {
		Assert.notNull(key, "Key is required");
		Assert.notNull(interceptor, "DeferredResultProcessingInterceptor is required");
		this.deferredResultInterceptors.put(key, interceptor);
	}

	/**
	 * 在没有指定键的情况下注册一个或多个{@link DeferredResultProcessingInterceptor}.
	 * 默认键是从拦截器类名和哈希代码派生的.
	 * 
	 * @param interceptors 要注册的一个或多个拦截器
	 */
	public void registerDeferredResultInterceptors(DeferredResultProcessingInterceptor... interceptors) {
		Assert.notNull(interceptors, "A DeferredResultProcessingInterceptor is required");
		for (DeferredResultProcessingInterceptor interceptor : interceptors) {
			String key = interceptor.getClass().getName() + ":" + interceptor.hashCode();
			this.deferredResultInterceptors.put(key, interceptor);
		}
	}

	/**
	 * 清理{@linkplain #getConcurrentResult() concurrentResult}
	 * 和{@linkplain #getConcurrentResultContext() concurrentResultContext}.
	 */
	public void clearConcurrentResult() {
		synchronized (WebAsyncManager.this) {
			this.concurrentResult = RESULT_NONE;
			this.concurrentResultContext = null;
		}
	}

	/**
	 * 启动并发请求处理并使用{@link #setTaskExecutor(AsyncTaskExecutor) AsyncTaskExecutor}执行给定任务.
	 * 保存任务执行的结果并调度请求以便继续处理该结果.
	 * 如果任务引发异常, 则保存的结果将是引发的异常.
	 * 
	 * @param callable 要异步执行的工作单元
	 * @param processingContext 保存的其他上下文可以通过{@link #getConcurrentResultContext()}访问
	 * 
	 * @throws Exception 如果并发处理无法启动
	 */
	@SuppressWarnings({"unchecked", "rawtypes"})
	public void startCallableProcessing(Callable<?> callable, Object... processingContext) throws Exception {
		Assert.notNull(callable, "Callable must not be null");
		startCallableProcessing(new WebAsyncTask(callable), processingContext);
	}

	/**
	 * 在委托给{@link #startCallableProcessing(Callable, Object...)}之前,
	 * 使用给定的{@link WebAsyncTask}配置任务执行器以及{@code AsyncWebRequest}的超时值.
	 * 
	 * @param webAsyncTask 一个包含目标{@code Callable}的WebAsyncTask
	 * @param processingContext 保存的其他上下文, 可以通过{@link #getConcurrentResultContext()}访问
	 * 
	 * @throws Exception 如果并发处理无法启动
	 */
	public void startCallableProcessing(final WebAsyncTask<?> webAsyncTask, Object... processingContext)
			throws Exception {

		Assert.notNull(webAsyncTask, "WebAsyncTask must not be null");
		Assert.state(this.asyncWebRequest != null, "AsyncWebRequest must not be null");

		Long timeout = webAsyncTask.getTimeout();
		if (timeout != null) {
			this.asyncWebRequest.setTimeout(timeout);
		}

		AsyncTaskExecutor executor = webAsyncTask.getExecutor();
		if (executor != null) {
			this.taskExecutor = executor;
		}

		List<CallableProcessingInterceptor> interceptors = new ArrayList<CallableProcessingInterceptor>();
		interceptors.add(webAsyncTask.getInterceptor());
		interceptors.addAll(this.callableInterceptors.values());
		interceptors.add(timeoutCallableInterceptor);

		final Callable<?> callable = webAsyncTask.getCallable();
		final CallableInterceptorChain interceptorChain = new CallableInterceptorChain(interceptors);

		this.asyncWebRequest.addTimeoutHandler(new Runnable() {
			@Override
			public void run() {
				logger.debug("Processing timeout");
				Object result = interceptorChain.triggerAfterTimeout(asyncWebRequest, callable);
				if (result != CallableProcessingInterceptor.RESULT_NONE) {
					setConcurrentResultAndDispatch(result);
				}
			}
		});

		if (this.asyncWebRequest instanceof StandardServletAsyncWebRequest) {
			((StandardServletAsyncWebRequest) this.asyncWebRequest).setErrorHandler(
					new StandardServletAsyncWebRequest.ErrorHandler() {
						@Override
						public void handle(Throwable ex) {
							setConcurrentResultAndDispatch(ex);
						}
					});
		}

		this.asyncWebRequest.addCompletionHandler(new Runnable() {
			@Override
			public void run() {
				interceptorChain.triggerAfterCompletion(asyncWebRequest, callable);
			}
		});

		interceptorChain.applyBeforeConcurrentHandling(this.asyncWebRequest, callable);
		startAsyncProcessing(processingContext);
		try {
			Future<?> future = this.taskExecutor.submit(new Runnable() {
				@Override
				public void run() {
					Object result = null;
					try {
						interceptorChain.applyPreProcess(asyncWebRequest, callable);
						result = callable.call();
					}
					catch (Throwable ex) {
						result = ex;
					}
					finally {
						result = interceptorChain.applyPostProcess(asyncWebRequest, callable, result);
					}
					setConcurrentResultAndDispatch(result);
				}
			});
			interceptorChain.setTaskFuture(future);
		}
		catch (RejectedExecutionException ex) {
			Object result = interceptorChain.applyPostProcess(this.asyncWebRequest, callable, ex);
			setConcurrentResultAndDispatch(result);
			throw ex;
		}
	}

	private void setConcurrentResultAndDispatch(Object result) {
		synchronized (WebAsyncManager.this) {
			if (this.concurrentResult != RESULT_NONE) {
				return;
			}
			this.concurrentResult = result;
		}

		if (this.asyncWebRequest.isAsyncComplete()) {
			logger.error("Could not complete async processing due to timeout or network error");
			return;
		}

		if (logger.isDebugEnabled()) {
			logger.debug("Concurrent result value [" + this.concurrentResult +
					"] - dispatching request to resume processing");
		}
		this.asyncWebRequest.dispatch();
	}

	/**
	 * 启动并发请求处理并使用{@link DeferredResultHandler}初始化给定的{@link DeferredResult},
	 * {@link DeferredResultHandler}用于保存结果并调度请求以继续处理该结果.
	 * {@code AsyncWebRequest}也使用完成处理器更新, 该处理器使{@code DeferredResult}到期,
	 * 并且超时处理器假定{@code DeferredResult}具有默认超时结果.
	 * 
	 * @param deferredResult 要初始化的DeferredResult实例
	 * @param processingContext 保存的其他上下文可以通过{@link #getConcurrentResultContext()}访问
	 * 
	 * @throws Exception 如果并发处理无法启动
	 */
	public void startDeferredResultProcessing(
			final DeferredResult<?> deferredResult, Object... processingContext) throws Exception {

		Assert.notNull(deferredResult, "DeferredResult must not be null");
		Assert.state(this.asyncWebRequest != null, "AsyncWebRequest must not be null");

		Long timeout = deferredResult.getTimeoutValue();
		if (timeout != null) {
			this.asyncWebRequest.setTimeout(timeout);
		}

		List<DeferredResultProcessingInterceptor> interceptors = new ArrayList<DeferredResultProcessingInterceptor>();
		interceptors.add(deferredResult.getInterceptor());
		interceptors.addAll(this.deferredResultInterceptors.values());
		interceptors.add(timeoutDeferredResultInterceptor);

		final DeferredResultInterceptorChain interceptorChain = new DeferredResultInterceptorChain(interceptors);

		this.asyncWebRequest.addTimeoutHandler(new Runnable() {
			@Override
			public void run() {
				try {
					interceptorChain.triggerAfterTimeout(asyncWebRequest, deferredResult);
				}
				catch (Throwable ex) {
					setConcurrentResultAndDispatch(ex);
				}
			}
		});

		if (this.asyncWebRequest instanceof StandardServletAsyncWebRequest) {
			((StandardServletAsyncWebRequest) this.asyncWebRequest).setErrorHandler(
					new StandardServletAsyncWebRequest.ErrorHandler() {
						@Override
						public void handle(Throwable ex) {
							deferredResult.setErrorResult(ex);
						}
					});
		}

		this.asyncWebRequest.addCompletionHandler(new Runnable() {
			@Override
			public void run() {
				interceptorChain.triggerAfterCompletion(asyncWebRequest, deferredResult);
			}
		});

		interceptorChain.applyBeforeConcurrentHandling(this.asyncWebRequest, deferredResult);
		startAsyncProcessing(processingContext);

		try {
			interceptorChain.applyPreProcess(this.asyncWebRequest, deferredResult);
			deferredResult.setResultHandler(new DeferredResultHandler() {
				@Override
				public void handleResult(Object result) {
					result = interceptorChain.applyPostProcess(asyncWebRequest, deferredResult, result);
					setConcurrentResultAndDispatch(result);
				}
			});
		}
		catch (Throwable ex) {
			setConcurrentResultAndDispatch(ex);
		}
	}

	private void startAsyncProcessing(Object[] processingContext) {
		synchronized (WebAsyncManager.this) {
			this.concurrentResult = RESULT_NONE;
			this.concurrentResultContext = processingContext;
		}
		this.asyncWebRequest.startAsync();

		if (logger.isDebugEnabled()) {
			HttpServletRequest request = this.asyncWebRequest.getNativeRequest(HttpServletRequest.class);
			String requestUri = urlPathHelper.getRequestUri(request);
			logger.debug("Concurrent handling starting for " + request.getMethod() + " [" + requestUri + "]");
		}
	}

}
