package org.springframework.scheduling.annotation;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.springframework.util.concurrent.FailureCallback;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;
import org.springframework.util.concurrent.SuccessCallback;

/**
 * 可用于方法签名的通过的{@code Future}句柄, 方法声明{@code Future}返回类型以进行异步执行.
 *
 * <p>从Spring 4.1开始, 这个类实现{@link ListenableFuture}, 而不仅仅是{@link java.util.concurrent.Future}, 以及{@code @Async}处理中的相应支持.
 *
 * <p>从Spring 4.2开始, 此类还支持将执行异常传递回调用者.
 */
public class AsyncResult<V> implements ListenableFuture<V> {

	private final V value;

	private final ExecutionException executionException;


	/**
	 * @param value 要通过的值
	 */
	public AsyncResult(V value) {
		this(value, null);
	}

	/**
	 * @param value 要通过的值
	 */
	private AsyncResult(V value, ExecutionException ex) {
		this.value = value;
		this.executionException = ex;
	}


	@Override
	public boolean cancel(boolean mayInterruptIfRunning) {
		return false;
	}

	@Override
	public boolean isCancelled() {
		return false;
	}

	@Override
	public boolean isDone() {
		return true;
	}

	@Override
	public V get() throws ExecutionException {
		if (this.executionException != null) {
			throw this.executionException;
		}
		return this.value;
	}

	@Override
	public V get(long timeout, TimeUnit unit) throws ExecutionException {
		return get();
	}

	@Override
	public void addCallback(ListenableFutureCallback<? super V> callback) {
		addCallback(callback, callback);
	}

	@Override
	public void addCallback(SuccessCallback<? super V> successCallback, FailureCallback failureCallback) {
		try {
			if (this.executionException != null) {
				Throwable cause = this.executionException.getCause();
				failureCallback.onFailure(cause != null ? cause : this.executionException);
			}
			else {
				successCallback.onSuccess(this.value);
			}
		}
		catch (Throwable ex) {
			// Ignore
		}
	}


	/**
	 * 创建一个新的异步结果, 公开来自{@link Future#get()}的给定值.
	 * 
	 * @param value 要公开的值
	 */
	public static <V> ListenableFuture<V> forValue(V value) {
		return new AsyncResult<V>(value, null);
	}

	/**
	 * 创建一个新的异步结果, 它将给定的异常作为{@link ExecutionException}从{@link Future#get()}公开.
	 * 
	 * @param ex 要公开的异常 (预构建的{@link ExecutionException}或要包装在{@link ExecutionException}中的原因)
	 */
	public static <V> ListenableFuture<V> forExecutionException(Throwable ex) {
		return new AsyncResult<V>(null,
				(ex instanceof ExecutionException ? (ExecutionException) ex : new ExecutionException(ex)));
	}

}
