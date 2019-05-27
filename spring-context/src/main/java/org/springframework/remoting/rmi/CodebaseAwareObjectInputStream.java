package org.springframework.remoting.rmi;

import java.io.IOException;
import java.io.InputStream;
import java.rmi.server.RMIClassLoader;

import org.springframework.core.ConfigurableObjectInputStream;

/**
 * 特殊的ObjectInputStream子类, 它回退到指定的代码库, 以便加载本地找不到的类.
 * 与动态类下载的标准RMI约定相比, 客户端在此处确定代码库URL, 而不是服务器上的"java.rmi.server.codebase"系统属性.
 *
 * <p>使用JDK的RMIClassLoader从指定的代码库加载类.
 * 代码库可以包含多个URL, 以空格分隔.
 * 请注意, RMIClassLoader需要设置SecurityManager, 就像使用标准RMI的动态类下载一样!
 * (有关详细信息, 请参阅RMI文档.)
 *
 * <p>尽管存在于RMI包中, 但此类不用于RmiClientInterceptor, RmiClientInterceptor使用标准的RMI基础结构,
 * 因此只能通过"java.rmi.server.codebase"依赖RMI的标准动态类下载.
 * CodebaseAwareObjectInputStream 由 HttpInvokerClientInterceptor使用 (参见"codebaseUrl"属性).
 *
 * <p>Thanks to Lionel Mestre for suggesting the option and providing a prototype!
 */
public class CodebaseAwareObjectInputStream extends ConfigurableObjectInputStream {

	private final String codebaseUrl;


	/**
	 * @param in 要读取的InputStream
	 * @param codebaseUrl 加载类的代码库URL, 如果本地未找到 (可以包含多个URL, 以空格分隔)
	 */
	public CodebaseAwareObjectInputStream(InputStream in, String codebaseUrl) throws IOException {
		this(in, null, codebaseUrl);
	}

	/**
	 * @param in 要读取的InputStream
	 * @param classLoader 用于加载本地类的ClassLoader (可能是{@code null}以指示RMI的默认ClassLoader)
	 * @param codebaseUrl 加载类的代码库URL, 如果本地未找到 (可以包含多个URL, 以空格分隔)
	 */
	public CodebaseAwareObjectInputStream(
			InputStream in, ClassLoader classLoader, String codebaseUrl) throws IOException {

		super(in, classLoader);
		this.codebaseUrl = codebaseUrl;
	}

	/**
	 * @param in 要读取的InputStream
	 * @param classLoader 用于加载本地类的ClassLoader (可能是{@code null}以指示RMI的默认ClassLoader)
	 * @param acceptProxyClasses 是否接受代理类的反序列化 (可以停用作为安全措施)
	 */
	public CodebaseAwareObjectInputStream(
			InputStream in, ClassLoader classLoader, boolean acceptProxyClasses) throws IOException {

		super(in, classLoader, acceptProxyClasses);
		this.codebaseUrl = null;
	}


	@Override
	protected Class<?> resolveFallbackIfPossible(String className, ClassNotFoundException ex)
			throws IOException, ClassNotFoundException {

		// 如果设置了codebaseUrl, 请尝试使用RMIClassLoader加载该类.
		// 否则, 传播ClassNotFoundException.
		if (this.codebaseUrl == null) {
			throw ex;
		}
		return RMIClassLoader.loadClass(this.codebaseUrl, className);
	}

	@Override
	protected ClassLoader getFallbackClassLoader() throws IOException {
		return RMIClassLoader.getClassLoader(this.codebaseUrl);
	}

}
