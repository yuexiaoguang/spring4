package org.springframework.aop.support.annotation;

import java.lang.annotation.Annotation;

import org.springframework.aop.ClassFilter;
import org.springframework.aop.MethodMatcher;
import org.springframework.aop.Pointcut;
import org.springframework.util.Assert;

/**
 * 寻找在{@link #forClassAnnotation class}或{@link #forMethodAnnotation method}上出现的特定Java 5注解的简单切点.
 */
public class AnnotationMatchingPointcut implements Pointcut {

	private final ClassFilter classFilter;

	private final MethodMatcher methodMatcher;


	/**
	 * @param classAnnotationType 要在类级别查找的注解类型
	 */
	public AnnotationMatchingPointcut(Class<? extends Annotation> classAnnotationType) {
		this(classAnnotationType, false);
	}

	/**
	 * @param classAnnotationType 要在类级别查找的注解类型
	 * @param checkInherited 是否还要检查注解类型的超类和接口以及元注解
	 */
	public AnnotationMatchingPointcut(Class<? extends Annotation> classAnnotationType, boolean checkInherited) {
		this.classFilter = new AnnotationClassFilter(classAnnotationType, checkInherited);
		this.methodMatcher = MethodMatcher.TRUE;
	}

	/**
	 * @param classAnnotationType 要在类级别查找的注解类型 (可以是 {@code null})
	 * @param methodAnnotationType 要在方法级别查找的注解类型 (可以是 {@code null})
	 */
	public AnnotationMatchingPointcut(
			Class<? extends Annotation> classAnnotationType, Class<? extends Annotation> methodAnnotationType) {

		Assert.isTrue((classAnnotationType != null || methodAnnotationType != null),
				"Either Class annotation type or Method annotation type needs to be specified (or both)");

		if (classAnnotationType != null) {
			this.classFilter = new AnnotationClassFilter(classAnnotationType);
		}
		else {
			this.classFilter = ClassFilter.TRUE;
		}

		if (methodAnnotationType != null) {
			this.methodMatcher = new AnnotationMethodMatcher(methodAnnotationType);
		}
		else {
			this.methodMatcher = MethodMatcher.TRUE;
		}
	}


	@Override
	public ClassFilter getClassFilter() {
		return this.classFilter;
	}

	@Override
	public MethodMatcher getMethodMatcher() {
		return this.methodMatcher;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof AnnotationMatchingPointcut)) {
			return false;
		}
		AnnotationMatchingPointcut otherPointcut = (AnnotationMatchingPointcut) other;
		return (this.classFilter.equals(otherPointcut.classFilter) &&
				this.methodMatcher.equals(otherPointcut.methodMatcher));
	}

	@Override
	public int hashCode() {
		return this.classFilter.hashCode() * 37 + this.methodMatcher.hashCode();
	}

	@Override
	public String toString() {
		return "AnnotationMatchingPointcut: " + this.classFilter + ", " +this.methodMatcher;
	}


	/**
	 * AnnotationMatchingPointcut的工厂方法，用于匹配类级别的指定注解.
	 * 
	 * @param annotationType 要在类级别查找的注解类型
	 * 
	 * @return 对应的AnnotationMatchingPointcut
	 */
	public static AnnotationMatchingPointcut forClassAnnotation(Class<? extends Annotation> annotationType) {
		Assert.notNull(annotationType, "Annotation type must not be null");
		return new AnnotationMatchingPointcut(annotationType);
	}

	/**
	 * AnnotationMatchingPointcut的工厂方法，匹配方法级别的指定注解.
	 * 
	 * @param annotationType 要在方法级别查找的注解类型
	 * 
	 * @return 对应的AnnotationMatchingPointcut
	 */
	public static AnnotationMatchingPointcut forMethodAnnotation(Class<? extends Annotation> annotationType) {
		Assert.notNull(annotationType, "Annotation type must not be null");
		return new AnnotationMatchingPointcut(null, annotationType);
	}

}
