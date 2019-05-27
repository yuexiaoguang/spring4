package org.springframework.beans.factory.support;

import java.lang.reflect.Method;

/**
 * 由可以重新实现IoC管理的对象上的任何方法的类实现的接口:
 * 依赖注入的<b>方法注入</b>形式.
 *
 * <p>这些方法可能是(但不一定是)抽象的, 在这种情况下, 容器将创建一个具体的子类来实例化.
 */
public interface MethodReplacer {

	/**
	 * 重新实现给定的方法.
	 * 
	 * @param obj 要重新实现方法的实例
	 * @param method 要重新实现的方法
	 * @param args 方法的参数
	 * 
	 * @return 方法的返回值
	 */
	Object reimplement(Object obj, Method method, Object[] args) throws Throwable;

}
