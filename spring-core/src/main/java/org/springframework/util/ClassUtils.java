package org.springframework.util;

import java.beans.Introspector;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * 主要供框架内部使用.
 */
public abstract class ClassUtils {

	/** 数组类名称的后缀: "[]" */
	public static final String ARRAY_SUFFIX = "[]";

	/** 内部数组类名的前缀: "[" */
	private static final String INTERNAL_ARRAY_PREFIX = "[";

	/** 内部非基本数组类名的前缀: "[L" */
	private static final String NON_PRIMITIVE_ARRAY_PREFIX = "[L";

	/** 包分隔符: '.' */
	private static final char PACKAGE_SEPARATOR = '.';

	/** 路径分隔符: '/' */
	private static final char PATH_SEPARATOR = '/';

	/** 内部类分隔符: '$' */
	private static final char INNER_CLASS_SEPARATOR = '$';

	/** CGLIB类分隔符: "$$" */
	public static final String CGLIB_CLASS_SEPARATOR = "$$";

	/** ".class"文件后缀 */
	public static final String CLASS_FILE_SUFFIX = ".class";


	/**
	 * 将原始包装类型作为键, 将相应的原始类型作为值, 例如: Integer.class -> int.class.
	 */
	private static final Map<Class<?>, Class<?>> primitiveWrapperTypeMap = new IdentityHashMap<Class<?>, Class<?>>(8);

	/**
	 * 将原始类型作为键, 将相应的包装类型作为值, 例如: int.class -> Integer.class.
	 */
	private static final Map<Class<?>, Class<?>> primitiveTypeToWrapperMap = new IdentityHashMap<Class<?>, Class<?>>(8);

	/**
	 * 将原始类型名称作为键, 将相应的原始类型作为值, 例如: "int" -> "int.class".
	 */
	private static final Map<String, Class<?>> primitiveTypeNameMap = new HashMap<String, Class<?>>(32);

	/**
	 * 将常用的"java.lang"类名称作为键, 并将相应的Class作为值.
	 * 主要用于远程调用的有效反序列化.
	 */
	private static final Map<String, Class<?>> commonClassCache = new HashMap<String, Class<?>>(32);


	static {
		primitiveWrapperTypeMap.put(Boolean.class, boolean.class);
		primitiveWrapperTypeMap.put(Byte.class, byte.class);
		primitiveWrapperTypeMap.put(Character.class, char.class);
		primitiveWrapperTypeMap.put(Double.class, double.class);
		primitiveWrapperTypeMap.put(Float.class, float.class);
		primitiveWrapperTypeMap.put(Integer.class, int.class);
		primitiveWrapperTypeMap.put(Long.class, long.class);
		primitiveWrapperTypeMap.put(Short.class, short.class);

		for (Map.Entry<Class<?>, Class<?>> entry : primitiveWrapperTypeMap.entrySet()) {
			primitiveTypeToWrapperMap.put(entry.getValue(), entry.getKey());
			registerCommonClasses(entry.getKey());
		}

		Set<Class<?>> primitiveTypes = new HashSet<Class<?>>(64);
		primitiveTypes.addAll(primitiveWrapperTypeMap.values());
		primitiveTypes.addAll(Arrays.asList(new Class<?>[] {
				boolean[].class, byte[].class, char[].class, double[].class,
				float[].class, int[].class, long[].class, short[].class}));
		primitiveTypes.add(void.class);
		for (Class<?> primitiveType : primitiveTypes) {
			primitiveTypeNameMap.put(primitiveType.getName(), primitiveType);
		}

		registerCommonClasses(Boolean[].class, Byte[].class, Character[].class, Double[].class,
				Float[].class, Integer[].class, Long[].class, Short[].class);
		registerCommonClasses(Number.class, Number[].class, String.class, String[].class,
				Class.class, Class[].class, Object.class, Object[].class);
		registerCommonClasses(Throwable.class, Exception.class, RuntimeException.class,
				Error.class, StackTraceElement.class, StackTraceElement[].class);
		registerCommonClasses(Enum.class, Iterable.class, Cloneable.class, Comparable.class);
	}


	/**
	 * 使用ClassUtils缓存注册给定的公共类.
	 */
	private static void registerCommonClasses(Class<?>... commonClasses) {
		for (Class<?> clazz : commonClasses) {
			commonClassCache.put(clazz.getName(), clazz);
		}
	}

	/**
	 * 返回要使用的默认ClassLoader: 通常是线程上下文ClassLoader;
	 * 加载ClassUtils类的ClassLoader将用作回退.
	 * <p>如果打算在明显更喜欢非null ClassLoader引用的场景中使用线程上下文ClassLoader, 请调用此方法:
	 * 例如, 对于类路径资源加载 (但不一定是{@code Class.forName}, 它也接受{@code null} ClassLoader引用).
	 * 
	 * @return 默认的ClassLoader (只有系统ClassLoader不可访问, 才为{@code null})
	 */
	public static ClassLoader getDefaultClassLoader() {
		ClassLoader cl = null;
		try {
			cl = Thread.currentThread().getContextClassLoader();
		}
		catch (Throwable ex) {
			// 无法访问线程上下文ClassLoader - falling back...
		}
		if (cl == null) {
			// 没有线程上下文类加载器 -> 使用此类的类加载器.
			cl = ClassUtils.class.getClassLoader();
			if (cl == null) {
				// getClassLoader() 返回null表示引导程序ClassLoader
				try {
					cl = ClassLoader.getSystemClassLoader();
				}
				catch (Throwable ex) {
					// 无法访问系统ClassLoader - 哦, 也许调用者可以使用null...
				}
			}
		}
		return cl;
	}

