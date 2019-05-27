package org.springframework.core.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import org.springframework.util.ReflectionUtils;

/**
 * 由{@link Annotation}支持的{@link AnnotationAttributeExtractor}策略的默认实现.
 */
class DefaultAnnotationAttributeExtractor extends AbstractAliasAwareAnnotationAttributeExtractor<Annotation> {

	/**
	 * @param annotation 要合成的注解; never {@code null}
	 * @param annotatedElement 使用提供的注解进行注解的元素; may be {@code null} if unknown
	 */
	DefaultAnnotationAttributeExtractor(Annotation annotation, Object annotatedElement) {
		super(annotation.annotationType(), annotatedElement, annotation);
	}


	@Override
	protected Object getRawAttributeValue(Method attributeMethod) {
		ReflectionUtils.makeAccessible(attributeMethod);
		return ReflectionUtils.invokeMethod(attributeMethod, getSource());
	}

	@Override
	protected Object getRawAttributeValue(String attributeName) {
		Method attributeMethod = ReflectionUtils.findMethod(getAnnotationType(), attributeName);
		return getRawAttributeValue(attributeMethod);
	}

}
