package org.springframework.aop.support.annotation;

import java.lang.annotation.Annotation;

import org.springframework.aop.ClassFilter;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.Assert;

/**
 * 查找类中存在的特定Java 5注解.
 */
public class AnnotationClassFilter implements ClassFilter {

	private final Class<? extends Annotation> annotationType;

	private final boolean checkInherited;


	/**
	 * @param annotationType 要查找的注解类型
	 */
	public AnnotationClassFilter(Class<? extends Annotation> annotationType) {
		this(annotationType, false);
	}

	/**
	 * @param annotationType 要查找的注解类型
	 * @param checkInherited 是否也显式检查注解类型的超类和接口 (即使注解类型未标记为继承本身)
	 */
	public AnnotationClassFilter(Class<? extends Annotation> annotationType, boolean checkInherited) {
		Assert.notNull(annotationType, "Annotation type must not be null");
		this.annotationType = annotationType;
		this.checkInherited = checkInherited;
	}


	@Override
	public boolean matches(Class<?> clazz) {
		return (this.checkInherited ?
				(AnnotationUtils.findAnnotation(clazz, this.annotationType) != null) :
				clazz.isAnnotationPresent(this.annotationType));
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof AnnotationClassFilter)) {
			return false;
		}
		AnnotationClassFilter otherCf = (AnnotationClassFilter) other;
		return (this.annotationType.equals(otherCf.annotationType) && this.checkInherited == otherCf.checkInherited);
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
