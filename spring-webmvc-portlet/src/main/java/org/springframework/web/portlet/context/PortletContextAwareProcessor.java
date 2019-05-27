package org.springframework.web.portlet.context;

import javax.portlet.PortletConfig;
import javax.portlet.PortletContext;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

/**
 * {@link org.springframework.beans.factory.config.BeanPostProcessor}
 * implementation that passes the PortletContext to beans that implement
 * the {@link PortletContextAware} interface.
 *
 * <p>Portlet application contexts will automatically register this with their
 * underlying bean factory. Applications do not use this directly.
 */
public class PortletContextAwareProcessor implements BeanPostProcessor {

	private PortletContext portletContext;

	private PortletConfig portletConfig;


	/**
	 * Create a new PortletContextAwareProcessor for the given context.
	 */
	public PortletContextAwareProcessor(PortletContext portletContext) {
		this(portletContext, null);
	}

	/**
	 * Create a new PortletContextAwareProcessor for the given config.
	 */
	public PortletContextAwareProcessor(PortletConfig portletConfig) {
		this(null, portletConfig);
	}

	/**
	 * Create a new PortletContextAwareProcessor for the given context and config.
	 */
	public PortletContextAwareProcessor(PortletContext portletContext, PortletConfig portletConfig) {
		this.portletContext = portletContext;
		this.portletConfig = portletConfig;
		if (portletContext == null && portletConfig != null) {
			this.portletContext = portletConfig.getPortletContext();
		}
	}


	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		if (this.portletContext != null && bean instanceof PortletContextAware) {
			((PortletContextAware) bean).setPortletContext(this.portletContext);
		}
		if (this.portletConfig != null && bean instanceof PortletConfigAware) {
			((PortletConfigAware) bean).setPortletConfig(this.portletConfig);
		}
		return bean;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) {
		return bean;
	}

}
