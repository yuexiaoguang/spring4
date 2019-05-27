package org.springframework.core.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * {@link AnnotationAttributeExtractor}实现的抽象基类, 透明地为注解属性强制执行属性别名语义,
 * 该注解属性使用了{@link AliasFor @AliasFor}注解.
 *
 * @param <S> 此提取器支持的源类型
 */
abstract class AbstractAliasAwareAnnotationAttributeExtractor<S> implements AnnotationAttributeExtractor<S> {

	private final Class<? extends Annotation> annotationType;

	private final Object annotatedElement;

	private final S source;

	private final Map<String, List<String>> attributeAliasMap;


	/**
	 * @param annotationType 要合成的注解类型; never {@code null}
	 * @param annotatedElement 使用提供的类型的注解进行注解的元素; 可能是{@code null}
	 * @param source 注解属性的底层源; never {@code null}
	 */
	AbstractAliasAwareAnnotationAttributeExtractor(
			Class<? extends Annotation> annotationType, Object annotatedElement, S source) {

		Assert.notNull(annotationType, "annotationType must not be null");
		Assert.notNull(source, "source must not be null");
		this.annotationType = annotationType;
		this.annotatedElement = annotatedElement;
		this.source = source;
		this.attributeAliasMap = AnnotationUtils.getAttributeAliasMap(annotationType);
	}


	@Override
	public final Class<? extends Annotation> getAnnotationType() {
		return this.annotationType;
	}

	@Override
	public final Object getAnnotatedElement() {
		return this.annotatedElement;
	}

	@Override
	public final S getSource() {
		return this.source;
	}

	@Override
	public final Object getAttributeValue(Method attributeMethod) {
		String attributeName = attributeMethod.getName();
		Object attributeValue = getRawAttributeValue(attributeMethod);

		List<String> aliasNames = this.attributeAliasMap.get(attributeName);
		if (aliasNames != null) {
			Object defaultValue = AnnotationUtils.getDefaultValue(this.annotationType, attributeName);
			for (String aliasName : aliasNames) {
				Object aliasValue = getRawAttributeValue(aliasName);

				if (!ObjectUtils.nullSafeEquals(attributeValue, aliasValue) &&
						!ObjectUtils.nullSafeEquals(attributeValue, defaultValue) &&
						!ObjectUtils.nullSafeEquals(aliasValue, defaultValue)) {
					String elementName = (this.annotatedElement != null ? this.annotatedElement.toString() : "unknown element");
					throw new AnnotationConfigurationException(String.format(
							"In annotation [%s] declared on %s and synthesized from [%s], attribute '%s' and its " +
							"alias '%s' are present with values of [%s] and [%s], but only one is permitted.",
							this.annotationType.getName(), elementName, this.source, attributeName, aliasName,
							ObjectUtils.nullSafeToString(attributeValue), ObjectUtils.nullSafeToString(aliasValue)));
				}

				// 如果用户未使用显式值声明注解, 请改用别名的值.
				if (ObjectUtils.nullSafeEquals(attributeValue, defaultValue)) {
					attributeValue = aliasValue;
				}
			}
		}

		return attributeValue;
	}


	/**
	 * 从与提供的属性方法对应的底层{@linkplain #getSource source}获取原始未修改的属性值.
	 */
	protected abstract Object getRawAttributeValue(Method attributeMethod);

	/**
	 * 从与提供的属性名称对应的底层{@linkplain #getSource source}获取原始未修改的属性值.
	 */
	protected abstract Object getRawAttributeValue(String attributeName);

}
