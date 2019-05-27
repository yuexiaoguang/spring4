package org.springframework.jca.support;

import java.util.Timer;
import javax.resource.spi.BootstrapContext;
import javax.resource.spi.UnavailableException;
import javax.resource.spi.XATerminator;
import javax.resource.spi.work.WorkManager;

/**
 * Simple implementation of the JCA 1.5 {@link javax.resource.spi.BootstrapContext}
 * interface, used for bootstrapping a JCA ResourceAdapter in a local environment.
 *
 * <p>Delegates to the given WorkManager and XATerminator, if any. Creates simple
 * local instances of {@code java.util.Timer}.
 */
public class SimpleBootstrapContext implements BootstrapContext {

	private WorkManager workManager;

	private XATerminator xaTerminator;


	/**
	 * Create a new SimpleBootstrapContext for the given WorkManager,
	 * with no XATerminator available.
	 * @param workManager the JCA WorkManager to use (may be {@code null})
	 */
	public SimpleBootstrapContext(WorkManager workManager) {
		this.workManager = workManager;
	}

	/**
	 * Create a new SimpleBootstrapContext for the given WorkManager and XATerminator.
	 * @param workManager the JCA WorkManager to use (may be {@code null})
	 * @param xaTerminator the JCA XATerminator to use (may be {@code null})
	 */
	public SimpleBootstrapContext(WorkManager workManager, XATerminator xaTerminator) {
		this.workManager = workManager;
		this.xaTerminator = xaTerminator;
	}


	@Override
	public WorkManager getWorkManager() {
		if (this.workManager == null) {
			throw new IllegalStateException("No WorkManager available");
		}
		return this.workManager;
	}

	@Override
	public XATerminator getXATerminator() {
		return this.xaTerminator;
	}

	@Override
	public Timer createTimer() throws UnavailableException {
		return new Timer();
	}

}
