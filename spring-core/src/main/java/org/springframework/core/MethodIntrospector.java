package org.springframework.core;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 * 定义用于搜索元数据相关方法的算法, 详尽地包括接口和父类, 同时还处理参数化的方法, 以及接口和基于类的代理遇到的常见场景.
 *
 * <p>通常但不一定用于查找带注解的处理器方法.
 */
public abstract class MethodIntrospector {

	/**
	 * 根据关联的元数据的查找, 选择给定目标类型的方法.
	 * <p>调用者通过{@link MetadataLookup}参数定义感兴趣的方法, 允许将关联的元数据收集到结果Map中.
	 * 
	 * @param targetType 要搜索方法的目标类型
	 * @param metadataLookup 检查感兴趣的方法的{@link MetadataLookup}回调,
	 * 如果匹配则返回与给定方法关联的非null元数据, 否则{@code null}
	 * 
	 * @return 与其元数据相关联的选择的方法(按检索顺序); 如果不匹配, 则为空Map
	 */
	public static <T> Map<Method, T> selectMethods(Class<?> targetType, final MetadataLookup<T> metadataLookup) {
		final Map<Method, T> methodMap = new LinkedHashMap<Method, T>();
		Set<Class<?>> handlerTypes = new LinkedHashSet<Class<?>>();
		Class<?> specificHandlerType = null;

		if (!Proxy.isProxyClass(targetType)) {
			handlerTypes.add(targetType);
			specificHandlerType = targetType;
		}
		handlerTypes.addAll(Arrays.asList(targetType.getInterfaces()));

		for (Class<?> currentHandlerType : handlerTypes) {
			final Class<?> targetClass = (specificHandlerType != null ? specificHandlerType : currentHandlerType);

			ReflectionUtils.doWithMethods(currentHandlerType, new ReflectionUtils.MethodCallback() {
				@Override
				public void doWith(Method method) {
					Method specificMethod = ClassUtils.getMostSpecificMethod(method, targetClass);
					T result = metadataLookup.inspect(specificMethod);
					if (result != null) {
						Method bridgedMethod = BridgeMethodResolver.findBridgedMethod(specificMethod);
						if (bridgedMethod == specificMethod || metadataLookup.inspect(bridgedMethod) == null) {
							methodMap.put(specificMethod, result);
						}
					}
				}
			}, ReflectionUtils.USER_DECLARED_METHODS);
		}

		return methodMap;
	}

	/**
	 * 根据过滤器选择给定目标类型的方法.
	 * <p>调用者通过{@code MethodFilter}参数定义感兴趣的方法.
	 * 
	 * @param targetType 要搜索方法的目标类型
	 * @param methodFilter 帮助识别感兴趣的处理器方法的{@code MethodFilter}
	 * 
	 * @return 选择的方法, 或空集合
	 */
	public static Set<Method> selectMethods(Class<?> targetType, final ReflectionUtils.MethodFilter methodFilter) {
		return selectMethods(targetType, new MetadataLookup<Boolean>() {
			@Override
			public Boolean inspect(Method method) {
				return (methodFilter.matches(method) ? Boolean.TRUE : null);
			}
		}).keySet();
	}

	/**
	 * 在目标类型上选择一个可调用的方法:
	 * 不论是实际暴露在目标类型上的给定方法本身, 还是在目标类型的接口之一或目标类型本身上的相应方法.
	 * <p>用户声明的接口上的匹配将是首选, 因为它们可能包含与目标类上的方法对应的相关元数据.
	 * 
	 * @param method 要检查的方法
	 * @param targetType 要搜索方法的目标类型 (通常是基于接口的JDK代理)
	 * 
	 * @return 目标类型上相应的可调用的方法
	 * @throws IllegalStateException 如果给定方法在给定的目标类型上不可调用(通常是由于代理不匹配)
	 */
	public static Method selectInvocableMethod(Method method, Class<?> targetType) {
		if (method.getDeclaringClass().isAssignableFrom(targetType)) {
			return method;
		}
		try {
			String methodName = method.getName();
			Class<?>[] parameterTypes = method.getParameterTypes();
			for (Class<?> ifc : targetType.getInterfaces()) {
				try {
					return ifc.getMethod(methodName, parameterTypes);
				}
				catch (NoSuchMethodException ex) {
					// Alright, not on this interface then...
				}
			}
			// 对代理类本身的最后尝试...
			return targetType.getMethod(methodName, parameterTypes);
		}
		catch (NoSuchMethodException ex) {
			throw new IllegalStateException(String.format(
					"Need to invoke method '%s' declared on target class '%s', " +
					"but not found in any interface(s) of the exposed proxy type. " +
					"Either pull the method up to an interface or switch to CGLIB " +
					"proxies by enforcing proxy-target-class mode in your configuration.",
					method.getName(), method.getDeclaringClass().getSimpleName()));
		}
	}


	/**
	 * 用于给定方法的元数据查找的回调接口.
	 * 
	 * @param <T> 返回的元数据的类型
	 */
	public interface MetadataLookup<T> {

		/**
		 * 对给定方法执行查找, 并返回关联的元数据.
		 * 
		 * @param method 要检查的方法
		 * 
		 * @return 如果匹配, 则与方法关联的非null元数据; 或{@code null}不匹配
		 */
		T inspect(Method method);
	}

}
