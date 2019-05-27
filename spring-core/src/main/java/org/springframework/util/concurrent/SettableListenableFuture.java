package org.springframework.util.concurrent;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.springframework.util.Assert;

/**
 * {@link ListenableFuture}, 其值可以通过{@link #set(Object)} 或 {@link #setException(Throwable)}设置.
 * 它也可能被取消.
 *
 * <p>灵感来自{@code com.google.common.util.concurrent.SettableFuture}.
 */
public class SettableListenableFuture<T> implements ListenableFuture<T> {

	private static final Callable<Object> DUMMY_CALLABLE = new Callable<Object>() {
		@Override
		public Object call() throws Exception {
			throw new IllegalStateException("Should never be called");
		}
	};


	private final SettableTask<T> settableTask = new SettableTask<T>();


	/**
	 * 设置这个Future的值.
	 * 如果值设置成功, 此方法将返回{@code true}; 如果已设置或取消Future, 则此方法将返回{@code false}.
	 * 
	 * @param value 要设置的值
	 * 
	 * @return {@code true}如果值已成功设置, 否则{@code false}
	 */
	public boolean set(T value) {
		return this.settableTask.setResultValue(value);
	}

	/**
	 * 设置这个Future的异常.
	 * 如果成功设置了异常, 此方法将返回{@code true}; 如果已设置或取消Future, 则此方法将返回{@code false}.
	 * 
	 * @param exception 要设置的值
	 * 
	 * @return {@code true}如果成功设置了异常, 否则{@code false}
	 */
	public boolean setException(Throwable exception) {
		Assert.notNull(exception, "Exception must not be null");
		return this.settableTask.setExceptionResult(exception);
	}

	@Override
	public void addCallback(ListenableFutureCallback<? super T> callback) {
		this.settableTask.addCallback(callback);
	}

	@Override
	public void addCallback(SuccessCallback<? super T> successCallback, FailureCallback failureCallback) {
		this.settableTask.addCallback(successCallback, failureCallback);
	}

	@Override
	public boolean cancel(boolean mayInterruptIfRunning) {
		boolean cancelled = this.settableTask.cancel(mayInterruptIfRunning);
		if (cancelled && mayInterruptIfRunning) {
			interruptTask();
		}
		return cancelled;
	}

	@Override
	public boolean isCancelled() {
		return this.settableTask.isCancelled();
	}

	@Override
	public boolean isDone() {
		return this.settableTask.isDone();
	}

	/**
	 * 检索值.
	 * <p>如果已通过{@link #set(Object)}设置该值, 则此方法返回该值,
	 * 抛出{@link java.util.concurrent.ExecutionException}, 如果通过{@link #setException(Throwable)}设置了异常,
	 * 或抛出{@link java.util.concurrent.CancellationException}, 如果Future被取消.
	 * 
	 * @return 与此Future关联的值
	 */
	@Override
	public T get() throws InterruptedException, ExecutionException {
		return this.settableTask.get();
	}

	/**
	 * 检索值.
	 * <p>如果已通过{@link #set(Object)}设置该值, 则此方法返回该值,
	 * 抛出{@link java.util.concurrent.ExecutionException}, 如果通过{@link #setException(Throwable)}设置了异常,
	 * 或抛出{@link java.util.concurrent.CancellationException}, 如果Future被取消.
	 * 
	 * @param timeout 最长等待时间
	 * @param unit 超时参数的单位
	 * 
	 * @return 与此Future关联的值
	 */
	@Override
	public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
		return this.settableTask.get(timeout, unit);
	}

	/**
	 * 子类可以覆盖此方法以实现对Future计算的中断.
	 * 通过成功调用{@link #cancel(boolean) cancel(true)}自动调用该方法.
	 * <p>默认实现为空.
	 */
	protected void interruptTask() {
	}


	private static class SettableTask<T> extends ListenableFutureTask<T> {

		private volatile Thread completingThread;

		@SuppressWarnings("unchecked")
		public SettableTask() {
			super((Callable<T>) DUMMY_CALLABLE);
		}

		public boolean setResultValue(T value) {
			set(value);
			return checkCompletingThread();
		}

		public boolean setExceptionResult(Throwable exception) {
			setException(exception);
			return checkCompletingThread();
		}

		@Override
		protected void done() {
			if (!isCancelled()) {
				// 由set/setException隐式调用:
				// 存储当前线程以确定给定结果是否实际触发完成 (因为 FutureTask.set/setException 不公开它)
				this.completingThread = Thread.currentThread();
			}
			super.done();
		}

		private boolean checkCompletingThread() {
			boolean check = (this.completingThread == Thread.currentThread());
			if (check) {
				this.completingThread = null;  // 只有第一个匹配才算数
			}
			return check;
		}
	}
}
