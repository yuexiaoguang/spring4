package org.springframework.core.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.util.ClassUtils;

/**
 * 由{@link Map}支持的{@link AnnotationAttributeExtractor}策略的实现.
 */
class MapAnnotationAttributeExtractor extends AbstractAliasAwareAnnotationAttributeExtractor<Map<String, Object>> {

	/**
	 * <p>提供的映射必须包含所提供的{@code annotationType}中定义的每个属性的键值对, 该属性没有别名或没有默认值.
	 * 
	 * @param attributes 注解属性的Map; never {@code null}
	 * @param annotationType 要合成的注解类型; never {@code null}
	 * @param annotatedElement 使用提供的类型的注解进行注解的元素; may be {@code null} if unknown
	 */
	MapAnnotationAttributeExtractor(Map<String, Object> attributes, Class<? extends Annotation> annotationType,
			AnnotatedElement annotatedElement) {

		super(annotationType, annotatedElement, enrichAndValidateAttributes(attributes, annotationType));
	}


	@Override
	protected Object getRawAttributeValue(Method attributeMethod) {
		return getRawAttributeValue(attributeMethod.getName());
	}

	@Override
	protected Object getRawAttributeValue(String attributeName) {
		return getSource().get(attributeName);
	}


	/**
	 * 通过确保它包含指定的{@code annotationType}中每个注解属性的非null条目, 并且条目的类型与相应的返回类型匹配,
	 * 来丰富并验证提供的<em>属性</em>Map.
	 * <p>如果条目是 Map (可能是注解属性), 则将尝试从中合成注解.
	 * 类似地, 如果条目是Map数组, 则将尝试从这些Map中合成注解数组.
	 * <p>如果提供的Map中缺少某个属性, 则将其设置为别名的值 (如果存在别名) 或属性的默认值 (如果已定义),
	 * 否则将抛出 {@link IllegalArgumentException}.
	 */
	@SuppressWarnings("unchecked")
	private static Map<String, Object> enrichAndValidateAttributes(
			Map<String, Object> originalAttributes, Class<? extends Annotation> annotationType) {

		Map<String, Object> attributes = new LinkedHashMap<String, Object>(originalAttributes);
		Map<String, List<String>> attributeAliasMap = AnnotationUtils.getAttributeAliasMap(annotationType);

		for (Method attributeMethod : AnnotationUtils.getAttributeMethods(annotationType)) {
			String attributeName = attributeMethod.getName();
			Object attributeValue = attributes.get(attributeName);

			// 如果属性不存在, 检查别名
			if (attributeValue == null) {
				List<String> aliasNames = attributeAliasMap.get(attributeName);
				if (aliasNames != null) {
					for (String aliasName : aliasNames) {
						Object aliasValue = attributes.get(aliasName);
						if (aliasValue != null) {
							attributeValue = aliasValue;
							attributes.put(attributeName, attributeValue);
							break;
						}
					}
				}
			}

			// 如果别名不存在, 检查默认
			if (attributeValue == null) {
				Object defaultValue = AnnotationUtils.getDefaultValue(annotationType, attributeName);
				if (defaultValue != null) {
					attributeValue = defaultValue;
					attributes.put(attributeName, attributeValue);
				}
			}

			// if still null
			if (attributeValue == null) {
				throw new IllegalArgumentException(String.format(
						"Attributes map %s returned null for required attribute '%s' defined by annotation type [%s].",
						attributes, attributeName, annotationType.getName()));
			}

			// 最后, 确保正确的类型
			Class<?> requiredReturnType = attributeMethod.getReturnType();
			Class<? extends Object> actualReturnType = attributeValue.getClass();

			if (!ClassUtils.isAssignable(requiredReturnType, actualReturnType)) {
				boolean converted = false;

				// 单个元素覆盖相同类型的数组?
				if (requiredReturnType.isArray() && requiredReturnType.getComponentType() == actualReturnType) {
					Object array = Array.newInstance(requiredReturnType.getComponentType(), 1);
					Array.set(array, 0, attributeValue);
					attributes.put(attributeName, array);
					converted = true;
				}

				// 嵌套的Map表示单个注解?
				else if (Annotation.class.isAssignableFrom(requiredReturnType) &&
						Map.class.isAssignableFrom(actualReturnType)) {
					Class<? extends Annotation> nestedAnnotationType =
							(Class<? extends Annotation>) requiredReturnType;
					Map<String, Object> map = (Map<String, Object>) attributeValue;
					attributes.put(attributeName, AnnotationUtils.synthesizeAnnotation(map, nestedAnnotationType, null));
					converted = true;
				}

				// 嵌套的Map数组表示注解数组?
				else if (requiredReturnType.isArray() && actualReturnType.isArray() &&
						Annotation.class.isAssignableFrom(requiredReturnType.getComponentType()) &&
						Map.class.isAssignableFrom(actualReturnType.getComponentType())) {
					Class<? extends Annotation> nestedAnnotationType =
							(Class<? extends Annotation>) requiredReturnType.getComponentType();
					Map<String, Object>[] maps = (Map<String, Object>[]) attributeValue;
					attributes.put(attributeName, AnnotationUtils.synthesizeAnnotationArray(maps, nestedAnnotationType));
					converted = true;
				}

				if (!converted) {
					throw new IllegalArgumentException(String.format(
							"Attributes map %s returned a value of type [%s] for attribute '%s', " +
							"but a value of type [%s] is required as defined by annotation type [%s].",
							attributes, actualReturnType.getName(), attributeName, requiredReturnType.getName(),
							annotationType.getName()));
				}
			}
		}

		return attributes;
	}

}
