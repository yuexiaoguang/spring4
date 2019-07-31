package org.springframework.web.method.support;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.MethodParameter;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.HandlerMethod;

/**
 * 提供一种方法, 用于在通过已注册的{@link HandlerMethodArgumentResolver}解析其方法参数值后, 调用给定请求的处理器方法.
 *
 * <p>参数解析通常需要{@link WebDataBinder}来进行数据绑定或类型转换.
 * 使用{@link #setDataBinderFactory(WebDataBinderFactory)}属性提供一个绑定工厂以传递给参数解析器.
 *
 * <p>使用{@link #setHandlerMethodArgumentResolvers}自定义参数解析器列表.
 */
public class InvocableHandlerMethod extends HandlerMethod {

	private WebDataBinderFactory dataBinderFactory;

	private HandlerMethodArgumentResolverComposite argumentResolvers = new HandlerMethodArgumentResolverComposite();

	private ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();


	public InvocableHandlerMethod(HandlerMethod handlerMethod) {
		super(handlerMethod);
	}

	public InvocableHandlerMethod(Object bean, Method method) {
		super(bean, method);
	}

	/**
	 * @param bean 对象bean
	 * @param methodName 方法名称
	 * @param parameterTypes 方法参数类型
	 * 
	 * @throws NoSuchMethodException 当无法找到方法时
	 */
	public InvocableHandlerMethod(Object bean, String methodName, Class<?>... parameterTypes)
			throws NoSuchMethodException {

		super(bean, methodName, parameterTypes);
	}


	/**
	 * 设置要传递给参数解析器的{@link WebDataBinderFactory}, 允许创建{@link WebDataBinder}用于数据绑定和类型转换.
	 * 
	 * @param dataBinderFactory 数据绑定器工厂.
	 */
	public void setDataBinderFactory(WebDataBinderFactory dataBinderFactory) {
		this.dataBinderFactory = dataBinderFactory;
	}

	/**
	 * 设置用于解析方法参数值的{@link HandlerMethodArgumentResolver}.
	 */
	public void setHandlerMethodArgumentResolvers(HandlerMethodArgumentResolverComposite argumentResolvers) {
		this.argumentResolvers = argumentResolvers;
	}

	/**
	 * 设置在需要时解析参数名称的ParameterNameDiscoverer (e.g. 默认请求属性名称).
	 * <p>默认{@link org.springframework.core.DefaultParameterNameDiscoverer}.
	 */
	public void setParameterNameDiscoverer(ParameterNameDiscoverer parameterNameDiscoverer) {
		this.parameterNameDiscoverer = parameterNameDiscoverer;
	}


	/**
	 * 在给定请求的上下文中解析其参数值后调用该方法.
	 * <p>参数值通常通过{@link HandlerMethodArgumentResolver}来解析.
	 * 然而, {@code providedArgs}参数可以提供要直接使用的参数值, i.e. 没有参数解析.
	 * 提供的参数值的示例包括{@link WebDataBinder}, {@link SessionStatus}, 或抛出的异常实例.
	 * 在参数解析器之前检查提供的参数值.
	 * 
	 * @param request 当前的请求
	 * @param mavContainer 此请求的ModelAndViewContainer
	 * @param providedArgs 与类型匹配的"给定"参数, 未解析
	 * 
	 * @return 调用方法返回的原始值
	 * @throws Exception 如果找不到合适的参数解析器, 或者方法引发异常
	 */
	public Object invokeForRequest(NativeWebRequest request, ModelAndViewContainer mavContainer,
			Object... providedArgs) throws Exception {

		Object[] args = getMethodArgumentValues(request, mavContainer, providedArgs);
		if (logger.isTraceEnabled()) {
			logger.trace("Invoking '" + ClassUtils.getQualifiedMethodName(getMethod(), getBeanType()) +
					"' with arguments " + Arrays.toString(args));
		}
		Object returnValue = doInvoke(args);
		if (logger.isTraceEnabled()) {
			logger.trace("Method [" + ClassUtils.getQualifiedMethodName(getMethod(), getBeanType()) +
					"] returned [" + returnValue + "]");
		}
		return returnValue;
	}

