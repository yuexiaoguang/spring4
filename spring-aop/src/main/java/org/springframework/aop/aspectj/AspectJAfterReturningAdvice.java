package org.springframework.aop.aspectj;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

import org.springframework.aop.AfterAdvice;
import org.springframework.aop.AfterReturningAdvice;
import org.springframework.util.ClassUtils;
import org.springframework.util.TypeUtils;

/**
 * 封装了AspectJ返回增强方法的Spring AOP增强.
 */
@SuppressWarnings("serial")
public class AspectJAfterReturningAdvice extends AbstractAspectJAdvice
		implements AfterReturningAdvice, AfterAdvice, Serializable {

	public AspectJAfterReturningAdvice(
			Method aspectJBeforeAdviceMethod, AspectJExpressionPointcut pointcut, AspectInstanceFactory aif) {

		super(aspectJBeforeAdviceMethod, pointcut, aif);
	}


	@Override
	public boolean isBeforeAdvice() {
		return false;
	}

	@Override
	public boolean isAfterAdvice() {
		return true;
	}

	@Override
	public void setReturningName(String name) {
		setReturningNameNoCheck(name);
	}

	@Override
	public void afterReturning(Object returnValue, Method method, Object[] args, Object target) throws Throwable {
		if (shouldInvokeOnReturnValueOf(method, returnValue)) {
			invokeAdviceMethod(getJoinPointMatch(), returnValue, null);
		}
	}


	/**
	 * 遵循AspectJ语义, 如果指定了返回子句, 那么只有在返回的值, 是给定的返回类型和泛型类型参数的实例时, 才会调用增强,
	 * 如果有的话, 匹配分配规则. 如果返回类型是Object, 总会执行增强.
	 * 
	 * @param returnValue 目标方法的返回值
	 * 
	 * @return 是否为给定的返回值调用增强方法
	 */
	private boolean shouldInvokeOnReturnValueOf(Method method, Object returnValue) {
		Class<?> type = getDiscoveredReturningType();
		Type genericType = getDiscoveredReturningGenericType();
		// 如果不处理原始类型, 检查通用参数是否可分配.
		return (matchesReturnValue(type, method, returnValue) &&
				(genericType == null || genericType == type ||
						TypeUtils.isAssignable(genericType, method.getGenericReturnType())));
	}

	/**
	 * 遵循AspectJ语义, 如果返回值是 null (或返回类型是 void), 那么应该使用目标方法的返回类型来确定是否调用增强.
	 * 此外, 即使返回类型是 void, 如果在增强方法中声明的参数类型是 Object, 那么增强仍然必须被调用.
	 * 
	 * @param type 在增强方法中声明的参数类型
	 * @param method 增强方法
	 * @param returnValue 目标方法的返回值
	 * 
	 * @return 是否为给定的返回值和类型调用增强方法
	 */
	private boolean matchesReturnValue(Class<?> type, Method method, Object returnValue) {
		if (returnValue != null) {
			return ClassUtils.isAssignableValue(type, returnValue);
		}
		else if (Object.class == type && void.class == method.getReturnType()) {
			return true;
		}
		else {
			return ClassUtils.isAssignable(type, method.getReturnType());
		}
	}

}
