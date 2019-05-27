package org.springframework.instrument.classloading;

import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.springframework.core.DecoratingClassLoader;
import org.springframework.util.Assert;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StringUtils;

/**
 * ClassLoader装饰器, 用于遮蔽封闭的ClassLoader, 将已注册的变换器应用于所有受影响的类.
 */
public class ShadowingClassLoader extends DecoratingClassLoader {

	/** 默认情况下排除的包 */
	public static final String[] DEFAULT_EXCLUDED_PACKAGES =
			new String[] {"java.", "javax.", "sun.", "oracle.", "com.sun.", "com.ibm.", "COM.ibm.",
					"org.w3c.", "org.xml.", "org.dom4j.", "org.eclipse", "org.aspectj.", "net.sf.cglib",
					"org.springframework.cglib", "org.apache.xerces.", "org.apache.commons.logging."};


	private final ClassLoader enclosingClassLoader;

	private final List<ClassFileTransformer> classFileTransformers = new LinkedList<ClassFileTransformer>();

	private final Map<String, Class<?>> classCache = new HashMap<String, Class<?>>();


	/**
	 * 装饰给定的ClassLoader, 应用{@link #DEFAULT_EXCLUDED_PACKAGES}.
	 * 
	 * @param enclosingClassLoader 要装饰的ClassLoader
	 */
	public ShadowingClassLoader(ClassLoader enclosingClassLoader) {
		this(enclosingClassLoader, true);
	}

	/**
	 * 装饰给定的ClassLoader.
	 * 
	 * @param enclosingClassLoader 要装饰的ClassLoader
	 * @param defaultExcludes 是否应用{@link #DEFAULT_EXCLUDED_PACKAGES}
	 */
	public ShadowingClassLoader(ClassLoader enclosingClassLoader, boolean defaultExcludes) {
		Assert.notNull(enclosingClassLoader, "Enclosing ClassLoader must not be null");
		this.enclosingClassLoader = enclosingClassLoader;
		if (defaultExcludes) {
			for (String excludedPackage : DEFAULT_EXCLUDED_PACKAGES) {
				excludePackage(excludedPackage);
			}
		}
	}


	/**
	 * 将给定的ClassFileTransformer添加到此ClassLoader将应用的变换器列表中.
	 * 
	 * @param transformer the ClassFileTransformer
	 */
	public void addTransformer(ClassFileTransformer transformer) {
		Assert.notNull(transformer, "Transformer must not be null");
		this.classFileTransformers.add(transformer);
	}

	/**
	 * 将所有ClassFileTransformers从给定的ClassLoader复制到此ClassLoader将应用的变换器列表.
	 * 
	 * @param other 要从中复制的ClassLoader
	 */
	public void copyTransformers(ShadowingClassLoader other) {
		Assert.notNull(other, "Other ClassLoader must not be null");
		this.classFileTransformers.addAll(other.classFileTransformers);
	}


	@Override
	public Class<?> loadClass(String name) throws ClassNotFoundException {
		if (shouldShadow(name)) {
			Class<?> cls = this.classCache.get(name);
			if (cls != null) {
				return cls;
			}
			return doLoadClass(name);
		}
		else {
			return this.enclosingClassLoader.loadClass(name);
		}
	}

	/**
	 * 确定是否应从遮蔽中排除给定的类.
	 * 
	 * @param className 类名
	 * 
	 * @return 是否应该遮蔽指定的类
	 */
	private boolean shouldShadow(String className) {
		return (!className.equals(getClass().getName()) && !className.endsWith("ShadowingClassLoader") &&
				isEligibleForShadowing(className));
	}

	/**
	 * 确定指定的类是否符合此类加载器的遮蔽条件.
	 * 
	 * @param className 要检查的类名
	 * 
	 * @return 指定的类是否符合条件
	 */
	protected boolean isEligibleForShadowing(String className) {
		return !isExcluded(className);
	}


	private Class<?> doLoadClass(String name) throws ClassNotFoundException {
		String internalName = StringUtils.replace(name, ".", "/") + ".class";
		InputStream is = this.enclosingClassLoader.getResourceAsStream(internalName);
		if (is == null) {
			throw new ClassNotFoundException(name);
		}
		try {
			byte[] bytes = FileCopyUtils.copyToByteArray(is);
			bytes = applyTransformers(name, bytes);
			Class<?> cls = defineClass(name, bytes, 0, bytes.length);
			// 如果尚未定义, 还要检查定义包.
			if (cls.getPackage() == null) {
				int packageSeparator = name.lastIndexOf('.');
				if (packageSeparator != -1) {
					String packageName = name.substring(0, packageSeparator);
					definePackage(packageName, null, null, null, null, null, null, null);
				}
			}
			this.classCache.put(name, cls);
			return cls;
		}
		catch (IOException ex) {
			throw new ClassNotFoundException("Cannot load resource for class [" + name + "]", ex);
		}
	}

	private byte[] applyTransformers(String name, byte[] bytes) {
		String internalName = StringUtils.replace(name, ".", "/");
		try {
			for (ClassFileTransformer transformer : this.classFileTransformers) {
				byte[] transformed = transformer.transform(this, internalName, null, null, bytes);
				bytes = (transformed != null ? transformed : bytes);
			}
			return bytes;
		}
		catch (IllegalClassFormatException ex) {
			throw new IllegalStateException(ex);
		}
	}


	@Override
	public URL getResource(String name) {
		return this.enclosingClassLoader.getResource(name);
	}

	@Override
	public InputStream getResourceAsStream(String name) {
		return this.enclosingClassLoader.getResourceAsStream(name);
	}

	@Override
	public Enumeration<URL> getResources(String name) throws IOException {
		return this.enclosingClassLoader.getResources(name);
	}

}
