package org.springframework.core;

import java.io.Externalizable;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * 支持整个框架中使用的各种命名和其他约定.
 * 主要供框架内部使用.
 */
public abstract class Conventions {

	/**
	 * 使用数组时, 添加到名称的后缀.
	 */
	private static final String PLURAL_SUFFIX = "List";

	/**
	 * 在搜索代理的'主要'接口时应该忽略的接口.
	 */
	private static final Set<Class<?>> IGNORED_INTERFACES;

	static {
		IGNORED_INTERFACES = Collections.unmodifiableSet(new HashSet<Class<?>>(
				Arrays.<Class<?>>asList(Serializable.class, Externalizable.class, Cloneable.class, Comparable.class)));
	}


	/**
	 * 根据具体类型确定提供的{@code Object}的常规变量名称.
	 * 根据JavaBeans属性命名规则, 使用的约定是返回{@code Class}的非大写短名称:
	 * 因此, {@code com.myapp.Product} 变成 {@code product};
	 * {@code com.myapp.MyProduct} 变成 {@code myProduct};
	 * {@code com.myapp.UKProduct} 变成 {@code UKProduct}.
	 * <p>对于数组, 使用数组组件类型的复数版本.
	 * 对于{@code Collection}, 尝试在{@code Collection}中'向前看', 以确定组件类型并返回该组件类型的复数版本.
	 * 
	 * @param value 为其生成变量名称的值
	 * 
	 * @return 生成的变量名称
	 */
	public static String getVariableName(Object value) {
		Assert.notNull(value, "Value must not be null");
		Class<?> valueClass;
		boolean pluralize = false;

		if (value.getClass().isArray()) {
			valueClass = value.getClass().getComponentType();
			pluralize = true;
		}
		else if (value instanceof Collection) {
			Collection<?> collection = (Collection<?>) value;
			if (collection.isEmpty()) {
				throw new IllegalArgumentException("Cannot generate variable name for an empty Collection");
			}
			Object valueToCheck = peekAhead(collection);
			valueClass = getClassForValue(valueToCheck);
			pluralize = true;
		}
		else {
			valueClass = getClassForValue(value);
		}

		String name = ClassUtils.getShortNameAsProperty(valueClass);
		return (pluralize ? pluralize(name) : name);
	}

	/**
	 * 确定所提供参数的常规变量名称, 并考虑泛型集合类型.
	 * 
	 * @param parameter 要生成变量名的方法或构造函数参数
	 * 
	 * @return 生成的变量名
	 */
	public static String getVariableNameForParameter(MethodParameter parameter) {
		Assert.notNull(parameter, "MethodParameter must not be null");
		Class<?> valueClass;
		boolean pluralize = false;

		if (parameter.getParameterType().isArray()) {
			valueClass = parameter.getParameterType().getComponentType();
			pluralize = true;
		}
		else if (Collection.class.isAssignableFrom(parameter.getParameterType())) {
			valueClass = ResolvableType.forMethodParameter(parameter).asCollection().resolveGeneric();
			if (valueClass == null) {
				throw new IllegalArgumentException(
						"Cannot generate variable name for non-typed Collection parameter type");
			}
			pluralize = true;
		}
		else {
			valueClass = parameter.getParameterType();
		}

		String name = ClassUtils.getShortNameAsProperty(valueClass);
		return (pluralize ? pluralize(name) : name);
	}

	/**
	 * 确定所提供方法的返回类型的常规变量名称, 并考虑泛型集合类型.
	 * 
	 * @param method 要生成变量名的方法
	 * 
	 * @return 生成的变量名
	 */
	public static String getVariableNameForReturnType(Method method) {
		return getVariableNameForReturnType(method, method.getReturnType(), null);
	}

	/**
	 * 确定所提供方法的返回类型的常规变量名, 考虑泛型集合类型, 如果方法声明不够具体, 则回退到给定的返回值
	 * (i.e. 如果返回类型被声明为{@code Object}或无类型集合).
	 * 
	 * @param method 要生成变量名的方法
	 * @param value 返回值 (may be {@code null} if not available)
	 * 
	 * @return 生成的变量名
	 */
	public static String getVariableNameForReturnType(Method method, Object value) {
		return getVariableNameForReturnType(method, method.getReturnType(), value);
	}

