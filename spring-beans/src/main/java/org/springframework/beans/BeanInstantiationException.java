package org.springframework.beans;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * 实例化bean失败时抛出的异常. 携带违规的bean类.
 */
@SuppressWarnings("serial")
public class BeanInstantiationException extends FatalBeanException {

	private Class<?> beanClass;

	private Constructor<?> constructor;

	private Method constructingMethod;


	/**
	 * @param beanClass 违规的bean类
	 * @param msg
	 */
	public BeanInstantiationException(Class<?> beanClass, String msg) {
		this(beanClass, msg, null);
	}

	/**
	 * @param beanClass 违规的bean类
	 * @param msg
	 * @param cause
	 */
	public BeanInstantiationException(Class<?> beanClass, String msg, Throwable cause) {
		super("Failed to instantiate [" + beanClass.getName() + "]: " + msg, cause);
		this.beanClass = beanClass;
	}

	/**
	 * @param constructor 违规的构造函数
	 * @param msg
	 * @param cause
	 * @since 4.3
	 */
	public BeanInstantiationException(Constructor<?> constructor, String msg, Throwable cause) {
		super("Failed to instantiate [" + constructor.getDeclaringClass().getName() + "]: " + msg, cause);
		this.beanClass = constructor.getDeclaringClass();
		this.constructor = constructor;
	}

	/**
	 * @param constructingMethod 用于bean构建目的的委托 (通常, 但不一定, 一个静态工厂方法)
	 * @param msg
	 * @param cause
	 */
	public BeanInstantiationException(Method constructingMethod, String msg, Throwable cause) {
		super("Failed to instantiate [" + constructingMethod.getReturnType().getName() + "]: " + msg, cause);
		this.beanClass = constructingMethod.getReturnType();
		this.constructingMethod = constructingMethod;
	}


	/**
	 * 返回有问题的bean类 (never {@code null}).
	 * 
	 * @return 要实例化的类
	 */
	public Class<?> getBeanClass() {
		return this.beanClass;
	}

	/**
	 * 返回有问题的构造函数.
	 * 
	 * @return 正在使用的构造函数, 或{@code null} 在工厂方法/默认实例化的情况下
	 * @since 4.3
	 */
	public Constructor<?> getConstructor() {
		return this.constructor;
	}

	/**
	 * 返回以进行bean构建的委托.
	 * 
	 * @return 使用的方法 (通常是一个静态工厂方法),或 {@code null} 在基于构造函数实例化的情况下
	 * @since 4.3
	 */
	public Method getConstructingMethod() {
		return this.constructingMethod;
	}

}
