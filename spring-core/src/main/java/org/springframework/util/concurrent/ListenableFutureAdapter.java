package org.springframework.util.concurrent;

import java.util.concurrent.ExecutionException;

/**
 * 抽象类, 将通过S参数化的{@link ListenableFuture}适配为通过T参数化的{@code ListenableFuture}.
 * 所有方法都委托给适配器, 其中{@link #get()}, {@link #get(long, java.util.concurrent.TimeUnit)},
 * 和{@link ListenableFutureCallback#onSuccess(Object)}在适配器的结果上调用{@link #adapt(Object)}.
 *
 * @param <T> 这个{@code Future}的类型
 * @param <S> 适配器的{@code Future}的类型
 */
public abstract class ListenableFutureAdapter<T, S> extends FutureAdapter<T, S> implements ListenableFuture<T> {

	/**
	 * @param adaptee 要适配的ListenableFuture
	 */
	protected ListenableFutureAdapter(ListenableFuture<S> adaptee) {
		super(adaptee);
	}


	@Override
	public void addCallback(final ListenableFutureCallback<? super T> callback) {
		addCallback(callback, callback);
	}

	@Override
	public void addCallback(final SuccessCallback<? super T> successCallback, final FailureCallback failureCallback) {
		ListenableFuture<S> listenableAdaptee = (ListenableFuture<S>) getAdaptee();
		listenableAdaptee.addCallback(new ListenableFutureCallback<S>() {
			@Override
			public void onSuccess(S result) {
				T adapted;
				try {
					adapted = adaptInternal(result);
				}
				catch (ExecutionException ex) {
					Throwable cause = ex.getCause();
					onFailure(cause != null ? cause : ex);
					return;
				}
				catch (Throwable ex) {
					onFailure(ex);
					return;
				}
				successCallback.onSuccess(adapted);
			}
			@Override
			public void onFailure(Throwable ex) {
				failureCallback.onFailure(ex);
			}
		});
	}

}
