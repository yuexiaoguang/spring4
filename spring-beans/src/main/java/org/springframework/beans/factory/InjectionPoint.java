package org.springframework.beans.factory;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Member;

import org.springframework.core.MethodParameter;
import org.springframework.util.Assert;

/**
 * 注入点的简单描述符, 指向方法/构造函数参数或字段.
 * 由{@link UnsatisfiedDependencyException}暴露.
 */
public class InjectionPoint {

	protected MethodParameter methodParameter;

	protected Field field;

	private volatile Annotation[] fieldAnnotations;


	/**
	 * @param methodParameter 要包装的MethodParameter
	 */
	public InjectionPoint(MethodParameter methodParameter) {
		Assert.notNull(methodParameter, "MethodParameter must not be null");
		this.methodParameter = methodParameter;
	}

	/**
	 * @param field 要包装的字段
	 */
	public InjectionPoint(Field field) {
		Assert.notNull(field, "Field must not be null");
		this.field = field;
	}

	/**
	 * 用于复制的构造参数.
	 * 
	 * @param original 从中创建副本的原始描述符
	 */
	protected InjectionPoint(InjectionPoint original) {
		this.methodParameter = (original.methodParameter != null ?
				new MethodParameter(original.methodParameter) : null);
		this.field = original.field;
		this.fieldAnnotations = original.fieldAnnotations;
	}

	protected InjectionPoint() {
	}


	/**
	 * 返回包装的MethodParameter.
	 * <p>Note: MethodParameter或Field都可用.
	 * 
	 * @return the MethodParameter, or {@code null} if none
	 */
	public MethodParameter getMethodParameter() {
		return this.methodParameter;
	}

	/**
	 * 返回包装的字段.
	 * <p>Note: MethodParameter或Field都可用.
	 * 
	 * @return the Field, or {@code null} if none
	 */
	public Field getField() {
		return this.field;
	}

	/**
	 * 获取与包装的字段或方法/构造函数参数关联的注解.
	 */
	public Annotation[] getAnnotations() {
		if (this.field != null) {
			if (this.fieldAnnotations == null) {
				this.fieldAnnotations = this.field.getAnnotations();
			}
			return this.fieldAnnotations;
		}
		else {
			return this.methodParameter.getParameterAnnotations();
		}
	}

	/**
	 * 检索给定类型的字段/参数注解.
	 * 
	 * @param annotationType 要检索的注解类型
	 * 
	 * @return 注解实例, 或{@code null}
	 * @since 4.3.9
	 */
	public <A extends Annotation> A getAnnotation(Class<A> annotationType) {
		return (this.field != null ? this.field.getAnnotation(annotationType) :
				this.methodParameter.getParameterAnnotation(annotationType));
	}

	/**
	 * 返回由基础字段或方法/构造函数参数声明的类型, 表示注入类型.
	 */
	public Class<?> getDeclaredType() {
		return (this.field != null ? this.field.getType() : this.methodParameter.getParameterType());
	}

	/**
	 * 返回包装的成员, 包含注入点.
	 * 
	 * @return the Field / Method / Constructor as Member
	 */
	public Member getMember() {
		return (this.field != null ? this.field : this.methodParameter.getMember());
	}

	/**
	 * 返回包装的被注解的元素.
	 * <p>Note: 在方法/构造函数参数的情况下, 这暴露了在方法或构造函数本身上声明的注解
	 * (i.e. 在方法或构造函数层级上, 不是在参数层级上).
	 * 在这种情况下, 使用{@link #getAnnotations()}获取参数级注释, 透明地使用相应的字段注解.
	 * 
	 * @return the Field / Method / Constructor as AnnotatedElement
	 */
	public AnnotatedElement getAnnotatedElement() {
		return (this.field != null ? this.field : this.methodParameter.getAnnotatedElement());
	}


	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (getClass() != other.getClass()) {
			return false;
		}
		InjectionPoint otherPoint = (InjectionPoint) other;
		return (this.field != null ? this.field.equals(otherPoint.field) :
				this.methodParameter.equals(otherPoint.methodParameter));
	}

	@Override
	public int hashCode() {
		return (this.field != null ? this.field.hashCode() : this.methodParameter.hashCode());
	}

	@Override
	public String toString() {
		return (this.field != null ? "field '" + this.field.getName() + "'" : this.methodParameter.toString());
	}
}
