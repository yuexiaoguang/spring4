package org.springframework.web.context.support;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.ui.context.Theme;
import org.springframework.ui.context.ThemeSource;
import org.springframework.ui.context.support.UiApplicationContextUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.context.ConfigurableWebApplicationContext;
import org.springframework.web.context.ConfigurableWebEnvironment;
import org.springframework.web.context.ServletContextAware;

/**
 * {@link GenericApplicationContext}的子类, 适用于Web环境.
 *
 * <p>实现{@link org.springframework.web.context.ConfigurableWebApplicationContext},
 * 但不用于{@code web.xml}中的声明性设置.
 * 相反, 它是为程序设置而设计的, 例如用于构建嵌套上下文或在Spring 3.1 {@link org.springframework.web.WebApplicationInitializer}中使用.
 *
 * <p><b>如果实现从配置文件中读取bean定义的WebApplicationContext, 考虑从AbstractRefreshableWebApplicationContext派生,
 * 在{@code loadBeanDefinitions}方法的实现中读取bean定义.</b>
 *
 * <p>将资源路径解释为servlet上下文资源, i.e. 作为Web应用程序根目录下的路径.
 * 绝对路径, e.g. 对于Web应用程序根目录之外的文件, 可以通过"file:" URL访问, 由AbstractApplicationContext实现.
 *
 * <p>除了{@link org.springframework.context.support.AbstractApplicationContext}检测到的特殊bean之外,
 * 此类还检测上下文中的ThemeSource bean, 名称为"themeSource".
 */
public class GenericWebApplicationContext extends GenericApplicationContext
		implements ConfigurableWebApplicationContext, ThemeSource {

	private ServletContext servletContext;

	private ThemeSource themeSource;


	public GenericWebApplicationContext() {
		super();
	}

	/**
	 * @param servletContext 要运行的ServletContext
	 */
	public GenericWebApplicationContext(ServletContext servletContext) {
		this.servletContext = servletContext;
	}

	/**
	 * @param beanFactory 用于此上下文的DefaultListableBeanFactory实例
	 */
	public GenericWebApplicationContext(DefaultListableBeanFactory beanFactory) {
		super(beanFactory);
	}

	/**
	 * @param beanFactory 用于此上下文的DefaultListableBeanFactory实例
	 * @param servletContext 要运行的ServletContext
	 */
	public GenericWebApplicationContext(DefaultListableBeanFactory beanFactory, ServletContext servletContext) {
		super(beanFactory);
		this.servletContext = servletContext;
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
	public String getApplicationName() {
		return (this.servletContext != null ? this.servletContext.getContextPath() : "");
	}

	/**
	 * 创建并返回{@link StandardServletEnvironment}.
	 */
	@Override
	protected ConfigurableEnvironment createEnvironment() {
		return new StandardServletEnvironment();
	}

	/**
	 * 注册ServletContextAwareProcessor.
	 */
	@Override
	protected void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
		beanFactory.addBeanPostProcessor(new ServletContextAwareProcessor(this.servletContext));
		beanFactory.ignoreDependencyInterface(ServletContextAware.class);

		WebApplicationContextUtils.registerWebApplicationScopes(beanFactory, this.servletContext);
		WebApplicationContextUtils.registerEnvironmentBeans(beanFactory, this.servletContext);
	}

	/**
	 * 此实现支持ServletContext根目录下的文件路径.
	 */
	@Override
	protected Resource getResourceByPath(String path) {
		return new ServletContextResource(this.servletContext, path);
	}

	/**
	 * 此实现也支持未展开的WAR中的模式匹配.
	 */
	@Override
	protected ResourcePatternResolver getResourcePatternResolver() {
		return new ServletContextResourcePatternResolver(this);
	}

	/**
	 * 初始化主题功能.
	 */
	@Override
	protected void onRefresh() {
		this.themeSource = UiApplicationContextUtils.initThemeSource(this);
	}

	/**
	 * {@inheritDoc}
	 * <p>替换{@code Servlet}相关的属性源.
	 */
	@Override
	protected void initPropertySources() {
		ConfigurableEnvironment env = getEnvironment();
		if (env instanceof ConfigurableWebEnvironment) {
			((ConfigurableWebEnvironment) env).initPropertySources(this.servletContext, null);
		}
	}

	@Override
	public Theme getTheme(String themeName) {
		return this.themeSource.getTheme(themeName);
	}


	// ---------------------------------------------------------------------
	// Pseudo-implementation of ConfigurableWebApplicationContext
	// ---------------------------------------------------------------------

	@Override
	public void setServletConfig(ServletConfig servletConfig) {
		// no-op
	}

	@Override
	public ServletConfig getServletConfig() {
		throw new UnsupportedOperationException(
				"GenericWebApplicationContext does not support getServletConfig()");
	}

	@Override
	public void setNamespace(String namespace) {
		// no-op
	}

	@Override
	public String getNamespace() {
		throw new UnsupportedOperationException(
				"GenericWebApplicationContext does not support getNamespace()");
	}

	@Override
	public void setConfigLocation(String configLocation) {
		if (StringUtils.hasText(configLocation)) {
			throw new UnsupportedOperationException(
					"GenericWebApplicationContext does not support setConfigLocation(). " +
					"Do you still have an 'contextConfigLocations' init-param set?");
		}
	}

	@Override
	public void setConfigLocations(String... configLocations) {
		if (!ObjectUtils.isEmpty(configLocations)) {
			throw new UnsupportedOperationException(
					"GenericWebApplicationContext does not support setConfigLocations(). " +
					"Do you still have an 'contextConfigLocations' init-param set?");
		}
	}

	@Override
	public String[] getConfigLocations() {
		throw new UnsupportedOperationException(
				"GenericWebApplicationContext does not support getConfigLocations()");
	}

}
