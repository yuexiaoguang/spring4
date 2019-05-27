package org.springframework.scheduling.quartz;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.spi.ClassLoadHelper;

import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

/**
 * 从Quartz {@link ClassLoadHelper}接口适配为Spring的{@link ResourceLoader}接口的包装器.
 * 当SchedulerFactoryBean在Spring ApplicationContext中运行时默认使用.
 */
public class ResourceLoaderClassLoadHelper implements ClassLoadHelper {

	protected static final Log logger = LogFactory.getLog(ResourceLoaderClassLoadHelper.class);

	private ResourceLoader resourceLoader;


	/**
	 * 使用默认的ResourceLoader.
	 */
	public ResourceLoaderClassLoadHelper() {
	}

	/**
	 * @param resourceLoader 要委托给的ResourceLoader
	 */
	public ResourceLoaderClassLoadHelper(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}


	@Override
	public void initialize() {
		if (this.resourceLoader == null) {
			this.resourceLoader = SchedulerFactoryBean.getConfigTimeResourceLoader();
			if (this.resourceLoader == null) {
				this.resourceLoader = new DefaultResourceLoader();
			}
		}
	}

	@Override
	public Class<?> loadClass(String name) throws ClassNotFoundException {
		return this.resourceLoader.getClassLoader().loadClass(name);
	}

	@SuppressWarnings("unchecked")
	public <T> Class<? extends T> loadClass(String name, Class<T> clazz) throws ClassNotFoundException {
		return (Class<? extends T>) loadClass(name);
	}

	@Override
	public URL getResource(String name) {
		Resource resource = this.resourceLoader.getResource(name);
		if (resource.exists()) {
			try {
				return resource.getURL();
			}
			catch (IOException ex) {
				if (logger.isWarnEnabled()) {
					logger.warn("Could not load " + resource);
				}
				return null;
			}
		}
		else {
			return getClassLoader().getResource(name);
		}
	}

	@Override
	public InputStream getResourceAsStream(String name) {
		Resource resource = this.resourceLoader.getResource(name);
		if (resource.exists()) {
			try {
				return resource.getInputStream();
			}
			catch (IOException ex) {
				if (logger.isWarnEnabled()) {
					logger.warn("Could not load " + resource);
				}
				return null;
			}
		}
		else {
			return getClassLoader().getResourceAsStream(name);
		}
	}

	@Override
	public ClassLoader getClassLoader() {
		return this.resourceLoader.getClassLoader();
	}

}
