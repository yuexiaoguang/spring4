package org.springframework.beans.factory.xml;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/**
 * {@link EntityResolver}实现, 尝试使用一组映射文件将模式URL解析为本地 {@link ClassPathResource 类路径资源}.
 *
 * <p>默认情况下, 此类将使用模式在类路径中查找映射文件:
 * {@code META-INF/spring.schemas} 允许在任何时候在类路径上存在多个文件.
 *
 * {@code META-INF/spring.schemas}的格式是一个属性文件, 其中每一行的格式应为 {@code systemId=schema-location},
 * {@code schema-location}也应该是一个模式文件.
 * 由于systemId通常是一个URL, 因此必须小心转义任何在属性文件中被视为分隔符的 ':'字符.
 *
 * <p>可以使用 {@link #PluggableSchemaResolver(ClassLoader, String)}构造函数覆盖映射文件的模式
 */
public class PluggableSchemaResolver implements EntityResolver {

	/**
	 * 定义模式映射的文件的位置.
	 * 可以存在于多个JAR文件中.
	 */
	public static final String DEFAULT_SCHEMA_MAPPINGS_LOCATION = "META-INF/spring.schemas";


	private static final Log logger = LogFactory.getLog(PluggableSchemaResolver.class);

	private final ClassLoader classLoader;

	private final String schemaMappingsLocation;

	/** 存储模式URL的映射 -> 本地模式路径 */
	private volatile Map<String, String> schemaMappings;


	/**
	 * 加载架构URL -> 使用默认映射文件模式 "META-INF/spring.schemas"的模式文件位置映射.
	 * 
	 * @param classLoader 用于加载的ClassLoader (可以是{@code null}), 使用默认的 ClassLoader)
	 */
	public PluggableSchemaResolver(ClassLoader classLoader) {
		this.classLoader = classLoader;
		this.schemaMappingsLocation = DEFAULT_SCHEMA_MAPPINGS_LOCATION;
	}

	/**
	 * 加载架构URL -> 使用给定映射文件模式的模式文件位置映射.
	 * 
	 * @param classLoader 用于加载的ClassLoader (可以是{@code null}), 使用默认的 ClassLoader)
	 * @param schemaMappingsLocation 定义模式映射的文件的位置 (不能为空)
	 */
	public PluggableSchemaResolver(ClassLoader classLoader, String schemaMappingsLocation) {
		Assert.hasText(schemaMappingsLocation, "'schemaMappingsLocation' must not be empty");
		this.classLoader = classLoader;
		this.schemaMappingsLocation = schemaMappingsLocation;
	}

	@Override
	public InputSource resolveEntity(String publicId, String systemId) throws IOException {
		if (logger.isTraceEnabled()) {
			logger.trace("Trying to resolve XML entity with public id [" + publicId +
					"] and system id [" + systemId + "]");
		}

		if (systemId != null) {
			String resourceLocation = getSchemaMappings().get(systemId);
			if (resourceLocation != null) {
				Resource resource = new ClassPathResource(resourceLocation, this.classLoader);
				try {
					InputSource source = new InputSource(resource.getInputStream());
					source.setPublicId(publicId);
					source.setSystemId(systemId);
					if (logger.isDebugEnabled()) {
						logger.debug("Found XML schema [" + systemId + "] in classpath: " + resourceLocation);
					}
					return source;
				}
				catch (FileNotFoundException ex) {
					if (logger.isDebugEnabled()) {
						logger.debug("Could not find XML schema [" + systemId + "]: " + resource, ex);
					}
				}
			}
		}
		return null;
	}

	/**
	 * 延迟加载指定的模式映射.
	 */
	private Map<String, String> getSchemaMappings() {
		Map<String, String> schemaMappings = this.schemaMappings;
		if (schemaMappings == null) {
			synchronized (this) {
				schemaMappings = this.schemaMappings;
				if (schemaMappings == null) {
					if (logger.isDebugEnabled()) {
						logger.debug("Loading schema mappings from [" + this.schemaMappingsLocation + "]");
					}
					try {
						Properties mappings =
								PropertiesLoaderUtils.loadAllProperties(this.schemaMappingsLocation, this.classLoader);
						if (logger.isDebugEnabled()) {
							logger.debug("Loaded schema mappings: " + mappings);
						}
						schemaMappings = new ConcurrentHashMap<String, String>(mappings.size());
						CollectionUtils.mergePropertiesIntoMap(mappings, schemaMappings);
						this.schemaMappings = schemaMappings;
					}
					catch (IOException ex) {
						throw new IllegalStateException(
								"Unable to load schema mappings from location [" + this.schemaMappingsLocation + "]", ex);
					}
				}
			}
		}
		return schemaMappings;
	}


	@Override
	public String toString() {
		return "EntityResolver using mappings " + getSchemaMappings();
	}

}
