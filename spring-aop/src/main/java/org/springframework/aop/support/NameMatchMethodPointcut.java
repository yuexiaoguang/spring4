package org.springframework.aop.support;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.springframework.util.ObjectUtils;
import org.springframework.util.PatternMatchUtils;

/**
 * 用于简单方法名称匹配的切点bean, 作为regexp模式的替代方案.
 * 不处理重载方法: 具有给定名称的所有方法都符合条件.
 */
@SuppressWarnings("serial")
public class NameMatchMethodPointcut extends StaticMethodMatcherPointcut implements Serializable {

	private List<String> mappedNames = new LinkedList<String>();


	/**
	 * 当只有一个方法名称匹配时的便捷方法.
	 * 使用此方法或{@code setMappedNames}, 而不是两者.
	 */
	public void setMappedName(String mappedName) {
		setMappedNames(mappedName);
	}

	/**
	 * 设置定义要匹配的方法的方法名称.
	 * 匹配将是所有这些的结合; 如果任何一个匹配, 切点匹配.
	 */
	public void setMappedNames(String... mappedNames) {
		this.mappedNames = new LinkedList<String>();
		if (mappedNames != null) {
			this.mappedNames.addAll(Arrays.asList(mappedNames));
		}
	}

	/**
	 * 添加其他合格的方法名称, 除了已经命名的那些.
	 * 类似 set 方法, 此方法用于配置代理, 在使用代理之前.
	 * <p><b>NB:</b> 在使用代理之后, 此方法不起作用, 因为增强链将被缓存.
	 * 
	 * @param name 将匹配的其他方法的名称
	 * 
	 * @return 此切点允许在一行中添加多个
	 */
	public NameMatchMethodPointcut addMethodName(String name) {
		this.mappedNames.add(name);
		return this;
	}


	@Override
	public boolean matches(Method method, Class<?> targetClass) {
		for (String mappedName : this.mappedNames) {
			if (mappedName.equals(method.getName()) || isMatch(method.getName(), mappedName)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 如果给定的方法名称与映射的名称匹配，则返回.
	 * <p>默认实现检查 "xxx*", "*xxx", "*xxx*" 匹配, 以及直接平等. 可以在子类中重写.
	 * 
	 * @param methodName 类的方法名称
	 * @param mappedName 描述符中的名称
	 * 
	 * @return 如果名称匹配
	 */
	protected boolean isMatch(String methodName, String mappedName) {
		return PatternMatchUtils.simpleMatch(mappedName, methodName);
	}


	@Override
	public boolean equals(Object other) {
		return (this == other || (other instanceof NameMatchMethodPointcut &&
				ObjectUtils.nullSafeEquals(this.mappedNames, ((NameMatchMethodPointcut) other).mappedNames)));
	}

	@Override
	public int hashCode() {
		return (this.mappedNames != null ? this.mappedNames.hashCode() : 0);
	}

}
