package org.springframework.mock.web.portlet;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import javax.activation.FileTypeMap;
import javax.portlet.PortletContext;
import javax.portlet.PortletRequestDispatcher;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.Assert;
import org.springframework.web.util.WebUtils;

/**
 * {@link javax.portlet.PortletContext}接口的模拟实现.
 */
public class MockPortletContext implements PortletContext {

	private static final String TEMP_DIR_SYSTEM_PROPERTY = "java.io.tmpdir";


	private final Log logger = LogFactory.getLog(getClass());

	private final String resourceBasePath;

	private final ResourceLoader resourceLoader;

	private final Map<String, Object> attributes = new LinkedHashMap<String, Object>();

	private final Map<String, String> initParameters = new LinkedHashMap<String, String>();

	private String portletContextName = "MockPortletContext";

	private Set<String> containerRuntimeOptions = new LinkedHashSet<String>();


	/**
	 * 没有基础路径, 并使用DefaultResourceLoader (i.e. classpath根目录为WAR根目录).
	 */
	public MockPortletContext() {
		this("", null);
	}

	/**
	 * 使用DefaultResourceLoader.
	 * 
	 * @param resourceBasePath WAR根目录 (不应以斜杠结尾)
	 */
	public MockPortletContext(String resourceBasePath) {
		this(resourceBasePath, null);
	}

	/**
	 * 没有基础路径, 并使用指定的ResourceLoader.
	 * 
	 * @param resourceLoader 要使用的ResourceLoader (或null使用默认值)
	 */
	public MockPortletContext(ResourceLoader resourceLoader) {
		this("", resourceLoader);
	}

	/**
	 * @param resourceBasePath WAR根目录 (不应以斜杠结尾)
	 * @param resourceLoader 要使用的ResourceLoader (或null使用默认值)
	 */
	public MockPortletContext(String resourceBasePath, ResourceLoader resourceLoader) {
		this.resourceBasePath = (resourceBasePath != null ? resourceBasePath : "");
		this.resourceLoader = (resourceLoader != null ? resourceLoader : new DefaultResourceLoader());

		// Use JVM temp dir as PortletContext temp dir.
		String tempDir = System.getProperty(TEMP_DIR_SYSTEM_PROPERTY);
		if (tempDir != null) {
			this.attributes.put(WebUtils.TEMP_DIR_CONTEXT_ATTRIBUTE, new File(tempDir));
		}
	}

	/**
	 * 为给定路径构建完整资源路径, 在前面追加此MockPortletContext的资源基础路径.
	 * 
	 * @param path 指定的路径
	 * 
	 * @return 完整资源路径
	 */
	protected String getResourceLocation(String path) {
		if (!path.startsWith("/")) {
			path = "/" + path;
		}
		return this.resourceBasePath + path;
	}


	@Override
	public String getServerInfo() {
		return "MockPortal/1.0";
	}

	@Override
	public PortletRequestDispatcher getRequestDispatcher(String path) {
		if (!path.startsWith("/")) {
			throw new IllegalArgumentException(
					"PortletRequestDispatcher path at PortletContext level must start with '/'");
		}
		return new MockPortletRequestDispatcher(path);
	}

	@Override
	public PortletRequestDispatcher getNamedDispatcher(String path) {
		return null;
	}

	@Override
	public InputStream getResourceAsStream(String path) {
		Resource resource = this.resourceLoader.getResource(getResourceLocation(path));
		try {
			return resource.getInputStream();
		}
		catch (IOException ex) {
			logger.info("Couldn't open InputStream for " + resource, ex);
			return null;
		}
	}

	@Override
	public int getMajorVersion() {
		return 2;
	}

	@Override
	public int getMinorVersion() {
		return 0;
	}

	@Override
	public String getMimeType(String filePath) {
		return MimeTypeResolver.getMimeType(filePath);
	}

	@Override
	public String getRealPath(String path) {
		Resource resource = this.resourceLoader.getResource(getResourceLocation(path));
		try {
			return resource.getFile().getAbsolutePath();
		}
		catch (IOException ex) {
			logger.info("Couldn't determine real path of resource " + resource, ex);
			return null;
		}
	}

	@Override
	public Set<String> getResourcePaths(String path) {
		Resource resource = this.resourceLoader.getResource(getResourceLocation(path));
		try {
			File file = resource.getFile();
			String[] fileList = file.list();
			String prefix = (path.endsWith("/") ? path : path + "/");
			Set<String> resourcePaths = new HashSet<String>(fileList.length);
			for (String fileEntry : fileList) {
				resourcePaths.add(prefix + fileEntry);
			}
			return resourcePaths;
		}
		catch (IOException ex) {
			logger.info("Couldn't get resource paths for " + resource, ex);
			return null;
		}
	}

	@Override
	public URL getResource(String path) throws MalformedURLException {
		Resource resource = this.resourceLoader.getResource(getResourceLocation(path));
		try {
			return resource.getURL();
		}
		catch (IOException ex) {
			logger.info("Couldn't get URL for " + resource, ex);
			return null;
		}
	}

	@Override
	public Object getAttribute(String name) {
		return this.attributes.get(name);
	}

	@Override
	public Enumeration<String> getAttributeNames() {
		return Collections.enumeration(new LinkedHashSet<String>(this.attributes.keySet()));
	}

	@Override
	public void setAttribute(String name, Object value) {
		if (value != null) {
			this.attributes.put(name, value);
		}
		else {
			this.attributes.remove(name);
		}
	}

	@Override
	public void removeAttribute(String name) {
		this.attributes.remove(name);
	}

	public void addInitParameter(String name, String value) {
		Assert.notNull(name, "Parameter name must not be null");
		this.initParameters.put(name, value);
	}

	@Override
	public String getInitParameter(String name) {
		Assert.notNull(name, "Parameter name must not be null");
		return this.initParameters.get(name);
	}

	@Override
	public Enumeration<String> getInitParameterNames() {
		return Collections.enumeration(this.initParameters.keySet());
	}

	@Override
	public void log(String message) {
		logger.info(message);
	}

	@Override
	public void log(String message, Throwable t) {
		logger.info(message, t);
	}

	public void setPortletContextName(String portletContextName) {
		this.portletContextName = portletContextName;
	}

	@Override
	public String getPortletContextName() {
		return this.portletContextName;
	}

	public void addContainerRuntimeOption(String key) {
		this.containerRuntimeOptions.add(key);
	}

	@Override
	public Enumeration<String> getContainerRuntimeOptions() {
		return Collections.enumeration(this.containerRuntimeOptions);
	}


	/**
	 * 内部工厂类, 用于在实际要求解析MIME类型时引入Java Activation Framework依赖项.
	 */
	private static class MimeTypeResolver {

		public static String getMimeType(String filePath) {
			return FileTypeMap.getDefaultFileTypeMap().getContentType(filePath);
		}
	}

}
