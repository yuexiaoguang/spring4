package org.springframework.core.io.support;

import java.io.IOException;

import org.springframework.core.env.PropertySource;

/**
 * 用于创建基于资源的{@link PropertySource}包装器的策略接口.
 */
public interface PropertySourceFactory {

	/**
	 * 创建一个包装给定资源的{@link PropertySource}.
	 * 
	 * @param name 属性源的名称
	 * @param resource 要包装的资源 (可能已编码)
	 * 
	 * @return 新的{@link PropertySource} (never {@code null})
	 * @throws IOException 如果资源解析失败
	 */
	PropertySource<?> createPropertySource(String name, EncodedResource resource) throws IOException;

}
