package org.springframework.ui.freemarker;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

import freemarker.cache.TemplateLoader;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

/**
 * 通过Spring {@link ResourceLoader}加载的FreeMarker {@link TemplateLoader}适配器.
 * 由{@link FreeMarkerConfigurationFactory}用于任何无法解析为{@link java.io.File}的资源加载器路径.
 */
public class SpringTemplateLoader implements TemplateLoader {

	protected final Log logger = LogFactory.getLog(getClass());

	private final ResourceLoader resourceLoader;

	private final String templateLoaderPath;


	/**
	 * @param resourceLoader 要使用的Spring ResourceLoader
	 * @param templateLoaderPath 要使用的模板加载器路径
	 */
	public SpringTemplateLoader(ResourceLoader resourceLoader, String templateLoaderPath) {
		this.resourceLoader = resourceLoader;
		if (!templateLoaderPath.endsWith("/")) {
			templateLoaderPath += "/";
		}
		this.templateLoaderPath = templateLoaderPath;
		if (logger.isInfoEnabled()) {
			logger.info("SpringTemplateLoader for FreeMarker: using resource loader [" + this.resourceLoader +
					"] and template loader path [" + this.templateLoaderPath + "]");
		}
	}


	@Override
	public Object findTemplateSource(String name) throws IOException {
		if (logger.isDebugEnabled()) {
			logger.debug("Looking for FreeMarker template with name [" + name + "]");
		}
		Resource resource = this.resourceLoader.getResource(this.templateLoaderPath + name);
		return (resource.exists() ? resource : null);
	}

	@Override
	public Reader getReader(Object templateSource, String encoding) throws IOException {
		Resource resource = (Resource) templateSource;
		try {
			return new InputStreamReader(resource.getInputStream(), encoding);
		}
		catch (IOException ex) {
			if (logger.isDebugEnabled()) {
				logger.debug("Could not find FreeMarker template: " + resource);
			}
			throw ex;
		}
	}

	@Override
	public long getLastModified(Object templateSource) {
		Resource resource = (Resource) templateSource;
		try {
			return resource.lastModified();
		}
		catch (IOException ex) {
			if (logger.isDebugEnabled()) {
				logger.debug("Could not obtain last-modified timestamp for FreeMarker template in " +
						resource + ": " + ex);
			}
			return -1;
		}
	}

	@Override
	public void closeTemplateSource(Object templateSource) throws IOException {
	}

}
