package org.springframework.util.concurrent;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiFunction;

import org.springframework.lang.UsesJava8;

/**
 * 将{@link CompletableFuture}或{@link CompletionStage}适配为Spring {@link ListenableFuture}.
 */
@UsesJava8
public class CompletableToListenableFutureAdapter<T> implements ListenableFuture<T> {

	private final CompletableFuture<T> completableFuture;

	private final ListenableFutureCallbackRegistry<T> callbacks = new ListenableFutureCallbackRegistry<T>();


	public CompletableToListenableFutureAdapter(CompletionStage<T> completionStage) {
		this(completionStage.toCompletableFuture());
	}

	public CompletableToListenableFutureAdapter(CompletableFuture<T> completableFuture) {
		this.completableFuture = completableFuture;
		this.completableFuture.handle(new BiFunction<T, Throwable, Object>() {
			@Override
			public Object apply(T result, Throwable ex) {
				if (ex != null) {
					callbacks.failure(ex);
				}
				else {
					callbacks.success(result);
				}
				return null;
			}
		});
	}


	@Override
	public void addCallback(ListenableFutureCallback<? super T> callback) {
		this.callbacks.addCallback(callback);
	}

	@Override
	public void addCallback(SuccessCallback<? super T> successCallback, FailureCallback failureCallback) {
		this.callbacks.addSuccessCallback(successCallback);
		this.callbacks.addFailureCallback(failureCallback);
	}

	@Override
	public boolean cancel(boolean mayInterruptIfRunning) {
		return this.completableFuture.cancel(mayInterruptIfRunning);
	}

	@Override
	public boolean isCancelled() {
		return this.completableFuture.isCancelled();
	}

	@Override
	public boolean isDone() {
		return this.completableFuture.isDone();
	}

	@Override
	public T get() throws InterruptedException, ExecutionException {
		return this.completableFuture.get();
	}

	@Override
	public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
		return this.completableFuture.get(timeout, unit);
	}

}
