package org.springframework.core.io.support;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.io.Resource;
import org.springframework.util.CollectionUtils;
import org.springframework.util.DefaultPropertiesPersister;
import org.springframework.util.PropertiesPersister;

/**
 * 需要从一个或多个资源加载属性的JavaBean样式组件的基类.
 * 支持本地属性, 具有可配置的覆盖.
 */
public abstract class PropertiesLoaderSupport {

	/** Logger available to subclasses */
	protected final Log logger = LogFactory.getLog(getClass());

	protected Properties[] localProperties;

	protected boolean localOverride = false;

	private Resource[] locations;

	private boolean ignoreResourceNotFound = false;

	private String fileEncoding;

	private PropertiesPersister propertiesPersister = new DefaultPropertiesPersister();


	/**
	 * 设置本地属性, e.g. 通过XML bean定义中的"props"标签.
	 * 这些可以被视为默认值, 由从文件加载的属性覆盖.
	 */
	public void setProperties(Properties properties) {
		this.localProperties = new Properties[] {properties};
	}

	/**
	 * 设置本地属性, e.g. 通过XML bean定义中的"props"标签, 允许将多个属性集合并为一个.
	 */
	public void setPropertiesArray(Properties... propertiesArray) {
		this.localProperties = propertiesArray;
	}

	/**
	 * 设置要加载的属性文件的位置.
	 * <p>可以指向经典属性文件, 或遵循JDK 1.5属性XML格式的XML文件
	 */
	public void setLocation(Resource location) {
		this.locations = new Resource[] {location};
	}

	/**
	 * 设置要加载的属性文件的位置.
	 * <p>可以指向经典属性文件, 或遵循JDK 1.5属性XML格式的XML文件.
	 * <p>Note: 在键重叠的情况下, 在以后的文件中定义的属性将覆盖先前定义的文件的属性.
	 * 因此, 请确保最具体的文件是给定位置列表中的最后一个文件.
	 */
	public void setLocations(Resource... locations) {
		this.locations = locations;
	}

	/**
	 * 设置本地属性是否覆盖文件的属性.
	 * <p>默认 "false": 文件中的属性会覆盖本地默认值.
	 * 可以切换为"true"以使本地属性覆盖文件的默认值.
	 */
	public void setLocalOverride(boolean localOverride) {
		this.localOverride = localOverride;
	}

	/**
	 * 设置是否应忽略找不到属性资源的情况.
	 * <p>如果属性文件是完全可选的, 则为"true".
	 * 默认 "false".
	 */
	public void setIgnoreResourceNotFound(boolean ignoreResourceNotFound) {
		this.ignoreResourceNotFound = ignoreResourceNotFound;
	}

	/**
	 * 设置用于解析属性文件的编码.
	 * <p>默认无, 使用{@code java.util.Properties}默认编码.
	 * <p>仅适用于经典属性文件, 而不适用于XML文件.
	 */
	public void setFileEncoding(String encoding) {
		this.fileEncoding = encoding;
	}

	/**
	 * 设置用于解析属性文件的PropertiesPersister.
	 * 默认DefaultPropertiesPersister.
	 */
	public void setPropertiesPersister(PropertiesPersister propertiesPersister) {
		this.propertiesPersister =
				(propertiesPersister != null ? propertiesPersister : new DefaultPropertiesPersister());
	}


	/**
	 * 返回合并的Properties实例, 该实例包含在此FactoryBean上设置的属性和已加载的属性.
	 */
	protected Properties mergeProperties() throws IOException {
		Properties result = new Properties();

		if (this.localOverride) {
			// 从文件前端加载属性, 以使本地属性覆盖.
			loadProperties(result);
		}

		if (this.localProperties != null) {
			for (Properties localProp : this.localProperties) {
				CollectionUtils.mergePropertiesIntoMap(localProp, result);
			}
		}

		if (!this.localOverride) {
			// 之后从文件加载属性, 以覆盖这些属性.
			loadProperties(result);
		}

		return result;
	}

	/**
	 * 将属性加载到给定实例中.
	 * 
	 * @param props 要加载到的Properties实例
	 * 
	 * @throws IOException 发生I/O错误
	 */
	protected void loadProperties(Properties props) throws IOException {
		if (this.locations != null) {
			for (Resource location : this.locations) {
				if (logger.isDebugEnabled()) {
					logger.debug("Loading properties file from " + location);
				}
				try {
					PropertiesLoaderUtils.fillProperties(
							props, new EncodedResource(location, this.fileEncoding), this.propertiesPersister);
				}
				catch (IOException ex) {
					// 尝试打开时找不到资源
					if (this.ignoreResourceNotFound &&
							(ex instanceof FileNotFoundException || ex instanceof UnknownHostException)) {
						if (logger.isInfoEnabled()) {
							logger.info("Properties resource not found: " + ex.getMessage());
						}
					}
					else {
						throw ex;
					}
				}
			}
		}
	}
}
