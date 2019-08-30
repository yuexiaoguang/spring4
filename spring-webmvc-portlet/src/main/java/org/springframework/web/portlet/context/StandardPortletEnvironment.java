package org.springframework.web.portlet.context;

import javax.portlet.PortletConfig;
import javax.portlet.PortletContext;
import javax.servlet.ServletContext;

import org.springframework.core.env.Environment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.PropertySource.StubPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.jndi.JndiLocatorDelegate;
import org.springframework.jndi.JndiPropertySource;
import org.springframework.web.context.support.StandardServletEnvironment;

/**
 * 基于{@code Servlet}的Web应用程序使用的{@link Environment}实现.
 * 所有与Portlet相关的{@code ApplicationContext}类都默认初始化一个实例.
 *
 * <p>贡献{@code ServletContext}, {@code PortletContext}, {@code PortletConfig}和基于JNDI的{@link PropertySource}实例.
 * 有关详细信息, 请参阅{@link #customizePropertySources}方法.
 */
public class StandardPortletEnvironment extends StandardEnvironment {

	/** Portlet上下文init参数属性源名称: {@value} */
	public static final String PORTLET_CONTEXT_PROPERTY_SOURCE_NAME = "portletContextInitParams";

	/** Portlet配置init参数属性源名称: {@value} */
	public static final String PORTLET_CONFIG_PROPERTY_SOURCE_NAME = "portletConfigInitParams";


	/**
	 * 使用超类提供的属性源以及适用于基于标准portlet的环境的属性源自定义属性源集:
	 * <ul>
	 * <li>{@value #PORTLET_CONFIG_PROPERTY_SOURCE_NAME}
	 * <li>{@value #PORTLET_CONTEXT_PROPERTY_SOURCE_NAME}
	 * <li>{@linkplain StandardServletEnvironment#SERVLET_CONTEXT_PROPERTY_SOURCE_NAME "servletContextInitParams"}
	 * <li>{@linkplain StandardServletEnvironment#JNDI_PROPERTY_SOURCE_NAME "jndiProperties"}
	 * </ul>
	 * <p>{@value #PORTLET_CONFIG_PROPERTY_SOURCE_NAME}中的属性将优先于{@value #PORTLET_CONTEXT_PROPERTY_SOURCE_NAME}中的属性,
	 * 该属性优先于
	 * {@linkplain StandardServletEnvironment#SERVLET_CONTEXT_PROPERTY_SOURCE_NAME "servletContextInitParams"}中的属性,
	 * 依此类推.
	 * <p>上述任何属性都将优先于{@link StandardEnvironment}超类提供的系统属性和环境变量.
	 * <p>属性源暂时添加为存根, 一旦实际的{@link PortletConfig}, {@link PortletContext}, 和{@link ServletContext}对象可用,
	 * 将{@linkplain PortletApplicationContextUtils#initPortletPropertySources 完全初始化}.
	 */
	@Override
	protected void customizePropertySources(MutablePropertySources propertySources) {
		propertySources.addLast(new StubPropertySource(PORTLET_CONFIG_PROPERTY_SOURCE_NAME));
		propertySources.addLast(new StubPropertySource(PORTLET_CONTEXT_PROPERTY_SOURCE_NAME));
		propertySources.addLast(new StubPropertySource(StandardServletEnvironment.SERVLET_CONTEXT_PROPERTY_SOURCE_NAME));
		if (JndiLocatorDelegate.isDefaultJndiEnvironmentAvailable()) {
			propertySources.addLast(new JndiPropertySource(StandardServletEnvironment.JNDI_PROPERTY_SOURCE_NAME));
		}
		super.customizePropertySources(propertySources);
	}

	/**
	 * 使用给定参数替换任何充当真实portlet上下文/配置属性源的占位符的
	 * {@linkplain org.springframework.core.env.PropertySource.StubPropertySource 存根属性源}实例.
	 * 
	 * @param servletContext  {@link ServletContext} (may be {@code null})
	 * @param portletContext  {@link PortletContext} (may not be {@code null})
	 * @param portletConfig  {@link PortletConfig} ({@code null} if not available)
	 */
	public void initPropertySources(ServletContext servletContext, PortletContext portletContext, PortletConfig portletConfig) {
		PortletApplicationContextUtils.initPortletPropertySources(getPropertySources(), servletContext, portletContext, portletConfig);
	}

}
