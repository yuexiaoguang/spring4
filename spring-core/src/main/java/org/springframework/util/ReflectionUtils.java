package org.springframework.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.UndeclaredThrowableException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * 用于处理反射API和处理反射异常.
 *
 * <p>仅供内部使用.
 */
public abstract class ReflectionUtils {

	/**
	 * CGLIB重命名方法的命名前缀.
	 */
	private static final String CGLIB_RENAMED_METHOD_PREFIX = "CGLIB$";

	private static final Method[] NO_METHODS = {};

	private static final Field[] NO_FIELDS = {};


	/**
	 * 缓存{@link Class#getDeclaredMethods()}以及基于Java 8的接口的等效默认方法, 允许快速迭代.
	 */
	private static final Map<Class<?>, Method[]> declaredMethodsCache =
			new ConcurrentReferenceHashMap<Class<?>, Method[]>(256);

	/**
	 * 缓存{@link Class#getDeclaredFields()}, 允许快速迭代.
	 */
	private static final Map<Class<?>, Field[]> declaredFieldsCache =
			new ConcurrentReferenceHashMap<Class<?>, Field[]>(256);


	/**
	 * 尝试使用提供的{@code name}在提供的{@link Class}上查找{@link Field field}.
	 * 搜索所有超类直至{@link Object}.
	 * 
	 * @param clazz 要内省的类
	 * @param name 字段的名称
	 * 
	 * @return 相应的Field对象, 或{@code null}
	 */
	public static Field findField(Class<?> clazz, String name) {
		return findField(clazz, name, null);
	}

	/**
	 * 尝试使用提供的{@code name}和/或{@link Class type}在提供的{@link Class}上查找{@link Field field}.
	 * 搜索所有超类直至{@link Object}.
	 * 
	 * @param clazz 要内省的类
	 * @param name 字段的名称 (可能是{@code null}, 如果指定了 type)
	 * @param type 字段的类型 (可能是{@code null}, 如果指定了 name)
	 * 
	 * @return 相应的Field对象, 或{@code null}
	 */
	public static Field findField(Class<?> clazz, String name, Class<?> type) {
		Assert.notNull(clazz, "Class must not be null");
		Assert.isTrue(name != null || type != null, "Either name or type of the field must be specified");
		Class<?> searchType = clazz;
		while (Object.class != searchType && searchType != null) {
			Field[] fields = getDeclaredFields(searchType);
			for (Field field : fields) {
				if ((name == null || name.equals(field.getName())) &&
						(type == null || type.equals(field.getType()))) {
					return field;
				}
			}
			searchType = searchType.getSuperclass();
		}
		return null;
	}

	/**
	 * 将指定的{@link Object 目标对象}上提供的{@link Field 字段对象}表示的字段设置为指定的{@code value}.
	 * 根据{@link Field#set(Object, Object)}语义, 如果底层字段具有基本类型, 则新值将自动解包.
	 * <p>通过调用{@link #handleReflectionException(Exception)}来处理抛出的异常.
	 * 
	 * @param field 要设置的字段
	 * @param target 要在其上设置字段的目标对象
	 * @param value 要设置的值 (may be {@code null})
	 */
	public static void setField(Field field, Object target, Object value) {
		try {
			field.set(target, value);
		}
		catch (IllegalAccessException ex) {
			handleReflectionException(ex);
			throw new IllegalStateException(
					"Unexpected reflection exception - " + ex.getClass().getName() + ": " + ex.getMessage());
		}
	}

	/**
	 * 在指定的{@link Object 目标对象}上获取由提供的{@link Field 字段对象}表示的字段.
	 * 根据{@link Field#get(Object)}语义, 如果底层字段具有基本类型, 则返回的值将自动包装.
	 * <p>通过调用{@link #handleReflectionException(Exception)}来处理抛出的异常.
	 * 
	 * @param field 要获取的字段
	 * @param target 要获取该字段的目标对象
	 * 
	 * @return 字段的当前值
	 */
	public static Object getField(Field field, Object target) {
		try {
			return field.get(target);
		}
		catch (IllegalAccessException ex) {
			handleReflectionException(ex);
			throw new IllegalStateException(
					"Unexpected reflection exception - " + ex.getClass().getName() + ": " + ex.getMessage());
		}
	}

