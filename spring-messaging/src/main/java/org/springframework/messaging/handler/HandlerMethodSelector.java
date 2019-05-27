package org.springframework.messaging.handler;

import java.lang.reflect.Method;
import java.util.Set;

import org.springframework.core.MethodIntrospector;
import org.springframework.util.ReflectionUtils.MethodFilter;

/**
 * 定义用于搜索处理器方法的算法, 包括接口和父类, 同时还处理参数化方法以及接口和基于类的代理.
 *
 * @deprecated as of Spring 4.2.3, in favor of the generalized and refined {@link MethodIntrospector}
 */
@Deprecated
public abstract class HandlerMethodSelector {

	/**
	 * 选择给定处理器类型的处理器方法.
	 * <p>调用者通过{@link MethodFilter}参数定义感兴趣的处理器方法.
	 * 
	 * @param handlerType 用于搜索处理器方法的处理器类型
	 * @param handlerMethodFilter 帮助识别感兴趣的处理器方法的{@link MethodFilter}
	 * 
	 * @return 选定的方法, 或空集
	 */
	public static Set<Method> selectMethods(Class<?> handlerType, MethodFilter handlerMethodFilter) {
		return MethodIntrospector.selectMethods(handlerType, handlerMethodFilter);
	}

}
