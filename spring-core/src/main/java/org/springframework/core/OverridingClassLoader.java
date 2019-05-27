package org.springframework.core;

import java.io.IOException;
import java.io.InputStream;

import org.springframework.lang.UsesJava7;
import org.springframework.util.FileCopyUtils;

/**
 * {@code ClassLoader}, <i>不</i>总是像普通类加载器一样委托给父加载器.
 * 例如, 这可以在重写的ClassLoader中强制执行检测, 或者"一次性"类加载行为,
 * 其中选定的应用程序类临时加载到覆盖的{@code ClassLoader}中以进行内省,
 * 在最终在给定父级{@code ClassLoader}中加载类的已检测版本之前.
 */
@UsesJava7
public class OverridingClassLoader extends DecoratingClassLoader {

	/** 默认情况下排除的包 */
	public static final String[] DEFAULT_EXCLUDED_PACKAGES = new String[]
			{"java.", "javax.", "sun.", "oracle.", "javassist.", "org.aspectj.", "net.sf.cglib."};

	private static final String CLASS_FILE_SUFFIX = ".class";

	static {
		if (parallelCapableClassLoaderAvailable) {
			ClassLoader.registerAsParallelCapable();
		}
	}


	private final ClassLoader overrideDelegate;


	/**
	 * @param parent 用于构建重载ClassLoader的ClassLoader
	 */
	public OverridingClassLoader(ClassLoader parent) {
		this(parent, null);
	}

	/**
	 * @param parent 用于构建重载ClassLoader的ClassLoader
	 * @param overrideDelegate 要委托覆盖的ClassLoader
	 */
	public OverridingClassLoader(ClassLoader parent, ClassLoader overrideDelegate) {
		super(parent);
		this.overrideDelegate = overrideDelegate;
		for (String packageName : DEFAULT_EXCLUDED_PACKAGES) {
			excludePackage(packageName);
		}
	}


	@Override
	public Class<?> loadClass(String name) throws ClassNotFoundException {
		if (this.overrideDelegate != null && isEligibleForOverriding(name)) {
			return this.overrideDelegate.loadClass(name);
		}
		return super.loadClass(name);
	}

	@Override
	protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
		if (isEligibleForOverriding(name)) {
			Class<?> result = loadClassForOverriding(name);
			if (result != null) {
				if (resolve) {
					resolveClass(result);
				}
				return result;
			}
		}
		return super.loadClass(name, resolve);
	}

	/**
	 * 确定指定的类是否有资格通过此类加载器覆盖.
	 * 
	 * @param className 要检查的类名
	 * 
	 * @return 指定的类是否符合条件
	 */
	protected boolean isEligibleForOverriding(String className) {
		return !isExcluded(className);
	}

	/**
	 * 在此ClassLoader中加载指定的类用于覆盖目的.
	 * <p>默认实现委托给{@link #findLoadedClass}, {@link #loadBytesForClass}, {@link #defineClass}.
	 * 
	 * @param name 类名
	 * 
	 * @return Class对象, 或{@code null}如果没有为该名称定义类
	 * @throws ClassNotFoundException 如果无法加载给定名称的类
	 */
	protected Class<?> loadClassForOverriding(String name) throws ClassNotFoundException {
		Class<?> result = findLoadedClass(name);
		if (result == null) {
			byte[] bytes = loadBytesForClass(name);
			if (bytes != null) {
				result = defineClass(name, bytes, 0, bytes.length);
			}
		}
		return result;
	}

	/**
	 * 加载给定类的定义字节, 通过{@link #defineClass}调用转换为Class对象.
	 * <p>默认实现委托给{@link #openStreamForClass} 和 {@link #transformIfNecessary}.
	 * 
	 * @param name 类名
	 * 
	 * @return 字节内容 (已经应用了变换器), 或{@code null} 如果没有为该名称定义类
	 * @throws ClassNotFoundException 如果无法加载给定名称的类
	 */
	protected byte[] loadBytesForClass(String name) throws ClassNotFoundException {
		InputStream is = openStreamForClass(name);
		if (is == null) {
			return null;
		}
		try {
			// 加载原始字节.
			byte[] bytes = FileCopyUtils.copyToByteArray(is);
			// 必要时进行转换, 并使用可能已转换的字节.
			return transformIfNecessary(name, bytes);
		}
		catch (IOException ex) {
			throw new ClassNotFoundException("Cannot load resource for class [" + name + "]", ex);
		}
	}

	/**
	 * 打开指定类的InputStream.
	 * <p>默认实现通过父级ClassLoader的{@code getResourceAsStream}方法加载标准类文件.
	 * 
	 * @param name 类名
	 * 
	 * @return 包含指定类的字节代码的InputStream
	 */
	protected InputStream openStreamForClass(String name) {
		String internalName = name.replace('.', '/') + CLASS_FILE_SUFFIX;
		return getParent().getResourceAsStream(internalName);
	}


	/**
	 * 由子类实现的转换钩子.
	 * <p>默认实现只是按原样返回给定的字节.
	 * 
	 * @param name 要转换的类的完全限定名称
	 * @param bytes 类的原始字节
	 * 
	 * @return 已转换的字节 (never {@code null}; 如果转换没有产生任何变化, 则与输入字节相同)
	 */
	protected byte[] transformIfNecessary(String name, byte[] bytes) {
		return bytes;
	}
}
