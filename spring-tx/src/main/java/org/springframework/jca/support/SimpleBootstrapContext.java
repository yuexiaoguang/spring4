package org.springframework.jca.support;

import java.util.Timer;
import javax.resource.spi.BootstrapContext;
import javax.resource.spi.UnavailableException;
import javax.resource.spi.XATerminator;
import javax.resource.spi.work.WorkManager;

/**
 * JCA 1.5 {@link javax.resource.spi.BootstrapContext}接口的简单实现, 用于在本地环境中引导JCA ResourceAdapter.
 *
 * <p>委托给给定的WorkManager和XATerminator. 创建{@code java.util.Timer}的简单本地实例.
 */
public class SimpleBootstrapContext implements BootstrapContext {

	private WorkManager workManager;

	private XATerminator xaTerminator;


	/**
	 * @param workManager 要使用的JCA WorkManager (may be {@code null})
	 */
	public SimpleBootstrapContext(WorkManager workManager) {
		this.workManager = workManager;
	}

	/**
	 * @param workManager 要使用的JCA WorkManager (may be {@code null})
	 * @param xaTerminator 要使用的JCA XATerminator (may be {@code null})
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
