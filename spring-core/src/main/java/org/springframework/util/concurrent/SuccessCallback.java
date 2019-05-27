package org.springframework.util.concurrent;

/**
 * {@link ListenableFuture}的成功回调.
 */
public interface SuccessCallback<T> {

	/**
	 * 当{@link ListenableFuture}成功完成时调用.
	 * <p>请注意, 此方法引发的异常将被忽略.
	 * 
	 * @param result 结果
	 */
	void onSuccess(T result);

}
