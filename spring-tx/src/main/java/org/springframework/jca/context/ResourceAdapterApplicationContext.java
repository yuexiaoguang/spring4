package org.springframework.jca.context;

import javax.resource.spi.BootstrapContext;
import javax.resource.spi.work.WorkManager;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.util.Assert;

/**
 * JCA ResourceAdapter的{@link org.springframework.context.ApplicationContext}实现.
 * 需要使用JCA {@link javax.resource.spi.BootstrapContext}进行初始化,
 * 并将其传递给实现{@link BootstrapContextAware}的Spring托管bean.
 */
public class ResourceAdapterApplicationContext extends GenericApplicationContext {

	private final BootstrapContext bootstrapContext;


	/**
	 * @param bootstrapContext ResourceAdapter已启动的JCA BootstrapContext
	 */
	public ResourceAdapterApplicationContext(BootstrapContext bootstrapContext) {
		Assert.notNull(bootstrapContext, "BootstrapContext must not be null");
		this.bootstrapContext = bootstrapContext;
	}


	@Override
	protected void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		beanFactory.addBeanPostProcessor(new BootstrapContextAwareProcessor(this.bootstrapContext));
		beanFactory.ignoreDependencyInterface(BootstrapContextAware.class);
		beanFactory.registerResolvableDependency(BootstrapContext.class, this.bootstrapContext);

		// JCA WorkManager延迟解析 - 可能无法使用.
		beanFactory.registerResolvableDependency(WorkManager.class, new ObjectFactory<WorkManager>() {
			@Override
			public WorkManager getObject() {
				return bootstrapContext.getWorkManager();
			}
		});
	}

}
