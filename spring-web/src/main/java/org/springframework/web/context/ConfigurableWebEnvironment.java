package org.springframework.web.context;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

import org.springframework.core.env.ConfigurableEnvironment;

/**
 * {@link ConfigurableEnvironment}的细化, 允许在{@link ServletContext}和(可选) {@link ServletConfig}可用的最早时刻,
 * 初始化与servlet相关的{@link org.springframework.core.env.PropertySource}对象.
 */
public interface ConfigurableWebEnvironment extends ConfigurableEnvironment {

	/**
	 * 使用给定参数将任何充当占位符的
	 * {@linkplain org.springframework.core.env.PropertySource.StubPropertySource 存根属性源}实例
	 * 替换为真实的servlet上下文/配置属性源.
	 * 
	 * @param servletContext the {@link ServletContext} (may not be {@code null})
	 * @param servletConfig the {@link ServletConfig} ({@code null} if not available)
	 */
	void initPropertySources(ServletContext servletContext, ServletConfig servletConfig);

}
