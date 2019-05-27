package org.springframework.aop.support;

import org.aopalliance.aop.Advice;

import org.springframework.aop.ClassFilter;
import org.springframework.aop.Pointcut;

/**
 * 用于保存Advice的名称匹配的方法切点的便捷类, 使他们成为Advisor.
 */
@SuppressWarnings("serial")
public class NameMatchMethodPointcutAdvisor extends AbstractGenericPointcutAdvisor {

	private final NameMatchMethodPointcut pointcut = new NameMatchMethodPointcut();


	public NameMatchMethodPointcutAdvisor() {
	}

	public NameMatchMethodPointcutAdvisor(Advice advice) {
		setAdvice(advice);
	}


	/**
	 * 设置{@link ClassFilter}以用于此切点.
	 * 默认是 {@link ClassFilter#TRUE}.
	 */
	public void setClassFilter(ClassFilter classFilter) {
		this.pointcut.setClassFilter(classFilter);
	}

	/**
	 * 当只有一个方法名称匹配时的便捷方法.
	 * 使用此方法或{@code setMappedNames}, 而不是两者.
	 */
	public void setMappedName(String mappedName) {
		this.pointcut.setMappedName(mappedName);
	}

	/**
	 * 设置定义要匹配的方法的方法名称.
	 * 匹配将是所有这些的结合; 如果任何一个匹配, 切点匹配.
	 */
	public void setMappedNames(String... mappedNames) {
		this.pointcut.setMappedNames(mappedNames);
	}

	/**
	 * 添加其他合格的方法名称, 除了已经命名的那些.
	 * 类似 set 方法, 此方法用于配置代理, 在使用代理之前.
	 * 
	 * @param name 将匹配的其他方法的名称
	 * 
	 * @return 此切点允许在一行中添加多个
	 */
	public NameMatchMethodPointcut addMethodName(String name) {
		return this.pointcut.addMethodName(name);
	}


	@Override
	public Pointcut getPointcut() {
		return this.pointcut;
	}

}
