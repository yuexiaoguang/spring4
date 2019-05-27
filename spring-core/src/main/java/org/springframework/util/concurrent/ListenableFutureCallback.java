package org.springframework.util.concurrent;

/**
 * 来自{@link ListenableFuture}的结果, 成功或失败的回调机制.
 */
public interface ListenableFutureCallback<T> extends SuccessCallback<T>, FailureCallback {

}
