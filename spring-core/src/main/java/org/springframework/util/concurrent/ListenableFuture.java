package org.springframework.util.concurrent;

import java.util.concurrent.Future;

/**
 * 扩展{@link Future}, 具有接受完成回调的功能.
 * 如果在添加回调时, Future已完成, 则立即触发回调.
 *
 * <p>灵感来自{@code com.google.common.util.concurrent.ListenableFuture}.
 */
public interface ListenableFuture<T> extends Future<T> {

	/**
	 * 注册给定的{@code ListenableFutureCallback}.
	 * 
	 * @param callback 要注册的回调
	 */
	void addCallback(ListenableFutureCallback<? super T> callback);

	/**
	 * Java 8 lambda友好的替代方案, 具有成功和失败的回调.
	 * 
	 * @param successCallback 成功的回调
	 * @param failureCallback 失败的回调
	 */
	void addCallback(SuccessCallback<? super T> successCallback, FailureCallback failureCallback);

}
