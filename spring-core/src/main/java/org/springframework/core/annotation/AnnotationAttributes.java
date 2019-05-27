package org.springframework.core.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Array;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * {@link LinkedHashMap}子类, 表示由{@link AnnotationUtils}, {@link AnnotatedElementUtils}读取的注解属性<em>key-value</em>对,
 * 以及Spring的反射和基于ASM的{@link org.springframework.core.type.AnnotationMetadata}实现.
 *
 * <p>提供'伪实现' 以避免调用代码中的噪声Map泛型, 以及以类型安全的方式查找注解属性的便捷方法.
 */
@SuppressWarnings("serial")
public class AnnotationAttributes extends LinkedHashMap<String, Object> {

	private static final String UNKNOWN = "unknown";

	private final Class<? extends Annotation> annotationType;

	private final String displayName;

	boolean validated = false;


	public AnnotationAttributes() {
		this.annotationType = null;
		this.displayName = UNKNOWN;
	}

	/**
	 * 使用给定的初始容量创建一个新的空{@link AnnotationAttributes}实例以优化性能.
	 * 
	 * @param initialCapacity 底层Map的初始大小
	 */
	public AnnotationAttributes(int initialCapacity) {
		super(initialCapacity);
		this.annotationType = null;
		this.displayName = UNKNOWN;
	}

	/**
	 * @param annotationType 此{@code AnnotationAttributes}实例表示的注解类型; never {@code null}
	 */
	public AnnotationAttributes(Class<? extends Annotation> annotationType) {
		Assert.notNull(annotationType, "'annotationType' must not be null");
		this.annotationType = annotationType;
		this.displayName = annotationType.getName();
	}

	/**
	 * @param annotationType 此{@code AnnotationAttributes}实例表示的注解类型名称; never {@code null}
	 * @param classLoader 尝试加载注解类型的ClassLoader, 或{@code null}只存储注解类型名称
	 */
	public AnnotationAttributes(String annotationType, ClassLoader classLoader) {
		Assert.notNull(annotationType, "'annotationType' must not be null");
		this.annotationType = getAnnotationType(annotationType, classLoader);
		this.displayName = annotationType;
	}

	@SuppressWarnings("unchecked")
	private static Class<? extends Annotation> getAnnotationType(String annotationType, ClassLoader classLoader) {
		if (classLoader != null) {
			try {
				return (Class<? extends Annotation>) classLoader.loadClass(annotationType);
			}
			catch (ClassNotFoundException ex) {
				// Annotation Class not resolvable
			}
		}
		return null;
	}

	/**
	 * 包装提供的Map及其所有<em>key-value</em>对.
	 * 
	 * @param map 注解属性<em>key-value</em>对的原始源
	 */
	public AnnotationAttributes(Map<String, Object> map) {
		super(map);
		this.annotationType = null;
		this.displayName = UNKNOWN;
	}

	/**
	 * 包装提供的Map及其所有<em>key-value</em>对.
	 * 
	 * @param other 注解属性<em>key-value</em>对的原始源
	 */
	public AnnotationAttributes(AnnotationAttributes other) {
		super(other);
		this.annotationType = other.annotationType;
		this.displayName = other.displayName;
		this.validated = other.validated;
	}


	/**
	 * 获取此{@code AnnotationAttributes}实例表示的注解类型.
	 * 
	 * @return 注解类型, 或{@code null}
	 */
	public Class<? extends Annotation> annotationType() {
		return this.annotationType;
	}

	/**
	 * 获取存储在指定的{@code attributeName}下的值.
	 * 
	 * @param attributeName 要获取的属性的名称; never {@code null} or empty
	 * 
	 * @return 值
	 * @throws IllegalArgumentException 如果该属性不存在, 或者它不是预期的类型
	 */
	public String getString(String attributeName) {
		return getRequiredAttribute(attributeName, String.class);
	}

