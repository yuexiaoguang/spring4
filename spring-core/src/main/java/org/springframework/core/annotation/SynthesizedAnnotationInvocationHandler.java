package org.springframework.core.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * 用于{@link Annotation}的{@link InvocationHandler},
 * Spring 已经<em>合成</em> (i.e., 包装在动态代理中), 具有其它功能.
 */
class SynthesizedAnnotationInvocationHandler implements InvocationHandler {

	private final AnnotationAttributeExtractor<?> attributeExtractor;

	private final Map<String, Object> valueCache = new ConcurrentHashMap<String, Object>(8);


	/**
	 * @param attributeExtractor 委托的提取器
	 */
	SynthesizedAnnotationInvocationHandler(AnnotationAttributeExtractor<?> attributeExtractor) {
		Assert.notNull(attributeExtractor, "AnnotationAttributeExtractor must not be null");
		this.attributeExtractor = attributeExtractor;
	}


	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		if (ReflectionUtils.isEqualsMethod(method)) {
			return annotationEquals(args[0]);
		}
		if (ReflectionUtils.isHashCodeMethod(method)) {
			return annotationHashCode();
		}
		if (ReflectionUtils.isToStringMethod(method)) {
			return annotationToString();
		}
		if (AnnotationUtils.isAnnotationTypeMethod(method)) {
			return annotationType();
		}
		if (!AnnotationUtils.isAttributeMethod(method)) {
			throw new AnnotationConfigurationException(String.format(
					"Method [%s] is unsupported for synthesized annotation type [%s]", method, annotationType()));
		}
		return getAttributeValue(method);
	}

	private Class<? extends Annotation> annotationType() {
		return this.attributeExtractor.getAnnotationType();
	}

	private Object getAttributeValue(Method attributeMethod) {
		String attributeName = attributeMethod.getName();
		Object value = this.valueCache.get(attributeName);
		if (value == null) {
			value = this.attributeExtractor.getAttributeValue(attributeMethod);
			if (value == null) {
				String msg = String.format("%s returned null for attribute name [%s] from attribute source [%s]",
						this.attributeExtractor.getClass().getName(), attributeName, this.attributeExtractor.getSource());
				throw new IllegalStateException(msg);
			}

			// 在返回之前合成嵌套注解.
			if (value instanceof Annotation) {
				value = AnnotationUtils.synthesizeAnnotation((Annotation) value, this.attributeExtractor.getAnnotatedElement());
			}
			else if (value instanceof Annotation[]) {
				value = AnnotationUtils.synthesizeAnnotationArray((Annotation[]) value, this.attributeExtractor.getAnnotatedElement());
			}

			this.valueCache.put(attributeName, value);
		}

		// 克隆数组, 以便用户无法更改缓存中值的内容.
		if (value.getClass().isArray()) {
			value = cloneArray(value);
		}

		return value;
	}

	/**
	 * 克隆提供的数组, 确保保留原始组件类型.
	 * 
	 * @param array 要克隆的数组
	 */
	private Object cloneArray(Object array) {
		if (array instanceof boolean[]) {
			return ((boolean[]) array).clone();
		}
		if (array instanceof byte[]) {
			return ((byte[]) array).clone();
		}
		if (array instanceof char[]) {
			return ((char[]) array).clone();
		}
		if (array instanceof double[]) {
			return ((double[]) array).clone();
		}
		if (array instanceof float[]) {
			return ((float[]) array).clone();
		}
		if (array instanceof int[]) {
			return ((int[]) array).clone();
		}
		if (array instanceof long[]) {
			return ((long[]) array).clone();
		}
		if (array instanceof short[]) {
			return ((short[]) array).clone();
		}

		// else
		return ((Object[]) array).clone();
	}

	/**
	 * 有关所需算法的定义, 请参阅{@link Annotation#equals(Object)}.
	 * 
	 * @param other 要比较的另一个对象
	 */
	private boolean annotationEquals(Object other) {
		if (this == other) {
			return true;
		}
		if (!annotationType().isInstance(other)) {
			return false;
		}

		for (Method attributeMethod : AnnotationUtils.getAttributeMethods(annotationType())) {
			Object thisValue = getAttributeValue(attributeMethod);
			Object otherValue = ReflectionUtils.invokeMethod(attributeMethod, other);
			if (!ObjectUtils.nullSafeEquals(thisValue, otherValue)) {
				return false;
			}
		}

		return true;
	}

	/**
	 * 有关所需算法的定义, 请参阅{@link Annotation#hashCode()}.
	 */
	private int annotationHashCode() {
		int result = 0;

		for (Method attributeMethod : AnnotationUtils.getAttributeMethods(annotationType())) {
			Object value = getAttributeValue(attributeMethod);
			int hashCode;
			if (value.getClass().isArray()) {
				hashCode = hashCodeForArray(value);
			}
			else {
				hashCode = value.hashCode();
			}
			result += (127 * attributeMethod.getName().hashCode()) ^ hashCode;
		}

		return result;
	}

	/**
	 * WARNING: 不能在Spring的{@link ObjectUtils}中使用任何{@code nullSafeHashCode()}方法,
	 * 因为这些哈希码生成算法不符合{@link Annotation#hashCode()}中指定的要求.
	 * 
	 * @param array 用于计算哈希码的数组
	 */
	private int hashCodeForArray(Object array) {
		if (array instanceof boolean[]) {
			return Arrays.hashCode((boolean[]) array);
		}
		if (array instanceof byte[]) {
			return Arrays.hashCode((byte[]) array);
		}
		if (array instanceof char[]) {
			return Arrays.hashCode((char[]) array);
		}
		if (array instanceof double[]) {
			return Arrays.hashCode((double[]) array);
		}
		if (array instanceof float[]) {
			return Arrays.hashCode((float[]) array);
		}
		if (array instanceof int[]) {
			return Arrays.hashCode((int[]) array);
		}
		if (array instanceof long[]) {
			return Arrays.hashCode((long[]) array);
		}
		if (array instanceof short[]) {
			return Arrays.hashCode((short[]) array);
		}

		// else
		return Arrays.hashCode((Object[]) array);
	}

	/**
	 * 有关推荐格式的指南, 请参阅{@link Annotation#toString()}.
	 */
	private String annotationToString() {
		StringBuilder sb = new StringBuilder("@").append(annotationType().getName()).append("(");

		Iterator<Method> iterator = AnnotationUtils.getAttributeMethods(annotationType()).iterator();
		while (iterator.hasNext()) {
			Method attributeMethod = iterator.next();
			sb.append(attributeMethod.getName());
			sb.append('=');
			sb.append(attributeValueToString(getAttributeValue(attributeMethod)));
			sb.append(iterator.hasNext() ? ", " : "");
		}

		return sb.append(")").toString();
	}

	private String attributeValueToString(Object value) {
		if (value instanceof Object[]) {
			return "[" + StringUtils.arrayToDelimitedString((Object[]) value, ", ") + "]";
		}
		return String.valueOf(value);
	}
}
