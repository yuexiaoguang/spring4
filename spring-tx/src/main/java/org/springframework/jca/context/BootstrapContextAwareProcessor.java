package org.springframework.jca.context;

import javax.resource.spi.BootstrapContext;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

/**
 * {@link org.springframework.beans.factory.config.BeanPostProcessor}实现,
 * 它将BootstrapContext传递给实现{@link BootstrapContextAware}接口的bean.
 *
 * <p>{@link ResourceAdapterApplicationContext}自动将此处理器注册到其底层bean工厂.
 */
class BootstrapContextAwareProcessor implements BeanPostProcessor {

	private final BootstrapContext bootstrapContext;


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