	/**
	 * 获取存储在指定的{@code attributeName}下的值, 同时考虑通过{@link AliasFor @AliasFor}定义的别名语义.
	 * <p>如果在指定的{@code attributeName}下没有存储值, 但该属性具有通过{@code @AliasFor}声明的别名, 则将返回别名的值.
	 * 
	 * @param attributeName 要获取的属性的名称; never {@code null} or empty
	 * @param annotationType 此{@code AnnotationAttributes}实例表示的注解类型; never {@code null}
	 * @param annotationSource 此{@code AnnotationAttributes}表示的注解的源 (e.g., {@link AnnotatedElement}); 或{@code null}
	 * 
	 * @return 值
	 * @throws IllegalArgumentException 如果属性及其别名不存在, 或者不是{@code String}类型
	 * @throws AnnotationConfigurationException 如果属性及其别名都存在, 且有不同的非空值
	 * 
	 * @since 4.2
	 * @deprecated as of Spring 4.3.2, in favor of built-in alias resolution
	 * in {@link #getString} itself
	 */
	@Deprecated
	public String getAliasedString(String attributeName, Class<? extends Annotation> annotationType,
			Object annotationSource) {

		return getRequiredAttributeWithAlias(attributeName, annotationType, annotationSource, String.class);
	}

	/**
	 * 获取存储在指定的{@code attributeName}下的值.
	 * <p>如果存储在指定的{@code attributeName}下的值是一个字符串, 它将在返回之前包装在单个元素数组中.
	 * 
	 * @param attributeName 要获取的属性的名称; never {@code null} or empty
	 * 
	 * @return 值
	 * @throws IllegalArgumentException 如果该属性不存在, 或者它不是预期的类型
	 */
	public String[] getStringArray(String attributeName) {
		return getRequiredAttribute(attributeName, String[].class);
	}

	/**
	 * 获取存储在指定的{@code attributeName}下的值, 同时考虑通过{@link AliasFor @AliasFor}定义的别名语义.
	 * <p>如果在指定的{@code attributeName}下没有存储值, 但该属性具有通过{@code @AliasFor}声明的别名, 则将返回别名的值.
	 * 
	 * @param attributeName 要获取的属性的名称; never {@code null} or empty
	 * @param annotationType 此{@code AnnotationAttributes}实例表示的注解类型; never {@code null}
	 * @param annotationSource 此{@code AnnotationAttributes}表示的注解的源 (e.g., {@link AnnotatedElement}); 或{@code null}
	 * 
	 * @return 值
	 * @throws IllegalArgumentException 如果属性及其别名不存在, 或者不是{@code String[]}类型
	 * @throws AnnotationConfigurationException 如果属性及其别名都存在, 且有不同的非空值
	 * 
	 * @since 4.2
	 * @deprecated as of Spring 4.3.2, in favor of built-in alias resolution in {@link #getStringArray} itself
	 */
	@Deprecated
	public String[] getAliasedStringArray(String attributeName, Class<? extends Annotation> annotationType,
			Object annotationSource) {

		return getRequiredAttributeWithAlias(attributeName, annotationType, annotationSource, String[].class);
	}

	/**
	 * 获取存储在指定的{@code attributeName}下的值.
	 * 
	 * @param attributeName 要获取的属性的名称; never {@code null} or empty
	 * 
	 * @return 值
	 * @throws IllegalArgumentException 如果该属性不存在, 或者它不是预期的类型
	 */
	public boolean getBoolean(String attributeName) {
		return getRequiredAttribute(attributeName, Boolean.class);
	}

	/**
	 * 获取存储在指定的{@code attributeName}下的值.
	 * 
	 * @param attributeName 要获取的属性的名称; never {@code null} or empty
	 * 
	 * @return 值
	 * @throws IllegalArgumentException 如果该属性不存在, 或者它不是预期的类型
	 */
	@SuppressWarnings("unchecked")
	public <N extends Number> N getNumber(String attributeName) {
		return (N) getRequiredAttribute(attributeName, Number.class);
	}

	/**
	 * 获取存储在指定的{@code attributeName}下的值.
	 * 
	 * @param attributeName 要获取的属性的名称; never {@code null} or empty
	 * 
	 * @return 值
	 * @throws IllegalArgumentException 如果该属性不存在, 或者它不是预期的类型
	 */
	@SuppressWarnings("unchecked")
	public <E extends Enum<?>> E getEnum(String attributeName) {
		return (E) getRequiredAttribute(attributeName, Enum.class);
	}

	/**
	 * 获取存储在指定的{@code attributeName}下的值.
	 * 
	 * @param attributeName 要获取的属性的名称; never {@code null} or empty
	 * 
	 * @return 值
	 * @throws IllegalArgumentException 如果该属性不存在, 或者它不是预期的类型
	 */
	@SuppressWarnings("unchecked")
	public <T> Class<? extends T> getClass(String attributeName) {
		return getRequiredAttribute(attributeName, Class.class);
	}

