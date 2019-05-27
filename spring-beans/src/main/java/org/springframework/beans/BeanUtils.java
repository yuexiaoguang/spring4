package org.springframework.beans;

import java.beans.PropertyDescriptor;
import java.beans.PropertyEditor;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.MethodParameter;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * JavaBeans的静态便捷方法: 用于实例化bean, 检查bean属性类型, 复制bean属性, etc.
 *
 * <p>主要用于框架内, 但在某种程度上也对应用程序类有用.
 */
public abstract class BeanUtils {

	private static final Log logger = LogFactory.getLog(BeanUtils.class);

	private static final Set<Class<?>> unknownEditorTypes =
			Collections.newSetFromMap(new ConcurrentReferenceHashMap<Class<?>, Boolean>(64));


	/**
	 * 使用no-arg构造函数实例化一个类.
	 * 
	 * @param clazz 要实例化的类
	 * 
	 * @return 新的实例
	 * @throws BeanInstantiationException 如果bean无法实例化
	 */
	public static <T> T instantiate(Class<T> clazz) throws BeanInstantiationException {
		Assert.notNull(clazz, "Class must not be null");
		if (clazz.isInterface()) {
			throw new BeanInstantiationException(clazz, "Specified class is an interface");
		}
		try {
			return clazz.newInstance();
		}
		catch (InstantiationException ex) {
			throw new BeanInstantiationException(clazz, "Is it an abstract class?", ex);
		}
		catch (IllegalAccessException ex) {
			throw new BeanInstantiationException(clazz, "Is the constructor accessible?", ex);
		}
	}

	/**
	 * 使用no-arg构造函数实例化一个类.
	 * <p>请注意, 此方法尝试设置构造函数是否可访问,  如果给出一个不可访问的（即非公共）构造函数.
	 * 
	 * @param clazz 要实例化的类
	 * 
	 * @return 新的实例
	 * @throws BeanInstantiationException 如果bean无法实例化
	 */
	public static <T> T instantiateClass(Class<T> clazz) throws BeanInstantiationException {
		Assert.notNull(clazz, "Class must not be null");
		if (clazz.isInterface()) {
			throw new BeanInstantiationException(clazz, "Specified class is an interface");
		}
		try {
			return instantiateClass(clazz.getDeclaredConstructor());
		}
		catch (NoSuchMethodException ex) {
			throw new BeanInstantiationException(clazz, "No default constructor found", ex);
		}
	}

	/**
	 * 使用其no-arg构造函数实例化一个类, 并将新实例作为指定的可赋值类型返回.
	 * <p>在实例化类（clazz）的类型不可用的情况下很有用, 但是所需的类型（assignableTo）是已知的.
	 * <p>请注意, 如果给定不可访问的（即非公共）构造函数, 此方法会尝试设置构造函数.
	 * 
	 * @param clazz 要实例化的类
	 * @param assignableTo 所需的类型
	 * 
	 * @return 新的实例
	 * @throws BeanInstantiationException 如果bean无法实例化
	 */
	@SuppressWarnings("unchecked")
	public static <T> T instantiateClass(Class<?> clazz, Class<T> assignableTo) throws BeanInstantiationException {
		Assert.isAssignable(assignableTo, clazz);
		return (T) instantiateClass(clazz);
	}

	/**
	 * 使用给定的构造函数实例化一个类.
	 * <p>请注意, 如果给定不可访问的（即非公共）构造函数, 此方法会尝试设置构造函数.
	 * 
	 * @param ctor 要实例化的构造函数
	 * @param args 要应用的构造函数参数
	 * 
	 * @return 新的实例
	 * @throws BeanInstantiationException 如果bean无法实例化
	 */
	public static <T> T instantiateClass(Constructor<T> ctor, Object... args) throws BeanInstantiationException {
		Assert.notNull(ctor, "Constructor must not be null");
		try {
			ReflectionUtils.makeAccessible(ctor);
			return ctor.newInstance(args);
		}
		catch (InstantiationException ex) {
			throw new BeanInstantiationException(ctor, "Is it an abstract class?", ex);
		}
		catch (IllegalAccessException ex) {
			throw new BeanInstantiationException(ctor, "Is the constructor accessible?", ex);
		}
		catch (IllegalArgumentException ex) {
			throw new BeanInstantiationException(ctor, "Illegal arguments for constructor", ex);
		}
		catch (InvocationTargetException ex) {
			throw new BeanInstantiationException(ctor, "Constructor threw exception", ex.getTargetException());
		}
	}

