package org.springframework.core.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import org.springframework.core.MethodParameter;

/**
 * {@link MethodParameter}变体, 它合成了通过{@link AliasFor @AliasFor}声明属性别名的注解.
 */
public class SynthesizingMethodParameter extends MethodParameter {

	/**
	 * 为给定方法创建一个新的{@code SynthesizingMethodParameter}, 嵌套级别为1.
	 * 
	 * @param method 要指定参数的Method
	 * @param parameterIndex 参数的索引:
	 * -1 对于方法返回类型;
	 * 0 对于第一个方法参数;
	 * 1 对于第二个方法参数, etc.
	 */
	public SynthesizingMethodParameter(Method method, int parameterIndex) {
		super(method, parameterIndex);
	}

	/**
	 * 为给定方法创建一个新的{@code SynthesizingMethodParameter}.
	 * 
	 * @param method 要指定参数的Method
	 * @param parameterIndex 参数的索引:
	 * -1 对于方法返回类型;
	 * 0 对于第一个方法参数;
	 * 1 对于第二个方法参数, etc.
	 * @param nestingLevel 目标类型的嵌套级别
	 * (通常为 1; e.g. 在List中嵌套List的情况下, 1 表示嵌套的 List, 而2表示嵌套List的元素)
	 */
	public SynthesizingMethodParameter(Method method, int parameterIndex, int nestingLevel) {
		super(method, parameterIndex, nestingLevel);
	}

	/**
	 * 为给定的构造函数创建一个新的{@code SynthesizingMethodParameter}, 嵌套级别为1.
	 * 
	 * @param constructor 要指定参数的Constructor
	 * @param parameterIndex 参数的索引
	 */
	public SynthesizingMethodParameter(Constructor<?> constructor, int parameterIndex) {
		super(constructor, parameterIndex);
	}

	/**
	 * 为给定的构造函数创建一个新的{@code SynthesizingMethodParameter}.
	 * 
	 * @param constructor 要指定参数的Constructor
	 * @param parameterIndex 参数的索引
	 * @param nestingLevel 目标类型的嵌套级别
	 * (通常为 1; e.g. 在List中嵌套List的情况下, 1 表示嵌套的 List, 而2表示嵌套List的元素)
	 */
	public SynthesizingMethodParameter(Constructor<?> constructor, int parameterIndex, int nestingLevel) {
		super(constructor, parameterIndex, nestingLevel);
	}

	/**
	 * 复制构造函数, 根据原始对象所在的相同元数据和缓存状态, 生成独立的{@code SynthesizingMethodParameter}.
	 * 
	 * @param original 要从中复制的原始SynthesizingMethodParameter对象
	 */
	protected SynthesizingMethodParameter(SynthesizingMethodParameter original) {
		super(original);
	}


	@Override
	protected <A extends Annotation> A adaptAnnotation(A annotation) {
		return AnnotationUtils.synthesizeAnnotation(annotation, getAnnotatedElement());
	}

	@Override
	protected Annotation[] adaptAnnotationArray(Annotation[] annotations) {
		return AnnotationUtils.synthesizeAnnotationArray(annotations, getAnnotatedElement());
	}


	@Override
	public SynthesizingMethodParameter clone() {
		return new SynthesizingMethodParameter(this);
	}

}
