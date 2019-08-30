package org.springframework.web.portlet.mvc.annotation;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.portlet.ClientDataRequest;
import javax.portlet.Event;
import javax.portlet.EventRequest;
import javax.portlet.MimeResponse;
import javax.portlet.PortalContext;
import javax.portlet.PortletMode;
import javax.portlet.PortletPreferences;
import javax.portlet.PortletRequest;
import javax.portlet.PortletResponse;
import javax.portlet.PortletSession;
import javax.portlet.WindowState;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.core.ExceptionDepthComparator;
import org.springframework.core.GenericTypeResolver;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.SynthesizingMethodParameter;
import org.springframework.ui.Model;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.support.WebArgumentResolver;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.portlet.ModelAndView;
import org.springframework.web.portlet.context.PortletWebRequest;
import org.springframework.web.portlet.handler.AbstractHandlerExceptionResolver;
import org.springframework.web.servlet.View;

/**
 * {@link org.springframework.web.portlet.HandlerExceptionResolver}接口的实现,
 * 通过{@link ExceptionHandler}注解处理异常.
 *
 * <p>默认情况下, 在{@link org.springframework.web.portlet.DispatcherPortlet}中启用此异常解析器.
 */
public class AnnotationMethodHandlerExceptionResolver extends AbstractHandlerExceptionResolver {

	/**
	 * 任意{@link Method}引用, 表示在缓存中找不到任何方法.
	 */
	private static final Method NO_METHOD_FOUND = ClassUtils.getMethodIfAvailable(System.class, "currentTimeMillis");


	private final Map<Class<?>, Map<Class<? extends Throwable>, Method>> exceptionHandlerCache =
			new ConcurrentHashMap<Class<?>, Map<Class<? extends Throwable>, Method>>(64);

	private WebArgumentResolver[] customArgumentResolvers;


	/**
	 * 设置用于特殊方法参数类型的自定义ArgumentResolvers.
	 * <p>这样的自定义ArgumentResolver将首先启动, 有机会在标准参数处理开始之前解析参数值.
	 */
	public void setCustomArgumentResolver(WebArgumentResolver argumentResolver) {
		this.customArgumentResolvers = new WebArgumentResolver[]{argumentResolver};
	}

	/**
	 * 设置用于特殊方法参数类型的一个或多个自定义ArgumentResolvers.
	 * <p>任何这样的自定义ArgumentResolver将首先启动, 有机会在标准参数处理开始之前解析参数值.
	 */
	public void setCustomArgumentResolvers(WebArgumentResolver[] argumentResolvers) {
		this.customArgumentResolvers = argumentResolvers;
	}


	@Override
	protected ModelAndView doResolveException(
			PortletRequest request, MimeResponse response, Object handler, Exception ex) {

		if (handler != null) {
			Method handlerMethod = findBestExceptionHandlerMethod(handler, ex);
			if (handlerMethod != null) {
				NativeWebRequest webRequest = new PortletWebRequest(request, response);
				try {
					Object[] args = resolveHandlerArguments(handlerMethod, handler, webRequest, ex);
					if (logger.isDebugEnabled()) {
						logger.debug("Invoking request handler method: " + handlerMethod);
					}
					Object retVal = doInvokeMethod(handlerMethod, handler, args);
					return getModelAndView(retVal);
				}
				catch (Exception invocationEx) {
					logger.error("Invoking request method resulted in exception : " + handlerMethod, invocationEx);
				}
			}
		}
		return null;
	}

	/**
	 * 查找最佳匹配抛出的异常的处理器方法.
	 * 
	 * @param handler 处理器对象
	 * @param thrownException 要处理的异常
	 * 
	 * @return 最佳匹配方法; 或{@code null}
	 */
	private Method findBestExceptionHandlerMethod(Object handler, final Exception thrownException) {
		final Class<?> handlerType = handler.getClass();
		final Class<? extends Throwable> thrownExceptionType = thrownException.getClass();
		Method handlerMethod;

		Map<Class<? extends Throwable>, Method> handlers = this.exceptionHandlerCache.get(handlerType);
		if (handlers != null) {
			handlerMethod = handlers.get(thrownExceptionType);
			if (handlerMethod != null) {
				return (handlerMethod == NO_METHOD_FOUND ? null : handlerMethod);
			}
		}
		else {
			handlers = new ConcurrentHashMap<Class<? extends Throwable>, Method>(16);
			this.exceptionHandlerCache.put(handlerType, handlers);
		}

		final Map<Class<? extends Throwable>, Method> matchedHandlers = new HashMap<Class<? extends Throwable>, Method>();

		ReflectionUtils.doWithMethods(handlerType, new ReflectionUtils.MethodCallback() {
			@Override
			public void doWith(Method method) {
				method = ClassUtils.getMostSpecificMethod(method, handlerType);
				List<Class<? extends Throwable>> handledExceptions = getHandledExceptions(method);
				for (Class<? extends Throwable> handledException : handledExceptions) {
					if (handledException.isAssignableFrom(thrownExceptionType)) {
						if (!matchedHandlers.containsKey(handledException)) {
							matchedHandlers.put(handledException, method);
						}
						else {
							Method oldMappedMethod = matchedHandlers.get(handledException);
							if (!oldMappedMethod.equals(method)) {
								throw new IllegalStateException(
										"Ambiguous exception handler mapped for " + handledException + "]: {" +
												oldMappedMethod + ", " + method + "}.");
							}
						}
					}
				}
			}
		});

		handlerMethod = getBestMatchingMethod(matchedHandlers, thrownException);
		handlers.put(thrownExceptionType, (handlerMethod == null ? NO_METHOD_FOUND : handlerMethod));
		return handlerMethod;
	}

