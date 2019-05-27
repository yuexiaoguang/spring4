package org.springframework.core;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * 用于本地Spring属性的静态持有者, i.e. 在Spring库级别定义的.
 *
 * <p>从Spring库类路径的根目录中读取{@code spring.properties}文件, 并允许通过{@link #setProperty}以编程方式设置属性.
 * 检查属性时, 首先检查本地条目, 然后通过{@link System#getProperty}检查回退到JVM级系统属性.
 *
 * <p>这是设置Spring相关系统属性的另一种方法, 例如"spring.getenv.ignore"和"spring.beaninfo.ignore",
 * 特别是在JVM系统属性被锁定在目标平台(e.g. WebSphere)的情况下.
 * 有关将本地标志设置为"true"的便捷方法, 请参阅{@link #setFlag}.
 */
public abstract class SpringProperties {

	private static final String PROPERTIES_RESOURCE_LOCATION = "spring.properties";

	private static final Log logger = LogFactory.getLog(SpringProperties.class);

	private static final Properties localProperties = new Properties();


	static {
		try {
			ClassLoader cl = SpringProperties.class.getClassLoader();
			URL url = (cl != null ? cl.getResource(PROPERTIES_RESOURCE_LOCATION) :
					ClassLoader.getSystemResource(PROPERTIES_RESOURCE_LOCATION));
			if (url != null) {
				logger.info("Found 'spring.properties' file in local classpath");
				InputStream is = url.openStream();
				try {
					localProperties.load(is);
				}
				finally {
					is.close();
				}
			}
		}
		catch (IOException ex) {
			if (logger.isInfoEnabled()) {
				logger.info("Could not load 'spring.properties' file from local classpath: " + ex);
			}
		}
	}


	/**
	 * 以编程方式设置本地属性, 覆盖{@code spring.properties}文件中的条.
	 * 
	 * @param key 属性key
	 * @param value 关联的属性值, 或{@code null}来重置它
	 */
	public static void setProperty(String key, String value) {
		if (value != null) {
			localProperties.setProperty(key, value);
		}
		else {
			localProperties.remove(key);
		}
	}

	/**
	 * 检索给定键的属性值, 首先检查本地Spring属性并回退到JVM级系统属性.
	 * 
	 * @param key 属性key
	 * 
	 * @return 关联的属性值, 或{@code null}
	 */
	public static String getProperty(String key) {
		String value = localProperties.getProperty(key);
		if (value == null) {
			try {
				value = System.getProperty(key);
			}
			catch (Throwable ex) {
				if (logger.isDebugEnabled()) {
					logger.debug("Could not retrieve system property '" + key + "': " + ex);
				}
			}
		}
		return value;
	}

	/**
	 * 以编程方式将本地标志设置为"true", 覆盖{@code spring.properties}文件中的条目.
	 * 
	 * @param key 属性key
	 */
	public static void setFlag(String key) {
		localProperties.put(key, Boolean.TRUE.toString());
	}

	/**
	 * 检索给定属性键的标志.
	 * 
	 * @param key 属性key
	 * 
	 * @return {@code true}如果属性被设置为"true", 否则{@code false}
	 */
	public static boolean getFlag(String key) {
		return Boolean.parseBoolean(getProperty(key));
	}
}
