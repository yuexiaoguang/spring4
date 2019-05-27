package org.springframework.beans.factory.xml;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

/**
 * Spring bean DTD的EntityResolver实现, 从Spring类路径(或JAR文件)加载DTD.
 *
 * <p>从类路径资源"/org/springframework/beans/factory/xml/spring-beans-2.0.dtd"获取"spring-beans-2.0.dtd",
 * 无论是指定为DTD名称中包含“spring-beans”的本地URL, 还是 "http://www.springframework.org/dtd/spring-beans-2.0.dtd".
 */
public class BeansDtdResolver implements EntityResolver {

	private static final String DTD_EXTENSION = ".dtd";

	private static final String DTD_FILENAME = "spring-beans-2.0";

	private static final String DTD_NAME = "spring-beans";

	private static final Log logger = LogFactory.getLog(BeansDtdResolver.class);


	@Override
	public InputSource resolveEntity(String publicId, String systemId) throws IOException {
		if (logger.isTraceEnabled()) {
			logger.trace("Trying to resolve XML entity with public ID [" + publicId +
					"] and system ID [" + systemId + "]");
		}
		if (systemId != null && systemId.endsWith(DTD_EXTENSION)) {
			int lastPathSeparator = systemId.lastIndexOf('/');
			int dtdNameStart = systemId.indexOf(DTD_NAME, lastPathSeparator);
			if (dtdNameStart != -1) {
				String dtdFile = DTD_FILENAME + DTD_EXTENSION;
				if (logger.isTraceEnabled()) {
					logger.trace("Trying to locate [" + dtdFile + "] in Spring jar on classpath");
				}
				try {
					Resource resource = new ClassPathResource(dtdFile, getClass());
					InputSource source = new InputSource(resource.getInputStream());
					source.setPublicId(publicId);
					source.setSystemId(systemId);
					if (logger.isDebugEnabled()) {
						logger.debug("Found beans DTD [" + systemId + "] in classpath: " + dtdFile);
					}
					return source;
				}
				catch (IOException ex) {
					if (logger.isDebugEnabled()) {
						logger.debug("Could not resolve beans DTD [" + systemId + "]: not found in classpath", ex);
					}
				}

			}
		}

		// 使用默认的行为 -> 从网站或任何地方下载.
		return null;
	}


	@Override
	public String toString() {
		return "EntityResolver for spring-beans DTD";
	}

}
