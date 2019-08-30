package org.springframework.web.portlet.context;

import javax.portlet.PortletConfig;
import javax.portlet.PortletContext;
import javax.servlet.ServletContext;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.AbstractRefreshableConfigApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.ServletContextAwareProcessor;

/**
 * {@link org.springframework.context.support.AbstractRefreshableApplicationContext}子类,
 * 为portlet环境实现{@link ConfigurablePortletApplicationContext}接口.
 * 提供"configLocations"属性, 通过Portlet应用程序启动时的ConfigurablePortletApplicationContext接口填充.
 *
 * <p>此类与AbstractRefreshableApplicationContext一样易于子类化:
 * 需要实现的只是{@link #loadBeanDefinitions}方法; 有关详细信息, 请参阅超类javadoc.
 * 请注意, 实现应该从{@link #getConfigLocations}方法返回的位置指定的文件中加载bean定义.
 *
 * <p>将资源路径解释为servlet上下文资源, i.e. 作为Web应用程序根目录下的路径.
 * 绝对路径, e.g. 对于Web应用程序根目录之外的文件, 可以通过"file:" URL访问,
 * 由{@link org.springframework.core.io.DefaultResourceLoader}实现.
 *
 * <p><b>这是要为不同的bean定义格式进行子类化的portlet上下文.</b>
 * 这样的上下文实现可以指定为FrameworkPortlet的"contextClass" init-param, 替换默认的{@link XmlPortletApplicationContext}.
 * 然后它将自动接收"contextConfigLocation" init-param.
 *
 * <p>请注意, 基于Portlet的上下文实现通常应根据通过{@link ConfigurablePortletApplicationContext}接口接收的配置进行自我配置.
 * 相反, 独立的应用程序上下文可能允许在自定义启动代码中进行配置
 * (例如, {@link org.springframework.context.support.GenericApplicationContext}).
 */
public abstract class AbstractRefreshablePortletApplicationContext extends AbstractRefreshableConfigApplicationContext
		implements WebApplicationContext, ConfigurablePortletApplicationContext {

	/** 此上下文运行的Servlet上下文 */
	private ServletContext servletContext;

	/** 此上下文运行的Portlet上下文 */
	private PortletContext portletContext;

	/** 此上下文运行的Portlet配置 */
	private PortletConfig portletConfig;

	/** 此上下文的命名空间, 如果是root, 则为null */
	private String namespace;


	public AbstractRefreshablePortletApplicationContext() {
		setDisplayName("Root PortletApplicationContext");
	}

	/**
	 * {@inheritDoc}
	 * <p>如果父级是{@link org.springframework.context.ConfigurableApplicationContext}实现,
	 * 则父级{@linkplain #getEnvironment() environment}被委托给此(子级)上下文.
	 * <p>如果父级是{@link WebApplicationContext}实现, 则父级{@linkplain #getServletContext() servlet上下文}被委托给此(子级)上下文.
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
			setDisplayName("PortletApplicationContext for namespace '" + namespace + "'");
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
		if (this.portletContext == null) {
			return "";
		}
		String name = this.portletContext.getPortletContextName();
		return (name != null ? name : "");
	}

	/**
	 * 创建并返回新的{@link StandardPortletEnvironment}.
	 */
	@Override
	protected ConfigurableEnvironment createEnvironment() {
		return new StandardPortletEnvironment();
	}

	/**
	 * 注册请求/会话范围, {@link PortletContextAwareProcessor}, etc.
	 */
	@Override
	protected void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
		beanFactory.addBeanPostProcessor(new ServletContextAwareProcessor(this.servletContext));
		beanFactory.addBeanPostProcessor(new PortletContextAwareProcessor(this.portletContext, this.portletConfig));
		beanFactory.ignoreDependencyInterface(ServletContextAware.class);
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

	@Override
	protected void customizeBeanFactory(DefaultListableBeanFactory beanFactory) {
		super.customizeBeanFactory(beanFactory);
	}

}
