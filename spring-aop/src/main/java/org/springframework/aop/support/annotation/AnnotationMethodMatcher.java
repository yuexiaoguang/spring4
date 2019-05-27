package org.springframework.aop.support.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import org.springframework.aop.support.AopUtils;
import org.springframework.aop.support.StaticMethodMatcher;
import org.springframework.util.Assert;

/**
 * 用于查找方法中存在的特定Java 5注解 (检查调用接口上的方法（如果有）以及目标类上的相应方法).
 */
public class AnnotationMethodMatcher extends StaticMethodMatcher {

	private final Class<? extends Annotation> annotationType;


	/**
	 * @param annotationType 要查找的注解类型
	 */
	public AnnotationMethodMatcher(Class<? extends Annotation> annotationType) {
		Assert.notNull(annotationType, "Annotation type must not be null");
		this.annotationType = annotationType;
	}


	@Override
	public boolean matches(Method method, Class<?> targetClass) {
		if (method.isAnnotationPresent(this.annotationType)) {
			return true;
		}
		// 该方法可以在接口上, 所以也要检查目标类.
		Method specificMethod = AopUtils.getMostSpecificMethod(method, targetClass);
		return (specificMethod != method && specificMethod.isAnnotationPresent(this.annotationType));
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof AnnotationMethodMatcher)) {
			return false;
		}
		AnnotationMethodMatcher otherMm = (AnnotationMethodMatcher) other;
		return this.annotationType.equals(otherMm.annotationType);
	}

	@Override
	public int hashCode() {
		return this.annotationType.hashCode();
	}

	@Override
	public String toString() {
		return getClass().getName() + ": " + this.annotationType;
	}

}