	/**
	 * 如果需要, 用环境的bean ClassLoader覆盖线程上下文ClassLoader,
	 * i.e. 如果bean ClassLoader已经不等同于线程上下文ClassLoader.
	 * 
	 * @param classLoaderToUse 用于线程上下文的实际ClassLoader
	 * 
	 * @return 原始线程上下文ClassLoader; 如果没有重写, 则为{@code null}
	 */
	public static ClassLoader overrideThreadContextClassLoader(ClassLoader classLoaderToUse) {
		Thread currentThread = Thread.currentThread();
		ClassLoader threadContextClassLoader = currentThread.getContextClassLoader();
		if (classLoaderToUse != null && !classLoaderToUse.equals(threadContextClassLoader)) {
			currentThread.setContextClassLoader(classLoaderToUse);
			return threadContextClassLoader;
		}
		else {
			return null;
		}
	}

	/**
	 * 替换{@code Class.forName()}, 它还返回基本数据类型的Class实例 (e.g. "int") 和数组类名 (e.g. "String[]").
	 * 此外, 它还能够以Java源代码样式解析内部类名称 (e.g. "java.lang.Thread.State", 而不是"java.lang.Thread$State").
	 * 
	 * @param name Class的名称
	 * @param classLoader 要使用的类加载器 (可能是{@code null}, 表示默认的类加载器)
	 * 
	 * @return 提供的名称的Class实例
	 * @throws ClassNotFoundException 如果没找到类
	 * @throws LinkageError 如果无法加载类文件
	 */
	public static Class<?> forName(String name, ClassLoader classLoader) throws ClassNotFoundException, LinkageError {
		Assert.notNull(name, "Name must not be null");

		Class<?> clazz = resolvePrimitiveClassName(name);
		if (clazz == null) {
			clazz = commonClassCache.get(name);
		}
		if (clazz != null) {
			return clazz;
		}

		// "java.lang.String[]" style arrays
		if (name.endsWith(ARRAY_SUFFIX)) {
			String elementClassName = name.substring(0, name.length() - ARRAY_SUFFIX.length());
			Class<?> elementClass = forName(elementClassName, classLoader);
			return Array.newInstance(elementClass, 0).getClass();
		}

		// "[Ljava.lang.String;" style arrays
		if (name.startsWith(NON_PRIMITIVE_ARRAY_PREFIX) && name.endsWith(";")) {
			String elementName = name.substring(NON_PRIMITIVE_ARRAY_PREFIX.length(), name.length() - 1);
			Class<?> elementClass = forName(elementName, classLoader);
			return Array.newInstance(elementClass, 0).getClass();
		}

		// "[[I" or "[[Ljava.lang.String;" style arrays
		if (name.startsWith(INTERNAL_ARRAY_PREFIX)) {
			String elementName = name.substring(INTERNAL_ARRAY_PREFIX.length());
			Class<?> elementClass = forName(elementName, classLoader);
			return Array.newInstance(elementClass, 0).getClass();
		}

		ClassLoader clToUse = classLoader;
		if (clToUse == null) {
			clToUse = getDefaultClassLoader();
		}
		try {
			return (clToUse != null ? clToUse.loadClass(name) : Class.forName(name));
		}
		catch (ClassNotFoundException ex) {
			int lastDotIndex = name.lastIndexOf(PACKAGE_SEPARATOR);
			if (lastDotIndex != -1) {
				String innerClassName =
						name.substring(0, lastDotIndex) + INNER_CLASS_SEPARATOR + name.substring(lastDotIndex + 1);
				try {
					return (clToUse != null ? clToUse.loadClass(innerClassName) : Class.forName(innerClassName));
				}
				catch (ClassNotFoundException ex2) {
					// Swallow - let original exception get through
				}
			}
			throw ex;
		}
	}

	/**
	 * 将给定的类名解析为Class实例. 支持基础类型 (like "int") 和数组类名 (like "String[]").
	 * <p>这实际上等同于具有相同参数的{@code forName}方法, 唯一的区别是类加载失败时抛出的异常.
	 * 
	 * @param className Class的名称
	 * @param classLoader 要使用的类加载器 (可能是{@code null}, 表示默认的类加载器)
	 * 
	 * @return 提供的名称的Class实例
	 * @throws IllegalArgumentException 如果类名不可解析 (也就是说, 找不到类, 或者无法加载类文件)
	 */
	public static Class<?> resolveClassName(String className, ClassLoader classLoader) throws IllegalArgumentException {
		try {
			return forName(className, classLoader);
		}
		catch (ClassNotFoundException ex) {
			throw new IllegalArgumentException("Could not find class [" + className + "]", ex);
		}
		catch (LinkageError err) {
			throw new IllegalArgumentException("Unresolvable class definition for class [" + className + "]", err);
		}
	}

	/**
	 * 确定由提供的名称标识的{@link Class}是否存在且可以加载.
	 * 如果类或其中一个依赖项不存在或无法加载, 将返回{@code false}.
	 * 
	 * @param className 要检查的类的名称
	 * @param classLoader 要使用的类加载器 (可能是{@code null}, 表示默认的类加载器)
	 *
	 * @return 是否存在指定的类
	 */
	public static boolean isPresent(String className, ClassLoader classLoader) {
		try {
			forName(className, classLoader);
			return true;
		}
		catch (Throwable ex) {
			// 类或其中一个依赖项不存在...
			return false;
		}
	}

	/**
	 * 检查给定的类在给定的ClassLoader中是否可见.
	 * 
	 * @param clazz 要检查的类 (通常是接口)
	 * @param classLoader 要检查的ClassLoader
	 * (可能是{@code null}, 在这种情况下, 此方法将始终返回{@code true})
	 */
	public static boolean isVisible(Class<?> clazz, ClassLoader classLoader) {
		if (classLoader == null) {
			return true;
		}
		try {
			return (clazz == classLoader.loadClass(clazz.getName()));
			// Else: 发现了同名的不同类
		}
		catch (ClassNotFoundException ex) {
			// 根本找不到相应的类
			return false;
		}
	}