	/**
	 * 获取当前请求的方法参数值.
	 */
	private Object[] getMethodArgumentValues(NativeWebRequest request, ModelAndViewContainer mavContainer,
			Object... providedArgs) throws Exception {

		MethodParameter[] parameters = getMethodParameters();
		Object[] args = new Object[parameters.length];
		for (int i = 0; i < parameters.length; i++) {
			MethodParameter parameter = parameters[i];
			parameter.initParameterNameDiscovery(this.parameterNameDiscoverer);
			args[i] = resolveProvidedArgument(parameter, providedArgs);
			if (args[i] != null) {
				continue;
			}
			if (this.argumentResolvers.supportsParameter(parameter)) {
				try {
					args[i] = this.argumentResolvers.resolveArgument(
							parameter, mavContainer, request, this.dataBinderFactory);
					continue;
				}
				catch (Exception ex) {
					if (logger.isDebugEnabled()) {
						logger.debug(getArgumentResolutionErrorMessage("Failed to resolve", i), ex);
					}
					throw ex;
				}
			}
			if (args[i] == null) {
				throw new IllegalStateException("Could not resolve method parameter at index " +
						parameter.getParameterIndex() + " in " + parameter.getMethod().toGenericString() +
						": " + getArgumentResolutionErrorMessage("No suitable resolver for", i));
			}
		}
		return args;
	}

	private String getArgumentResolutionErrorMessage(String text, int index) {
		Class<?> paramType = getMethodParameters()[index].getParameterType();
		return text + " argument " + index + " of type '" + paramType.getName() + "'";
	}

	/**
	 * 尝试从提供的参数值列表中解析方法参数.
	 */
	private Object resolveProvidedArgument(MethodParameter parameter, Object... providedArgs) {
		if (providedArgs == null) {
			return null;
		}
		for (Object providedArg : providedArgs) {
			if (parameter.getParameterType().isInstance(providedArg)) {
				return providedArg;
			}
		}
		return null;
	}


	/**
	 * 使用给定的参数值调用处理器方法.
	 */
	protected Object doInvoke(Object... args) throws Exception {
		ReflectionUtils.makeAccessible(getBridgedMethod());
		try {
			return getBridgedMethod().invoke(getBean(), args);
		}
		catch (IllegalArgumentException ex) {
			assertTargetBean(getBridgedMethod(), getBean(), args);
			String text = (ex.getMessage() != null ? ex.getMessage() : "Illegal argument");
			throw new IllegalStateException(getInvocationErrorMessage(text, args), ex);
		}
		catch (InvocationTargetException ex) {
			// Unwrap for HandlerExceptionResolvers ...
			Throwable targetException = ex.getTargetException();
			if (targetException instanceof RuntimeException) {
				throw (RuntimeException) targetException;
			}
			else if (targetException instanceof Error) {
				throw (Error) targetException;
			}
			else if (targetException instanceof Exception) {
				throw (Exception) targetException;
			}
			else {
				String text = getInvocationErrorMessage("Failed to invoke handler method", args);
				throw new IllegalStateException(text, targetException);
			}
		}
	}

	/**
	 * 断言目标bean类是声明给定方法的类的实例.
	 * 在某些情况下, 请求处理时的实际控制器实例可能是JDK动态代理 (延迟初始化, 原型bean等).
	 * 需要代理的{@code @Controller}应该更喜欢基于类的代理机制.
	 */
	private void assertTargetBean(Method method, Object targetBean, Object[] args) {
		Class<?> methodDeclaringClass = method.getDeclaringClass();
		Class<?> targetBeanClass = targetBean.getClass();
		if (!methodDeclaringClass.isAssignableFrom(targetBeanClass)) {
			String text = "The mapped handler method class '" + methodDeclaringClass.getName() +
					"' is not an instance of the actual controller bean class '" +
					targetBeanClass.getName() + "'. If the controller requires proxying " +
					"(e.g. due to @Transactional), please use class-based proxying.";
			throw new IllegalStateException(getInvocationErrorMessage(text, args));
		}
	}

	private String getInvocationErrorMessage(String text, Object[] resolvedArgs) {
		StringBuilder sb = new StringBuilder(getDetailedErrorMessage(text));
		sb.append("Resolved arguments: \n");
		for (int i = 0; i < resolvedArgs.length; i++) {
			sb.append("[").append(i).append("] ");
			if (resolvedArgs[i] == null) {
				sb.append("[null] \n");
			}
			else {
				sb.append("[type=").append(resolvedArgs[i].getClass().getName()).append("] ");
				sb.append("[value=").append(resolvedArgs[i]).append("]\n");
			}
		}
		return sb.toString();
	}

	/**
	 * 向消息添加HandlerMethod详细信息, 例如bean类型和方法签名.
	 * 
	 * @param text 要将HandlerMethod详细信息附加到的错误消息
	 */
	protected String getDetailedErrorMessage(String text) {
		StringBuilder sb = new StringBuilder(text).append("\n");
		sb.append("HandlerMethod details: \n");
		sb.append("Controller [").append(getBeanType().getName()).append("]\n");
		sb.append("Method [").append(getBridgedMethod().toGenericString()).append("]\n");
		return sb.toString();
	}

}
