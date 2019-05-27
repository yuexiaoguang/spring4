package org.springframework.messaging.tcp.reactor;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import reactor.fn.Consumer;
import reactor.rx.Promise;

import org.springframework.util.Assert;
import org.springframework.util.concurrent.FailureCallback;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;
import org.springframework.util.concurrent.ListenableFutureCallbackRegistry;
import org.springframework.util.concurrent.SuccessCallback;

/**
 * 使反应器{@link Promise}适配为{@link ListenableFuture},
 * 可选择将结果对象类型{@code <S>}转换为预期的目标类型{@code <T>}.
 *
 * @param <S> {@link Promise}期望的对象类型
 * @param <T> {@link ListenableFuture}期望的对象类型
 */
abstract class AbstractPromiseToListenableFutureAdapter<S, T> implements ListenableFuture<T> {

	private final Promise<S> promise;

	private final ListenableFutureCallbackRegistry<T> registry = new ListenableFutureCallbackRegistry<T>();


	protected AbstractPromiseToListenableFutureAdapter(Promise<S> promise) {
		Assert.notNull(promise, "Promise must not be null");
		this.promise = promise;

		this.promise.onSuccess(new Consumer<S>() {
			@Override
			public void accept(S result) {
				T adapted;
				try {
					adapted = adapt(result);
				}
				catch (Throwable ex) {
					registry.failure(ex);
					return;
				}
				registry.success(adapted);
			}
		});

		this.promise.onError(new Consumer<Throwable>() {
			@Override
			public void accept(Throwable ex) {
				registry.failure(ex);
			}
		});
	}


	@Override
	public T get() throws InterruptedException {
		S result = this.promise.await();
		return adapt(result);
	}

	@Override
	public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
		S result = this.promise.await(timeout, unit);
		if (!this.promise.isComplete()) {
			throw new TimeoutException();
		}
		return adapt(result);
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
		return this.promise.isComplete();
	}

	@Override
	public void addCallback(ListenableFutureCallback<? super T> callback) {
		this.registry.addCallback(callback);
	}

	@Override
	public void addCallback(SuccessCallback<? super T> successCallback, FailureCallback failureCallback) {
		this.registry.addSuccessCallback(successCallback);
		this.registry.addFailureCallback(failureCallback);
	}


	protected abstract T adapt(S result);

}
