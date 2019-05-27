package org.springframework.aop.framework.adapter;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

/**
 * 使用{@link AdvisorAdapterRegistry}(默认是 {@link GlobalAdvisorAdapterRegistry})
 * 在BeanFactory中注册{@link AdvisorAdapter} bean的BeanPostProcessor.
 *
 * <p>它运行的唯一要求是需要在应用程序上下文中定义需要由Spring的AOP框架“识别”的“非本地”Spring AdvisorAdapter.
 */
public class AdvisorAdapterRegistrationManager implements BeanPostProcessor {

	private AdvisorAdapterRegistry advisorAdapterRegistry = GlobalAdvisorAdapterRegistry.getInstance();


	/**
	 * 指定AdvisorAdapterRegistry以注册AdvisorAdapter bean.
	 * 默认是全局 AdvisorAdapterRegistry.
	 */
	public void setAdvisorAdapterRegistry(AdvisorAdapterRegistry advisorAdapterRegistry) {
		this.advisorAdapterRegistry = advisorAdapterRegistry;
	}


	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		return bean;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		if (bean instanceof AdvisorAdapter){
			this.advisorAdapterRegistry.registerAdvisorAdapter((AdvisorAdapter) bean);
		}
		return bean;
	}

}