	/**
	 * 获取存储在指定的{@code attributeName}下的值.
	 * <p>如果存储在指定的{@code attributeName}下的值是一个类, 它将在返回之前包装在单个元素数组中.
	 * 
	 * @param attributeName 要获取的属性的名称; never {@code null} or empty
	 * 
	 * @return 值
	 * @throws IllegalArgumentException 如果该属性不存在, 或者它不是预期的类型
	 */
	public Class<?>[] getClassArray(String attributeName) {
		return getRequiredAttribute(attributeName, Class[].class);
	}

	/**
	 * 获取存储在指定的{@code attributeName}下的值, 同时考虑通过{@link AliasFor @AliasFor}定义的别名语义.
	 * <p>如果在指定的{@code attributeName}下没有存储值, 但该属性具有通过{@code @AliasFor}声明的别名, 则将返回别名的值.
	 * 
	 * @param attributeName 要获取的属性的名称; never {@code null} or empty
	 * @param annotationType 此{@code AnnotationAttributes}实例表示的注解类型; never {@code null}
	 * @param annotationSource 此{@code AnnotationAttributes}表示的注解的源 (e.g., {@link AnnotatedElement}); 或{@code null}
	 * 
	 * @return 类数组
	 * @throws IllegalArgumentException 如果属性及其别名不存在, 或者不是{@code Class[]}类型
	 * @throws AnnotationConfigurationException 如果属性及其别名都存在, 且有不同的非空值
	 * 
	 * @since 4.2
	 * @deprecated as of Spring 4.3.2, in favor of built-in alias resolution in {@link #getClassArray} itself
	 */
	@Deprecated
	public Class<?>[] getAliasedClassArray(String attributeName, Class<? extends Annotation> annotationType,
			Object annotationSource) {

		return getRequiredAttributeWithAlias(attributeName, annotationType, annotationSource, Class[].class);
	}

	/**
	 * 获取存储在指定{@code attributeName}下的{@link AnnotationAttributes}.
	 * <p>Note: 如果期望实际的注解, 请调用{@link #getAnnotation(String, Class)}.
	 * 
	 * @param attributeName 要获取的属性的名称; never {@code null} or empty
	 * 
	 * @return {@code AnnotationAttributes}
	 * @throws IllegalArgumentException 如果该属性不存在或者它不是预期的类型
	 */
	public AnnotationAttributes getAnnotation(String attributeName) {
		return getRequiredAttribute(attributeName, AnnotationAttributes.class);
	}

	/**
	 * 获取存储在指定{@code attributeName}下的{@code annotationType}类型的注解.
	 * 
	 * @param attributeName 要获取的属性的名称; never {@code null} or empty
	 * @param annotationType 预期的注解类型; never {@code null}
	 * 
	 * @return 注解
	 * @throws IllegalArgumentException 如果该属性不存在或者它不是预期的类型
	 */
	public <A extends Annotation> A getAnnotation(String attributeName, Class<A> annotationType) {
		return getRequiredAttribute(attributeName, annotationType);
	}

	/**
	 * 获取存储在指定{@code attributeName}下的{@link AnnotationAttributes}数组.
	 * <p>如果存储在指定的{@code attributeName}下的值是{@code AnnotationAttributes}的实例, 则在返回之前它将被包装在单个元素的数组中.
	 * <p>Note: 如果期望实际的注解数组, 请调用{@link #getAnnotationArray(String, Class)}.
	 * 
	 * @param attributeName 要获取的属性的名称; never {@code null} or empty
	 * 
	 * @return {@code AnnotationAttributes}数组
	 * @throws IllegalArgumentException 如果该属性不存在, 或者它不是预期的类型
	 */
	public AnnotationAttributes[] getAnnotationArray(String attributeName) {
		return getRequiredAttribute(attributeName, AnnotationAttributes[].class);
	}

	/**
	 * 获取存储在指定{@code attributeName}下的{@code annotationType}类型的数组.
	 * <p>如果存储在指定的{@code attributeName}下的值是{@code Annotation}, 则在返回之前它将被包装在单个元素的数组中.
	 * 
	 * @param attributeName 要获取的属性的名称; never {@code null} or empty
	 * @param annotationType 预期的注解类型; never {@code null}
	 * 
	 * @return 注解数组
	 * @throws IllegalArgumentException 如果该属性不存在, 或者它不是预期的类型
	 */
	@SuppressWarnings("unchecked")
	public <A extends Annotation> A[] getAnnotationArray(String attributeName, Class<A> annotationType) {
		Object array = Array.newInstance(annotationType, 0);
		return (A[]) getRequiredAttribute(attributeName, array.getClass());
	}

