package org.springframework.core.io.support;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;

import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.util.StringUtils;

/**
 * {@link PropertiesPropertySource}的子类, 它从给定的{@link org.springframework.core.io.Resource}
 * 或资源位置加载{@link Properties}对象, 例如{@code "classpath:/com/myco/foo.properties"}或{@code "file:/path/to/file.xml"}.
 *
 * <p>支持传统和基于XML的属性文件格式;
 * 但是, 为了使XML处理生效, 底层{@code Resource}的
 * {@link org.springframework.core.io.Resource#getFilename() getFilename()}方法
 * 必须返回非{@code null}值, 以{@code ".xml"}结尾.
 */
public class ResourcePropertySource extends PropertiesPropertySource {

	/** 原始资源名称, 如果与给定名称不同 */
	private final String resourceName;


	/**
	 * 根据从给定编码的资源加载的属性, 创建具有给定名称的PropertySource.
	 */
	public ResourcePropertySource(String name, EncodedResource resource) throws IOException {
		super(name, PropertiesLoaderUtils.loadProperties(resource));
		this.resourceName = getNameForResource(resource.getResource());
	}

	/**
	 * 基于从给定资源加载的属性创建PropertySource.
	 * PropertySource的名称将基于给定资源的{@link Resource#getDescription() description}生成.
	 */
	public ResourcePropertySource(EncodedResource resource) throws IOException {
		super(getNameForResource(resource.getResource()), PropertiesLoaderUtils.loadProperties(resource));
		this.resourceName = null;
	}

	/**
	 * 根据从给定编码的资源加载的属性, 创建具有给定名称的PropertySource.
	 */
	public ResourcePropertySource(String name, Resource resource) throws IOException {
		super(name, PropertiesLoaderUtils.loadProperties(new EncodedResource(resource)));
		this.resourceName = getNameForResource(resource);
	}

	/**
	 * 基于从给定资源加载的属性创建PropertySource.
	 * PropertySource的名称将基于给定资源的 {@link Resource#getDescription() description}生成.
	 */
	public ResourcePropertySource(Resource resource) throws IOException {
		super(getNameForResource(resource), PropertiesLoaderUtils.loadProperties(new EncodedResource(resource)));
		this.resourceName = null;
	}

	/**
	 * 根据从给定资源位置加载的属性, 创建具有给定名称的PropertySource,
	 * 并使用给定的类加载器加载资源 (假设它以{@code classpath:}为前缀).
	 */
	public ResourcePropertySource(String name, String location, ClassLoader classLoader) throws IOException {
		this(name, new DefaultResourceLoader(classLoader).getResource(location));
	}

	/**
	 * 基于从给定资源位置加载的Properties创建PropertySource, 并使用给定的类加载器加载资源, 假设它以{@code classpath:}为前缀.
	 * PropertySource的名称将基于给定资源的 {@link Resource#getDescription() description}生成.
	 */
	public ResourcePropertySource(String location, ClassLoader classLoader) throws IOException {
		this(new DefaultResourceLoader(classLoader).getResource(location));
	}

	/**
	 * 根据从给定资源位置加载的属性, 创建具有给定名称的PropertySource.
	 * 默认的线程上下文类加载器将用于加载资源 (假设位置字符串以{@code classpath:}为前缀).
	 */
	public ResourcePropertySource(String name, String location) throws IOException {
		this(name, new DefaultResourceLoader().getResource(location));
	}

	/**
	 * 基于从给定资源位置加载的属性创建PropertySource.
	 * PropertySource的名称将基于资源的{@link Resource#getDescription() description}生成.
	 */
	public ResourcePropertySource(String location) throws IOException {
		this(new DefaultResourceLoader().getResource(location));
	}

	private ResourcePropertySource(String name, String resourceName, Map<String, Object> source) {
		super(name, source);
		this.resourceName = resourceName;
	}


	/**
	 * 返回此{@link ResourcePropertySource}的可能适配的变体, 使用指定的名称覆盖先前给定 (或派生)的名称.
	 */
	public ResourcePropertySource withName(String name) {
		if (this.name.equals(name)) {
			return this;
		}
		// 请存储原始资源名称...
		if (this.resourceName != null) {
			if (this.resourceName.equals(name)) {
				return new ResourcePropertySource(this.resourceName, null, this.source);
			}
			else {
				return new ResourcePropertySource(name, this.resourceName, this.source);
			}
		}
		else {
			// 当前名称是资源名称 -> 将其保留在额外字段中...
			return new ResourcePropertySource(name, this.name, this.source);
		}
	}

	/**
	 * 返回此{@link ResourcePropertySource}的可能适配的变体,
	 * 使用原始资源名称(相当于无名构造函数变体生成的名称)覆盖先前给定的名称.
	 */
	public ResourcePropertySource withResourceName() {
		if (this.resourceName == null) {
			return this;
		}
		return new ResourcePropertySource(this.resourceName, null, this.source);
	}


	/**
	 * 返回给定资源的描述;
	 * 如果描述为空, 则返回资源的类名及其标识哈希码.
	 */
	private static String getNameForResource(Resource resource) {
		String name = resource.getDescription();
		if (!StringUtils.hasText(name)) {
			name = resource.getClass().getSimpleName() + "@" + System.identityHashCode(resource);
		}
		return name;
	}
}
