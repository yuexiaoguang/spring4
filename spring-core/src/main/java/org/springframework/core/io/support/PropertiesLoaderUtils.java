package org.springframework.core.io.support;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Enumeration;
import java.util.Properties;

import org.springframework.core.io.Resource;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.DefaultPropertiesPersister;
import org.springframework.util.PropertiesPersister;
import org.springframework.util.ResourceUtils;

/**
 * 方便的实用方法, 用于加载{@code java.util.Properties}, 执行输入流的标准处理.
 *
 * <p>要获得更多可配置的属性加载, 包括自定义编码选项, 请考虑使用 PropertiesLoaderSupport 类.
 */
public abstract class PropertiesLoaderUtils {

	private static final String XML_FILE_EXTENSION = ".xml";


	/**
	 * 从给定的EncodedResource加载属性, 可能定义属性文件的特定编码.
	 */
	public static Properties loadProperties(EncodedResource resource) throws IOException {
		Properties props = new Properties();
		fillProperties(props, resource);
		return props;
	}

	/**
	 * 从给定的EncodedResource填充给定的属性, 可能为属性文件定义特定的编码.
	 * 
	 * @param props 要加载到的Properties实例
	 * @param resource 要加载的资源
	 * 
	 * @throws IOException 发生I/O 错误
	 */
	public static void fillProperties(Properties props, EncodedResource resource)
			throws IOException {

		fillProperties(props, resource, new DefaultPropertiesPersister());
	}

	/**
	 * 实际将给定EncodedResource的属性加载到给定的Properties实例中.
	 * 
	 * @param props 要加载到的Properties实例
	 * @param resource 要加载的资源
	 * @param persister 要使用的PropertiesPersister
	 * 
	 * @throws IOException 发生I/O 错误
	 */
	static void fillProperties(Properties props, EncodedResource resource, PropertiesPersister persister)
			throws IOException {

		InputStream stream = null;
		Reader reader = null;
		try {
			String filename = resource.getResource().getFilename();
			if (filename != null && filename.endsWith(XML_FILE_EXTENSION)) {
				stream = resource.getInputStream();
				persister.loadFromXml(props, stream);
			}
			else if (resource.requiresReader()) {
				reader = resource.getReader();
				persister.load(props, reader);
			}
			else {
				stream = resource.getInputStream();
				persister.load(props, stream);
			}
		}
		finally {
			if (stream != null) {
				stream.close();
			}
			if (reader != null) {
				reader.close();
			}
		}
	}

	/**
	 * 从给定资源加载属性 (采用ISO-8859-1编码).
	 * 
	 * @param resource 要加载的资源
	 * 
	 * @return 填充后的Properties实例
	 * @throws IOException 如果加载失败
	 */
	public static Properties loadProperties(Resource resource) throws IOException {
		Properties props = new Properties();
		fillProperties(props, resource);
		return props;
	}

	/**
	 * 从给定资源填充给定属性 (采用ISO-8859-1编码).
	 * 
	 * @param props 要填充的Properties实例
	 * @param resource 要加载的资源
	 * 
	 * @throws IOException 如果加载失败
	 */
	public static void fillProperties(Properties props, Resource resource) throws IOException {
		InputStream is = resource.getInputStream();
		try {
			String filename = resource.getFilename();
			if (filename != null && filename.endsWith(XML_FILE_EXTENSION)) {
				props.loadFromXML(is);
			}
			else {
				props.load(is);
			}
		}
		finally {
			is.close();
		}
	}

	/**
	 * 使用默认的类加载器从指定的类路径资源 (采用 ISO-8859-1 编码)加载所有属性.
	 * <p>如果在类路径中找到多个同名资源, 则合并属性.
	 * 
	 * @param resourceName 类路径资源的名称
	 * 
	 * @return 填充后的Properties实例
	 * @throws IOException 如果加载失败
	 */
	public static Properties loadAllProperties(String resourceName) throws IOException {
		return loadAllProperties(resourceName, null);
	}

	/**
	 * 使用给定的类加载器从指定的类路径资源 (采用 ISO-8859-1 编码)加载所有属性.
	 * <p>如果在类路径中找到多个同名资源, 则合并属性.
	 * 
	 * @param resourceName 类路径资源的名称
	 * @param classLoader 用于加载的ClassLoader (或{@code null}以使用默认的类加载器)
	 * 
	 * @return 填充后的Properties实例
	 * @throws IOException 如果加载失败
	 */
	public static Properties loadAllProperties(String resourceName, ClassLoader classLoader) throws IOException {
		Assert.notNull(resourceName, "Resource name must not be null");
		ClassLoader classLoaderToUse = classLoader;
		if (classLoaderToUse == null) {
			classLoaderToUse = ClassUtils.getDefaultClassLoader();
		}
		Enumeration<URL> urls = (classLoaderToUse != null ? classLoaderToUse.getResources(resourceName) :
				ClassLoader.getSystemResources(resourceName));
		Properties props = new Properties();
		while (urls.hasMoreElements()) {
			URL url = urls.nextElement();
			URLConnection con = url.openConnection();
			ResourceUtils.useCachesIfNecessary(con);
			InputStream is = con.getInputStream();
			try {
				if (resourceName.endsWith(XML_FILE_EXTENSION)) {
					props.loadFromXML(is);
				}
				else {
					props.load(is);
				}
			}
			finally {
				is.close();
			}
		}
		return props;
	}
}
