package org.springframework.beans.factory.support;

import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;

import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * MethodOverride的扩展, 表示IoC容器对方法的任意覆盖.
 *
 * <p>无论其参数和返回类型如何, 都可以覆盖任何非final方法.
 */
public class ReplaceOverride extends MethodOverride {

	private final String methodReplacerBeanName;

	private List<String> typeIdentifiers = new LinkedList<String>();


	/**
	 * @param methodName 要覆盖的方法的名称
	 * @param methodReplacerBeanName MethodReplacer的bean名称
	 */
	public ReplaceOverride(String methodName, String methodReplacerBeanName) {
		super(methodName);
		Assert.notNull(methodName, "Method replacer bean name must not be null");
		this.methodReplacerBeanName = methodReplacerBeanName;
	}


	/**
	 * 返回实现MethodReplacer的bean的名称.
	 */
	public String getMethodReplacerBeanName() {
		return this.methodReplacerBeanName;
	}

	/**
	 * 添加类字符串的片段, 例如"Exception"或"java.lang.Exc", 识别参数类型.
	 * 
	 * @param identifier 完全限定类名的子字符串
	 */
	public void addTypeIdentifier(String identifier) {
		this.typeIdentifiers.add(identifier);
	}

	@Override
	public boolean matches(Method method) {
		if (!method.getName().equals(getMethodName())) {
			return false;
		}
		if (!isOverloaded()) {
			// Not overloaded: 不要担心arg类型匹配...
			return true;
		}
		// 如果到这里, 需要坚持精确的参数匹配...
		if (this.typeIdentifiers.size() != method.getParameterTypes().length) {
			return false;
		}
		for (int i = 0; i < this.typeIdentifiers.size(); i++) {
			String identifier = this.typeIdentifiers.get(i);
			if (!method.getParameterTypes()[i].getName().contains(identifier)) {
				return false;
			}
		}
		return true;
	}


	@Override
	public boolean equals(Object other) {
		if (!(other instanceof ReplaceOverride) || !super.equals(other)) {
			return false;
		}
		ReplaceOverride that = (ReplaceOverride) other;
		return (ObjectUtils.nullSafeEquals(this.methodReplacerBeanName, that.methodReplacerBeanName) &&
				ObjectUtils.nullSafeEquals(this.typeIdentifiers, that.typeIdentifiers));
	}

	@Override
	public int hashCode() {
		int hashCode = super.hashCode();
		hashCode = 29 * hashCode + ObjectUtils.nullSafeHashCode(this.methodReplacerBeanName);
		hashCode = 29 * hashCode + ObjectUtils.nullSafeHashCode(this.typeIdentifiers);
		return hashCode;
	}

	@Override
	public String toString() {
		return "Replace override for method '" + getMethodName() + "'";
	}

}
