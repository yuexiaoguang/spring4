package org.springframework.core;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * 用于发现方法和构造函数的参数名称的接口.
 *
 * <p>参数名称发现并不总是可行, 但可以尝试各种策略, 例如查找可能在编译时发出的调试信息, 以及查找可选的带AspectJ注解的方法的argname注解值.
 */
public interface ParameterNameDiscoverer {

	/**
	 * 返回此方法的参数名称, 或{@code null}.
	 * 
	 * @param method 要查找其参数名称的方法
	 * 
	 * @return 如果可以解析名称, 则为参数名称数组; 如果不能, 则为{@code null}
	 */
	String[] getParameterNames(Method method);

	/**
	 * 返回此构造函数的参数名称, 或{@code null}.
	 * 
	 * @param ctor 要查找其参数名称的构造函数
	 * 
	 * @return 如果可以解析名称, 则为参数名称数组; 如果不能, 则为{@code null}
	 */
	String[] getParameterNames(Constructor<?> ctor);

}
