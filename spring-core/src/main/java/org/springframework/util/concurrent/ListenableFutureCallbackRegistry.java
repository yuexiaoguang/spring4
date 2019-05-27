package org.springframework.util.concurrent;

import java.util.LinkedList;
import java.util.Queue;

import org.springframework.util.Assert;

/**
 * {@link ListenableFuture}实现的助手类, 它维护成功和失败回调并帮助通知它们.
 *
 * <p>灵感来自{@code com.google.common.util.concurrent.ExecutionList}.
 */
public class ListenableFutureCallbackRegistry<T> {

	private final Queue<SuccessCallback<? super T>> successCallbacks = new LinkedList<SuccessCallback<? super T>>();

	private final Queue<FailureCallback> failureCallbacks = new LinkedList<FailureCallback>();

	private State state = State.NEW;

	private Object result = null;

	private final Object mutex = new Object();


	/**
	 * 将给定的回调添加到此注册表.
	 * 
	 * @param callback 要添加的回调
	 */
	public void addCallback(ListenableFutureCallback<? super T> callback) {
		Assert.notNull(callback, "'callback' must not be null");
		synchronized (this.mutex) {
			switch (this.state) {
				case NEW:
					this.successCallbacks.add(callback);
					this.failureCallbacks.add(callback);
					break;
				case SUCCESS:
					notifySuccess(callback);
					break;
				case FAILURE:
					notifyFailure(callback);
					break;
			}
		}
	}

	@SuppressWarnings("unchecked")
	private void notifySuccess(SuccessCallback<? super T> callback) {
		try {
			callback.onSuccess((T) this.result);
		}
		catch (Throwable ex) {
			// Ignore
		}
	}

	private void notifyFailure(FailureCallback callback) {
		try {
			callback.onFailure((Throwable) this.result);
		}
		catch (Throwable ex) {
			// Ignore
		}
	}

	/**
	 * 将给定的成功回调添加到此注册表.
	 * 
	 * @param callback 要添加的成功回调
	 */
	@SuppressWarnings("unchecked")
	public void addSuccessCallback(SuccessCallback<? super T> callback) {
		Assert.notNull(callback, "'callback' must not be null");
		synchronized (this.mutex) {
			switch (this.state) {
				case NEW:
					this.successCallbacks.add(callback);
					break;
				case SUCCESS:
					notifySuccess(callback);
					break;
			}
		}
	}

	/**
	 * 将给定的失败回调添加到此注册表.
	 * 
	 * @param callback 要添加的失败回调
	 */
	public void addFailureCallback(FailureCallback callback) {
		Assert.notNull(callback, "'callback' must not be null");
		synchronized (this.mutex) {
			switch (this.state) {
				case NEW:
					this.failureCallbacks.add(callback);
					break;
				case FAILURE:
					notifyFailure(callback);
					break;
			}
		}
	}

	/**
	 * 在具有给定结果的所有添加的回调上触发{@link ListenableFutureCallback#onSuccess(Object)}调用.
	 * 
	 * @param result 用于触发回调的结果
	 */
	public void success(T result) {
		synchronized (this.mutex) {
			this.state = State.SUCCESS;
			this.result = result;
			SuccessCallback<? super T> callback;
			while ((callback = this.successCallbacks.poll()) != null) {
				notifySuccess(callback);
			}
		}
	}

	/**
	 * 使用给定的{@code Throwable}在所有添加的回调上触发{@link ListenableFutureCallback#onFailure(Throwable)}调用.
	 * 
	 * @param ex 用于触发回调的异常
	 */
	public void failure(Throwable ex) {
		synchronized (this.mutex) {
			this.state = State.FAILURE;
			this.result = ex;
			FailureCallback callback;
			while ((callback = this.failureCallbacks.poll()) != null) {
				notifyFailure(callback);
			}
		}
	}


	private enum State {NEW, SUCCESS, FAILURE}

}