	/**
	 * 查找具有给定方法名称和给定参数类型的方法, 在给定的类或其超类上声明.
	 * 倾向于 public方法, 但也可以返回protected, 包级私有, 或private方法.
	 * <p>首先检查{@code Class.getMethod}, 回到{@code findDeclaredMethod}.
	 * 这样即使在具有受限Java安全设置的环境中也可以找到没有问题的公共方法.
	 * 
	 * @param clazz 要检查的类
	 * @param methodName 要查找的方法的名称
	 * @param paramTypes 要查找的方法的参数类型
	 * 
	 * @return Method对象, 或{@code null}
	 */
	public static Method findMethod(Class<?> clazz, String methodName, Class<?>... paramTypes) {
		try {
			return clazz.getMethod(methodName, paramTypes);
		}
		catch (NoSuchMethodException ex) {
			return findDeclaredMethod(clazz, methodName, paramTypes);
		}
	}

	/**
	 * 查找具有给定方法名称和给定参数类型的方法, 在给定的类或其超类上声明.
	 * 将返回 public, protected, 包级私有, 或private方法.
	 * <p>检查{@code Class.getDeclaredMethod}, 向上到所有超类.
	 * 
	 * @param clazz 要检查的类
	 * @param methodName 要查找的方法的名称
	 * @param paramTypes 要查找的方法的参数类型
	 * 
	 * @return Method对象, 或{@code null}
	 */
	public static Method findDeclaredMethod(Class<?> clazz, String methodName, Class<?>... paramTypes) {
		try {
			return clazz.getDeclaredMethod(methodName, paramTypes);
		}
		catch (NoSuchMethodException ex) {
			if (clazz.getSuperclass() != null) {
				return findDeclaredMethod(clazz.getSuperclass(), methodName, paramTypes);
			}
			return null;
		}
	}

	/**
	 * 查找具有给定方法名称和最小参数的方法 (best case: none), 在给定的类或其超类上声明.
	 * 倾向于 public方法, 但也可以返回protected, 包级私有, 或private方法.
	 * <p>首先检查{@code Class.getMethod}, 回到{@code findDeclaredMethod}.
	 * 这样即使在具有受限Java安全设置的环境中也可以找到没有问题的公共方法.
	 * 
	 * @param clazz 要检查的类
	 * @param methodName 要查找的方法的名称
	 * 
	 * @return Method对象, 或{@code null}
	 * @throws IllegalArgumentException 如果找到给定名称的方法, 但无法解析为具有最小参数的唯一方法
	 */
	public static Method findMethodWithMinimalParameters(Class<?> clazz, String methodName)
			throws IllegalArgumentException {

		Method targetMethod = findMethodWithMinimalParameters(clazz.getMethods(), methodName);
		if (targetMethod == null) {
			targetMethod = findDeclaredMethodWithMinimalParameters(clazz, methodName);
		}
		return targetMethod;
	}

	/**
	 * 查找具有给定方法名称和最小参数的方法 (best case: none), 在给定的类或其超类上声明.
	 * 将返回 public, protected, 包级私有, 或private方法.
	 * <p>检查{@code Class.getDeclaredMethods}, 向上到所有超类.
	 * 
	 * @param clazz 要检查的类
	 * @param methodName 要查找的方法的名称
	 * 
	 * @return Method对象, 或{@code null}
	 * @throws IllegalArgumentException 如果找到给定名称的方法, 但无法解析为具有最小参数的唯一方法
	 */
	public static Method findDeclaredMethodWithMinimalParameters(Class<?> clazz, String methodName)
			throws IllegalArgumentException {

		Method targetMethod = findMethodWithMinimalParameters(clazz.getDeclaredMethods(), methodName);
		if (targetMethod == null && clazz.getSuperclass() != null) {
			targetMethod = findDeclaredMethodWithMinimalParameters(clazz.getSuperclass(), methodName);
		}
		return targetMethod;
	}

