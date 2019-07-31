package org.springframework.web.context.request.async;

import java.util.PriorityQueue;
import java.util.concurrent.Callable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.util.Assert;
import org.springframework.web.context.request.NativeWebRequest;

/**
 * {@code DeferredResult}提供了使用{@link Callable}进行异步请求处理的替代方法.
 * 代表应用程序并发执行{@code Callable}, 使用应用程序可以从其选择的线程生成结果的{@code DeferredResult}.
 *
 * <p>子类可以扩展此类以轻松地将其他数据或行为与{@link DeferredResult}相关联.
 * 例如, 可能希望通过扩展类并为用户添加其他属性来关联用于创建{@link DeferredResult}的用户.
 * 通过这种方式, 可以在以后轻松访问用户, 而无需使用数据结构来进行映射.
 *
 * <p>可以通过扩展类以实现其它接口, 来实现将其它的行为与该类相关联的示例.
 * 例如, 有人可能希望实现{@link Comparable}, 以便当{@link DeferredResult}添加到{@link PriorityQueue}时, 它将以正确的顺序处理.
 */
public class DeferredResult<T> {

	private static final Object RESULT_NONE = new Object();

	private static final Log logger = LogFactory.getLog(DeferredResult.class);


	private final Long timeout;

	private final Object timeoutResult;

	private Runnable timeoutCallback;

	private Runnable completionCallback;

	private DeferredResultHandler resultHandler;

	private volatile Object result = RESULT_NONE;

	private volatile boolean expired = false;


	public DeferredResult() {
		this(null, RESULT_NONE);
	}

	/**
	 * <p>默认情况下不设置, 使用MVC Java Config或MVC命名空间中配置的默认值,
	 * 或者如果未设置默认值, 则超时取决于底层服务器的默认值.
	 * 
	 * @param timeout 超时值, 以毫秒为单位
	 */
	public DeferredResult(Long timeout) {
		this(timeout, RESULT_NONE);
	}

	/**
	 * @param timeout 超时值, 以毫秒为单位 (如果是{@code null}忽略)
	 * @param timeoutResult 要使用的结果
	 */
	public DeferredResult(Long timeout, Object timeoutResult) {
		this.timeoutResult = timeoutResult;
		this.timeout = timeout;
	}


	/**
	 * 如果此DeferredResult不再可用, 则返回{@code true}, 因为它之前已设置或因为底层请求已过期.
	 * <p>如果为构造函数提供了超时结果, 则可能通过调用{@link #setResult(Object)},
	 * 或{@link #setErrorResult(Object)}, 或者作为超时的结果, 来设置结果.
	 * 由于超时或网络错误, 请求也可能会过期.
	 */
	public final boolean isSetOrExpired() {
		return (this.result != RESULT_NONE || this.expired);
	}

	/**
	 * 如果已设置DeferredResult, 则返回{@code true}.
	 */
	public boolean hasResult() {
		return (this.result != RESULT_NONE);
	}

	/**
	 * 返回结果, 如果未设置结果, 则返回{@code null}.
	 * 由于结果也可以是{@code null}, 因此建议先使用{@link #hasResult()}检查调用此方法之前是否有结果.
	 */
	public Object getResult() {
		Object resultToCheck = this.result;
		return (resultToCheck != RESULT_NONE ? resultToCheck : null);
	}

	/**
	 * 返回配置的超时值, 以毫秒为单位.
	 */
	final Long getTimeoutValue() {
		return this.timeout;
	}

	/**
	 * 注册在异步请求超时时调用的代码.
	 * <p>当异步请求在填充{@code DeferredResult}之前超时时, 从容器线程调用此方法.
	 * 它可以调用{@link DeferredResult#setResult setResult}或{@link DeferredResult#setErrorResult setErrorResult}来恢复处理.
	 */
	public void onTimeout(Runnable callback) {
		this.timeoutCallback = callback;
	}

	/**
	 * 注册在异步请求完成时调用的代码.
	 * <p>当异步请求因任何原因(包括超时和网络错误)完成时, 将从容器线程调用此方法.
	 * 这对于检测{@code DeferredResult}实例不再可用非常有用.
	 */
	public void onCompletion(Runnable callback) {
		this.completionCallback = callback;
	}

