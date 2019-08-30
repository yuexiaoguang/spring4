package org.springframework.web.portlet.context;

import javax.portlet.PortletConfig;
import javax.portlet.PortletContext;
import javax.servlet.ServletContext;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.ServletContextAwareProcessor;

/**
 * 基于静态Portlet的{@link org.springframework.context.ApplicationContext}实现, 用于测试.
 * 不适用于生产应用.
 *
 * <p>实现
 * {@link org.springframework.web.portlet.context.ConfigurablePortletApplicationContext}接口,
 * 以允许直接替换{@link XmlPortletApplicationContext}, 尽管实际上不支持外部配置文件.
 *
 * <p>将资源路径解释为portlet上下文资源, 即作为portlet应用程序根目录下的路径.
 * 绝对路径, 例如对于portlet应用程序根目录之外的文件, 可以通过"file:" URL访问,
 * 由{@link org.springframework.core.io.DefaultResourceLoader}实现.
 */
public class StaticPortletApplicationContext extends StaticApplicationContext
		implements ConfigurablePortletApplicationContext {

	private ServletContext servletContext;

	private PortletContext portletContext;

	private PortletConfig portletConfig;

	private String namespace;


	public StaticPortletApplicationContext() {
		setDisplayName("Root Portlet ApplicationContext");
	}


	/**
	 * 返回新的{@link StandardPortletEnvironment}
	 */
	@Override
	protected ConfigurableEnvironment createEnvironment() {
		return new StandardPortletEnvironment();
	}

	/**
	 * {@inheritDoc}
	 * <p>替换{@code Portlet}和{@code Servlet}相关的属性源.
	 */
	@Override
	protected void initPropertySources() {
		PortletApplicationContextUtils.initPortletPropertySources(getEnvironment().getPropertySources(),
				this.servletContext, this.portletContext, this.portletConfig);
	}

	/**
	 * {@inheritDoc}
	 * <p>如果父级是{@link org.springframework.context.ConfigurableApplicationContext}实现,
	 * 则父级{@linkplain #getEnvironment() 环境}被委托给此 (子级)上下文.
	 * <p>如果父级是{@link WebApplicationContext}实现, 则父级{@linkplain #getServletContext() servlet 上下文}被委托给此 (子级)上下文.
	 */
	@Override
	public void setParent(ApplicationContext parent) {
		super.setParent(parent);
		if (parent instanceof WebApplicationContext) {
			this.servletContext = ((WebApplicationContext) parent).getServletContext();
		}
	}

	@Override
	public ServletContext getServletContext() {
		return this.servletContext;
	}

	@Override
	public void setPortletContext(PortletContext portletContext) {
		this.portletContext = portletContext;
	}

	@Override
	public PortletContext getPortletContext() {
		return this.portletContext;
	}

	@Override
	public void setPortletConfig(PortletConfig portletConfig) {
		this.portletConfig = portletConfig;
		if (portletConfig != null && this.portletContext == null) {
			this.portletContext = portletConfig.getPortletContext();
		}
	}

	@Override
	public PortletConfig getPortletConfig() {
		return this.portletConfig;
	}

	@Override
	public void setNamespace(String namespace) {
		this.namespace = namespace;
		if (namespace != null) {
			setDisplayName("Portlet ApplicationContext for namespace '" + namespace + "'");
		}
	}

	@Override
	public String getNamespace() {
		return this.namespace;
	}

	/**
	 * {@link StaticPortletApplicationContext}类不支持此方法.
	 * 
	 * @throws UnsupportedOperationException <b>always</b>
	 */
	@Override
	public void setConfigLocation(String configLocation) {
		if (configLocation != null) {
			throw new UnsupportedOperationException("StaticPortletApplicationContext does not support config locations");
		}
	}

	/**
	 * {@link StaticPortletApplicationContext}类不支持此方法.
	 * 
	 * @throws UnsupportedOperationException <b>always</b>
	 */
	@Override
	public void setConfigLocations(String... configLocations) {
		if (configLocations != null) {
			throw new UnsupportedOperationException("StaticPortletApplicationContext does not support config locations");
		}
	}

	@Override
	public String[] getConfigLocations() {
		return null;
	}


	/**
	 * 注册请求/会话范围, {@link PortletContextAwareProcessor}, etc.
	 */
	@Override
	protected void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
		beanFactory.addBeanPostProcessor(new ServletContextAwareProcessor(this.servletContext));
		beanFactory.addBeanPostProcessor(new PortletContextAwareProcessor(this.portletContext, this.portletConfig));
		beanFactory.ignoreDependencyInterface(PortletContextAware.class);
		beanFactory.ignoreDependencyInterface(PortletConfigAware.class);

		PortletApplicationContextUtils.registerPortletApplicationScopes(beanFactory, this.portletContext);
		PortletApplicationContextUtils.registerEnvironmentBeans(
				beanFactory, this.servletContext, this.portletContext, this.portletConfig);
	}

	/**
	 * 此实现支持PortletContext根目录下的文件路径.
	 */
	@Override
	protected Resource getResourceByPath(String path) {
		return new PortletContextResource(this.portletContext, path);
	}

	/**
	 * 此实现也支持未展开的WAR中的模式匹配.
	 */
	@Override
	protected ResourcePatternResolver getResourcePatternResolver() {
		return new PortletContextResourcePatternResolver(this);
	}

}
