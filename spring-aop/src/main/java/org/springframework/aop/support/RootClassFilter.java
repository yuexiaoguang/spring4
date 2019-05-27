package org.springframework.aop.support;

import java.io.Serializable;

import org.springframework.aop.ClassFilter;

/**
 * 传递类的简单ClassFilter实现 (和可选的子类).
 */
@SuppressWarnings("serial")
public class RootClassFilter implements ClassFilter, Serializable {

	private Class<?> clazz;


	public RootClassFilter(Class<?> clazz) {
		this.clazz = clazz;
	}


	@Override
	public boolean matches(Class<?> candidate) {
		return this.clazz.isAssignableFrom(candidate);
	}

}