	/**
	 * 尝试使用提供的名称和没有参数, 在提供的类上查找{@link Method}.
	 * 搜索所有超类直至{@code Object}.
	 * <p>如果找不到{@link Method}, 则返回{@code null}.
	 * 
	 * @param clazz 要内省的类
	 * @param name 方法名
	 * 
	 * @return Method对象, 或{@code null}
	 */
	public static Method findMethod(Class<?> clazz, String name) {
		return findMethod(clazz, name, new Class<?>[0]);
	}

	/**
	 * 尝试使用提供的名称和参数类型, 在提供的类上查找{@link Method}.
	 * 搜索所有超类直至{@code Object}.
	 * <p>如果找不到{@link Method}, 则返回{@code null}.
	 * 
	 * @param clazz 要内省的类
	 * @param name 方法名
	 * @param paramTypes 方法的参数类型 (可以是{@code null}来表示任何签名)
	 * 
	 * @return Method对象, 或{@code null}
	 */
	public static Method findMethod(Class<?> clazz, String name, Class<?>... paramTypes) {
		Assert.notNull(clazz, "Class must not be null");
		Assert.notNull(name, "Method name must not be null");
		Class<?> searchType = clazz;
		while (searchType != null) {
			Method[] methods = (searchType.isInterface() ? searchType.getMethods() : getDeclaredMethods(searchType));
			for (Method method : methods) {
				if (name.equals(method.getName()) &&
						(paramTypes == null || Arrays.equals(paramTypes, method.getParameterTypes()))) {
					return method;
				}
			}
			searchType = searchType.getSuperclass();
		}
		return null;
	}

	/**
	 * 在没有参数的情况下针对提供的目标对象调用指定的{@link Method}.
	 * 调用静态 {@link Method}时, 目标对象可以是{@code null}.
	 * <p>通过调用{@link #handleReflectionException}来处理抛出的异常.
	 * 
	 * @param method 要调用的方法
	 * @param target 要调用方法的目标对象
	 * 
	 * @return 调用结果
	 */
	public static Object invokeMethod(Method method, Object target) {
		return invokeMethod(method, target, new Object[0]);
	}

	/**
	 * 使用提供的参数针对提供的目标对象调用指定的{@link Method}.
	 * 调用静态 {@link Method}时, 目标对象可以是{@code null}.
	 * <p>通过调用{@link #handleReflectionException}来处理抛出的异常.
	 * 
	 * @param method 要调用的方法
	 * @param target 要调用方法的目标对象
	 * @param args 调用参数 (may be {@code null})
	 * 
	 * @return 调用结果
	 */
	public static Object invokeMethod(Method method, Object target, Object... args) {
		try {
			return method.invoke(target, args);
		}
		catch (Exception ex) {
			handleReflectionException(ex);
		}
		throw new IllegalStateException("Should never get here");
	}

	/**
	 * 在没有参数的情况下针对提供的目标对象调用指定的JDBC API {@link Method}.
	 * 
	 * @param method 要调用的方法
	 * @param target 要调用方法的目标对象
	 * 
	 * @return 调用结果
	 * @throws SQLException 要重新抛出的JDBC API SQLException
	 */
	public static Object invokeJdbcMethod(Method method, Object target) throws SQLException {
		return invokeJdbcMethod(method, target, new Object[0]);
	}

