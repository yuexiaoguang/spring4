package org.springframework.web.servlet.mvc.method.annotation;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.function.BiFunction;

import org.springframework.core.MethodParameter;
import org.springframework.lang.UsesJava8;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.context.request.async.WebAsyncUtils;
import org.springframework.web.method.support.AsyncHandlerMethodReturnValueHandler;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * {@link DeferredResult}, {@link ListenableFuture}, {@link CompletionStage}类型,
 * 以及带有{@link #getAdapterMap() 注册的适配器}的任何其他异步类型的返回值的处理器.
 */
@SuppressWarnings("deprecation")
public class DeferredResultMethodReturnValueHandler implements AsyncHandlerMethodReturnValueHandler {

	private final Map<Class<?>, DeferredResultAdapter> adapterMap;


	public DeferredResultMethodReturnValueHandler() {
		this.adapterMap = new HashMap<Class<?>, DeferredResultAdapter>(5);
		this.adapterMap.put(DeferredResult.class, new SimpleDeferredResultAdapter());
		this.adapterMap.put(ListenableFuture.class, new ListenableFutureAdapter());
		if (ClassUtils.isPresent("java.util.concurrent.CompletionStage", getClass().getClassLoader())) {
			this.adapterMap.put(CompletionStage.class, new CompletionStageAdapter());
		}
	}


	/**
	 * 返回使用{@code DeferredResult}适配器的Map.
	 * <p>默认情况下，映射包含 {@code DeferredResult}的适配器, 它只是向下转型, {@link ListenableFuture}, 和{@link CompletionStage}.
	 * 
	 * @return 适配器的映射
	 * @deprecated in 4.3.8, see comments on {@link DeferredResultAdapter}
	 */
	@Deprecated
	public Map<Class<?>, DeferredResultAdapter> getAdapterMap() {
		return this.adapterMap;
	}

	private DeferredResultAdapter getAdapterFor(Class<?> type) {
		for (Class<?> adapteeType : getAdapterMap().keySet()) {
			if (adapteeType.isAssignableFrom(type)) {
				return getAdapterMap().get(adapteeType);
			}
		}
		return null;
	}


	@Override
	public boolean supportsReturnType(MethodParameter returnType) {
		return (getAdapterFor(returnType.getParameterType()) != null);
	}

	@Override
	public boolean isAsyncReturnValue(Object returnValue, MethodParameter returnType) {
		return (returnValue != null && (getAdapterFor(returnValue.getClass()) != null));
	}

	@Override
	public void handleReturnValue(Object returnValue, MethodParameter returnType,
			ModelAndViewContainer mavContainer, NativeWebRequest webRequest) throws Exception {

		if (returnValue == null) {
			mavContainer.setRequestHandled(true);
			return;
		}

		DeferredResultAdapter adapter = getAdapterFor(returnValue.getClass());
		if (adapter == null) {
			throw new IllegalStateException(
					"Could not find DeferredResultAdapter for return value type: " + returnValue.getClass());
		}
		DeferredResult<?> result = adapter.adaptToDeferredResult(returnValue);
		WebAsyncUtils.getAsyncManager(webRequest).startDeferredResultProcessing(result, mavContainer);
	}


	/**
	 * {@code DeferredResult}返回值的适配器.
	 */
	private static class SimpleDeferredResultAdapter implements DeferredResultAdapter {

		@Override
		public DeferredResult<?> adaptToDeferredResult(Object returnValue) {
			Assert.isInstanceOf(DeferredResult.class, returnValue, "DeferredResult expected");
			return (DeferredResult<?>) returnValue;
		}
	}


	/**
	 * {@code ListenableFuture}返回值的适配器.
	 */
	private static class ListenableFutureAdapter implements DeferredResultAdapter {

		@Override
		public DeferredResult<?> adaptToDeferredResult(Object returnValue) {
			Assert.isInstanceOf(ListenableFuture.class, returnValue, "ListenableFuture expected");
			final DeferredResult<Object> result = new DeferredResult<Object>();
			((ListenableFuture<?>) returnValue).addCallback(new ListenableFutureCallback<Object>() {
				@Override
				public void onSuccess(Object value) {
					result.setResult(value);
				}
				@Override
				public void onFailure(Throwable ex) {
					result.setErrorResult(ex);
				}
			});
			return result;
		}
	}


	/**
	 * {@code CompletionStage}返回值的适配器.
	 */
	@UsesJava8
	private static class CompletionStageAdapter implements DeferredResultAdapter {

		@Override
		public DeferredResult<?> adaptToDeferredResult(Object returnValue) {
			Assert.isInstanceOf(CompletionStage.class, returnValue, "CompletionStage expected");
			final DeferredResult<Object> result = new DeferredResult<Object>();
			@SuppressWarnings("unchecked")
			CompletionStage<?> future = (CompletionStage<?>) returnValue;
			future.handle(new BiFunction<Object, Throwable, Object>() {
				@Override
				public Object apply(Object value, Throwable ex) {
					if (ex != null) {
						result.setErrorResult(ex);
					}
					else {
						result.setResult(value);
					}
					return null;
				}
			});
			return result;
		}
	}

}
