package org.springframework.context.support;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.core.DecoratingClassLoader;
import org.springframework.core.OverridingClassLoader;
import org.springframework.core.SmartClassLoader;
import org.springframework.lang.UsesJava7;
import org.springframework.util.ReflectionUtils;

/**
 * 覆盖ClassLoader的特殊变体, 用于{@link AbstractApplicationContext}中的临时类型匹配.
 * 为每个{@code loadClass}调用重新定义缓存的字节数组中的类, 以便在父类ClassLoader中获取最近加载的类型.
 */
@UsesJava7
class ContextTypeMatchClassLoader extends DecoratingClassLoader implements SmartClassLoader {

	static {
		if (parallelCapableClassLoaderAvailable) {
			ClassLoader.registerAsParallelCapable();
		}
	}


	private static Method findLoadedClassMethod;

	static {
		try {
			findLoadedClassMethod = ClassLoader.class.getDeclaredMethod("findLoadedClass", String.class);
		}
		catch (NoSuchMethodException ex) {
			throw new IllegalStateException("Invalid [java.lang.ClassLoader] class: no 'findLoadedClass' method defined!");
		}
	}


	/** 每个类名的字节数组缓存 */
	private final Map<String, byte[]> bytesCache = new ConcurrentHashMap<String, byte[]>(256);


	public ContextTypeMatchClassLoader(ClassLoader parent) {
		super(parent);
	}

	@Override
	public Class<?> loadClass(String name) throws ClassNotFoundException {
		return new ContextOverridingClassLoader(getParent()).loadClass(name);
	}

	@Override
	public boolean isClassReloadable(Class<?> clazz) {
		return (clazz.getClassLoader() instanceof ContextOverridingClassLoader);
	}


	/**
	 * 要为每个加载的类创建ClassLoader.
	 * 缓存类文件内容, 但重新定义每个调用的类.
	 */
	private class ContextOverridingClassLoader extends OverridingClassLoader {

		public ContextOverridingClassLoader(ClassLoader parent) {
			super(parent);
		}

		@Override
		protected boolean isEligibleForOverriding(String className) {
			if (isExcluded(className) || ContextTypeMatchClassLoader.this.isExcluded(className)) {
				return false;
			}
			ReflectionUtils.makeAccessible(findLoadedClassMethod);
			ClassLoader parent = getParent();
			while (parent != null) {
				if (ReflectionUtils.invokeMethod(findLoadedClassMethod, parent, className) != null) {
					return false;
				}
				parent = parent.getParent();
			}
			return true;
		}

		@Override
		protected Class<?> loadClassForOverriding(String name) throws ClassNotFoundException {
			byte[] bytes = bytesCache.get(name);
			if (bytes == null) {
				bytes = loadBytesForClass(name);
				if (bytes != null) {
					bytesCache.put(name, bytes);
				}
				else {
					return null;
				}
			}
			return defineClass(name, bytes, 0, bytes.length);
		}
	}

}