	/**
	 * 返回给定方法处理的所有异常类.
	 * <p>默认实现在{@linkplain ExceptionHandler#value() 注解}中查找异常,
	 * 或者 - 如果该注解元素为空 - 如果方法使用{@code @ExceptionHandler}注解, 则为方法参数中列出的任何异常.
	 * 
	 * @param method 方法
	 * 
	 * @return 处理的异常
	 */
	@SuppressWarnings("unchecked")
	protected List<Class<? extends Throwable>> getHandledExceptions(Method method) {
		List<Class<? extends Throwable>> result = new ArrayList<Class<? extends Throwable>>();
		ExceptionHandler exceptionHandler = AnnotationUtils.findAnnotation(method, ExceptionHandler.class);
		if (exceptionHandler != null) {
			if (!ObjectUtils.isEmpty(exceptionHandler.value())) {
				result.addAll(Arrays.asList(exceptionHandler.value()));
			}
			else {
				for (Class<?> param : method.getParameterTypes()) {
					if (Throwable.class.isAssignableFrom(param)) {
						result.add((Class<? extends Throwable>) param);
					}
				}
			}
		}
		return result;
	}

	/**
	 * 使用{@link ExceptionDepthComparator}查找最佳匹配方法.
	 * 
	 * @return 最佳匹配方法, 或{@code null}
	 */
	private Method getBestMatchingMethod(
			Map<Class<? extends Throwable>, Method> resolverMethods, Exception thrownException) {

		if (resolverMethods.isEmpty()) {
			return null;
		}
		Class<? extends Throwable> closestMatch =
				ExceptionDepthComparator.findClosestMatch(resolverMethods.keySet(), thrownException);
		Method method = resolverMethods.get(closestMatch);
		return (method == null || NO_METHOD_FOUND == method ? null : method);
	}

	/**
	 * 解析给定方法的参数. 委托给{@link #resolveCommonArgument}.
	 */
	private Object[] resolveHandlerArguments(Method handlerMethod, Object handler,
			NativeWebRequest webRequest, Exception thrownException) throws Exception {

		Class<?>[] paramTypes = handlerMethod.getParameterTypes();
		Object[] args = new Object[paramTypes.length];
		Class<?> handlerType = handler.getClass();
		for (int i = 0; i < args.length; i++) {
			MethodParameter methodParam = new SynthesizingMethodParameter(handlerMethod, i);
			GenericTypeResolver.resolveParameterType(methodParam, handlerType);
			Class<?> paramType = methodParam.getParameterType();
			Object argValue = resolveCommonArgument(methodParam, webRequest, thrownException);
			if (argValue != WebArgumentResolver.UNRESOLVED) {
				args[i] = argValue;
			}
			else {
				throw new IllegalStateException("Unsupported argument [" + paramType.getName() +
						"] for @ExceptionHandler method: " + handlerMethod);
			}
		}
		return args;
	}

	/**
	 * 解析常见的方法参数.
	 * 首先委托给注册的{@link #setCustomArgumentResolver argumentResolvers}, 然后检查{@link #resolveStandardArgument}.
	 * 
	 * @param methodParameter 方法参数
	 * @param webRequest 请求
	 * @param thrownException 抛出的异常
	 * 
	 * @return 参数值, 或{@link org.springframework.web.bind.support.WebArgumentResolver#UNRESOLVED}
	 */
	protected Object resolveCommonArgument(MethodParameter methodParameter, NativeWebRequest webRequest,
			Exception thrownException) throws Exception {

		// 调用自定义参数解析器...
		if (this.customArgumentResolvers != null) {
			for (WebArgumentResolver argumentResolver : this.customArgumentResolvers) {
				Object value = argumentResolver.resolveArgument(methodParameter, webRequest);
				if (value != WebArgumentResolver.UNRESOLVED) {
					return value;
				}
			}
		}

		// 标准参数类型的解析...
		Class<?> paramType = methodParameter.getParameterType();
		Object value = resolveStandardArgument(paramType, webRequest, thrownException);
		if (value != WebArgumentResolver.UNRESOLVED && !ClassUtils.isAssignableValue(paramType, value)) {
			throw new IllegalStateException("Standard argument type [" + paramType.getName() +
					"] resolved to incompatible value of type [" + (value != null ? value.getClass() : null) +
					"]. Consider declaring the argument type in a less specific fashion.");
		}
		return value;
	}

