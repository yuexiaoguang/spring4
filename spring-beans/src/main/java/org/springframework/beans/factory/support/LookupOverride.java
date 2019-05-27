package org.springframework.beans.factory.support;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.springframework.util.ObjectUtils;

/**
 * 在同一IoC上下文中查找对象的方法的覆盖.
 *
 * <p>符合查询覆盖条件的方法必须没有参数.
 */
public class LookupOverride extends MethodOverride {

	private final String beanName;

	private Method method;


	/**
	 * @param methodName 要覆盖的方法的名称
	 * @param beanName 当前{@code BeanFactory}中被覆盖的方法应返回的bean的名称 (may be {@code null})
	 */
	public LookupOverride(String methodName, String beanName) {
		super(methodName);
		this.beanName = beanName;
	}

	/**
	 * @param method 要覆盖的方法
	 * @param beanName 当前{@code BeanFactory}中被覆盖的方法应返回的bean的名称 (may be {@code null})
	 */
	public LookupOverride(Method method, String beanName) {
		super(method.getName());
		this.method = method;
		this.beanName = beanName;
	}


	/**
	 * 返回此方法应返回的bean的名称.
	 */
	public String getBeanName() {
		return this.beanName;
	}

	/**
	 * 通过{@link Method}引用或方法名称匹配指定的方法.
	 * <p>出于向后兼容性原因, 在具有给定名称的非抽象方法重载的场景中,
	 * 只有方法的无参数变体才会变成容器驱动的查找方法.
	 * <p>如果提供了{@link Method}, 则只考虑直接匹配, 通常由{@code @Lookup}注解划分.
	 */
	@Override
	public boolean matches(Method method) {
		if (this.method != null) {
			return method.equals(this.method);
		}
		else {
			return (method.getName().equals(getMethodName()) && (!isOverloaded() ||
					Modifier.isAbstract(method.getModifiers()) || method.getParameterTypes().length == 0));
		}
	}


	@Override
	public boolean equals(Object other) {
		if (!(other instanceof LookupOverride) || !super.equals(other)) {
			return false;
		}
		LookupOverride that = (LookupOverride) other;
		return (ObjectUtils.nullSafeEquals(this.method, that.method) &&
				ObjectUtils.nullSafeEquals(this.beanName, that.beanName));
	}

	@Override
	public int hashCode() {
		return (29 * super.hashCode() + ObjectUtils.nullSafeHashCode(this.beanName));
	}

	@Override
	public String toString() {
		return "LookupOverride for method '" + getMethodName() + "'";
	}

}
