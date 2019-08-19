package org.springframework.web.servlet.mvc.method.annotation;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.concurrent.Callable;

import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.http.HttpStatus;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.method.support.HandlerMethodReturnValueHandlerComposite;
import org.springframework.web.method.support.InvocableHandlerMethod;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.View;
import org.springframework.web.util.NestedServletException;

/**
 * 扩展{@link InvocableHandlerMethod}, 能够通过已注册的{@link HandlerMethodReturnValueHandler}处理返回值,
 * 并且还支持根据方法级{@code @ResponseStatus}注解设置响应状态.
 *
 * <p>{@code null}返回值 (包括void) 可以被解释为请求处理的结束与{@code @ResponseStatus}注解,
 * 一个未修改的检查条件 (see {@link ServletWebRequest#checkNotModified(long)}),
 * 或提供对响应流的访问的方法参数的组合.
 */
public class ServletInvocableHandlerMethod extends InvocableHandlerMethod {

	private static final Method CALLABLE_METHOD = ClassUtils.getMethod(Callable.class, "call");

	private HandlerMethodReturnValueHandlerComposite returnValueHandlers;


	public ServletInvocableHandlerMethod(Object handler, Method method) {
		super(handler, method);
	}

	public ServletInvocableHandlerMethod(HandlerMethod handlerMethod) {
		super(handlerMethod);
	}


	/**
	 * 注册用于处理返回值的{@link HandlerMethodReturnValueHandler}实例.
	 */
	public void setHandlerMethodReturnValueHandlers(HandlerMethodReturnValueHandlerComposite returnValueHandlers) {
		this.returnValueHandlers = returnValueHandlers;
	}


	/**
	 * 调用该方法并通过其中一个配置的{@link HandlerMethodReturnValueHandler}处理返回值.
	 * 
	 * @param webRequest 当前的请求
	 * @param mavContainer 此请求的ModelAndViewContainer
	 * @param providedArgs 按类型匹配的"given"参数 (未解析)
	 */
	public void invokeAndHandle(ServletWebRequest webRequest, ModelAndViewContainer mavContainer,
			Object... providedArgs) throws Exception {

		Object returnValue = invokeForRequest(webRequest, mavContainer, providedArgs);
		setResponseStatus(webRequest);

		if (returnValue == null) {
			if (isRequestNotModified(webRequest) || getResponseStatus() != null || mavContainer.isRequestHandled()) {
				mavContainer.setRequestHandled(true);
				return;
			}
		}
		else if (StringUtils.hasText(getResponseStatusReason())) {
			mavContainer.setRequestHandled(true);
			return;
		}

		mavContainer.setRequestHandled(false);
		try {
			this.returnValueHandlers.handleReturnValue(
					returnValue, getReturnValueType(returnValue), mavContainer, webRequest);
		}
		catch (Exception ex) {
			if (logger.isTraceEnabled()) {
				logger.trace(getReturnValueHandlingErrorMessage("Error handling return value", returnValue), ex);
			}
			throw ex;
		}
	}

	/**
	 * 根据{@link ResponseStatus}注解设置响应状态.
	 */
	private void setResponseStatus(ServletWebRequest webRequest) throws IOException {
		HttpStatus status = getResponseStatus();
		if (status == null) {
			return;
		}

		String reason = getResponseStatusReason();
		if (StringUtils.hasText(reason)) {
			webRequest.getResponse().sendError(status.value(), reason);
		}
		else {
			webRequest.getResponse().setStatus(status.value());
		}

		// 被RedirectView选中
		webRequest.getRequest().setAttribute(View.RESPONSE_STATUS_ATTRIBUTE, status);
	}

	/**
	 * 给定的请求是否符合"未修改"的条件?
	 */
	private boolean isRequestNotModified(ServletWebRequest webRequest) {
		return webRequest.isNotModified();
	}