	/**
	 * 使用提供的参数针对提供的目标对象调用指定的JDBC API {@link Method}.
	 * 
	 * @param method 要调用的方法
	 * @param target 要调用方法的目标对象
	 * @param args 调用参数 (may be {@code null})
	 * 
	 * @return 调用结果
	 * @throws SQLException 要重新抛出的JDBC API SQLException
	 */
	public static Object invokeJdbcMethod(Method method, Object target, Object... args) throws SQLException {
		try {
			return method.invoke(target, args);
		}
		catch (IllegalAccessException ex) {
			handleReflectionException(ex);
		}
		catch (InvocationTargetException ex) {
			if (ex.getTargetException() instanceof SQLException) {
				throw (SQLException) ex.getTargetException();
			}
			handleInvocationTargetException(ex);
		}
		throw new IllegalStateException("Should never get here");
	}

	/**
	 * 处理给定的反射异常. 只有在目标方法不会抛出受检异常时才应调用.
	 * <p>如果出现具有此根本原因的InvocationTargetException, 则抛出底层RuntimeException或Error.
	 * 使用适当的消息抛出IllegalStateException, 否则抛出UndeclaredThrowableException.
	 * 
	 * @param ex 要处理的反射异常
	 */
	public static void handleReflectionException(Exception ex) {
		if (ex instanceof NoSuchMethodException) {
			throw new IllegalStateException("Method not found: " + ex.getMessage());
		}
		if (ex instanceof IllegalAccessException) {
			throw new IllegalStateException("Could not access method: " + ex.getMessage());
		}
		if (ex instanceof InvocationTargetException) {
			handleInvocationTargetException((InvocationTargetException) ex);
		}
		if (ex instanceof RuntimeException) {
			throw (RuntimeException) ex;
		}
		throw new UndeclaredThrowableException(ex);
	}

	/**
	 * 处理给定的调用目标异常.
	 * 只有在目标方法不会抛出受检异常时才应调用.
	 * <p>如果出现这种根本原因, 则抛出底层RuntimeException或Error. 否则抛出 UndeclaredThrowableException.
	 * 
	 * @param ex 要处理的调用目标异常
	 */
	public static void handleInvocationTargetException(InvocationTargetException ex) {
		rethrowRuntimeException(ex.getTargetException());
	}

	/**
	 * 重新抛出给定的{@link Throwable exception}, 这可能是{@link InvocationTargetException}的<em>目标异常</em>.
	 * 只有在目标方法不会抛出受检异常时才应调用.
	 * <p>如果合适, 将底层异常转换为{@link RuntimeException}或{@link Error};
	 * 否则抛出 {@link UndeclaredThrowableException}.
	 * 
	 * @param ex 要重新抛出的异常
	 * 
	 * @throws RuntimeException 重新抛出的异常
	 */
	public static void rethrowRuntimeException(Throwable ex) {
		if (ex instanceof RuntimeException) {
			throw (RuntimeException) ex;
		}
		if (ex instanceof Error) {
			throw (Error) ex;
		}
		throw new UndeclaredThrowableException(ex);
	}

	/**
	 * 重新抛出给定的{@link Throwable exception}, 这可能是{@link InvocationTargetException}的<em>目标异常</em>.
	 * 只有在目标方法不会抛出受检异常时才应调用.
	 * <p>如果合适, 将底层异常转换为{@link RuntimeException}或{@link Error};
	 * 否则抛出 {@link UndeclaredThrowableException}.
	 * 
	 * @param ex 要重新抛出的异常
	 * 
	 * @throws Exception 重新抛出的异常 (在受检异常的情况下)
	 */
	public static void rethrowException(Throwable ex) throws Exception {
		if (ex instanceof Exception) {
			throw (Exception) ex;
		}
		if (ex instanceof Error) {
			throw (Error) ex;
		}
		throw new UndeclaredThrowableException(ex);
	}

