package org.springframework.util;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * 限制对特定资源的并发访问的支持类.
 *
 * <p>设计用作基类，子类在其工作流的适当位置调用{@link #beforeAccess()}和{@link #afterAccess()}方法.
 * 请注意, {@code afterAccess}通常应在finally块中调用!
 *
 * <p>此支持类的默认并发限制为 -1 ("无限并发").
 * 子类可以覆盖此默认值; 检查您正在使用的具体类的javadoc.
 */
@SuppressWarnings("serial")
public abstract class ConcurrencyThrottleSupport implements Serializable {

	/**
	 * 允许任意数量的并发调用: 即不要限制并发.
	 */
	public static final int UNBOUNDED_CONCURRENCY = -1;

	/**
	 * 切换并发'关': 即不允许任何并发调用.
	 */
	public static final int NO_CONCURRENCY = 0;


	/** 优化序列化 */
	protected transient Log logger = LogFactory.getLog(getClass());

	private transient Object monitor = new Object();

	private int concurrencyLimit = UNBOUNDED_CONCURRENCY;

	private int concurrencyCount = 0;


	/**
	 * 设置允许的最大并发访问尝试次数.
	 * -1表示无限并发.
	 * <p>原则上, 此限制可以在运行时更改, 但通常设计为配置时间设置.
	 * <p>NOTE: 不要在运行时在-1和任何具体限制之间切换, 因为这会导致并发计数不一致:
	 * -1的限制有效地完全关闭了并发计数.
	 */
	public void setConcurrencyLimit(int concurrencyLimit) {
		this.concurrencyLimit = concurrencyLimit;
	}

	/**
	 * 返回允许的最大并发访问尝试次数.
	 */
	public int getConcurrencyLimit() {
		return this.concurrencyLimit;
	}

	/**
	 * 返回此限制当前是否有效.
	 * 
	 * @return {@code true} 如果此实例的并发限制处于活动状态
	 */
	public boolean isThrottleActive() {
		return (this.concurrencyLimit >= 0);
	}


	/**
	 * 在具体子类的主执行逻辑之前调用.
	 * <p>此实现应用并发限制.
	 */
	protected void beforeAccess() {
		if (this.concurrencyLimit == NO_CONCURRENCY) {
			throw new IllegalStateException(
					"Currently no invocations allowed - concurrency limit set to NO_CONCURRENCY");
		}
		if (this.concurrencyLimit > 0) {
			boolean debug = logger.isDebugEnabled();
			synchronized (this.monitor) {
				boolean interrupted = false;
				while (this.concurrencyCount >= this.concurrencyLimit) {
					if (interrupted) {
						throw new IllegalStateException("Thread was interrupted while waiting for invocation access, " +
								"but concurrency limit still does not allow for entering");
					}
					if (debug) {
						logger.debug("Concurrency count " + this.concurrencyCount +
								" has reached limit " + this.concurrencyLimit + " - blocking");
					}
					try {
						this.monitor.wait();
					}
					catch (InterruptedException ex) {
						// 重新中断当前线程, 以允许其他线程做出反应.
						Thread.currentThread().interrupt();
						interrupted = true;
					}
				}
				if (debug) {
					logger.debug("Entering throttle at concurrency count " + this.concurrencyCount);
				}
				this.concurrencyCount++;
			}
		}
	}

	/**
	 * 在具体子类的主执行逻辑之后调用.
	 */
	protected void afterAccess() {
		if (this.concurrencyLimit >= 0) {
			synchronized (this.monitor) {
				this.concurrencyCount--;
				if (logger.isDebugEnabled()) {
					logger.debug("Returning from throttle at concurrency count " + this.concurrencyCount);
				}
				this.monitor.notify();
			}
		}
	}


	//---------------------------------------------------------------------
	// Serialization support
	//---------------------------------------------------------------------

	private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
		// 依赖于默认序列化, 只需在反序列化后初始化状态.
		ois.defaultReadObject();

		// Initialize transient fields.
		this.logger = LogFactory.getLog(getClass());
		this.monitor = new Object();
	}

}