	/**
	 * 获取存储在指定的{@code attributeName}下的值, 确保该值为{@code expectedType}.
	 * <p>如果{@code expectedType}是一个数组, 并且存储在指定的{@code attributeName}下的值是预期数组类型的组件类型的单个元素,
	 * 则单个元素将被包装在单个元素的数组中.
	 * 
	 * @param attributeName 要获取的属性的名称; never {@code null} or empty
	 * @param expectedType 预期的类型; never {@code null}
	 * 
	 * @return 值
	 * @throws IllegalArgumentException 如果该属性不存在, 或者它不是预期的类型
	 */
	@SuppressWarnings("unchecked")
	private <T> T getRequiredAttribute(String attributeName, Class<T> expectedType) {
		Assert.hasText(attributeName, "'attributeName' must not be null or empty");
		Object value = get(attributeName);
		assertAttributePresence(attributeName, value);
		assertNotException(attributeName, value);
		if (!expectedType.isInstance(value) && expectedType.isArray() &&
				expectedType.getComponentType().isInstance(value)) {
			Object array = Array.newInstance(expectedType.getComponentType(), 1);
			Array.set(array, 0, value);
			value = array;
		}
		assertAttributeType(attributeName, value, expectedType);
		return (T) value;
	}

	/**
	 * 获取存储在指定的{@code attributeName}下的值, 返回{@code expectedType}的对象,
	 * 同时考虑通过{@link AliasFor @AliasFor}定义的别名语义.
	 * <p>如果在指定的{@code attributeName}下没有存储值, 但该属性具有通过{@code @AliasFor}声明的别名, 则将返回别名的值.
	 * 
	 * @param attributeName 要获取的属性的名称; never {@code null} or empty
	 * @param annotationType 此{@code AnnotationAttributes}实例表示的注解类型; never {@code null}
	 * @param annotationSource 此{@code AnnotationAttributes}表示的注解的源 (e.g., {@link AnnotatedElement}); 和{@code null}
	 * @param expectedType 预期的类型; never {@code null}
	 * 
	 * @return 值
	 * @throws IllegalArgumentException 如果属性及其别名不存在, 或不属于{@code expectedType}
	 * @throws AnnotationConfigurationException 如果属性及其别名都存在, 且有不同的非空值
	 */
	private <T> T getRequiredAttributeWithAlias(String attributeName, Class<? extends Annotation> annotationType,
			Object annotationSource, Class<T> expectedType) {

		Assert.hasText(attributeName, "'attributeName' must not be null or empty");
		Assert.notNull(annotationType, "'annotationType' must not be null");
		Assert.notNull(expectedType, "'expectedType' must not be null");

		T attributeValue = getAttribute(attributeName, expectedType);

		List<String> aliasNames = AnnotationUtils.getAttributeAliasMap(annotationType).get(attributeName);
		if (aliasNames != null) {
			for (String aliasName : aliasNames) {
				T aliasValue = getAttribute(aliasName, expectedType);
				boolean attributeEmpty = ObjectUtils.isEmpty(attributeValue);
				boolean aliasEmpty = ObjectUtils.isEmpty(aliasValue);

				if (!attributeEmpty && !aliasEmpty && !ObjectUtils.nullSafeEquals(attributeValue, aliasValue)) {
					String elementName = (annotationSource == null ? "unknown element" : annotationSource.toString());
					String msg = String.format("In annotation [%s] declared on [%s], attribute [%s] and its " +
							"alias [%s] are present with values of [%s] and [%s], but only one is permitted.",
							annotationType.getName(), elementName, attributeName, aliasName,
							ObjectUtils.nullSafeToString(attributeValue), ObjectUtils.nullSafeToString(aliasValue));
					throw new AnnotationConfigurationException(msg);
				}

				// 如果期望一个数组, 并且当前跟踪的值为null, 但当前别名值为非null, 则将当前的null值替换为非null值 (可能是一个空数组).
				if (expectedType.isArray() && attributeValue == null && aliasValue != null) {
					attributeValue = aliasValue;
				}
				// Else: 如果不期望一个数组, 可以依赖于 ObjectUtils.isEmpty()的行为.
				else if (attributeEmpty && !aliasEmpty) {
					attributeValue = aliasValue;
				}
			}
			assertAttributePresence(attributeName, aliasNames, attributeValue);
		}

		return attributeValue;
	}

