package org.springframework.jca.support;

import javax.resource.ResourceException;
import javax.resource.spi.BootstrapContext;
import javax.resource.spi.ResourceAdapter;
import javax.resource.spi.XATerminator;
import javax.resource.spi.work.WorkManager;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

/**
 * {@link org.springframework.beans.factory.FactoryBean}
 * 引导指定的JCA 1.7 {@link javax.resource.spi.ResourceAdapter},
 * 以本地{@link javax.resource.spi.BootstrapContext}启动它, 并公开它用于bean引用.
 * 它还将在上下文关闭时停止ResourceAdapter. 根据JCA 1.7规范, 这对应于本地环境中的'非托管'引导程序.
 *
 * <p>这本质上是一个适用于JCA ResourceAdapter的bean风格引导的适配器,
 * 允许通过bean属性指定BootstrapContext或其元素 (例如JCA WorkManager).
 */
public class ResourceAdapterFactoryBean implements FactoryBean<ResourceAdapter>, InitializingBean, DisposableBean {

	private ResourceAdapter resourceAdapter;

	private BootstrapContext bootstrapContext;

	private WorkManager workManager;

	private XATerminator xaTerminator;


	/**
	 * 将目标JCA ResourceAdapter指定为类, 使用其默认配置进行实例化.
	 * <p>或者, 通过"resourceAdapter"属性指定预配置的ResourceAdapter实例.
	 */
	public void setResourceAdapterClass(Class<? extends ResourceAdapter> resourceAdapterClass) {
		this.resourceAdapter = BeanUtils.instantiateClass(resourceAdapterClass);
	}

	/**
	 * 指定目标JCA ResourceAdapter, 作为尚未启动的已配置实例传入.
	 * 这通常作为内部bean定义发生, 通过其特定于供应商的bean属性配置ResourceAdapter实例.
	 */
	public void setResourceAdapter(ResourceAdapter resourceAdapter) {
		this.resourceAdapter = resourceAdapter;
	}

	/**
	 * 指定用于启动ResourceAdapter的JCA BootstrapContext.
	 * <p>或者, 可以将各个部件 (例如 JCA WorkManager) 指定为单独的引用.
	 */
	public void setBootstrapContext(BootstrapContext bootstrapContext) {
		this.bootstrapContext = bootstrapContext;
	}

	/**
	 * 指定用于引导ResourceAdapter的JCA WorkManager.
	 */
	public void setWorkManager(WorkManager workManager) {
		this.workManager = workManager;
	}

	/**
	 * 指定用于引导ResourceAdapter的JCA XATerminator.
	 */
	public void setXaTerminator(XATerminator xaTerminator) {
		this.xaTerminator = xaTerminator;
	}


	/**
	 * 构建BootstrapContext并使用它启动ResourceAdapter.
	 */
	@Override
	public void afterPropertiesSet() throws ResourceException {
		if (this.resourceAdapter == null) {
			throw new IllegalArgumentException("'resourceAdapter' or 'resourceAdapterClass' is required");
		}
		if (this.bootstrapContext == null) {
			this.bootstrapContext = new SimpleBootstrapContext(this.workManager, this.xaTerminator);
		}
		this.resourceAdapter.start(this.bootstrapContext);
	}


	@Override
	public ResourceAdapter getObject() {
		return this.resourceAdapter;
	}

	@Override
	public Class<? extends ResourceAdapter> getObjectType() {
		return (this.resourceAdapter != null ? this.resourceAdapter.getClass() : ResourceAdapter.class);
	}

	@Override
	public boolean isSingleton() {
		return true;
	}


	/**
	 * 停止ResourceAdapter.
	 */
	@Override
	public void destroy() {
		this.resourceAdapter.stop();
	}

}
