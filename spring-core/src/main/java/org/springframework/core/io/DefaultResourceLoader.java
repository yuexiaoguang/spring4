package org.springframework.core.io;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * {@link ResourceLoader}接口的默认实现.
 * 由{@link ResourceEditor}使用, 并作为{@link org.springframework.context.support.AbstractApplicationContext}的基类.
 * 也可以单独使用.
 *
 * <p>如果位置值是URL, 则返回{@link UrlResource}; 如果是非URL路径或"classpath:"伪URL, 则返回{@link ClassPathResource}.
 */
public class DefaultResourceLoader implements ResourceLoader {

	private ClassLoader classLoader;

	private final Set<ProtocolResolver> protocolResolvers = new LinkedHashSet<ProtocolResolver>(4);


	/**
	 * <p>在此ResourceLoader初始化时, 将使用线程上下文类加载器进行ClassLoader访问.
	 */
	public DefaultResourceLoader() {
		this.classLoader = ClassUtils.getDefaultClassLoader();
	}

	/**
	 * @param classLoader 用于加载类路径资源的ClassLoader, 或{@code null} 用于在实际资源访问时使用线程上下文类加载器
	 */
	public DefaultResourceLoader(ClassLoader classLoader) {
		this.classLoader = classLoader;
	}


	/**
	 * 指定用于加载类路径资源的ClassLoader, 或{@code null} 用于在实际资源访问时使用线程上下文类加载器.
	 * <p>默认情况下, 在此ResourceLoader初始化时, 使用线程上下文类加载器将发生ClassLoader访问.
	 */
	public void setClassLoader(ClassLoader classLoader) {
		this.classLoader = classLoader;
	}

	/**
	 * 返回用于加载类路径资源的ClassLoader.
	 * <p>将传递给ClassPathResource的构造函数, 用于由此资源加载器创建的所有ClassPathResource对象.
	 */
	@Override
	public ClassLoader getClassLoader() {
		return (this.classLoader != null ? this.classLoader : ClassUtils.getDefaultClassLoader());
	}

	/**
	 * 使用此资源加载器注册给定的解析器, 允许处理其他协议.
	 * <p>任何此类解析器都将在此加载器的标准解析规则之前调用. 因此它也可以覆盖任何默认规则.
	 */
	public void addProtocolResolver(ProtocolResolver resolver) {
		Assert.notNull(resolver, "ProtocolResolver must not be null");
		this.protocolResolvers.add(resolver);
	}

	/**
	 * 返回当前注册的协议解析器的集合, 允许内省和修改.
	 */
	public Collection<ProtocolResolver> getProtocolResolvers() {
		return this.protocolResolvers;
	}


	@Override
	public Resource getResource(String location) {
		Assert.notNull(location, "Location must not be null");

		for (ProtocolResolver protocolResolver : this.protocolResolvers) {
			Resource resource = protocolResolver.resolve(location, this);
			if (resource != null) {
				return resource;
			}
		}

		if (location.startsWith("/")) {
			return getResourceByPath(location);
		}
		else if (location.startsWith(CLASSPATH_URL_PREFIX)) {
			return new ClassPathResource(location.substring(CLASSPATH_URL_PREFIX.length()), getClassLoader());
		}
		else {
			try {
				// 尝试将位置解析为URL...
				URL url = new URL(location);
				return new UrlResource(url);
			}
			catch (MalformedURLException ex) {
				// No URL -> 解析为资源路径.
				return getResourceByPath(location);
			}
		}
	}

	/**
	 * 返回给定路径上资源的Resource句柄.
	 * <p>默认实现支持类路径位置.
	 * 这应该适用于独立实现, 但可以被覆盖, e.g. 用于针对Servlet容器的实现.
	 * 
	 * @param path 资源的路径
	 * 
	 * @return 相应的Resource句柄
	 */
	protected Resource getResourceByPath(String path) {
		return new ClassPathContextResource(path, getClassLoader());
	}


	/**
	 * 通过实现ContextResource接口显式表达上下文相关路径的ClassPathResource.
	 */
	protected static class ClassPathContextResource extends ClassPathResource implements ContextResource {

		public ClassPathContextResource(String path, ClassLoader classLoader) {
			super(path, classLoader);
		}

		@Override
		public String getPathWithinContext() {
			return getPath();
		}

		@Override
		public Resource createRelative(String relativePath) {
			String pathToUse = StringUtils.applyRelativePath(getPath(), relativePath);
			return new ClassPathContextResource(pathToUse, getClassLoader());
		}
	}

}