	/**
	 * 确定给定方法是否显式声明给定的异常或其超类之一, 这意味着该类型的异常可以在反射调用中按原样传播.
	 * 
	 * @param method 声明方法
	 * @param exceptionType 要抛出的异常的类型
	 * 
	 * @return {@code true} 如果异常可以按原样抛出; {@code false}如果它需要包装
	 */
	public static boolean declaresException(Method method, Class<?> exceptionType) {
		Assert.notNull(method, "Method must not be null");
		Class<?>[] declaredExceptions = method.getExceptionTypes();
		for (Class<?> declaredException : declaredExceptions) {
			if (declaredException.isAssignableFrom(exceptionType)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 确定给定字段是否为 "public static final"常量.
	 * 
	 * @param field 要检查的字段
	 */
	public static boolean isPublicStaticFinal(Field field) {
		int modifiers = field.getModifiers();
		return (Modifier.isPublic(modifiers) && Modifier.isStatic(modifiers) && Modifier.isFinal(modifiers));
	}

	/**
	 * 确定给定方法是否为 "equals"方法.
	 */
	public static boolean isEqualsMethod(Method method) {
		if (method == null || !method.getName().equals("equals")) {
			return false;
		}
		Class<?>[] paramTypes = method.getParameterTypes();
		return (paramTypes.length == 1 && paramTypes[0] == Object.class);
	}

	/**
	 * 确定给定方法是否为 "hashCode"方法.
	 */
	public static boolean isHashCodeMethod(Method method) {
		return (method != null && method.getName().equals("hashCode") && method.getParameterTypes().length == 0);
	}

	/**
	 * 确定给定方法是否为 "toString"方法.
	 */
	public static boolean isToStringMethod(Method method) {
		return (method != null && method.getName().equals("toString") && method.getParameterTypes().length == 0);
	}

	/**
	 * 确定给定方法是否最初由{@link java.lang.Object}声明.
	 */
	public static boolean isObjectMethod(Method method) {
		if (method == null) {
			return false;
		}
		try {
			Object.class.getDeclaredMethod(method.getName(), method.getParameterTypes());
			return true;
		}
		catch (Exception ex) {
			return false;
		}
	}

	/**
	 * 确定给定方法是否是CGLIB'重命名'方法, 遵循模式"CGLIB$methodName$0".
	 * 
	 * @param renamedMethod 要检查的方法
	 */
	public static boolean isCglibRenamedMethod(Method renamedMethod) {
		String name = renamedMethod.getName();
		if (name.startsWith(CGLIB_RENAMED_METHOD_PREFIX)) {
			int i = name.length() - 1;
			while (i >= 0 && Character.isDigit(name.charAt(i))) {
				i--;
			}
			return ((i > CGLIB_RENAMED_METHOD_PREFIX.length()) &&
						(i < name.length() - 1) && name.charAt(i) == '$');
		}
		return false;
	}

	/**
	 * 使给定字段可访问, 必要时明确设置它可访问.
	 * 只有在实际需要时才会调用{@code setAccessible(true)}方法, 以避免与JVM SecurityManager发生不必要的冲突.
	 * 
	 * @param field 要使其可访问的字段
	 */
	public static void makeAccessible(Field field) {
		if ((!Modifier.isPublic(field.getModifiers()) ||
				!Modifier.isPublic(field.getDeclaringClass().getModifiers()) ||
				Modifier.isFinal(field.getModifiers())) && !field.isAccessible()) {
			field.setAccessible(true);
		}
	}

	/**
	 * 使给定方法可访问, 必要时明确设置它可访问.
	 * 只有在实际需要时才会调用{@code setAccessible(true)}方法, 以避免与JVM SecurityManager发生不必要的冲突.
	 * 
	 * @param method 要使其可访问的方法
	 */
	public static void makeAccessible(Method method) {
		if ((!Modifier.isPublic(method.getModifiers()) ||
				!Modifier.isPublic(method.getDeclaringClass().getModifiers())) && !method.isAccessible()) {
			method.setAccessible(true);
		}
	}

	/**
	 * 使给定的构造函数可访问, 必要时显式设置它是可访问的.
	 * 只有在实际需要时才会调用{@code setAccessible(true)}方法, 以避免与JVM SecurityManager发生不必要的冲突.
	 * 
	 * @param ctor 要使其可访问的构造函数
	 */
	public static void makeAccessible(Constructor<?> ctor) {
		if ((!Modifier.isPublic(ctor.getModifiers()) ||
				!Modifier.isPublic(ctor.getDeclaringClass().getModifiers())) && !ctor.isAccessible()) {
			ctor.setAccessible(true);
		}
	}

	/**
	 * 对给定类的所有匹配方法执行给定的回调操作, 如本地声明的或等效的 (例如给定类实现的基于Java 8的接口上的默认方法).
	 * 
	 * @param clazz 要内省的类
	 * @param mc 为每个方法调用的回调
	 */
	public static void doWithLocalMethods(Class<?> clazz, MethodCallback mc) {
		Method[] methods = getDeclaredMethods(clazz);
		for (Method method : methods) {
			try {
				mc.doWith(method);
			}
			catch (IllegalAccessException ex) {
				throw new IllegalStateException("Not allowed to access method '" + method.getName() + "': " + ex);
			}
		}
	}

	/**
	 * 对给定类和超类的所有匹配方法执行给定的回调操作.
	 * <p>在子类和超类上发生的相同命名方法将出现两次, 除非{@link MethodFilter}排除.
	 * 
	 * @param clazz 要内省的类
	 * @param mc 为每个方法调用的回调
	 */
	public static void doWithMethods(Class<?> clazz, MethodCallback mc) {
		doWithMethods(clazz, mc, null);
	}

	/**
	 * 对给定类和超类 (或给定的接口和超级接口)的所有匹配方法执行给定的回调操作.
	 * <p>在子类和超类上发生的相同命名方法将出现两次, 除非{@link MethodFilter}排除.
	 * 
	 * @param clazz 要内省的类
	 * @param mc 为每个方法调用的回调
	 * @param mf 确定应用回调的方法的过滤器
	 */
	public static void doWithMethods(Class<?> clazz, MethodCallback mc, MethodFilter mf) {
		// 继续备份继承层次结构.
		Method[] methods = getDeclaredMethods(clazz);
		for (Method method : methods) {
			if (mf != null && !mf.matches(method)) {
				continue;
			}
			try {
				mc.doWith(method);
			}
			catch (IllegalAccessException ex) {
				throw new IllegalStateException("Not allowed to access method '" + method.getName() + "': " + ex);
			}
		}
		if (clazz.getSuperclass() != null) {
			doWithMethods(clazz.getSuperclass(), mc, mf);
		}
		else if (clazz.isInterface()) {
			for (Class<?> superIfc : clazz.getInterfaces()) {
				doWithMethods(superIfc, mc, mf);
			}
		}
	}

	/**
	 * 获取叶子类和所有超类上的所有声明的方法.
	 * 首先包括叶子类方法.
	 * 
	 * @param leafClass 要内省的类
	 */
	public static Method[] getAllDeclaredMethods(Class<?> leafClass) {
		final List<Method> methods = new ArrayList<Method>(32);
		doWithMethods(leafClass, new MethodCallback() {
			@Override
			public void doWith(Method method) {
				methods.add(method);
			}
		});
		return methods.toArray(new Method[methods.size()]);
	}

	/**
	 * 在叶子类和所有超类上获取唯一的声明的方法集.
	 * 首先包括叶子类方法, 并且在遍历超类层次结构时, 使用与已包含的方法匹配的签名找到的任何方法都被过滤掉.
	 * 
	 * @param leafClass 要内省的类
	 */
	public static Method[] getUniqueDeclaredMethods(Class<?> leafClass) {
		final List<Method> methods = new ArrayList<Method>(32);
		doWithMethods(leafClass, new MethodCallback() {
			@Override
			public void doWith(Method method) {
				boolean knownSignature = false;
				Method methodBeingOverriddenWithCovariantReturnType = null;
				for (Method existingMethod : methods) {
					if (method.getName().equals(existingMethod.getName()) &&
							Arrays.equals(method.getParameterTypes(), existingMethod.getParameterTypes())) {
						// 这是一种协变返回类型的情况?
						if (existingMethod.getReturnType() != method.getReturnType() &&
								existingMethod.getReturnType().isAssignableFrom(method.getReturnType())) {
							methodBeingOverriddenWithCovariantReturnType = existingMethod;
						}
						else {
							knownSignature = true;
						}
						break;
					}
				}
				if (methodBeingOverriddenWithCovariantReturnType != null) {
					methods.remove(methodBeingOverriddenWithCovariantReturnType);
				}
				if (!knownSignature && !isCglibRenamedMethod(method)) {
					methods.add(method);
				}
			}
		});
		return methods.toArray(new Method[methods.size()]);
	}

	/**
	 * 此变体从本地缓存中检索{@link Class#getDeclaredMethods()}, 以避免JVM的SecurityManager检查和防御数组复制.
	 * 此外, 它还包括来自本地实现的接口的Java 8默认方法, 因为这些方法与声明的方法一样被有效地处理.
	 * 
	 * @param clazz 要内省的类
	 * 
	 * @return 方法的缓存数组
	 */
	private static Method[] getDeclaredMethods(Class<?> clazz) {
		Assert.notNull(clazz, "Class must not be null");
		Method[] result = declaredMethodsCache.get(clazz);
		if (result == null) {
			Method[] declaredMethods = clazz.getDeclaredMethods();
			List<Method> defaultMethods = findConcreteMethodsOnInterfaces(clazz);
			if (defaultMethods != null) {
				result = new Method[declaredMethods.length + defaultMethods.size()];
				System.arraycopy(declaredMethods, 0, result, 0, declaredMethods.length);
				int index = declaredMethods.length;
				for (Method defaultMethod : defaultMethods) {
					result[index] = defaultMethod;
					index++;
				}
			}
			else {
				result = declaredMethods;
			}
			declaredMethodsCache.put(clazz, (result.length == 0 ? NO_METHODS : result));
		}
		return result;
	}

	private static List<Method> findConcreteMethodsOnInterfaces(Class<?> clazz) {
		List<Method> result = null;
		for (Class<?> ifc : clazz.getInterfaces()) {
			for (Method ifcMethod : ifc.getMethods()) {
				if (!Modifier.isAbstract(ifcMethod.getModifiers())) {
					if (result == null) {
						result = new LinkedList<Method>();
					}
					result.add(ifcMethod);
				}
			}
		}
		return result;
	}

	/**
	 * 在给定类的所有本地声明的字段上调用给定的回调.
	 * 
	 * @param clazz 要分析的目标类
	 * @param fc 为每个字段调用的回调
	 */
	public static void doWithLocalFields(Class<?> clazz, FieldCallback fc) {
		for (Field field : getDeclaredFields(clazz)) {
			try {
				fc.doWith(field);
			}
			catch (IllegalAccessException ex) {
				throw new IllegalStateException("Not allowed to access field '" + field.getName() + "': " + ex);
			}
		}
	}

	/**
	 * 在目标类的所有字段上调用给定的回调, 向上遍历类层次结构以获取所有声明的字段.
	 * 
	 * @param clazz 要分析的目标类
	 * @param fc 为每个字段调用的回调
	 */
	public static void doWithFields(Class<?> clazz, FieldCallback fc) {
		doWithFields(clazz, fc, null);
	}

	/**
	 * 在目标类的所有字段上调用给定的回调, 向上遍历类层次结构以获取所有声明的字段.
	 * 
	 * @param clazz 要分析的目标类
	 * @param fc 为每个字段调用的回调
	 * @param ff 确定要应用回调的字段的过滤器
	 */
	public static void doWithFields(Class<?> clazz, FieldCallback fc, FieldFilter ff) {
		// 继续备份继承层次结构.
		Class<?> targetClass = clazz;
		do {
			Field[] fields = getDeclaredFields(targetClass);
			for (Field field : fields) {
				if (ff != null && !ff.matches(field)) {
					continue;
				}
				try {
					fc.doWith(field);
				}
				catch (IllegalAccessException ex) {
					throw new IllegalStateException("Not allowed to access field '" + field.getName() + "': " + ex);
				}
			}
			targetClass = targetClass.getSuperclass();
		}
		while (targetClass != null && targetClass != Object.class);
	}

	/**
	 * 此变体从本地缓存中检索{@link Class#getDeclaredFields()}, 以避免JVM的SecurityManager检查和防御数组复制.
	 * 
	 * @param clazz 要内省的类
	 * 
	 * @return 缓存的字段数组
	 */
	private static Field[] getDeclaredFields(Class<?> clazz) {
		Assert.notNull(clazz, "Class must not be null");
		Field[] result = declaredFieldsCache.get(clazz);
		if (result == null) {
			result = clazz.getDeclaredFields();
			declaredFieldsCache.put(clazz, (result.length == 0 ? NO_FIELDS : result));
		}
		return result;
	}

	/**
	 * 给定源对象和目标, 必须是同一个类或子类, 复制所有字段, 包括继承的字段.
	 * 设计用于处理具有公共无参数构造函数的对象.
	 */
	public static void shallowCopyFieldState(final Object src, final Object dest) {
		Assert.notNull(src, "Source for field copy cannot be null");
		Assert.notNull(dest, "Destination for field copy cannot be null");
		if (!src.getClass().isAssignableFrom(dest.getClass())) {
			throw new IllegalArgumentException("Destination class [" + dest.getClass().getName() +
					"] must be same or subclass as source class [" + src.getClass().getName() + "]");
		}
		doWithFields(src.getClass(), new FieldCallback() {
			@Override
			public void doWith(Field field) throws IllegalArgumentException, IllegalAccessException {
				makeAccessible(field);
				Object srcValue = field.get(src);
				field.set(dest, srcValue);
			}
		}, COPYABLE_FIELDS);
	}

	/**
	 * 清除内部方法/字段缓存.
	 */
	public static void clearCache() {
		declaredMethodsCache.clear();
		declaredFieldsCache.clear();
	}


	/**
	 * 对每种方法采取的动作.
	 */
	public interface MethodCallback {

		/**
		 * 使用给定方法执行操作.
		 * 
		 * @param method 要操作的方法
		 */
		void doWith(Method method) throws IllegalArgumentException, IllegalAccessException;
	}


	/**
	 * 用于过滤方法回调要操作的方法.
	 */
	public interface MethodFilter {

		/**
		 * 确定给定方法是否匹配.
		 * 
		 * @param method 要检查的方法
		 */
		boolean matches(Method method);
	}


	/**
	 * 在层次结构中的每个字段上调用的回调接口.
	 */
	public interface FieldCallback {

		/**
		 * 使用给定字段执行操作.
		 * 
		 * @param field 要操作的字段
		 */
		void doWith(Field field) throws IllegalArgumentException, IllegalAccessException;
	}


	/**
	 * 用于过滤由字段回调要操作的字段.
	 */
	public interface FieldFilter {

		/**
		 * 确定给定字段是否匹配.
		 * 
		 * @param field 要检查的字段
		 */
		boolean matches(Field field);
	}


	/**
	 * 预构建的FieldFilter, 匹配所有非static, 非final字段.
	 */
	public static final FieldFilter COPYABLE_FIELDS = new FieldFilter() {

		@Override
		public boolean matches(Field field) {
			return !(Modifier.isStatic(field.getModifiers()) || Modifier.isFinal(field.getModifiers()));
		}
	};


	/**
	 * 预构建的MethodFilter, 匹配所有非桥接方法.
	 */
	public static final MethodFilter NON_BRIDGED_METHODS = new MethodFilter() {

		@Override
		public boolean matches(Method method) {
			return !method.isBridge();
		}
	};


	/**
	 * 预构建的MethodFilter, 它匹配未在{@code java.lang.Object}上声明的所有非桥接方法.
	 */
	public static final MethodFilter USER_DECLARED_METHODS = new MethodFilter() {

		@Override
		public boolean matches(Method method) {
			return (!method.isBridge() && method.getDeclaringClass() != Object.class);
		}
	};
}
