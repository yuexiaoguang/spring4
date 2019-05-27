package org.springframework.core.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

/**
 * {@code AnnotationAttributeExtractor}负责{@linkplain #getAttributeValue 提取}来自底层{@linkplain #getSource source}的注解属性值,
 * 例如{@code Annotation}或{@code Map}.
 *
 * @param <S> 此提取器支持的源类型
 */
interface AnnotationAttributeExtractor<S> {

	/**
	 * 获取此提取器为其提取属性值的注解类型.
	 */
	Class<? extends Annotation> getAnnotationType();

	/**
	 * 获取使用此提取器支持的注解类型的注解进行注解的元素.
	 * 
	 * @return 带注解的元素, 或{@code null}
	 */
	Object getAnnotatedElement();

	/**
	 * 获取注解属性的底层源.
	 */
	S getSource();

	/**
	 * 从与提供的属性方法对应的底层{@linkplain #getSource source}中获取属性值.
	 * 
	 * @param attributeMethod 此提取器支持的注解类型的属性方法
	 * 
	 * @return 注解属性的值
	 */
	Object getAttributeValue(Method attributeMethod);

}
