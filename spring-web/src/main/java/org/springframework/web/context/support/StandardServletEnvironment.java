package org.springframework.web.context.support;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

import org.springframework.core.env.Environment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.PropertySource.StubPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.jndi.JndiLocatorDelegate;
import org.springframework.jndi.JndiPropertySource;
import org.springframework.web.context.ConfigurableWebEnvironment;

/**
 * 基于{@code Servlet}的Web应用程序使用的{@link Environment}实现.
 * 所有与Web相关的 (基于servlet的) {@code ApplicationContext}类都默认初始化一个实例.
 *
 * <p>提供{@code ServletConfig}, {@code ServletContext}, 和基于JNDI的{@link PropertySource}实例.
 * 有关详细信息, 请参阅{@link #customizePropertySources}方法文档.
 */
public class StandardServletEnvironment extends StandardEnvironment implements ConfigurableWebEnvironment {

	/** Servlet上下文init参数属性源名称: {@value} */
	public static final String SERVLET_CONTEXT_PROPERTY_SOURCE_NAME = "servletContextInitParams";

	/** Servlet配置init参数属性源名称: {@value} */
	public static final String SERVLET_CONFIG_PROPERTY_SOURCE_NAME = "servletConfigInitParams";

	/** JNDI属性源名称: {@value} */
	public static final String JNDI_PROPERTY_SOURCE_NAME = "jndiProperties";


	/**
	 * 使用超类提供的属性源以及适用于基于标准servlet的环境的属性源自定义属性源集:
	 * <ul>
	 * <li>{@value #SERVLET_CONFIG_PROPERTY_SOURCE_NAME}
	 * <li>{@value #SERVLET_CONTEXT_PROPERTY_SOURCE_NAME}
	 * <li>{@value #JNDI_PROPERTY_SOURCE_NAME}
	 * </ul>
	 * <p>{@value #SERVLET_CONFIG_PROPERTY_SOURCE_NAME}中的属性优先于
	 * {@value #SERVLET_CONTEXT_PROPERTY_SOURCE_NAME}中的属性,
	 * 上述任一项中的属性优先于{@value #JNDI_PROPERTY_SOURCE_NAME}中的属性.
	 * <p>上述任何属性都将优先于{@link StandardEnvironment}超类提供的系统属性和环境变量.
	 * <p>{@code Servlet}相关属性源在此阶段添加为{@link StubPropertySource 存根},
	 * 一旦实际的{@link ServletContext}对象可用, 
	 * 将{@linkplain #initPropertySources(ServletContext, ServletConfig) 完全初始化}.
	 */
	@Override
	protected void customizePropertySources(MutablePropertySources propertySources) {
		propertySources.addLast(new StubPropertySource(SERVLET_CONFIG_PROPERTY_SOURCE_NAME));
		propertySources.addLast(new StubPropertySource(SERVLET_CONTEXT_PROPERTY_SOURCE_NAME));
		if (JndiLocatorDelegate.isDefaultJndiEnvironmentAvailable()) {
			propertySources.addLast(new JndiPropertySource(JNDI_PROPERTY_SOURCE_NAME));
		}
		super.customizePropertySources(propertySources);
	}

	@Override
	public void initPropertySources(ServletContext servletContext, ServletConfig servletConfig) {
		WebApplicationContextUtils.initServletPropertySources(getPropertySources(), servletContext, servletConfig);
	}

}
