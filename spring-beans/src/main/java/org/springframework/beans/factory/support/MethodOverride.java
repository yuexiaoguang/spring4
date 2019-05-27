package org.springframework.beans.factory.support;

import java.lang.reflect.Method;

import org.springframework.beans.BeanMetadataElement;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * 表示IoC容器对托管对象的方法的覆盖的对象.
 *
 * <p>请注意, 覆盖机制不是插入横切代码的通用方法: 使用AOP.
 */
public abstract class MethodOverride implements BeanMetadataElement {

	private final String methodName;

	private boolean overloaded = true;

	private Object source;


	/**
	 * @param methodName 要覆盖的方法的名称
	 */
	protected MethodOverride(String methodName) {
		Assert.notNull(methodName, "Method name must not be null");
		this.methodName = methodName;
	}


	/**
	 * 返回要覆盖的方法的名称.
	 */
	public String getMethodName() {
		return this.methodName;
	}

	/**
	 * 设置重写的方法是否重载 (i.e., 是否需要进行参数类型匹配以消除同名方法的歧义).
	 * <p>默认是 {@code true}; 可以切换到{@code false}以优化运行时性能.
	 */
	protected void setOverloaded(boolean overloaded) {
		this.overloaded = overloaded;
	}

	/**
	 * 返回重写的方法是否重载 (i.e., 是否需要进行参数类型匹配以消除同名方法的歧义).
	 */
	protected boolean isOverloaded() {
		return this.overloaded;
	}

	/**
	 * 为此元数据元素设置配置源{@code Object}.
	 * <p>对象的精确类型取决于所使用的配置机制.
	 */
	public void setSource(Object source) {
		this.source = source;
	}

	@Override
	public Object getSource() {
		return this.source;
	}

	/**
	 * 子类必须覆盖它, 以指示它们是否与给定方法匹配.
	 * 这允许参数列表检查以及方法名称检查.
	 * 
	 * @param method 要检查的方法
	 * 
	 * @return 此覆盖是否与给定方法匹配
	 */
	public abstract boolean matches(Method method);


	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof MethodOverride)) {
			return false;
		}
		MethodOverride that = (MethodOverride) other;
		return (ObjectUtils.nullSafeEquals(this.methodName, that.methodName) &&
				ObjectUtils.nullSafeEquals(this.source, that.source));
	}

	@Override
	public int hashCode() {
		int hashCode = ObjectUtils.nullSafeHashCode(this.methodName);
		hashCode = 29 * hashCode + ObjectUtils.nullSafeHashCode(this.source);
		return hashCode;
	}

}
