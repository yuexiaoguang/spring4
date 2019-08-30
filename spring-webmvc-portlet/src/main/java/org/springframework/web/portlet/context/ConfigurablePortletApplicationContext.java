package org.springframework.web.portlet.context;

import javax.portlet.PortletConfig;
import javax.portlet.PortletContext;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.web.context.WebApplicationContext;

/**
 * 由可配置的portlet应用程序上下文实现的接口.
 * 由{@link org.springframework.web.portlet.FrameworkPortlet}支持.
 *
 * <p>Note: 在调用从{@link org.springframework.context.ConfigurableApplicationContext}
 * 继承的{@link #refresh}方法之前, 需要调用此接口的setter.
 * 它们不会导致自己初始化上下文.
 */
public interface ConfigurablePortletApplicationContext
		extends WebApplicationContext, ConfigurableApplicationContext {

	/**
	 * 引用portlet名称的ApplicationContext id的前缀.
	 */
	String APPLICATION_CONTEXT_ID_PREFIX = WebApplicationContext.class.getName() + ":";

	/**
	 * 工厂中PortletContext环境bean的名称.
	 */
	String PORTLET_CONTEXT_BEAN_NAME = "portletContext";

	/**
	 * 工厂中PortletConfig环境bean的名称.
	 */
	String PORTLET_CONFIG_BEAN_NAME = "portletConfig";


	/**
	 * 设置此portlet应用程序上下文的PortletContext.
	 * <p>不会导致上下文初始化: 需要在设置所有配置属性后调用refresh.
	 */
	void setPortletContext(PortletContext portletContext);

	/**
	 * 返回此应用程序的标准Portlet API PortletContext.
	 */
	PortletContext getPortletContext();

	/**
	 * 设置此portlet应用程序上下文的PortletConfig.
	 */
	void setPortletConfig(PortletConfig portletConfig);

	/**
	 * 返回此portlet应用程序上下文的PortletConfig.
	 */
	PortletConfig getPortletConfig();

	/**
	 * 设置此portlet应用程序上下文的命名空间, 用于构建默认上下文配置位置.
	 */
	void setNamespace(String namespace);

	/**
	 * 返回此Web应用程序上下文的命名空间.
	 */
	String getNamespace();

	/**
	 * 以init-param样式设置此portlet应用程序上下文的配置位置, i.e. 使用逗号, 分号, 或空格分隔的不同位置.
	 * <p>如果未设置, 则实现应该使用给定命名空间的默认值.
	 */
	void setConfigLocation(String configLocation);

	/**
	 * 设置此portlet应用程序上下文的配置位置.
	 * <p>如果未设置, 则实现应该使用给定命名空间的默认值.
	 */
	void setConfigLocations(String... configLocations);

	/**
	 * 返回此Web应用程序上下文的配置位置, 或{@code null}.
	 */
	String[] getConfigLocations();

}
