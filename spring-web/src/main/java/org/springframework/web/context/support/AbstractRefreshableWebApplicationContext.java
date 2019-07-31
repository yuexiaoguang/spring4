package org.springframework.web.context.support;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.support.AbstractRefreshableConfigApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.ui.context.Theme;
import org.springframework.ui.context.ThemeSource;
import org.springframework.ui.context.support.UiApplicationContextUtils;
import org.springframework.web.context.ConfigurableWebApplicationContext;
import org.springframework.web.context.ConfigurableWebEnvironment;
import org.springframework.web.context.ServletConfigAware;
import org.springframework.web.context.ServletContextAware;

/**
 * {@link org.springframework.context.support.AbstractRefreshableApplicationContext}子类,
 * 为Web环境实现{@link org.springframework.web.context.ConfigurableWebApplicationContext}接口.
 * 提供"configLocations"属性, 通过Web应用程序启动时的ConfigurableWebApplicationContext接口填充.
 *
 * <p>此类与AbstractRefreshableApplicationContext一样易于子类化:
 * 需要实现的只是{@link #loadBeanDefinitions}方法; 有关详细信息, 请参阅超类javadoc.
 * 请注意, 实现应该从{@link #getConfigLocations}方法返回的位置指定的文件中加载bean定义.
 *
 * <p>将资源路径解释为servlet上下文资源, i.e. 作为Web应用程序根目录下的路径.
 * 绝对路径, e.g. 对于Web应用程序根目录之外的文件, 可以通过"file:" URL访问,
 * 由{@link org.springframework.core.io.DefaultResourceLoader}实现.
 *
 * <p>除了{@link org.springframework.context.support.AbstractApplicationContext}检测到的特殊bean之外,
 * 此类在上下文中检测类型为{@link org.springframework.ui.context.ThemeSource},
 * bean名称为"themeSource"的bean.
 *
 * <p><b>这是要为不同的bean定义格式进行子类化的Web上下文.</b>
 * 这样的上下文实现可以被指定为{@link org.springframework.web.context.ContextLoader}的"contextClass" context-param,
 * 或{@link org.springframework.web.servlet.FrameworkServlet}的"contextClass" init-param,
 * 替换默认的{@link XmlWebApplicationContext}.
 * 然后它将分别自动接收"contextConfigLocation" context-param或init-param.
 *
 * <p>请注意, WebApplicationContext实现通常应根据通过{@link ConfigurableWebApplicationContext}接口接收的配置进行自我配置.
 * 相反, 独立的应用程序上下文可能允许在自定义启动代码中进行配置
 * (例如, {@link org.springframework.context.support.GenericApplicationContext}).
 */
public abstract class AbstractRefreshableWebApplicationContext extends AbstractRefreshableConfigApplicationContext
		implements ConfigurableWebApplicationContext, ThemeSource {

	/** 此上下文运行的Servlet上下文 */
	private ServletContext servletContext;

	/** 此上下文运行的Servlet配置 */
	private ServletConfig servletConfig;

	/** 此上下文的命名空间, 如果是root, 则为{@code null} */
	private String namespace;

	/** 此ApplicationContext的ThemeSource */
	private ThemeSource themeSource;


	public AbstractRefreshableWebApplicationContext() {
		setDisplayName("Root WebApplicationContext");
	}


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
			setServletContext(servletConfig.getServletContext());
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

	@Override
	public String[] getConfigLocations() {
		return super.getConfigLocations();
	}

	@Override
	public String getApplicationName() {
		return (this.servletContext != null ? this.servletContext.getContextPath() : "");
	}

	/**
	 * 创建并返回{@link StandardServletEnvironment}.
	 * 子类可以覆盖以配置环境或细化返回的环境类型.
	 */
	@Override
	protected ConfigurableEnvironment createEnvironment() {
		return new StandardServletEnvironment();
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
			((ConfigurableWebEnvironment) env).initPropertySources(this.servletContext, this.servletConfig);
		}
	}

	@Override
	public Theme getTheme(String themeName) {
		return this.themeSource.getTheme(themeName);
	}
}
