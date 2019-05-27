package org.springframework.core.io;

/**
 * 特定于协议的资源句柄的解析策略.
 *
 * <p>用作{@link DefaultResourceLoader}的SPI, 允许处理自定义协议而无需子类化加载器实现 (或应用程序上下文实现).
 */
public interface ProtocolResolver {

	/**
	 * 如果此实现的协议匹配, 则针对给定的资源加载器解析给定位置.
	 * 
	 * @param location 用户指定的资源位置
	 * @param resourceLoader 关联的资源加载器
	 * 
	 * @return 如果给定位置与此解析器的协议匹配, 则为相应的{@code Resource}句柄; 否则为{@code null}
	 */
	Resource resolve(String location, ResourceLoader resourceLoader);

}