	/**
	 * 解析标准方法参数.
	 * 默认实现处理{@link NativeWebRequest}, {@link ServletRequest}, {@link ServletResponse}, {@link HttpSession}, {@link Principal},
	 * {@link Locale}, 请求{@link InputStream}, 请求{@link Reader}, 响应{@link OutputStream},
	 * 响应{@link Writer}, 和给定的{@code thrownException}.
	 * 
	 * @param parameterType 方法参数类型
	 * @param webRequest 请求
	 * @param thrownException 抛出的请求
	 * 
	 * @return 参数值, 或{@link org.springframework.web.bind.support.WebArgumentResolver#UNRESOLVED}
	 */
	protected Object resolveStandardArgument(Class<?> parameterType, NativeWebRequest webRequest,
			Exception thrownException) throws Exception {

		if (parameterType.isInstance(thrownException)) {
			return thrownException;
		}
		else if (WebRequest.class.isAssignableFrom(parameterType)) {
			return webRequest;
		}

		PortletRequest request = webRequest.getNativeRequest(PortletRequest.class);
		PortletResponse response = webRequest.getNativeResponse(PortletResponse.class);

		if (PortletRequest.class.isAssignableFrom(parameterType)) {
			return request;
		}
		else if (PortletResponse.class.isAssignableFrom(parameterType)) {
			return response;
		}
		else if (PortletSession.class.isAssignableFrom(parameterType)) {
			return request.getPortletSession();
		}
		else if (PortletPreferences.class.isAssignableFrom(parameterType)) {
			return request.getPreferences();
		}
		else if (PortletMode.class.isAssignableFrom(parameterType)) {
			return request.getPortletMode();
		}
		else if (WindowState.class.isAssignableFrom(parameterType)) {
			return request.getWindowState();
		}
		else if (PortalContext.class.isAssignableFrom(parameterType)) {
			return request.getPortalContext();
		}
		else if (Principal.class.isAssignableFrom(parameterType)) {
			return request.getUserPrincipal();
		}
		else if (Locale.class == parameterType) {
			return request.getLocale();
		}
		else if (InputStream.class.isAssignableFrom(parameterType)) {
			if (!(request instanceof ClientDataRequest)) {
				throw new IllegalStateException("InputStream can only get obtained for Action/ResourceRequest");
			}
			return ((ClientDataRequest) request).getPortletInputStream();
		}
		else if (Reader.class.isAssignableFrom(parameterType)) {
			if (!(request instanceof ClientDataRequest)) {
				throw new IllegalStateException("Reader can only get obtained for Action/ResourceRequest");
			}
			return ((ClientDataRequest) request).getReader();
		}
		else if (OutputStream.class.isAssignableFrom(parameterType)) {
			if (!(response instanceof MimeResponse)) {
				throw new IllegalStateException("OutputStream can only get obtained for Render/ResourceResponse");
			}
			return ((MimeResponse) response).getPortletOutputStream();
		}
		else if (Writer.class.isAssignableFrom(parameterType)) {
			if (!(response instanceof MimeResponse)) {
				throw new IllegalStateException("Writer can only get obtained for Render/ResourceResponse");
			}
			return ((MimeResponse) response).getWriter();
		}
		else if (Event.class == parameterType) {
			if (!(request instanceof EventRequest)) {
				throw new IllegalStateException("Event can only get obtained from EventRequest");
			}
			return ((EventRequest) request).getEvent();
		}
		else {
			return WebArgumentResolver.UNRESOLVED;
		}
	}

	private Object doInvokeMethod(Method method, Object target, Object[] args) throws Exception {
		ReflectionUtils.makeAccessible(method);
		try {
			return method.invoke(target, args);
		}
		catch (InvocationTargetException ex) {
			ReflectionUtils.rethrowException(ex.getTargetException());
		}
		throw new IllegalStateException("Should never get here");
	}

	@SuppressWarnings("unchecked")
	private ModelAndView getModelAndView(Object returnValue) {
		if (returnValue instanceof ModelAndView) {
			return (ModelAndView) returnValue;
		}
		else if (returnValue instanceof Model) {
			return new ModelAndView().addAllObjects(((Model) returnValue).asMap());
		}
		else if (returnValue instanceof Map) {
			return new ModelAndView().addAllObjects((Map<String, Object>) returnValue);
		}
		else if (returnValue instanceof View) {
			return new ModelAndView(returnValue);
		}
		else if (returnValue instanceof String) {
			return new ModelAndView((String) returnValue);
		}
		else if (returnValue == null) {
			return new ModelAndView();
		}
		else {
			throw new IllegalArgumentException("Invalid handler method return value: " + returnValue);
		}
	}

}
