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
 * {@link org.springframework.beans.factory.FactoryBean} that bootstraps
 * the specified JCA 1.7 {@link javax.resource.spi.ResourceAdapter},
 * starting it with a local {@link javax.resource.spi.BootstrapContext}
 * and exposing it for bean references. It will also stop the ResourceAdapter
 * on context shutdown. This corresponds to 'non-managed' bootstrap in a
 * local environment, according to the JCA 1.7 specification.
 *
 * <p>This is essentially an adapter for bean-style bootstrapping of a
 * JCA ResourceAdapter, allowing the BootstrapContext or its elements
 * (such as the JCA WorkManager) to be specified through bean properties.
 */
public class ResourceAdapterFactoryBean implements FactoryBean<ResourceAdapter>, InitializingBean, DisposableBean {

	private ResourceAdapter resourceAdapter;

	private BootstrapContext bootstrapContext;

	private WorkManager workManager;

	private XATerminator xaTerminator;


	/**
	 * Specify the target JCA ResourceAdapter as class, to be instantiated
	 * with its default configuration.
	 * <p>Alternatively, specify a pre-configured ResourceAdapter instance
	 * through the "resourceAdapter" property.
	 * @see #setResourceAdapter
	 */
	public void setResourceAdapterClass(Class<? extends ResourceAdapter> resourceAdapterClass) {
		this.resourceAdapter = BeanUtils.instantiateClass(resourceAdapterClass);
	}

	/**
	 * Specify the target JCA ResourceAdapter, passed in as configured instance
	 * which hasn't been started yet. This will typically happen as an
	 * inner bean definition, configuring the ResourceAdapter instance
	 * through its vendor-specific bean properties.
	 */
	public void setResourceAdapter(ResourceAdapter resourceAdapter) {
		this.resourceAdapter = resourceAdapter;
	}

	/**
	 * Specify the JCA BootstrapContext to use for starting the ResourceAdapter.
	 * <p>Alternatively, you can specify the individual parts (such as the
	 * JCA WorkManager) as individual references.
	 * @see #setWorkManager
	 * @see #setXaTerminator
	 */
	public void setBootstrapContext(BootstrapContext bootstrapContext) {
		this.bootstrapContext = bootstrapContext;
	}

	/**
	 * Specify the JCA WorkManager to use for bootstrapping the ResourceAdapter.
	 * @see #setBootstrapContext
	 */
	public void setWorkManager(WorkManager workManager) {
		this.workManager = workManager;
	}

	/**
	 * Specify the JCA XATerminator to use for bootstrapping the ResourceAdapter.
	 * @see #setBootstrapContext
	 */
	public void setXaTerminator(XATerminator xaTerminator) {
		this.xaTerminator = xaTerminator;
	}


	/**
	 * Builds the BootstrapContext and starts the ResourceAdapter with it.
	 * @see javax.resource.spi.ResourceAdapter#start(javax.resource.spi.BootstrapContext)
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
	 * Stops the ResourceAdapter.
	 * @see javax.resource.spi.ResourceAdapter#stop()
	 */
	@Override
	public void destroy() {
		this.resourceAdapter.stop();
	}

}