	/**
	 * 获取存储在指定的{@code attributeName}下的值, 确保该值为{@code expectedType}.
	 * 
	 * @param attributeName 要获取的属性的名称; never {@code null} or empty
	 * @param expectedType 预期的类型; never {@code null}
	 * 
	 * @return 值
	 * @throws IllegalArgumentException 如果属性不是预期的类型
	 */
	@SuppressWarnings("unchecked")
	private <T> T getAttribute(String attributeName, Class<T> expectedType) {
		Object value = get(attributeName);
		if (value != null) {
			assertNotException(attributeName, value);
			assertAttributeType(attributeName, value, expectedType);
		}
		return (T) value;
	}

	private void assertAttributePresence(String attributeName, Object attributeValue) {
		if (attributeValue == null) {
			throw new IllegalArgumentException(String.format(
					"Attribute '%s' not found in attributes for annotation [%s]", attributeName, this.displayName));
		}
	}

	private void assertAttributePresence(String attributeName, List<String> aliases, Object attributeValue) {
		if (attributeValue == null) {
			throw new IllegalArgumentException(String.format(
					"Neither attribute '%s' nor one of its aliases %s was found in attributes for annotation [%s]",
					attributeName, aliases, this.displayName));
		}
	}

	private void assertNotException(String attributeName, Object attributeValue) {
		if (attributeValue instanceof Exception) {
			throw new IllegalArgumentException(String.format(
					"Attribute '%s' for annotation [%s] was not resolvable due to exception [%s]",
					attributeName, this.displayName, attributeValue), (Exception) attributeValue);
		}
	}

	private void assertAttributeType(String attributeName, Object attributeValue, Class<?> expectedType) {
		if (!expectedType.isInstance(attributeValue)) {
			throw new IllegalArgumentException(String.format(
					"Attribute '%s' is of type [%s], but [%s] was expected in attributes for annotation [%s]",
					attributeName, attributeValue.getClass().getSimpleName(), expectedType.getSimpleName(),
					this.displayName));
		}
	}

	/**
	 * 将提供的{@code value}存储在指定的{@code key}下的Map中, 除非该值已存储在该键下.
	 * 
	 * @param key 要在其下存储值的键
	 * @param value 要存储的值
	 * 
	 * @return 存储在此Map中的当前值, 或{@code null} 如果此Map中未存储任何值
	 */
	@Override
	public Object putIfAbsent(String key, Object value) {
		Object obj = get(key);
		if (obj == null) {
			obj = put(key, value);
		}
		return obj;
	}

	@Override
	public String toString() {
		Iterator<Map.Entry<String, Object>> entries = entrySet().iterator();
		StringBuilder sb = new StringBuilder("{");
		while (entries.hasNext()) {
			Map.Entry<String, Object> entry = entries.next();
			sb.append(entry.getKey());
			sb.append('=');
			sb.append(valueToString(entry.getValue()));
			sb.append(entries.hasNext() ? ", " : "");
		}
		sb.append("}");
		return sb.toString();
	}

	private String valueToString(Object value) {
		if (value == this) {
			return "(this Map)";
		}
		if (value instanceof Object[]) {
			return "[" + StringUtils.arrayToDelimitedString((Object[]) value, ", ") + "]";
		}
		return String.valueOf(value);
	}


	/**
	 * 根据给定的Map返回{@link AnnotationAttributes}实例.
	 * <p>如果Map已经是{@code AnnotationAttributes}实例, 则会在不创建新实例的情况下立即转换并返回.
	 * 否则, 通过将提供的Map传递给{@link #AnnotationAttributes(Map)}构造函数来创建新实例.
	 * 
	 * @param map 注解属性<em>key-value</em>对的原始源
	 */
	public static AnnotationAttributes fromMap(Map<String, Object> map) {
		if (map == null) {
			return null;
		}
		if (map instanceof AnnotationAttributes) {
			return (AnnotationAttributes) map;
		}
		return new AnnotationAttributes(map);
	}
}
