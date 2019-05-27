package org.springframework.aop.support;

import org.springframework.aop.ClassFilter;
import org.springframework.aop.MethodMatcher;
import org.springframework.aop.Pointcut;

/**
 * 当我们想强制子类实现{@link MethodMatcher}接口, 但是子类想要成为切点时, 使用的超类.
 *
 * <p>{@link #setClassFilter "classFilter"} 属性可以设置为自定义{@link ClassFilter}行为. 默认是{@link ClassFilter#TRUE}.
 */
public abstract class StaticMethodMatcherPointcut extends StaticMethodMatcher implements Pointcut {

	private ClassFilter classFilter = ClassFilter.TRUE;


	/**
	 * 设置{@link ClassFilter}以用于此切点.
	 * 默认是{@link ClassFilter#TRUE}.
	 */
	public void setClassFilter(ClassFilter classFilter) {
		this.classFilter = classFilter;
	}

	@Override
	public ClassFilter getClassFilter() {
		return this.classFilter;
	}


	@Override
	public final MethodMatcher getMethodMatcher() {
		return this;
	}

}
