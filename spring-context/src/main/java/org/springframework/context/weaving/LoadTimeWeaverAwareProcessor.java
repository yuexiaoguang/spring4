package org.springframework.context.weaving;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.instrument.classloading.LoadTimeWeaver;
import org.springframework.util.Assert;

/**
 * {@link org.springframework.beans.factory.config.BeanPostProcessor}实现,
 * 将上下文的默认{@link LoadTimeWeaver}传递给实现{@link LoadTimeWeaverAware}接口的bean.
 *
 * <p>{@link org.springframework.context.ApplicationContext Application contexts}
 * 将自动将其注册到它们的底层{@link BeanFactory bean factory},
 * 只要默认的{@code LoadTimeWeaver}实际可用.
 *
 * <p>应用程序不应直接使用此类.
 */
public class LoadTimeWeaverAwareProcessor implements BeanPostProcessor, BeanFactoryAware {

	private LoadTimeWeaver loadTimeWeaver;

	private BeanFactory beanFactory;


	/**
	 * 创建一个新的{@code LoadTimeWeaverAwareProcessor},
	 * 它将从{@link BeanFactory}的{@link LoadTimeWeaver}中自动检索,
	 * 期待一个名为{@link ConfigurableApplicationContext#LOAD_TIME_WEAVER_BEAN_NAME "loadTimeWeaver"}的bean.
	 */
	public LoadTimeWeaverAwareProcessor() {
	}

	/**
	 * <p>如果给定的{@code loadTimeWeaver}是{@code null}, 那么将从{@link BeanFactory}中自动检索{@code LoadTimeWeaver},
	 * 期待一个名为{@link ConfigurableApplicationContext#LOAD_TIME_WEAVER_BEAN_NAME "loadTimeWeaver"}的bean.
	 * 
	 * @param loadTimeWeaver 要使用的特定{@code LoadTimeWeaver}
	 */
	public LoadTimeWeaverAwareProcessor(LoadTimeWeaver loadTimeWeaver) {
		this.loadTimeWeaver = loadTimeWeaver;
	}

	/**
	 * <p>将从给定的{@link BeanFactory}中自动检索{@code LoadTimeWeaver},
	 * 期待一个名为{@link ConfigurableApplicationContext#LOAD_TIME_WEAVER_BEAN_NAME "loadTimeWeaver"}的bean.
	 * 
	 * @param beanFactory 从中检索LoadTimeWeaver的BeanFactory
	 */
	public LoadTimeWeaverAwareProcessor(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}


	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}


	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		if (bean instanceof LoadTimeWeaverAware) {
			LoadTimeWeaver ltw = this.loadTimeWeaver;
			if (ltw == null) {
				Assert.state(this.beanFactory != null,
						"BeanFactory required if no LoadTimeWeaver explicitly specified");
				ltw = this.beanFactory.getBean(
						ConfigurableApplicationContext.LOAD_TIME_WEAVER_BEAN_NAME, LoadTimeWeaver.class);
			}
			((LoadTimeWeaverAware) bean).setLoadTimeWeaver(ltw);
		}
		return bean;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String name) {
		return bean;
	}

}
