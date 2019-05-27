package org.springframework.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URL;

import org.apache.log4j.LogManager;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.xml.DOMConfigurator;

/**
 * 便捷类, 具有自定义log4j配置的简单方法.
 *
 * <p>仅用于非默认的log4j初始化, 例如具有自定义配置位置或刷新间隔.
 * 默认情况下, log4j将简单地从类路径根目录中的"log4j.properties"或"log4j.xml"文件中读取其配置.
 *
 * <p>对于Web环境, 可以在Web包中找到类似的Log4jWebConfigurer类, 从{@code web.xml}中的context-params读取其配置.
 * 在J2EE Web应用程序中, log4j通常通过Log4jConfigListener设置, 委托给下面的Log4jWebConfigurer.
 *
 * @deprecated as of Spring 4.2.1, in favor of Apache Log4j 2 (following Apache's EOL declaration for log4j 1.x)
 */
@Deprecated
public abstract class Log4jConfigurer {

	/** 用于从类路径加载的伪URL前缀: "classpath:" */
	public static final String CLASSPATH_URL_PREFIX = "classpath:";

	/** 表示log4j XML配置文件的扩展名: ".xml" */
	public static final String XML_FILE_EXTENSION = ".xml";


	/**
	 * 从给定文件位置初始化log4j, 不刷新配置文件.
	 * 假设 ".xml"文件扩展名为XML文件, 否则为属性文件.
	 * 
	 * @param location 配置文件的位置:
	 * 可以是"classpath:"位置 (e.g. "classpath:myLog4j.properties"),
	 * 绝对文件URL (e.g. "file:C:/log4j.properties),
	 * 或文件系统中的普通绝对路径 (e.g. "C:/log4j.properties")
	 * 
	 * @throws FileNotFoundException 如果位置指定无效的文件路径
	 */
	public static void initLogging(String location) throws FileNotFoundException {
		String resolvedLocation = SystemPropertyUtils.resolvePlaceholders(location);
		URL url = ResourceUtils.getURL(resolvedLocation);
		if (ResourceUtils.URL_PROTOCOL_FILE.equals(url.getProtocol()) && !ResourceUtils.getFile(url).exists()) {
			throw new FileNotFoundException("Log4j config file [" + resolvedLocation + "] not found");
		}

		if (resolvedLocation.toLowerCase().endsWith(XML_FILE_EXTENSION)) {
			DOMConfigurator.configure(url);
		}
		else {
			PropertyConfigurator.configure(url);
		}
	}

	/**
	 * 使用配置文件的给定刷新间隔从给定位置初始化log4j.
	 * 假设 ".xml"文件扩展名为XML文件, 否则为属性文件.
	 * <p>Log4j的监视程序线程异步检查配置文件的时间戳是否已更改, 将使用检查之间的给定间隔.
	 * 刷新间隔为1000毫秒 (一秒), 允许立即执行日志级别更改, 这是不可行的.
	 * <p><b>WARNING:</b> 在VM关闭之前, Log4j的监视程序线程不会终止;
	 * 特别是, 它不会在LogManager关闭时终止.
	 * 因此, 建议<i>不要</i>在生产J2EE环境中使用配置文件刷新;
	 * 监视程序线程不会在应用程序关闭时停止.
	 * 
	 * @param location 配置文件的位置:
	 * 可以是"classpath:"位置 (e.g. "classpath:myLog4j.properties"),
	 * 绝对文件URL (e.g. "file:C:/log4j.properties),
	 * 或文件系统中的普通绝对路径 (e.g. "C:/log4j.properties")
	 * @param refreshInterval 配置文件刷新检查之间的间隔, 以毫秒为单位
	 * 
	 * @throws FileNotFoundException 如果位置指定无效的文件路径
	 */
	public static void initLogging(String location, long refreshInterval) throws FileNotFoundException {
		String resolvedLocation = SystemPropertyUtils.resolvePlaceholders(location);
		File file = ResourceUtils.getFile(resolvedLocation);
		if (!file.exists()) {
			throw new FileNotFoundException("Log4j config file [" + resolvedLocation + "] not found");
		}

		if (resolvedLocation.toLowerCase().endsWith(XML_FILE_EXTENSION)) {
			DOMConfigurator.configureAndWatch(file.getAbsolutePath(), refreshInterval);
		}
		else {
			PropertyConfigurator.configureAndWatch(file.getAbsolutePath(), refreshInterval);
		}
	}

	/**
	 * 关闭log4j, 正确释放所有文件锁.
	 * <p>这不是严格必要的, 但建议在主机VM保持活动的情况下关闭log4j (例如, 在J2EE环境中关闭应用程序时).
	 */
	public static void shutdownLogging() {
		LogManager.shutdown();
	}

	/**
	 * 将指定的系统属性设置为当前工作目录.
	 * <p>这可以用于 e.g. 适用于测试环境, 适用于在Web环境中利用 Log4jWebConfigurer的 "webAppRootKey"支持的应用程序.
	 * 
	 * @param key 要使用的系统属性键, 如Log4j配置中所预期的那样
	 * (例如: "demo.root", 用作 "${demo.root}/WEB-INF/demo.log")
	 */
	public static void setWorkingDirSystemProperty(String key) {
		System.setProperty(key, new File("").getAbsolutePath());
	}
}