	/**
	 * 提供用于处理结果值的处理器.
	 * 
	 * @param resultHandler 处理器
	 */
	public final void setResultHandler(DeferredResultHandler resultHandler) {
		Assert.notNull(resultHandler, "DeferredResultHandler is required");
		// 结果锁定之外的立即到期检查
		if (this.expired) {
			return;
		}
		Object resultToHandle;
		synchronized (this) {
			// 在此期间获得锁定: double-check到期状态
			if (this.expired) {
				return;
			}
			resultToHandle = this.result;
			if (resultToHandle == RESULT_NONE) {
				// 还没有结果: 存储处理器, 一旦进入就进行处理
				this.resultHandler = resultHandler;
				return;
			}
		}
		// 如果到达这里, 需要立即处理现有的结果对象.
		// 决定是在结果锁中做出的; 只是在它之外的句柄调用, 避免Servlet容器锁的任何死锁.
		try {
			resultHandler.handleResult(resultToHandle);
		}
		catch (Throwable ex) {
			logger.debug("Failed to handle existing result", ex);
		}
	}

	/**
	 * 设置DeferredResult的值并处理它.
	 * 
	 * @param result 要设置的值
	 * 
	 * @return {@code true} 如果结果已设置并传递以进行处理;
	 * {@code false} 如果结果已设置或异步请求已过期
	 */
	public boolean setResult(T result) {
		return setResultInternal(result);
	}

	private boolean setResultInternal(Object result) {
		// 结果锁定之外的立即到期检查
		if (isSetOrExpired()) {
			return false;
		}
		DeferredResultHandler resultHandlerToUse;
		synchronized (this) {
			// 在此期间获得锁: double-check到期状态
			if (isSetOrExpired()) {
				return false;
			}
			// 在这一点上, 得到了一个新的结果来处理
			this.result = result;
			resultHandlerToUse = this.resultHandler;
			if (resultHandlerToUse == null) {
				// 尚未设置结果处理器 -> 让setResultHandler实现获取结果对象并为其调用结果处理器.
				return true;
			}
			// 结果处理器可用 -> 清除存储的引用, 因为不再需要它了.
			this.resultHandler = null;
		}
		// 如果到达这里, 需要立即处理现有的结果对象.
		// 决定是在结果锁中做出的; 只是在它之外的句柄调用, 避免Servlet容器锁的任何死锁.
		resultHandlerToUse.handleResult(result);
		return true;
	}

	/**
	 * 为{@link DeferredResult}设置错误值并处理它.
	 * 该值可能是{@link Exception}或{@link Throwable}, 在这种情况下, 它将被处理, 就像处理器引发异常一样.
	 * 
	 * @param result 错误结果值
	 * 
	 * @return {@code true} 如果结果设置为错误值并传递以进行处理;
	 * {@code false} 如果结果已设置或异步请求已过期
	 */
	public boolean setErrorResult(Object result) {
		return setResultInternal(result);
	}


	final DeferredResultProcessingInterceptor getInterceptor() {
		return new DeferredResultProcessingInterceptorAdapter() {
			@Override
			public <S> boolean handleTimeout(NativeWebRequest request, DeferredResult<S> deferredResult) {
				boolean continueProcessing = true;
				try {
					if (timeoutCallback != null) {
						timeoutCallback.run();
					}
				}
				finally {
					if (timeoutResult != RESULT_NONE) {
						continueProcessing = false;
						try {
							setResultInternal(timeoutResult);
						}
						catch (Throwable ex) {
							logger.debug("Failed to handle timeout result", ex);
						}
					}
				}
				return continueProcessing;
			}
			@Override
			public <S> void afterCompletion(NativeWebRequest request, DeferredResult<S> deferredResult) {
				expired = true;
				if (completionCallback != null) {
					completionCallback.run();
				}
			}
		};
	}


	/**
	 * 设置时处理DeferredResult值.
	 */
	public interface DeferredResultHandler {

		void handleResult(Object result);
	}

}