	/**
	 * 确定所提供方法的返回类型的常规变量名, 考虑泛型集合类型, 如果方法声明不够具体, 则回退到给定的返回值
	 * (i.e. 如果返回类型被声明为{@code Object}或无类型集合).
	 * 
	 * @param method 要生成变量名的方法
	 * @param resolvedType 已解析的方法返回类型
	 * @param value 返回值 (may be {@code null} if not available)
	 * 
	 * @return 生成的变量名
	 */
	public static String getVariableNameForReturnType(Method method, Class<?> resolvedType, Object value) {
		Assert.notNull(method, "Method must not be null");

		if (Object.class == resolvedType) {
			if (value == null) {
				throw new IllegalArgumentException("Cannot generate variable name for an Object return type with null value");
			}
			return getVariableName(value);
		}

		Class<?> valueClass;
		boolean pluralize = false;

		if (resolvedType.isArray()) {
			valueClass = resolvedType.getComponentType();
			pluralize = true;
		}
		else if (Collection.class.isAssignableFrom(resolvedType)) {
			valueClass = ResolvableType.forMethodReturnType(method).asCollection().resolveGeneric();
			if (valueClass == null) {
				if (!(value instanceof Collection)) {
					throw new IllegalArgumentException(
							"Cannot generate variable name for non-typed Collection return type and a non-Collection value");
				}
				Collection<?> collection = (Collection<?>) value;
				if (collection.isEmpty()) {
					throw new IllegalArgumentException(
							"Cannot generate variable name for non-typed Collection return type and an empty Collection value");
				}
				Object valueToCheck = peekAhead(collection);
				valueClass = getClassForValue(valueToCheck);
			}
			pluralize = true;
		}
		else {
			valueClass = resolvedType;
		}

		String name = ClassUtils.getShortNameAsProperty(valueClass);
		return (pluralize ? pluralize(name) : name);
	}

	/**
	 * 将{@code String}以属性名称格式(小写, 连字符分隔的单词) 转换为属性名称格式(驼峰式).
	 * 例如, {@code transaction-manager}将转换为{@code transactionManager}.
	 */
	public static String attributeNameToPropertyName(String attributeName) {
		Assert.notNull(attributeName, "'attributeName' must not be null");
		if (!attributeName.contains("-")) {
			return attributeName;
		}
		char[] chars = attributeName.toCharArray();
		char[] result = new char[chars.length -1]; // 不完全准确, 但很好的猜测
		int currPos = 0;
		boolean upperCaseNext = false;
		for (char c : chars) {
			if (c == '-') {
				upperCaseNext = true;
			}
			else if (upperCaseNext) {
				result[currPos++] = Character.toUpperCase(c);
				upperCaseNext = false;
			}
			else {
				result[currPos++] = c;
			}
		}
		return new String(result, 0, currPos);
	}

	/**
	 * 返回由提供的封闭{@link Class}限定的属性名称.
	 * 例如, {@link Class}'{@code com.myapp.SomeClass}'限定的属性名称'{@code foo}'将为 '{@code com.myapp.SomeClass.foo}'
	 */
	public static String getQualifiedAttributeName(Class<?> enclosingClass, String attributeName) {
		Assert.notNull(enclosingClass, "'enclosingClass' must not be null");
		Assert.notNull(attributeName, "'attributeName' must not be null");
		return enclosingClass.getName() + '.' + attributeName;
	}


	/**
	 * 确定用于命名包含给定值的变量的类.
	 * <p>将返回给定值的类, 除非遇到JDK代理, 在这种情况下, 它将确定该代理实现的'主要'接口.
	 * 
	 * @param value 要检查的值
	 * 
	 * @return 用于命名变量的类
	 */
	private static Class<?> getClassForValue(Object value) {
		Class<?> valueClass = value.getClass();
		if (Proxy.isProxyClass(valueClass)) {
			Class<?>[] ifcs = valueClass.getInterfaces();
			for (Class<?> ifc : ifcs) {
				if (!IGNORED_INTERFACES.contains(ifc)) {
					return ifc;
				}
			}
		}
		else if (valueClass.getName().lastIndexOf('$') != -1 && valueClass.getDeclaringClass() == null) {
			// 类名中的'$', 但不是内部类 - 假设它是一个特殊的子类 (e.g. by OpenJPA)
			valueClass = valueClass.getSuperclass();
		}
		return valueClass;
	}

	/**
	 * 多个给定的名称.
	 */
	private static String pluralize(String name) {
		return name + PLURAL_SUFFIX;
	}

	/**
	 * 检索{@code Collection}中元素的{@code Class}.
	 * 检索{@code Class}的确切元素取决于具体的{@code Collection}实现.
	 */
	private static <E> E peekAhead(Collection<E> collection) {
		Iterator<E> it = collection.iterator();
		if (!it.hasNext()) {
			throw new IllegalStateException(
					"Unable to peek ahead in non-empty collection - no element found");
		}
		E value = it.next();
		if (value == null) {
			throw new IllegalStateException(
					"Unable to peek ahead in non-empty collection - only null element found");
		}
		return value;
	}
}
