package org.springframework.messaging.handler.annotation.support;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.core.MethodIntrospector;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.invocation.AbstractExceptionHandlerMethodResolver;
import org.springframework.util.ReflectionUtils.MethodFilter;

/**
 * {@link AbstractExceptionHandlerMethodResolver}的子类, 用于查找给定类中的带{@link MessageExceptionHandler}注解的方法.
 * 处理的实际异常类型可以从注解中提取, 也可以从方法签名中提取作为后备选项.
 */
public class AnnotationExceptionHandlerMethodResolver extends AbstractExceptionHandlerMethodResolver {

	/**
	 * 在给定类型中查找{@link MessageExceptionHandler}方法的构造函数.
	 * 
	 * @param handlerType 要内省的类型
	 */
	public AnnotationExceptionHandlerMethodResolver(Class<?> handlerType) {
		super(initExceptionMappings(handlerType));
	}

	private static Map<Class<? extends Throwable>, Method> initExceptionMappings(Class<?> handlerType) {
		Map<Method, MessageExceptionHandler> methods = MethodIntrospector.selectMethods(handlerType,
				new MethodIntrospector.MetadataLookup<MessageExceptionHandler>() {
					@Override
					public MessageExceptionHandler inspect(Method method) {
						return AnnotationUtils.findAnnotation(method, MessageExceptionHandler.class);
					}
				});

		Map<Class<? extends Throwable>, Method> result = new HashMap<Class<? extends Throwable>, Method>();
		for (Map.Entry<Method, MessageExceptionHandler> entry : methods.entrySet()) {
			Method method = entry.getKey();
			List<Class<? extends Throwable>> exceptionTypes = new ArrayList<Class<? extends Throwable>>();
			exceptionTypes.addAll(Arrays.asList(entry.getValue().value()));
			if (exceptionTypes.isEmpty()) {
				exceptionTypes.addAll(getExceptionsFromMethodSignature(method));
			}
			for (Class<? extends Throwable> exceptionType : exceptionTypes) {
				Method oldMethod = result.put(exceptionType, method);
				if (oldMethod != null && !oldMethod.equals(method)) {
					throw new IllegalStateException("Ambiguous @ExceptionHandler method mapped for [" +
							exceptionType + "]: {" + oldMethod + ", " + method + "}");
				}
			}
		}
		return result;
	}


	/**
	 * 用于选择带注解的异常处理方法的过滤器.
	 * 
	 * @deprecated as of Spring 4.2.3, since it isn't used anymore
	 */
	@Deprecated
	public final static MethodFilter EXCEPTION_HANDLER_METHOD_FILTER = new MethodFilter() {

		@Override
		public boolean matches(Method method) {
			return AnnotationUtils.findAnnotation(method, MessageExceptionHandler.class) != null;
		}
	};

}