	/**
	 * 在给定的方法列表中查找具有给定方法名称和最小参数的方法 (best case: none).
	 * 
	 * @param methods 要检查的方法
	 * @param methodName 要查找的方法的名称
	 * 
	 * @return Method对象, 或{@code null}
	 * @throws IllegalArgumentException 如果找到给定名称的方法, 但无法解析为具有最小参数的唯一方法
	 */
	public static Method findMethodWithMinimalParameters(Method[] methods, String methodName)
			throws IllegalArgumentException {

		Method targetMethod = null;
		int numMethodsFoundWithCurrentMinimumArgs = 0;
		for (Method method : methods) {
			if (method.getName().equals(methodName)) {
				int numParams = method.getParameterTypes().length;
				if (targetMethod == null || numParams < targetMethod.getParameterTypes().length) {
					targetMethod = method;
					numMethodsFoundWithCurrentMinimumArgs = 1;
				}
				else if (!method.isBridge() && targetMethod.getParameterTypes().length == numParams) {
					if (targetMethod.isBridge()) {
						// Prefer regular method over bridge...
						targetMethod = method;
					}
					else {
						// 长度相同的其他候选
						numMethodsFoundWithCurrentMinimumArgs++;
					}
				}
			}
		}
		if (numMethodsFoundWithCurrentMinimumArgs > 1) {
			throw new IllegalArgumentException("Cannot resolve method '" + methodName +
					"' to a unique method. Attempted to resolve to overloaded method with " +
					"the least number of parameters but there were " +
					numMethodsFoundWithCurrentMinimumArgs + " candidates.");
		}
		return targetMethod;
	}

	/**
	 * 解析 {@code methodName[([arg_list])]}格式的方法签名, {@code arg_list}可选,
	 * 以逗号分隔的完全限定类型名称列表, 并尝试根据提供的{@code Class}解析该签名.
	 * <p>如果不提供参数列表 ({@code methodName}), 将返回名称匹配且参数数量最少的方法.
	 * 提供参数类型列表时, 只返回名称和参数类型匹配的方法.
	 * <p>请注意, {@code methodName}和{@code methodName()}没有以相同的方式解析.
	 * 签名{@code methodName}表示名称为{@code methodName}且参数最少的方法, 而{@code methodName()}表示名称为{@code methodName}且参数正好为0的方法.
	 * <p>如果找不到方法, 将返回{@code null}.
	 * 
	 * @param signature 方法签名
	 * @param clazz 要解析方法签名的类
	 * 
	 * @return 解析的 Method
	 */
	public static Method resolveSignature(String signature, Class<?> clazz) {
		Assert.hasText(signature, "'signature' must not be empty");
		Assert.notNull(clazz, "Class must not be null");
		int startParen = signature.indexOf('(');
		int endParen = signature.indexOf(')');
		if (startParen > -1 && endParen == -1) {
			throw new IllegalArgumentException("Invalid method signature '" + signature +
					"': expected closing ')' for args list");
		}
		else if (startParen == -1 && endParen > -1) {
			throw new IllegalArgumentException("Invalid method signature '" + signature +
					"': expected opening '(' for args list");
		}
		else if (startParen == -1 && endParen == -1) {
			return findMethodWithMinimalParameters(clazz, signature);
		}
		else {
			String methodName = signature.substring(0, startParen);
			String[] parameterTypeNames =
					StringUtils.commaDelimitedListToStringArray(signature.substring(startParen + 1, endParen));
			Class<?>[] parameterTypes = new Class<?>[parameterTypeNames.length];
			for (int i = 0; i < parameterTypeNames.length; i++) {
				String parameterTypeName = parameterTypeNames[i].trim();
				try {
					parameterTypes[i] = ClassUtils.forName(parameterTypeName, clazz.getClassLoader());
				}
				catch (Throwable ex) {
					throw new IllegalArgumentException("Invalid method signature: unable to resolve type [" +
							parameterTypeName + "] for argument " + i + ". Root cause: " + ex);
				}
			}
			return findMethod(clazz, methodName, parameterTypes);
		}
	}


