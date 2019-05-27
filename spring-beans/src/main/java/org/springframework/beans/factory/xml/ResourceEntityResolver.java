package org.springframework.beans.factory.xml;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

/**
 * 试图通过 {@link org.springframework.core.io.ResourceLoader}解析实体引用的EntityResolver实现
 * (通常, 相对于ApplicationContext的资源库).
 * 扩展{@link DelegatingEntityResolver}以提供DTD和XSD查找.
 *
 * <p>允许使用标准XML实体将XML片段包含到应用程序上下文定义中, 例如将大型XML文件拆分为各种模块.
 * include路径可以像往常一样相对于应用程序上下文的资源库, 而不是相对于JVM工作目录 (XML解析器的默认值).
 *
 * <p>Note: 除了相对路径之外, 还指定了当前系统根目录中的文件的每个URL,
 * i.e. JVM工作目录也将相对于应用程序上下文进行解释.
 */
public class ResourceEntityResolver extends DelegatingEntityResolver {

	private static final Log logger = LogFactory.getLog(ResourceEntityResolver.class);

	private final ResourceLoader resourceLoader;


	/**
	 * 为指定的ResourceLoader创建ResourceEntityResolver (通常是ApplicationContext).
	 * 
	 * @param resourceLoader 用于加载XML实体的ResourceLoader (或ApplicationContext)
	 */
	public ResourceEntityResolver(ResourceLoader resourceLoader) {
		super(resourceLoader.getClassLoader());
		this.resourceLoader = resourceLoader;
	}


	@Override
	public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
		InputSource source = super.resolveEntity(publicId, systemId);
		if (source == null && systemId != null) {
			String resourcePath = null;
			try {
				String decodedSystemId = URLDecoder.decode(systemId, "UTF-8");
				String givenUrl = new URL(decodedSystemId).toString();
				String systemRootUrl = new File("").toURI().toURL().toString();
				// 如果当前在系统根目录中, 相对于资源库.
				if (givenUrl.startsWith(systemRootUrl)) {
					resourcePath = givenUrl.substring(systemRootUrl.length());
				}
			}
			catch (Exception ex) {
				// Typically a MalformedURLException or AccessControlException.
				if (logger.isDebugEnabled()) {
					logger.debug("Could not resolve XML entity [" + systemId + "] against system root URL", ex);
				}
				// No URL (或者没有可解析的URL) -> 尝试相对于资源库.
				resourcePath = systemId;
			}
			if (resourcePath != null) {
				if (logger.isTraceEnabled()) {
					logger.trace("Trying to locate XML entity [" + systemId + "] as resource [" + resourcePath + "]");
				}
				Resource resource = this.resourceLoader.getResource(resourcePath);
				source = new InputSource(resource.getInputStream());
				source.setPublicId(publicId);
				source.setSystemId(systemId);
				if (logger.isDebugEnabled()) {
					logger.debug("Found XML entity [" + systemId + "]: " + resource);
				}
			}
		}
		return source;
	}

}