	/**
	 * 检查给定的类在给定的上下文中是否是缓存安全的, i.e. 它是由给定的ClassLoader还是由其父类加载的.
	 * 
	 * @param clazz 要分析的类
	 * @param classLoader 可能在其中缓存元数据的ClassLoader
	 * (可能是{@code null}, 表示系统类加载器)
	 */
	public static boolean isCacheSafe(Class<?> clazz, ClassLoader classLoader) {
		Assert.notNull(clazz, "Class must not be null");
		try {
			ClassLoader target = clazz.getClassLoader();
			// 一般情况
			if (target == classLoader || target == null) {
				return true;
			}
			if (classLoader == null) {
				return false;
			}
			// 检查祖先的匹配情况 -> positive
			ClassLoader current = classLoader;
			while (current != null) {
				current = current.getParent();
				if (current == target) {
					return true;
				}
			}
			// 检查子级的匹配情况 -> negative
			while (target != null) {
				target = target.getParent();
				if (target == classLoader) {
					return false;
				}
			}
		}
		catch (SecurityException ex) {
			// 请参阅下面的Class reference比较
		}

		// 没有父/子关系的ClassLoaders的后备:
		// 如果可以从给定的ClassLoader加载相同的类, 则安全
		return (classLoader != null && isVisible(clazz, classLoader));
	}

	/**
	 * 根据JVM的基本类命名规则, 将给定的类名解析为基本类型.
	 * <p>还支持JVM的基本类型数组的内部类名.
	 * <i>不</i>支持基本类型数组的"[]"后缀表示法; 这仅由{@link #forName(String, ClassLoader)}支持.
	 * 
	 * @param name 潜在的基本类型的Class的名称
	 * 
	 * @return 基本类型Class, 或{@code null}如果名称不表示基本类型或基本类型数组
	 */
	public static Class<?> resolvePrimitiveClassName(String name) {
		Class<?> result = null;
		// 大多数类名都会很长, 考虑到他们应该在一个包里, 所以进行长度检查是值得的.
		if (name != null && name.length() <= 8) {
			// 可能是原始的 - 可能.
			result = primitiveTypeNameMap.get(name);
		}
		return result;
	}

	/**
	 * 检查给定的类是否表示基本类型包装器, i.e. Boolean, Byte, Character, Short, Integer, Long, Float, Double.
	 * 
	 * @param clazz 要检查的类
	 * 
	 * @return 给定的类是否是基本类型包装类
	 */
	public static boolean isPrimitiveWrapper(Class<?> clazz) {
		Assert.notNull(clazz, "Class must not be null");
		return primitiveWrapperTypeMap.containsKey(clazz);
	}

	/**
	 * 检查给定的类是否表示基本类型 (i.e. boolean, byte, char, short, int, long, float, double)
	 * 或基本类型包装器 (i.e. Boolean, Byte, Character, Short, Integer, Long, Float, Double).
	 * 
	 * @param clazz 要检查的类
	 * 
	 * @return 给定的类是基本类型包装类, 还是基本类型类
	 */
	public static boolean isPrimitiveOrWrapper(Class<?> clazz) {
		Assert.notNull(clazz, "Class must not be null");
		return (clazz.isPrimitive() || isPrimitiveWrapper(clazz));
	}

	/**
	 * 检查给定的类是否表示基本类型组, i.e. boolean, byte, char, short, int, long, float, double.
	 * 
	 * @param clazz 要检查的类
	 * 
	 * @return 给定的类是否表示基本类型组
	 */
	public static boolean isPrimitiveArray(Class<?> clazz) {
		Assert.notNull(clazz, "Class must not be null");
		return (clazz.isArray() && clazz.getComponentType().isPrimitive());
	}

	/**
	 * 检查给定的类是否表示基本类型包装器的数组,
	 * i.e. Boolean, Byte, Character, Short, Integer, Long, Float, Double.
	 * 
	 * @param clazz 要检查的类
	 * 
	 * @return 给定的类是否表示基本类型包装器的数组
	 */
	public static boolean isPrimitiveWrapperArray(Class<?> clazz) {
		Assert.notNull(clazz, "Class must not be null");
		return (clazz.isArray() && isPrimitiveWrapper(clazz.getComponentType()));
	}

	/**
	 * 如果它是基本类型, 则解析给定的类, 返回相应的基本类型包装类型.
	 * 
	 * @param clazz 要检查的类
	 * 
	 * @return 基本类型Class, 或基本类型的包装器
	 */
	public static Class<?> resolvePrimitiveIfNecessary(Class<?> clazz) {
		Assert.notNull(clazz, "Class must not be null");
		return (clazz.isPrimitive() && clazz != void.class ? primitiveTypeToWrapperMap.get(clazz) : clazz);
	}