	/**
	 * 检索给定类的JavaBeans {@code PropertyDescriptor}.
	 * 
	 * @param clazz 要检索PropertyDescriptors的类
	 * 
	 * @return 给定类的{@code PropertyDescriptors}数组
	 * @throws BeansException 如果PropertyDescriptor查找失败
	 */
	public static PropertyDescriptor[] getPropertyDescriptors(Class<?> clazz) throws BeansException {
		CachedIntrospectionResults cr = CachedIntrospectionResults.forClass(clazz);
		return cr.getPropertyDescriptors();
	}

	/**
	 * 检索给定属性的JavaBeans {@code PropertyDescriptors}.
	 * 
	 * @param clazz 要检索PropertyDescriptors的类
	 * @param propertyName 属性名
	 * 
	 * @return 对应的PropertyDescriptor, 或{@code null}
	 * @throws BeansException 如果PropertyDescriptor查找失败
	 */
	public static PropertyDescriptor getPropertyDescriptor(Class<?> clazz, String propertyName)
			throws BeansException {

		CachedIntrospectionResults cr = CachedIntrospectionResults.forClass(clazz);
		return cr.getPropertyDescriptor(propertyName);
	}

	/**
	 * 查找给定方法的JavaBeans {@code PropertyDescriptor}, 使用该方法作为该bean属性的read方法或write方法.
	 * 
	 * @param method 查找相应PropertyDescriptor的方法
	 * 
	 * @return 对应的PropertyDescriptor, 或{@code null}
	 * @throws BeansException 如果PropertyDescriptor查找失败
	 */
	public static PropertyDescriptor findPropertyForMethod(Method method) throws BeansException {
		return findPropertyForMethod(method, method.getDeclaringClass());
	}

	/**
	 * 查找给定方法的JavaBeans {@code PropertyDescriptor}, 使用该方法作为该bean属性的read方法或write方法.
	 * 
	 * @param method 查找相应PropertyDescriptor的方法
	 * @param clazz 要反射的类(最具体的)
	 * 
	 * @return 对应的PropertyDescriptor, 或{@code null}
	 * @throws BeansException 如果PropertyDescriptor查找失败
	 * @since 3.2.13
	 */
	public static PropertyDescriptor findPropertyForMethod(Method method, Class<?> clazz) throws BeansException {
		Assert.notNull(method, "Method must not be null");
		PropertyDescriptor[] pds = getPropertyDescriptors(clazz);
		for (PropertyDescriptor pd : pds) {
			if (method.equals(pd.getReadMethod()) || method.equals(pd.getWriteMethod())) {
				return pd;
			}
		}
		return null;
	}

	/**
	 * 按照'Editor'后缀约定查找JavaBeans PropertyEditor (e.g. "mypackage.MyDomainClass" -> "mypackage.MyDomainClassEditor").
	 * <p>与{@link java.beans.PropertyEditorManager}实现的标准JavaBeans约定兼容, 但与后者注册的基本类型的默认编辑器隔离.
	 * 
	 * @param targetType 要查找编辑器的类型
	 * 
	 * @return 对应的编辑器, 或{@code null}
	 */
	public static PropertyEditor findEditorByConvention(Class<?> targetType) {
		if (targetType == null || targetType.isArray() || unknownEditorTypes.contains(targetType)) {
			return null;
		}
		ClassLoader cl = targetType.getClassLoader();
		if (cl == null) {
			try {
				cl = ClassLoader.getSystemClassLoader();
				if (cl == null) {
					return null;
				}
			}
			catch (Throwable ex) {
				// e.g. AccessControlException on Google App Engine
				if (logger.isDebugEnabled()) {
					logger.debug("Could not access system ClassLoader: " + ex);
				}
				return null;
			}
		}
		String editorName = targetType.getName() + "Editor";
		try {
			Class<?> editorClass = cl.loadClass(editorName);
			if (!PropertyEditor.class.isAssignableFrom(editorClass)) {
				if (logger.isWarnEnabled()) {
					logger.warn("Editor class [" + editorName +
							"] does not implement [java.beans.PropertyEditor] interface");
				}
				unknownEditorTypes.add(targetType);
				return null;
			}
			return (PropertyEditor) instantiateClass(editorClass);
		}
		catch (ClassNotFoundException ex) {
			if (logger.isDebugEnabled()) {
				logger.debug("No property editor [" + editorName + "] found for type " +
						targetType.getName() + " according to 'Editor' suffix convention");
			}
			unknownEditorTypes.add(targetType);
			return null;
		}
	}

