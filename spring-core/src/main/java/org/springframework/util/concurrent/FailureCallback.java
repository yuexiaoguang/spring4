package org.springframework.util.concurrent;

/**
 * {@link ListenableFuture}的失败回调.
 */
public interface FailureCallback {

	/**
	 * 当{@link ListenableFuture}以失败告终时调用.
	 * <p>请注意, 此方法引发的异常将被忽略.
	 * 
	 * @param ex 失败
	 */
	void onFailure(Throwable ex);

}
