package org.springframework.web.portlet.context;

import javax.portlet.PortletConfig;
import javax.portlet.PortletContext;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

/**
 * {@link org.springframework.beans.factory.config.BeanPostProcessor}实现,
 * 将PortletContext传递给实现了{@link PortletContextAware}接口的bean.
 *
 * <p>Portlet应用程序上下文将自动将其注册到其底层bean工厂. 应用程序不直接使用它.
 */
public class PortletContextAwareProcessor implements BeanPostProcessor {

	private PortletContext portletContext;

	private PortletConfig portletConfig;


	public PortletContextAwareProcessor(PortletContext portletContext) {
		this(portletContext, null);
	}

	public PortletContextAwareProcessor(PortletConfig portletConfig) {
		this(null, portletConfig);
	}

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