	/**
	 * 从给定的类/接口确定给定属性的bean属性类型.
	 * 
	 * @param propertyName bean属性的名称
	 * @param beanClasses 要检查的类
	 * 
	 * @return 属性类型, 或{@code Object.class}
	 */
	public static Class<?> findPropertyType(String propertyName, Class<?>... beanClasses) {
		if (beanClasses != null) {
			for (Class<?> beanClass : beanClasses) {
				PropertyDescriptor pd = getPropertyDescriptor(beanClass, propertyName);
				if (pd != null) {
					return pd.getPropertyType();
				}
			}
		}
		return Object.class;
	}

	/**
	 * 获取指定属性的write方法的新MethodParameter对象.
	 * 
	 * @param pd 属性的PropertyDescriptor
	 * 
	 * @return 一个对应的MethodParameter对象
	 */
	public static MethodParameter getWriteMethodParameter(PropertyDescriptor pd) {
		if (pd instanceof GenericTypeAwarePropertyDescriptor) {
			return new MethodParameter(((GenericTypeAwarePropertyDescriptor) pd).getWriteMethodParameter());
		}
		else {
			return new MethodParameter(pd.getWriteMethod(), 0);
		}
	}

	/**
	 * 检查给定类型是否代表“简单”属性:
	 * a primitive, a String or other CharSequence, a Number, a Date,
	 * a URI, a URL, a Locale, a Class, or a corresponding array.
	 * <p>用于确定要检查“简单”依赖项检查的属性.
	 * 
	 * @param clazz 要检查的类型
	 * 
	 * @return 给定类型是否代表“简单”属性
	 */
	public static boolean isSimpleProperty(Class<?> clazz) {
		Assert.notNull(clazz, "Class must not be null");
		return isSimpleValueType(clazz) || (clazz.isArray() && isSimpleValueType(clazz.getComponentType()));
	}

	/**
	 * 检查给定类型是否代表“简单”属性:
	 * a primitive, an enum, a String or other CharSequence, a Number, a Date,
	 * a URI, a URL, a Locale or a Class.
	 * 
	 * @param clazz 要检查的类型
	 * 
	 * @return 给定类型是否代表“简单”属性
	 */
	public static boolean isSimpleValueType(Class<?> clazz) {
		return (ClassUtils.isPrimitiveOrWrapper(clazz) ||
				Enum.class.isAssignableFrom(clazz) ||
				CharSequence.class.isAssignableFrom(clazz) ||
				Number.class.isAssignableFrom(clazz) ||
				Date.class.isAssignableFrom(clazz) ||
				URI.class == clazz || URL.class == clazz ||
				Locale.class == clazz || Class.class == clazz);
	}


	/**
	 * 将给定源bean的属性值复制到目标bean中.
	 * <p>Note: 源类和目标类不必相互匹配, 甚至不必相互派生, 只要属性匹配.
	 * 源bean暴露但目标bean不会被忽略的bean属性.
	 * <p>满足更复杂的转移需求, 考虑使用完整的BeanWrapper.
	 * 
	 * @param source 源bean
	 * @param target 目标bean
	 * 
	 * @throws BeansException 如果复制失败
	 */
	public static void copyProperties(Object source, Object target) throws BeansException {
		copyProperties(source, target, null, (String[]) null);
	}

