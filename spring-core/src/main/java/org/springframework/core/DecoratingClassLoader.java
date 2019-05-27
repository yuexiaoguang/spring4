package org.springframework.core;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.lang.UsesJava7;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * 装饰ClassLoader的基类, 例如{@link OverridingClassLoader}
 * 和{@link org.springframework.instrument.classloading.ShadowingClassLoader},
 * 提供排除的包和类的常见处理.
 */
@UsesJava7
public abstract class DecoratingClassLoader extends ClassLoader {

	/**
	 * Java 7+ {@code ClassLoader.registerAsParallelCapable()}可用?
	 */
	protected static final boolean parallelCapableClassLoaderAvailable =
			ClassUtils.hasMethod(ClassLoader.class, "registerAsParallelCapable");

	static {
		if (parallelCapableClassLoaderAvailable) {
			ClassLoader.registerAsParallelCapable();
		}
	}


	private final Set<String> excludedPackages =
			Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>(8));

	private final Set<String> excludedClasses =
			Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>(8));


	/**
	 * 没有父级ClassLoader.
	 */
	public DecoratingClassLoader() {
	}

	/**
	 * 使用给定的父级ClassLoader以进行委派.
	 */
	public DecoratingClassLoader(ClassLoader parent) {
		super(parent);
	}


	/**
	 * 添加要从装饰中排除的包名称 (e.g. 覆盖).
	 * <p>完全限定名称以此处注册的名称开头的任何类, 将由父级ClassLoader以往常的方式处理.
	 * 
	 * @param packageName 要排除的包名
	 */
	public void excludePackage(String packageName) {
		Assert.notNull(packageName, "Package name must not be null");
		this.excludedPackages.add(packageName);
	}

	/**
	 * 添加要从装饰中排除的类名 (e.g. 覆盖).
	 * <p>此处注册的任何类名将由父级ClassLoader以往常的方式处理.
	 * 
	 * @param className 要排除的类名
	 */
	public void excludeClass(String className) {
		Assert.notNull(className, "Class name must not be null");
		this.excludedClasses.add(className);
	}

	/**
	 * 确定此类加载器是否从装饰中排除指定的类.
	 * <p>默认实现检查排除的包和类.
	 * 
	 * @param className 要检查的类名
	 * 
	 * @return 指定的类是否符合条件
	 */
	protected boolean isExcluded(String className) {
		if (this.excludedClasses.contains(className)) {
			return true;
		}
		for (String packageName : this.excludedPackages) {
			if (className.startsWith(packageName)) {
				return true;
			}
		}
		return false;
	}
}
