package org.springframework.jca.context;

import javax.resource.spi.BootstrapContext;

import org.springframework.beans.factory.Aware;

/**
 * Interface to be implemented by any object that wishes to be
 * notified of the BootstrapContext (typically determined by the
 * {@link ResourceAdapterApplicationContext}) that it runs in.
 */
public interface BootstrapContextAware extends Aware {

	/**
	 * Set the BootstrapContext that this object runs in.
	 * <p>Invoked after population of normal bean properties but before an init
	 * callback like InitializingBean's {@code afterPropertiesSet} or a
	 * custom init-method. Invoked after ApplicationContextAware's
	 * {@code setApplicationContext}.
	 * @param bootstrapContext BootstrapContext object to be used by this object
	 */
	void setBootstrapContext(BootstrapContext bootstrapContext);

}
