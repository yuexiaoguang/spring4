package org.springframework.core;

import java.io.IOException;
import java.io.InputStream;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;

import org.springframework.util.ClassUtils;

/**
 * 特殊的ObjectInputStream子类, 它根据特定的ClassLoader解析类名.
 * 作为{@link org.springframework.remoting.rmi.CodebaseAwareObjectInputStream}的基类.
 */
public class ConfigurableObjectInputStream extends ObjectInputStream {

	private final ClassLoader classLoader;

	private final boolean acceptProxyClasses;


	/**
	 * @param in 要读取的InputStream
	 * @param classLoader 用于加载本地类的ClassLoader
	 */
	public ConfigurableObjectInputStream(InputStream in, ClassLoader classLoader) throws IOException {
		this(in, classLoader, true);
	}

	/**
	 * @param in 要读取的InputStream
	 * @param classLoader 用于加载本地类的ClassLoader
	 * @param acceptProxyClasses 是否接受代理类的反序列化 (可能因为安全措施停用)
	 */
	public ConfigurableObjectInputStream(
			InputStream in, ClassLoader classLoader, boolean acceptProxyClasses) throws IOException {

		super(in);
		this.classLoader = classLoader;
		this.acceptProxyClasses = acceptProxyClasses;
	}


	@Override
	protected Class<?> resolveClass(ObjectStreamClass classDesc) throws IOException, ClassNotFoundException {
		try {
			if (this.classLoader != null) {
				// 使用指定的ClassLoader来解析本地类.
				return ClassUtils.forName(classDesc.getName(), this.classLoader);
			}
			else {
				// 使用默认的ClassLoader...
				return super.resolveClass(classDesc);
			}
		}
		catch (ClassNotFoundException ex) {
			return resolveFallbackIfPossible(classDesc.getName(), ex);
		}
	}

	@Override
	protected Class<?> resolveProxyClass(String[] interfaces) throws IOException, ClassNotFoundException {
		if (!this.acceptProxyClasses) {
			throw new NotSerializableException("Not allowed to accept serialized proxy classes");
		}
		if (this.classLoader != null) {
			// 使用指定的ClassLoader来解析本地代理类.
			Class<?>[] resolvedInterfaces = new Class<?>[interfaces.length];
			for (int i = 0; i < interfaces.length; i++) {
				try {
					resolvedInterfaces[i] = ClassUtils.forName(interfaces[i], this.classLoader);
				}
				catch (ClassNotFoundException ex) {
					resolvedInterfaces[i] = resolveFallbackIfPossible(interfaces[i], ex);
				}
			}
			try {
				return ClassUtils.createCompositeInterface(resolvedInterfaces, this.classLoader);
			}
			catch (IllegalArgumentException ex) {
				throw new ClassNotFoundException(null, ex);
			}
		}
		else {
			// 使用ObjectInputStream的默认ClassLoader...
			try {
				return super.resolveProxyClass(interfaces);
			}
			catch (ClassNotFoundException ex) {
				Class<?>[] resolvedInterfaces = new Class<?>[interfaces.length];
				for (int i = 0; i < interfaces.length; i++) {
					resolvedInterfaces[i] = resolveFallbackIfPossible(interfaces[i], ex);
				}
				return ClassUtils.createCompositeInterface(resolvedInterfaces, getFallbackClassLoader());
			}
		}
	}


	/**
	 * 针对回退类加载器解析给定的类名.
	 * <p>默认实现只是重新抛出原始异常, 因为没有可用的回退.
	 * 
	 * @param className 要解析的类名
	 * @param ex 尝试加载类时抛出的原始异常
	 * 
	 * @return 新解析的类 (never {@code null})
	 */
	protected Class<?> resolveFallbackIfPossible(String className, ClassNotFoundException ex)
			throws IOException, ClassNotFoundException{

		throw ex;
	}

	/**
	 * 当没有指定ClassLoader并且ObjectInputStream自己的默认类加载器失败时, 返回要使用的回退ClassLoader.
	 * <p>默认实现只返回{@code null}, 表示没有可用的特定回退.
	 */
	protected ClassLoader getFallbackClassLoader() throws IOException {
		return null;
	}

}
