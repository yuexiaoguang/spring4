package org.springframework.beans.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringValueResolver;

/**
 * 用于在JavaBeans样式中使用注解的公共方法.
 */
public abstract class AnnotationBeanUtils {

	/**
	 * 将提供的{@link Annotation}的属性复制到提供的目标bean.
	 * {@code excludedProperties}中定义的任何属性都不会被复制.
	 * 
	 * @param ann 要复制的注解
	 * @param bean 要复制到的bean实例
	 * @param excludedProperties 要被排除的属性名称
	 */
	public static void copyPropertiesToBean(Annotation ann, Object bean, String... excludedProperties) {
		copyPropertiesToBean(ann, bean, null, excludedProperties);
	}

	/**
	 * 将提供的{@link Annotation}的属性复制到提供的目标bean.
	 * {@code excludedProperties}中定义的任何属性都不会被复制.
	 * <p>例如,指定的值解析程序可以解析属性值中的占位符.
	 * 
	 * @param ann 要复制的注解
	 * @param bean 要复制到的bean实例
	 * @param valueResolver 用于后处理String属性值的解析器 (may be {@code null})
	 * @param excludedProperties 要被排除的属性名称
	 */
	public static void copyPropertiesToBean(Annotation ann, Object bean, StringValueResolver valueResolver, String... excludedProperties) {
		Set<String> excluded = new HashSet<String>(Arrays.asList(excludedProperties));
		Method[] annotationProperties = ann.annotationType().getDeclaredMethods();
		BeanWrapper bw = PropertyAccessorFactory.forBeanPropertyAccess(bean);
		for (Method annotationProperty : annotationProperties) {
			String propertyName = annotationProperty.getName();
			if (!excluded.contains(propertyName) && bw.isWritableProperty(propertyName)) {
				Object value = ReflectionUtils.invokeMethod(annotationProperty, ann);
				if (valueResolver != null && value instanceof String) {
					value = valueResolver.resolveStringValue((String) value);
				}
				bw.setPropertyValue(propertyName, value);
			}
		}
	}
}