	/**
	 * 假设通过反射进行设置, 检查右侧类型是否可以指定为左侧类型.
	 * 将基本类型包装类视为可分配给相应的基本类型.
	 * 
	 * @param lhsType 目标类型
	 * @param rhsType 应分配给目标类型的值类型
	 * 
	 * @return 如果目标类型可从值类型分配
	 */
	public static boolean isAssignable(Class<?> lhsType, Class<?> rhsType) {
		Assert.notNull(lhsType, "Left-hand side type must not be null");
		Assert.notNull(rhsType, "Right-hand side type must not be null");
		if (lhsType.isAssignableFrom(rhsType)) {
			return true;
		}
		if (lhsType.isPrimitive()) {
			Class<?> resolvedPrimitive = primitiveWrapperTypeMap.get(rhsType);
			if (lhsType == resolvedPrimitive) {
				return true;
			}
		}
		else {
			Class<?> resolvedWrapper = primitiveTypeToWrapperMap.get(rhsType);
			if (resolvedWrapper != null && lhsType.isAssignableFrom(resolvedWrapper)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 假定通过反射设置, 确定给定类型是否可从给定值分配.
	 * 将基本类型包装类视为可分配给相应的基本类型.
	 * 
	 * @param type 目标类型
	 * @param value 应该分配给该类型的值
	 * 
	 * @return 如果类型可从值中分配
	 */
	public static boolean isAssignableValue(Class<?> type, Object value) {
		Assert.notNull(type, "Type must not be null");
		return (value != null ? isAssignable(type, value.getClass()) : !type.isPrimitive());
	}

	/**
	 * 将基于"/"的资源路径转换为基于"."的完全限定类名.
	 * 
	 * @param resourcePath 指向类的资源路径
	 * 
	 * @return 相应的完全限定类名
	 */
	public static String convertResourcePathToClassName(String resourcePath) {
		Assert.notNull(resourcePath, "Resource path must not be null");
		return resourcePath.replace(PATH_SEPARATOR, PACKAGE_SEPARATOR);
	}

	/**
	 * 将基于 "."的完全限定类名转换为基于 "/"的资源路径.
	 * 
	 * @param className 完全限定的类名
	 * 
	 * @return 相应的资源路径, 指向该类
	 */
	public static String convertClassNameToResourcePath(String className) {
		Assert.notNull(className, "Class name must not be null");
		return className.replace(PACKAGE_SEPARATOR, PATH_SEPARATOR);
	}

	/**
	 * 返回适合{@code ClassLoader.getResource}使用的路径
	 * (也适用于{@code Class.getResource}, 通过在返回值前加斜杠 ('/')).
	 * 通过获取指定类文件的包来构建, 将所有点 ('.') 转换为斜杠('/'), 必要时添加尾部斜杠, 并将指定的资源名称连接到此.
	 * <br/>因此, 此函数可用于构建适合加载与类文件位于同一包中的资源文件的路径,
	 * 尽管{@link org.springframework.core.io.ClassPathResource}通常更方便.
	 * 
	 * @param clazz 将其包用作基础的Class
	 * @param resourceName 要追加的资源名称. 前导斜杠是可选的.
	 * 
	 * @return 建立的资源路径
	 */
	public static String addResourcePathToPackagePath(Class<?> clazz, String resourceName) {
		Assert.notNull(resourceName, "Resource name must not be null");
		if (!resourceName.startsWith("/")) {
			return classPackageAsResourcePath(clazz) + '/' + resourceName;
		}
		return classPackageAsResourcePath(clazz) + resourceName;
	}

	/**
	 * 给定一个输入类对象, 返回一个由类的包名称组成路径名的字符串, i.e., 所有点('.')都用斜杠 ('/')替换.
	 * 既不添加前导斜杠也不添加尾随斜杠. 结果可以与斜杠和资源名称连接, 并直接提供给{@code ClassLoader.getResource()}.
	 * 要将它提供给{@code Class.getResource}, 还必须在返回值之前添加前导斜杠.
	 * 
	 * @param clazz 输入类. {@code null}值或默认 (空)包将导致返回空字符串 ("").
	 * 
	 * @return 表示包名称的路径
	 */
	public static String classPackageAsResourcePath(Class<?> clazz) {
		if (clazz == null) {
			return "";
		}
		String className = clazz.getName();
		int packageEndIndex = className.lastIndexOf(PACKAGE_SEPARATOR);
		if (packageEndIndex == -1) {
			return "";
		}
		String packageName = className.substring(0, packageEndIndex);
		return packageName.replace(PACKAGE_SEPARATOR, PATH_SEPARATOR);
	}

	/**
	 * 构建一个由给定数组中的类/接口的名称组成的String.
	 * <p>基本上像{@code AbstractCollection.toString()}, 但在每个类名之前剥去 "class "/"interface "前缀.
	 * 
	 * @param classes Class对象
	 * 
	 * @return "[com.foo.Bar, com.foo.Baz]"的字符串形式
	 */
	public static String classNamesToString(Class<?>... classes) {
		return classNamesToString(Arrays.asList(classes));
	}

	/**
	 * 构建一个由给定集合中的类/接口的名称组成的String.
	 * <p>基本上像{@code AbstractCollection.toString()}, 但在每个类名之前剥去"class "/"interface "前缀.
	 * 
	 * @param classes Class对象 (may be {@code null})
	 * 
	 * @return "[com.foo.Bar, com.foo.Baz]"的字符串形式
	 */
	public static String classNamesToString(Collection<Class<?>> classes) {
		if (CollectionUtils.isEmpty(classes)) {
			return "[]";
		}
		StringBuilder sb = new StringBuilder("[");
		for (Iterator<Class<?>> it = classes.iterator(); it.hasNext(); ) {
			Class<?> clazz = it.next();
			sb.append(clazz.getName());
			if (it.hasNext()) {
				sb.append(", ");
			}
		}
		sb.append("]");
		return sb.toString();
	}

	/**
	 * 将给定的{@code Collection}复制到{@code Class}数组中.
	 * <p>{@code Collection}只能包含{@code Class}元素.
	 * 
	 * @param collection 要复制的{@code Collection}
	 * 
	 * @return {@code Class}数组
	 */
	public static Class<?>[] toClassArray(Collection<Class<?>> collection) {
		if (collection == null) {
			return null;
		}
		return collection.toArray(new Class<?>[collection.size()]);
	}

	/**
	 * 返回给定实例实现的所有接口, 包括由超类实现的接口.
	 * 
	 * @param instance 要分析接口的实例
	 * 
	 * @return 给定实例实现的所有接口
	 */
	public static Class<?>[] getAllInterfaces(Object instance) {
		Assert.notNull(instance, "Instance must not be null");
		return getAllInterfacesForClass(instance.getClass());
	}

	/**
	 * 返回给定类实现的所有接口, 包括由超类实现的接口.
	 * <p>如果类本身是一个接口, 它将作为唯一接口返回.
	 * 
	 * @param clazz 要分析接口的类
	 * 
	 * @return 给定对象实现的所有接口
	 */
	public static Class<?>[] getAllInterfacesForClass(Class<?> clazz) {
		return getAllInterfacesForClass(clazz, null);
	}

	/**
	 * 返回给定类实现的所有接口, 包括由超类实现的接口.
	 * <p>如果类本身是一个接口, 它将作为唯一接口返回.
	 * 
	 * @param clazz 要分析接口的类
	 * @param classLoader 接口需要在其中可见的ClassLoader
	 * (接受所有声明的接口时, 可能是{@code null})
	 * 
	 * @return 给定对象实现的所有接口
	 */
	public static Class<?>[] getAllInterfacesForClass(Class<?> clazz, ClassLoader classLoader) {
		return toClassArray(getAllInterfacesForClassAsSet(clazz, classLoader));
	}

	/**
	 * 返回给定实例实现的所有接口, 包括由超类实现的接口.
	 * 
	 * @param instance 要分析接口的实例
	 * 
	 * @return 给定实例实现的所有接口
	 */
	public static Set<Class<?>> getAllInterfacesAsSet(Object instance) {
		Assert.notNull(instance, "Instance must not be null");
		return getAllInterfacesForClassAsSet(instance.getClass());
	}

	/**
	 * 返回给定类实现的所有接口, 包括由超类实现的接口.
	 * <p>如果类本身是一个接口, 它将作为唯一接口返回.
	 * 
	 * @param clazz 要分析接口的类
	 * 
	 * @return 给定对象实现的所有接口
	 */
	public static Set<Class<?>> getAllInterfacesForClassAsSet(Class<?> clazz) {
		return getAllInterfacesForClassAsSet(clazz, null);
	}

	/**
	 * 返回给定类实现的所有接口, 包括由超类实现的接口.
	 * <p>如果类本身是一个接口, 它将作为唯一接口返回.
	 * 
	 * @param clazz 要分析接口的类
	 * @param classLoader 接口需要在其中可见的ClassLoader
	 * (接受所有声明的接口时, 可能是{@code null})
	 * 
	 * @return 给定对象实现的所有接口
	 */
	public static Set<Class<?>> getAllInterfacesForClassAsSet(Class<?> clazz, ClassLoader classLoader) {
		Assert.notNull(clazz, "Class must not be null");
		if (clazz.isInterface() && isVisible(clazz, classLoader)) {
			return Collections.<Class<?>>singleton(clazz);
		}
		Set<Class<?>> interfaces = new LinkedHashSet<Class<?>>();
		Class<?> current = clazz;
		while (current != null) {
			Class<?>[] ifcs = current.getInterfaces();
			for (Class<?> ifc : ifcs) {
				interfaces.addAll(getAllInterfacesForClassAsSet(ifc, classLoader));
			}
			current = current.getSuperclass();
		}
		return interfaces;
	}

	/**
	 * 为给定接口创建复合接口Class, 在单个Class中实现给定接口.
	 * <p>此实现为给定接口构建JDK代理类.
	 * 
	 * @param interfaces 要合并的接口
	 * @param classLoader 用于创建复合类的ClassLoader
	 * 
	 * @return 合并后的接口
	 */
	public static Class<?> createCompositeInterface(Class<?>[] interfaces, ClassLoader classLoader) {
		Assert.notEmpty(interfaces, "Interfaces must not be empty");
		return Proxy.getProxyClass(classLoader, interfaces);
	}

	/**
	 * 确定给定类的共同祖先.
	 * 
	 * @param clazz1 要内省的类
	 * @param clazz2 要内省的其它类
	 * 
	 * @return 共同的祖先(i.e. 公共超类, 一个接口扩展另一个), 或{@code null}.
	 * 如果任何给定的类是{@code null}, 则返回另一个类.
	 */
	public static Class<?> determineCommonAncestor(Class<?> clazz1, Class<?> clazz2) {
		if (clazz1 == null) {
			return clazz2;
		}
		if (clazz2 == null) {
			return clazz1;
		}
		if (clazz1.isAssignableFrom(clazz2)) {
			return clazz1;
		}
		if (clazz2.isAssignableFrom(clazz1)) {
			return clazz2;
		}
		Class<?> ancestor = clazz1;
		do {
			ancestor = ancestor.getSuperclass();
			if (ancestor == null || Object.class == ancestor) {
				return null;
			}
		}
		while (!ancestor.isAssignableFrom(clazz2));
		return ancestor;
	}

	/**
	 * 检查给定对象是否为CGLIB代理.
	 * 
	 * @param object 要检查的对象
	 */
	public static boolean isCglibProxy(Object object) {
		return isCglibProxyClass(object.getClass());
	}

	/**
	 * 检查指定的类是否是CGLIB生成的类.
	 * 
	 * @param clazz 要检查的类
	 */
	public static boolean isCglibProxyClass(Class<?> clazz) {
		return (clazz != null && isCglibProxyClassName(clazz.getName()));
	}

	/**
	 * 检查指定的类名是否是CGLIB生成的类.
	 * 
	 * @param className 要检查的类名
	 */
	public static boolean isCglibProxyClassName(String className) {
		return (className != null && className.contains(CGLIB_CLASS_SEPARATOR));
	}

	/**
	 * 返回给定实例的用户定义的类:
	 * 通常只是给定实例的类, 但在CGLIB生成的子类的情况下是原始类.
	 * 
	 * @param instance 要检查的实例
	 * 
	 * @return 用户定义的类
	 */
	public static Class<?> getUserClass(Object instance) {
		Assert.notNull(instance, "Instance must not be null");
		return getUserClass(instance.getClass());
	}

	/**
	 * 返回给定类的用户定义的类:
	 * 通常只是给定的类, 但在CGLIB生成的子类的情况下是原始类.
	 * 
	 * @param clazz 要检查的类
	 * 
	 * @return 用户定义的类
	 */
	public static Class<?> getUserClass(Class<?> clazz) {
		if (clazz != null && clazz.getName().contains(CGLIB_CLASS_SEPARATOR)) {
			Class<?> superclass = clazz.getSuperclass();
			if (superclass != null && Object.class != superclass) {
				return superclass;
			}
		}
		return clazz;
	}

	/**
	 * 返回给定对象类型的描述性名称:
	 * 通常只是类名, 但是数组的组件类型类名 + "[]", 以及JDK代理的已实现接口的附加列表.
	 * 
	 * @param value 要内省的值
	 * 
	 * @return 该类的限定名称
	 */
	public static String getDescriptiveType(Object value) {
		if (value == null) {
			return null;
		}
		Class<?> clazz = value.getClass();
		if (Proxy.isProxyClass(clazz)) {
			StringBuilder result = new StringBuilder(clazz.getName());
			result.append(" implementing ");
			Class<?>[] ifcs = clazz.getInterfaces();
			for (int i = 0; i < ifcs.length; i++) {
				result.append(ifcs[i].getName());
				if (i < ifcs.length - 1) {
					result.append(',');
				}
			}
			return result.toString();
		}
		else if (clazz.isArray()) {
			return getQualifiedNameForArray(clazz);
		}
		else {
			return clazz.getName();
		}
	}

	/**
	 * 检查给定的类是否与用户指定的类型名称匹配.
	 * 
	 * @param clazz 要检查的类
	 * @param typeName 要匹配的类型名称
	 */
	public static boolean matchesTypeName(Class<?> clazz, String typeName) {
		return (typeName != null &&
				(typeName.equals(clazz.getName()) || typeName.equals(clazz.getSimpleName()) ||
						(clazz.isArray() && typeName.equals(getQualifiedNameForArray(clazz)))));
	}

	/**
	 * 获取没有限定包名的类名.
	 * 
	 * @param className 要获取短名称的className
	 * 
	 * @return 没有包名的类的类名
	 * @throws IllegalArgumentException 如果className为空
	 */
	public static String getShortName(String className) {
		Assert.hasLength(className, "Class name must not be empty");
		int lastDotIndex = className.lastIndexOf(PACKAGE_SEPARATOR);
		int nameEndIndex = className.indexOf(CGLIB_CLASS_SEPARATOR);
		if (nameEndIndex == -1) {
			nameEndIndex = className.length();
		}
		String shortName = className.substring(lastDotIndex + 1, nameEndIndex);
		shortName = shortName.replace(INNER_CLASS_SEPARATOR, PACKAGE_SEPARATOR);
		return shortName;
	}

	/**
	 * 获取没有限定包名的类名.
	 * 
	 * @param clazz 要获取短名称的类
	 * 
	 * @return 没有包名的类的类名
	 */
	public static String getShortName(Class<?> clazz) {
		return getShortName(getQualifiedName(clazz));
	}

	/**
	 * 以非大写JavaBeans属性格式返回Java类的短字符串名称.
	 * 在内部类的情况下剥离外部类名.
	 * 
	 * @param clazz 类
	 * 
	 * @return 以标准JavaBeans属性格式呈现的短名称
	 */
	public static String getShortNameAsProperty(Class<?> clazz) {
		String shortName = getShortName(clazz);
		int dotIndex = shortName.lastIndexOf(PACKAGE_SEPARATOR);
		shortName = (dotIndex != -1 ? shortName.substring(dotIndex + 1) : shortName);
		return Introspector.decapitalize(shortName);
	}

	/**
	 * 确定相对于包的类文件的名称: e.g. "String.class"
	 * 
	 * @param clazz 类
	 * 
	 * @return ".class"文件的文件名
	 */
	public static String getClassFileName(Class<?> clazz) {
		Assert.notNull(clazz, "Class must not be null");
		String className = clazz.getName();
		int lastDotIndex = className.lastIndexOf(PACKAGE_SEPARATOR);
		return className.substring(lastDotIndex + 1) + CLASS_FILE_SUFFIX;
	}

	/**
	 * 确定给定类的包的名称, e.g. {@code java.lang.String}类的包名为"java.lang".
	 * 
	 * @param clazz 类
	 * 
	 * @return 包名称; 如果在默认包中定义了类, 则为空String
	 */
	public static String getPackageName(Class<?> clazz) {
		Assert.notNull(clazz, "Class must not be null");
		return getPackageName(clazz.getName());
	}

	/**
	 * 确定给定的完全限定类名的包的名称, e.g. "java.lang" for the {@code java.lang.String} class name.
	 * 
	 * @param fqClassName 完全限定的类名
	 * 
	 * @return 包名称; 如果在默认包中定义了类, 则为空String
	 */
	public static String getPackageName(String fqClassName) {
		Assert.notNull(fqClassName, "Class name must not be null");
		int lastDotIndex = fqClassName.lastIndexOf(PACKAGE_SEPARATOR);
		return (lastDotIndex != -1 ? fqClassName.substring(0, lastDotIndex) : "");
	}

	/**
	 * 返回给定类的限定名称: 通常只是类名, 但组件类型类名 + "[]" 表示数组.
	 * 
	 * @param clazz 类
	 * 
	 * @return 该类的限定名称
	 */
	public static String getQualifiedName(Class<?> clazz) {
		Assert.notNull(clazz, "Class must not be null");
		if (clazz.isArray()) {
			return getQualifiedNameForArray(clazz);
		}
		else {
			return clazz.getName();
		}
	}

	/**
	 * 为数组构建一个很好的限定名: 组件类型类名 + "[]".
	 * 
	 * @param clazz 数组类
	 * 
	 * @return 数组类的限定名称
	 */
	private static String getQualifiedNameForArray(Class<?> clazz) {
		StringBuilder result = new StringBuilder();
		while (clazz.isArray()) {
			clazz = clazz.getComponentType();
			result.append(ARRAY_SUFFIX);
		}
		result.insert(0, clazz.getName());
		return result.toString();
	}

	/**
	 * 返回给定方法的限定名称, 由完全​​限定的接口/类名 + "." + 组成.
	 * 
	 * @param method 方法
	 * 
	 * @return 方法的限定名称
	 */
	public static String getQualifiedMethodName(Method method) {
		return getQualifiedMethodName(method, null);
	}

	/**
	 * 返回给定方法的限定名称, 由完全​​限定的接口/类名 + "." + 组成.
	 * 
	 * @param method 方法
	 * @param clazz 调用该方法的clazz
	 * (可能是{@code null}以指示方法的声明类)
	 * 
	 * @return 方法的限定名称
	 */
	public static String getQualifiedMethodName(Method method, Class<?> clazz) {
		Assert.notNull(method, "Method must not be null");
		return (clazz != null ? clazz : method.getDeclaringClass()).getName() + '.' + method.getName();
	}

	/**
	 * 确定给定的类是否具有具有给定签名的公共构造函数.
	 * <p>基本上将 {@code NoSuchMethodException}翻译为"false".
	 * 
	 * @param clazz 要分析的clazz
	 * @param paramTypes 方法的参数类型
	 * 
	 * @return 该类是否具有相应的构造函数
	 */
	public static boolean hasConstructor(Class<?> clazz, Class<?>... paramTypes) {
		return (getConstructorIfAvailable(clazz, paramTypes) != null);
	}

	/**
	 * 确定给定的类是否具有给定签名的公共构造函数, 并在可用时返回它 (否则返回 {@code null}).
	 * <p>基本上将{@code NoSuchMethodException}翻译为{@code null}.
	 * 
	 * @param clazz 要分析的clazz
	 * @param paramTypes 方法的参数类型
	 * 
	 * @return 构造函数, 或{@code null}
	 */
	public static <T> Constructor<T> getConstructorIfAvailable(Class<T> clazz, Class<?>... paramTypes) {
		Assert.notNull(clazz, "Class must not be null");
		try {
			return clazz.getConstructor(paramTypes);
		}
		catch (NoSuchMethodException ex) {
			return null;
		}
	}

	/**
	 * 确定给定的类是否具有给定签名的公共方法.
	 * <p>基本上将{@code NoSuchMethodException}翻译为"false".
	 * 
	 * @param clazz 要分析的clazz
	 * @param methodName 方法名
	 * @param paramTypes 方法的参数类型
	 * 
	 * @return 该类是否有相应的方法
	 */
	public static boolean hasMethod(Class<?> clazz, String methodName, Class<?>... paramTypes) {
		return (getMethodIfAvailable(clazz, methodName, paramTypes) != null);
	}

	/**
	 * 确定给定的类是否具有给定签名的公共方法, 并在可用时返回它 (否则抛出{@code IllegalStateException}).
	 * <p>如果指定了任何签名, 则仅在存在唯一候选者时才返回该方法, i.e. 具有指定名称的单个公共方法.
	 * <p>基本上将{@code NoSuchMethodException}翻译为{@code IllegalStateException}.
	 * 
	 * @param clazz 要分析的clazz
	 * @param methodName 方法名
	 * @param paramTypes 方法的参数类型 (可以{@code null}表示任何签名)
	 * 
	 * @return 方法 (never {@code null})
	 * @throws IllegalStateException 如果没有找到该方法
	 */
	public static Method getMethod(Class<?> clazz, String methodName, Class<?>... paramTypes) {
		Assert.notNull(clazz, "Class must not be null");
		Assert.notNull(methodName, "Method name must not be null");
		if (paramTypes != null) {
			try {
				return clazz.getMethod(methodName, paramTypes);
			}
			catch (NoSuchMethodException ex) {
				throw new IllegalStateException("Expected method not found: " + ex);
			}
		}
		else {
			Set<Method> candidates = new HashSet<Method>(1);
			Method[] methods = clazz.getMethods();
			for (Method method : methods) {
				if (methodName.equals(method.getName())) {
					candidates.add(method);
				}
			}
			if (candidates.size() == 1) {
				return candidates.iterator().next();
			}
			else if (candidates.isEmpty()) {
				throw new IllegalStateException("Expected method not found: " + clazz.getName() + '.' + methodName);
			}
			else {
				throw new IllegalStateException("No unique method found: " + clazz.getName() + '.' + methodName);
			}
		}
	}

	/**
	 * 确定给定的类是否具有给定签名的公共方法, 并在可用时返回它 (否则返回 {@code null}).
	 * <p>如果指定了任何签名, 则仅在存在唯一候选者时才返回该方法, i.e. 具有指定名称的单个公共方法.
	 * <p>基本上将{@code NoSuchMethodException}翻译为{@code null}.
	 * 
	 * @param clazz 要分析的clazz
	 * @param methodName 方法名
	 * @param paramTypes 方法的参数类型 (可以{@code null}表示任何签名)
	 * 
	 * @return 方法, 或{@code null}
	 */
	public static Method getMethodIfAvailable(Class<?> clazz, String methodName, Class<?>... paramTypes) {
		Assert.notNull(clazz, "Class must not be null");
		Assert.notNull(methodName, "Method name must not be null");
		if (paramTypes != null) {
			try {
				return clazz.getMethod(methodName, paramTypes);
			}
			catch (NoSuchMethodException ex) {
				return null;
			}
		}
		else {
			Set<Method> candidates = new HashSet<Method>(1);
			Method[] methods = clazz.getMethods();
			for (Method method : methods) {
				if (methodName.equals(method.getName())) {
					candidates.add(method);
				}
			}
			if (candidates.size() == 1) {
				return candidates.iterator().next();
			}
			return null;
		}
	}

	/**
	 * 对于给定的类和/或其超类, 返回具有给定名称(具有任何参数类型)的方法的数量.
	 * 包括非public 方法.
	 * 
	 * @param clazz	要检查的clazz
	 * @param methodName 方法名
	 * 
	 * @return 具有给定名称的方法的数量
	 */
	public static int getMethodCountForName(Class<?> clazz, String methodName) {
		Assert.notNull(clazz, "Class must not be null");
		Assert.notNull(methodName, "Method name must not be null");
		int count = 0;
		Method[] declaredMethods = clazz.getDeclaredMethods();
		for (Method method : declaredMethods) {
			if (methodName.equals(method.getName())) {
				count++;
			}
		}
		Class<?>[] ifcs = clazz.getInterfaces();
		for (Class<?> ifc : ifcs) {
			count += getMethodCountForName(ifc, methodName);
		}
		if (clazz.getSuperclass() != null) {
			count += getMethodCountForName(clazz.getSuperclass(), methodName);
		}
		return count;
	}

	/**
	 * 给定的类或其超类之一是否至少具有一个或多个具有所提供名称的方法 (具有任何参数类型)?
	 * 包括非public 方法.
	 * 
	 * @param clazz	要检查的clazz
	 * @param methodName 方法名
	 * 
	 * @return 是否至少有一个具有给定名称的方法
	 */
	public static boolean hasAtLeastOneMethodWithName(Class<?> clazz, String methodName) {
		Assert.notNull(clazz, "Class must not be null");
		Assert.notNull(methodName, "Method name must not be null");
		Method[] declaredMethods = clazz.getDeclaredMethods();
		for (Method method : declaredMethods) {
			if (method.getName().equals(methodName)) {
				return true;
			}
		}
		Class<?>[] ifcs = clazz.getInterfaces();
		for (Class<?> ifc : ifcs) {
			if (hasAtLeastOneMethodWithName(ifc, methodName)) {
				return true;
			}
		}
		return (clazz.getSuperclass() != null && hasAtLeastOneMethodWithName(clazz.getSuperclass(), methodName));
	}

	/**
	 * 给定一个可能来自接口的方法, 以及当前反射调用中使用的目标类, 查找相应的目标方法.
	 * E.g. 方法可能是{@code IFoo.bar()}, 目标类可能是{@code DefaultFoo}.
	 * 在这种情况下, 方法可能是{@code DefaultFoo.bar()}. 这样可以找到该方法的属性.
	 * <p><b>NOTE:</b>与{@link org.springframework.aop.support.AopUtils#getMostSpecificMethod}相反,
	 * 此方法<i>不</i>自动解析Java 5桥接方法.
	 * 如果需要桥接方法解析, 请调用
	 * {@link org.springframework.core.BridgeMethodResolver#findBridgedMethod} (e.g. 从原始方法定义中获取元数据).
	 * <p><b>NOTE:</b>从Spring 3.1.1开始, 如果Java安全设置不允许反射访问 (e.g. 调用{@code Class#getDeclaredMethods}等,
	 * 则此实现将回退到返回最初提供的方法.
	 * 
	 * @param method 要调用的方法, 可能来自接口
	 * @param targetClass 当前调用的目标类.
	 * 可能是{@code null}, 甚至可能没有实现该方法.
	 * 
	 * @return 特定的目标方法; 或原始方法, 如果{@code targetClass}没有实现它或{@code null}
	 */
	public static Method getMostSpecificMethod(Method method, Class<?> targetClass) {
		if (method != null && isOverridable(method, targetClass) &&
				targetClass != null && targetClass != method.getDeclaringClass()) {
			try {
				if (Modifier.isPublic(method.getModifiers())) {
					try {
						return targetClass.getMethod(method.getName(), method.getParameterTypes());
					}
					catch (NoSuchMethodException ex) {
						return method;
					}
				}
				else {
					Method specificMethod =
							ReflectionUtils.findMethod(targetClass, method.getName(), method.getParameterTypes());
					return (specificMethod != null ? specificMethod : method);
				}
			}
			catch (SecurityException ex) {
				// 安全设置禁止反射访问; 回到下面的'method'.
			}
		}
		return method;
	}

	/**
	 * 确定给定方法是由用户声明, 还是至少指向用户声明的方法.
	 * <p>检查{@link Method#isSynthetic()} (对于实现方法)以及{@code GroovyObject}接口
	 * (对于接口方法; 在实现类上, {@code GroovyObject}方法的实现将被标记为合成).
	 * 请注意, 尽管是合成的, 但桥接方法 ({@link Method#isBridge()})被视为用户级方法,
	 * 因为它们最终指向用户声明的泛型方法.
	 * 
	 * @param method 要检查的方法
	 * 
	 * @return {@code true}如果该方法可以被视为用户声明的; 否则{@code false}
	 */
	public static boolean isUserLevelMethod(Method method) {
		Assert.notNull(method, "Method must not be null");
		return (method.isBridge() || (!method.isSynthetic() && !isGroovyObjectMethod(method)));
	}

	private static boolean isGroovyObjectMethod(Method method) {
		return method.getDeclaringClass().getName().equals("groovy.lang.GroovyObject");
	}

	/**
	 * 确定给定方法在给定目标类中是否可覆盖.
	 * 
	 * @param method 要检查的方法
	 * @param targetClass 要检查的目标类
	 */
	private static boolean isOverridable(Method method, Class<?> targetClass) {
		if (Modifier.isPrivate(method.getModifiers())) {
			return false;
		}
		if (Modifier.isPublic(method.getModifiers()) || Modifier.isProtected(method.getModifiers())) {
			return true;
		}
		return getPackageName(method.getDeclaringClass()).equals(getPackageName(targetClass));
	}

	/**
	 * 返回类的公共静态方法.
	 * 
	 * @param clazz 定义方法的类
	 * @param methodName 静态方法名称
	 * @param args 方法的参数类型
	 * 
	 * @return 静态方法, 或{@code null}如果没有找到静态方法
	 * @throws IllegalArgumentException 如果方法名称为空或clazz为null
	 */
	public static Method getStaticMethod(Class<?> clazz, String methodName, Class<?>... args) {
		Assert.notNull(clazz, "Class must not be null");
		Assert.notNull(methodName, "Method name must not be null");
		try {
			Method method = clazz.getMethod(methodName, args);
			return Modifier.isStatic(method.getModifiers()) ? method : null;
		}
		catch (NoSuchMethodException ex) {
			return null;
		}
	}
}