	private String getReturnValueHandlingErrorMessage(String message, Object returnValue) {
		StringBuilder sb = new StringBuilder(message);
		if (returnValue != null) {
			sb.append(" [type=").append(returnValue.getClass().getName()).append("]");
		}
		sb.append(" [value=").append(returnValue).append("]");
		return getDetailedErrorMessage(sb.toString());
	}

	/**
	 * 创建一个嵌套的ServletInvocableHandlerMethod子类, 它返回给定的值 (如果值为1则引发异常), 而不是实际调用控制器方法.
	 * 处理异步返回值时很有用 (e.g. Callable, DeferredResult, ListenableFuture).
	 */
	ServletInvocableHandlerMethod wrapConcurrentResult(Object result) {
		return new ConcurrentResultHandlerMethod(result, new ConcurrentResultMethodParameter(result));
	}


	/**
	 * {@code ServletInvocableHandlerMethod}的嵌套子类, 它使用简单的{@link Callable}而不是原始控制器作为处理器,
	 * 以便返回给定的固定 (并发) 结果值.
	 * 使用异步生成的返回值有效地"恢复"处理.
	 */
	private class ConcurrentResultHandlerMethod extends ServletInvocableHandlerMethod {

		private final MethodParameter returnType;

		public ConcurrentResultHandlerMethod(final Object result, ConcurrentResultMethodParameter returnType) {
			super(new Callable<Object>() {
				@Override
				public Object call() throws Exception {
					if (result instanceof Exception) {
						throw (Exception) result;
					}
					else if (result instanceof Throwable) {
						throw new NestedServletException("Async processing failed", (Throwable) result);
					}
					return result;
				}
			}, CALLABLE_METHOD);

			setHandlerMethodReturnValueHandlers(ServletInvocableHandlerMethod.this.returnValueHandlers);
			this.returnType = returnType;
		}

		/**
		 * 桥接到实际控制器类型级别注解.
		 */
		@Override
		public Class<?> getBeanType() {
			return ServletInvocableHandlerMethod.this.getBeanType();
		}

		/**
		 * 桥接到声明的异步返回类型中的实际返回值或泛型类型, e.g. Foo 而不是 {@code DeferredResult<Foo>}.
		 */
		@Override
		public MethodParameter getReturnValueType(Object returnValue) {
			return this.returnType;
		}

		/**
		 * 桥接到控制器方法级别注解.
		 */
		@Override
		public <A extends Annotation> A getMethodAnnotation(Class<A> annotationType) {
			return ServletInvocableHandlerMethod.this.getMethodAnnotation(annotationType);
		}

		/**
		 * 桥接到控制器方法级别注解.
		 */
		@Override
		public <A extends Annotation> boolean hasMethodAnnotation(Class<A> annotationType) {
			return ServletInvocableHandlerMethod.this.hasMethodAnnotation(annotationType);
		}
	}


	/**
	 * MethodParameter子类, 基于实际的返回值类型, 或者如果是null, 返回到声明的异步返回类型中的泛型类型,
	 * e.g. Foo 而不是 {@code DeferredResult<Foo>}.
	 */
	private class ConcurrentResultMethodParameter extends HandlerMethodParameter {

		private final Object returnValue;

		private final ResolvableType returnType;

		public ConcurrentResultMethodParameter(Object returnValue) {
			super(-1);
			this.returnValue = returnValue;
			this.returnType = ResolvableType.forType(super.getGenericParameterType()).getGeneric(0);
		}

		public ConcurrentResultMethodParameter(ConcurrentResultMethodParameter original) {
			super(original);
			this.returnValue = original.returnValue;
			this.returnType = original.returnType;
		}

		@Override
		public Class<?> getParameterType() {
			if (this.returnValue != null) {
				return this.returnValue.getClass();
			}
			if (!ResolvableType.NONE.equals(this.returnType)) {
				return this.returnType.resolve(Object.class);
			}
			return super.getParameterType();
		}

		@Override
		public Type getGenericParameterType() {
			return this.returnType.getType();
		}

		@Override
		public ConcurrentResultMethodParameter clone() {
			return new ConcurrentResultMethodParameter(this);
		}
	}

}
