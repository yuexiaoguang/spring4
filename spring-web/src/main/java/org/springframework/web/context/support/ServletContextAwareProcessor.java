package org.springframework.web.context.support;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.web.context.ServletConfigAware;
import org.springframework.web.context.ServletContextAware;

/**
 * {@link org.springframework.beans.factory.config.BeanPostProcessor}实现,
 * 它将ServletContext传递给实现{@link ServletContextAware}接口的bean.
 *
 * <p>Web应用程序上下文将自动将其注册到其底层bean工厂. 应用程序不直接使用它.
 */
public class ServletContextAwareProcessor implements BeanPostProcessor {

	private ServletContext servletContext;

	private ServletConfig servletConfig;


	/**
	 * 使用此构造函数时, 应重写{@link #getServletContext()}和/或{@link #getServletConfig()}方法.
	 */
	protected ServletContextAwareProcessor() {
	}

	public ServletContextAwareProcessor(ServletContext servletContext) {
		this(servletContext, null);
	}

	public ServletContextAwareProcessor(ServletConfig servletConfig) {
		this(null, servletConfig);
	}

	public ServletContextAwareProcessor(ServletContext servletContext, ServletConfig servletConfig) {
		this.servletContext = servletContext;
		this.servletConfig = servletConfig;
	}


	/**
	 * 返回要注入的{@link ServletContext}或{@code null}.
	 * 在后处理器注册后获得上下文时, 子类可以重写此方法.
	 */
	protected ServletContext getServletContext() {
		if (this.servletContext == null && getServletConfig() != null) {
			return getServletConfig().getServletContext();
		}
		return this.servletContext;
	}

	/**
	 * 返回要注入的{@link ServletContext}或{@code null}.
	 * 在后处理器注册后获得上下文时, 子类可以重写此方法.
	 */
	protected ServletConfig getServletConfig() {
		return this.servletConfig;
	}

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		if (getServletContext() != null && bean instanceof ServletContextAware) {
			((ServletContextAware) bean).setServletContext(getServletContext());
		}
		if (getServletConfig() != null && bean instanceof ServletConfigAware) {
			((ServletConfigAware) bean).setServletConfig(getServletConfig());
		}
		return bean;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) {
		return bean;
	}

}
