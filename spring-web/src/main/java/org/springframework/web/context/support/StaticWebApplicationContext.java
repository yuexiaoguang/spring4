package org.springframework.web.context.support;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.ui.context.Theme;
import org.springframework.ui.context.ThemeSource;
import org.springframework.ui.context.support.UiApplicationContextUtils;
import org.springframework.web.context.ConfigurableWebApplicationContext;
import org.springframework.web.context.ServletConfigAware;
import org.springframework.web.context.ServletContextAware;

/**
 * 静态{@link org.springframework.web.context.WebApplicationContext}实现, 用于测试.
 * 不适用于生产应用.
 *
 * <p>实现{@link org.springframework.web.context.ConfigurableWebApplicationContext}接口,
 * 以允许直接替换{@link XmlWebApplicationContext}, 尽管实际上不支持外部配置文件.
 *
 * <p>将资源路径解释为servlet上下文资源, i.e. 作为Web应用程序根目录下的路径.
 * 绝对路径, e.g. 对于Web应用程序根目录之外的文件, 可以通过"file:" URL访问,
 * 由{@link org.springframework.core.io.DefaultResourceLoader}实现.
 *
 * <p>除了{@link org.springframework.context.support.AbstractApplicationContext}检测到的特殊bean之外,
 * 此类还在上下文中检测{@link org.springframework.ui.context.ThemeSource}类型,
 * bean名称为"themeSource"的bean.
 */
public class StaticWebApplicationContext extends StaticApplicationContext
		implements ConfigurableWebApplicationContext, ThemeSource {

	private ServletContext servletContext;

	private ServletConfig servletConfig;

	private String namespace;

	private ThemeSource themeSource;


	public StaticWebApplicationContext() {
		setDisplayName("Root WebApplicationContext");
	}


	/**
	 * 设置此WebApplicationContext运行的ServletContext.
	 */
	@Override
	public void setServletContext(ServletContext servletContext) {
		this.servletContext = servletContext;
	}

	@Override
	public ServletContext getServletContext() {
		return this.servletContext;
	}

	@Override
	public void setServletConfig(ServletConfig servletConfig) {
		this.servletConfig = servletConfig;
		if (servletConfig != null && this.servletContext == null) {
			this.servletContext = servletConfig.getServletContext();
		}
	}

	@Override
	public ServletConfig getServletConfig() {
		return this.servletConfig;
	}

	@Override
	public void setNamespace(String namespace) {
		this.namespace = namespace;
		if (namespace != null) {
			setDisplayName("WebApplicationContext for namespace '" + namespace + "'");
		}
	}

	@Override
	public String getNamespace() {
		return this.namespace;
	}

	/**
	 * {@link StaticWebApplicationContext}类不支持此方法.
	 * 
	 * @throws UnsupportedOperationException <b>始终抛出</b>
	 */
	@Override
	public void setConfigLocation(String configLocation) {
		if (configLocation != null) {
			throw new UnsupportedOperationException("StaticWebApplicationContext does not support config locations");
		}
	}

	/**
	 * {@link StaticWebApplicationContext}类不支持此方法.
	 * 
	 * @throws UnsupportedOperationException <b>始终抛出</b>
	 */
	@Override
	public void setConfigLocations(String... configLocations) {
		if (configLocations != null) {
			throw new UnsupportedOperationException("StaticWebApplicationContext does not support config locations");
		}
	}

	@Override
	public String[] getConfigLocations() {
		return null;
	}


	/**
	 * 注册request/session范围, {@link ServletContextAwareProcessor}, etc.
	 */
	@Override
	protected void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
		beanFactory.addBeanPostProcessor(new ServletContextAwareProcessor(this.servletContext, this.servletConfig));
		beanFactory.ignoreDependencyInterface(ServletContextAware.class);
		beanFactory.ignoreDependencyInterface(ServletConfigAware.class);

		WebApplicationContextUtils.registerWebApplicationScopes(beanFactory, this.servletContext);
		WebApplicationContextUtils.registerEnvironmentBeans(beanFactory, this.servletContext, this.servletConfig);
	}

	/**
	 * 此实现支持ServletContext根目录下的文件路径.
	 */
	@Override
	protected Resource getResourceByPath(String path) {
		return new ServletContextResource(this.servletContext, path);
	}

	/**
	 * 此实现也支持匹配未展开的WAR中的模式.
	 */
	@Override
	protected ResourcePatternResolver getResourcePatternResolver() {
		return new ServletContextResourcePatternResolver(this);
	}

	/**
	 * 创建并返回{@link StandardServletEnvironment}.
	 */
	@Override
	protected ConfigurableEnvironment createEnvironment() {
		return new StandardServletEnvironment();
	}

	/**
	 * 初始化主题功能.
	 */
	@Override
	protected void onRefresh() {
		this.themeSource = UiApplicationContextUtils.initThemeSource(this);
	}

	@Override
	protected void initPropertySources() {
		WebApplicationContextUtils.initServletPropertySources(getEnvironment().getPropertySources(),
				this.servletContext, this.servletConfig);
	}

	@Override
	public Theme getTheme(String themeName) {
		return this.themeSource.getTheme(themeName);
	}
}
