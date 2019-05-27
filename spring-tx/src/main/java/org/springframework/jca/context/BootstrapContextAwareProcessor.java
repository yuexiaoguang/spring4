package org.springframework.jca.context;

import javax.resource.spi.BootstrapContext;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

/**
 * {@link org.springframework.beans.factory.config.BeanPostProcessor}
 * implementation that passes the BootstrapContext to beans that implement
 * the {@link BootstrapContextAware} interface.
 *
 * <p>{@link ResourceAdapterApplicationContext} automatically registers
 * this processor with its underlying bean factory.
 */
class BootstrapContextAwareProcessor implements BeanPostProcessor {

	private final BootstrapContext bootstrapContext;


	/**
	 * Create a new BootstrapContextAwareProcessor for the given context.
	 */
	public BootstrapContextAwareProcessor(BootstrapContext bootstrapContext) {
		this.bootstrapContext = bootstrapContext;
	}


	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		if (this.bootstrapContext != null && bean instanceof BootstrapContextAware) {
			((BootstrapContextAware) bean).setBootstrapContext(this.bootstrapContext);
		}
		return bean;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		return bean;
	}

}
