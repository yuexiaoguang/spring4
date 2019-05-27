package org.springframework.messaging.handler.invocation;

import org.springframework.core.MethodParameter;
import org.springframework.util.concurrent.ListenableFuture;

/**
 * {@link HandlerMethodReturnValueHandler}的扩展, 用于处理支持成功和错误回调的异步, 类似Future的返回值类型.
 * 基本上可以适配于{@link ListenableFuture}的东西.
 *
 * <p>实现应该考虑扩展基类{@link AbstractAsyncReturnValueHandler}.
 */
public interface AsyncHandlerMethodReturnValueHandler extends HandlerMethodReturnValueHandler {

	/**
	 * 返回值是否表示具有成功和错误回调的异步, 类似Future的类型.
	 * 如果此方法返回{@code true}, 则接下来调用{@link #toListenableFuture}.
	 * 如果它返回{@code false}, 则调用{@link #handleReturnValue}.
	 * <p><strong>Note:</strong> 只有在调用{@link #supportsReturnType(org.springframework.core.MethodParameter)}
	 * 并且它返回{@code true}之后才会调用此方法.
	 * 
	 * @param returnValue 处理器方法返回的值
	 * @param returnType 返回值的类型
	 * 
	 * @return 如果返回值类型表示异步值, 则返回true.
	 */
	boolean isAsyncReturnValue(Object returnValue, MethodParameter returnType);

	/**
	 * 将异步返回值适配为{@link ListenableFuture}.
	 * 实现应该考虑返回{@link org.springframework.util.concurrent.SettableListenableFuture SettableListenableFuture}的实例.
	 * 当ListenableFuture以成功或错误完成时, 返回值处理将继续.
	 * <p><strong>Note:</strong> 只有在调用{@link #supportsReturnType(org.springframework.core.MethodParameter)}
	 * 并且它返回{@code true}之后才会调用此方法.
	 * 
	 * @param returnValue 处理器方法返回的值
	 * @param returnType 返回值的类型
	 * 
	 * @return 生成的ListenableFuture, 或 {@code null} 在这种情况下不会执行进一步的处理
	 */
	ListenableFuture<?> toListenableFuture(Object returnValue, MethodParameter returnType);

}