	/**
	 * 将给定源bean的属性值复制到给定的目标bean中, 仅设置在editable类(接口)中定义的属性 .
	 * <p>Note: 源类和目标类不必相互匹配, 甚至不必相互派生, 只要属性匹配.
	 * 源bean暴露但目标bean不会被忽略的bean属性.
	 * <p>满足更复杂的转移需求, 考虑使用完整的BeanWrapper.
	 * 
	 * @param source 源bean
	 * @param target 目标bean
	 * @param editable 要限制属性设置的类 (接口)
	 * 
	 * @throws BeansException 如果复制失败
	 */
	public static void copyProperties(Object source, Object target, Class<?> editable) throws BeansException {
		copyProperties(source, target, editable, (String[]) null);
	}

	/**
	 * 将给定源bean的属性值复制到给定的目标bean中, 忽略给定的"ignoreProperties".
	 * <p>Note: 源类和目标类不必相互匹配, 甚至不必相互派生, 只要属性匹配.
	 * 源bean暴露但目标bean不会被忽略的bean属性.
	 * <p>满足更复杂的转移需求, 考虑使用完整的BeanWrapper.
	 * 
	 * @param source 源bean
	 * @param target 目标bean
	 * @param ignoreProperties 要忽略的属性名称数组
	 * 
	 * @throws BeansException 如果复制失败
	 */
	public static void copyProperties(Object source, Object target, String... ignoreProperties) throws BeansException {
		copyProperties(source, target, null, ignoreProperties);
	}

	/**
	 * 将给定源bean的属性值复制到给定的目标bean中.
	 * <p>Note: 源类和目标类不必相互匹配, 甚至不必相互派生, 只要属性匹配.
	 * 源bean暴露但目标bean不会被忽略的bean属性.
	 * 
	 * @param source 源bean
	 * @param target 目标bean
	 * @param editable 要限制属性设置的类 (接口)
	 * @param ignoreProperties 要忽略的属性名称数组
	 * 
	 * @throws BeansException 如果复制失败
	 */
	private static void copyProperties(Object source, Object target, Class<?> editable, String... ignoreProperties)
			throws BeansException {

		Assert.notNull(source, "Source must not be null");
		Assert.notNull(target, "Target must not be null");

		Class<?> actualEditable = target.getClass();
		if (editable != null) {
			if (!editable.isInstance(target)) {
				throw new IllegalArgumentException("Target class [" + target.getClass().getName() +
						"] not assignable to Editable class [" + editable.getName() + "]");
			}
			actualEditable = editable;
		}
		PropertyDescriptor[] targetPds = getPropertyDescriptors(actualEditable);
		List<String> ignoreList = (ignoreProperties != null ? Arrays.asList(ignoreProperties) : null);

		for (PropertyDescriptor targetPd : targetPds) {
			Method writeMethod = targetPd.getWriteMethod();
			if (writeMethod != null && (ignoreList == null || !ignoreList.contains(targetPd.getName()))) {
				PropertyDescriptor sourcePd = getPropertyDescriptor(source.getClass(), targetPd.getName());
				if (sourcePd != null) {
					Method readMethod = sourcePd.getReadMethod();
					if (readMethod != null &&
							ClassUtils.isAssignable(writeMethod.getParameterTypes()[0], readMethod.getReturnType())) {
						try {
							if (!Modifier.isPublic(readMethod.getDeclaringClass().getModifiers())) {
								readMethod.setAccessible(true);
							}
							Object value = readMethod.invoke(source);
							if (!Modifier.isPublic(writeMethod.getDeclaringClass().getModifiers())) {
								writeMethod.setAccessible(true);
							}
							writeMethod.invoke(target, value);
						}
						catch (Throwable ex) {
							throw new FatalBeanException(
									"Could not copy property '" + targetPd.getName() + "' from source to target", ex);
						}
					}
				}
			}
		}
	}

}
