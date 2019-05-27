package org.springframework.scheduling.concurrent;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

/**
 * 允许以bean风格配置{@link java.util.concurrent.ThreadPoolExecutor}的JavaBean
 * (通过它的"corePoolSize", "maxPoolSize", "keepAliveSeconds", "queueCapacity"属性)
 * 并将其公开为其原生{@link java.util.concurrent.ExecutorService}类型的bean引用.
 *
 * <p>默认配置是核心池大小为1, 具有无限的最大池大小和无限的队列容量.
 * 这大致相当于{@link java.util.concurrent.Executors#newSingleThreadExecutor()}, 共享所有任务的单个线程.
 * 将{@link #setQueueCapacity "queueCapacity"}设置为0,
 * 模仿{@link java.util.concurrent.Executors#newCachedThreadPool()}, 立即将池中的线程扩展为可能非常高的数字.
 * 考虑在那时设置{@link #setMaxPoolSize "maxPoolSize"}, 以及可能更高的{@link #setCorePoolSize "corePoolSize"}
 * (另见{@link #setAllowCoreThreadTimeOut "allowCoreThreadTimeOut"}缩放模式).
 *
 * <p>或者, 可以使用构造函数注入直接设置{@link ThreadPoolExecutor}实例,
 * 或使用指向{@link java.util.concurrent.Executors}类的工厂方法定义.
 * <b>强烈建议特别是配置类中的普通{@code @Bean}方法, 其中此{@code FactoryBean}变体将强制您返回{@code FactoryBean}类型,
 * 而不是实际的{@code Executor}类型.</b>
 *
 * <p>如果需要基于时间的{@link java.util.concurrent.ScheduledExecutorService}, 请考虑{@link ScheduledExecutorFactoryBean}.
 */
@SuppressWarnings("serial")
public class ThreadPoolExecutorFactoryBean extends ExecutorConfigurationSupport
		implements FactoryBean<ExecutorService>, InitializingBean, DisposableBean {

	private int corePoolSize = 1;

	private int maxPoolSize = Integer.MAX_VALUE;

	private int keepAliveSeconds = 60;

	private boolean allowCoreThreadTimeOut = false;

	private int queueCapacity = Integer.MAX_VALUE;

	private boolean exposeUnconfigurableExecutor = false;

	private ExecutorService exposedExecutor;


	/**
	 * 设置ThreadPoolExecutor的核心池大小.
	 * 默认 1.
	 */
	public void setCorePoolSize(int corePoolSize) {
		this.corePoolSize = corePoolSize;
	}

	/**
	 * 设置ThreadPoolExecutor的最大池大小.
	 * 默认 {@code Integer.MAX_VALUE}.
	 */
	public void setMaxPoolSize(int maxPoolSize) {
		this.maxPoolSize = maxPoolSize;
	}

	/**
	 * 设置ThreadPoolExecutor的keep-alive秒数.
	 * 默认 60.
	 */
	public void setKeepAliveSeconds(int keepAliveSeconds) {
		this.keepAliveSeconds = keepAliveSeconds;
	}

	/**
	 * 指定是否允许核心线程超时.
	 * 即使与非零队列结合, 也可以实现动态增长和收缩 (因为一旦队列满, 最大池大小才会增长).
	 * <p>默认"false".
	 */
	public void setAllowCoreThreadTimeOut(boolean allowCoreThreadTimeOut) {
		this.allowCoreThreadTimeOut = allowCoreThreadTimeOut;
	}

	/**
	 * 设置ThreadPoolExecutor的BlockingQueue的容量.
	 * 默认{@code Integer.MAX_VALUE}.
	 * <p>正值都将导致LinkedBlockingQueue实例;
	 * 其他值都将导致SynchronousQueue实例.
	 */
	public void setQueueCapacity(int queueCapacity) {
		this.queueCapacity = queueCapacity;
	}

	/**
	 * 指定此FactoryBean是否应为创建的执行器公开不可配置的装饰器.
	 * <p>默认 "false", 将原始执行器公开为bean引用.
	 * 将此标志切换为 "true", 以严格阻止客户端修改执行器的配置.
	 */
	public void setExposeUnconfigurableExecutor(boolean exposeUnconfigurableExecutor) {
		this.exposeUnconfigurableExecutor = exposeUnconfigurableExecutor;
	}


	@Override
	protected ExecutorService initializeExecutor(
			ThreadFactory threadFactory, RejectedExecutionHandler rejectedExecutionHandler) {

		BlockingQueue<Runnable> queue = createQueue(this.queueCapacity);
		ThreadPoolExecutor executor  = createExecutor(this.corePoolSize, this.maxPoolSize,
				this.keepAliveSeconds, queue, threadFactory, rejectedExecutionHandler);
		if (this.allowCoreThreadTimeOut) {
			executor.allowCoreThreadTimeOut(true);
		}

		// 使用不可配置的装饰器包装执行器.
		this.exposedExecutor = (this.exposeUnconfigurableExecutor ?
				Executors.unconfigurableExecutorService(executor) : executor);

		return executor;
	}

	/**
	 * 创建{@link ThreadPoolExecutor}的新实例或其子类.
	 * <p>默认实现创建标准的{@link ThreadPoolExecutor}.
	 * 可以重写以提供自定义的{@link ThreadPoolExecutor}子类.
	 * 
	 * @param corePoolSize 指定的核心池大小
	 * @param maxPoolSize 指定的最大池大小
	 * @param keepAliveSeconds 指定的keep-alive时间, 以秒为单位
	 * @param queue 要使用的BlockingQueue
	 * @param threadFactory 要使用的ThreadFactory
	 * @param rejectedExecutionHandler 要使用的RejectedExecutionHandler
	 * 
	 * @return 新的ThreadPoolExecutor实例
	 */
	protected ThreadPoolExecutor createExecutor(
			int corePoolSize, int maxPoolSize, int keepAliveSeconds, BlockingQueue<Runnable> queue,
			ThreadFactory threadFactory, RejectedExecutionHandler rejectedExecutionHandler) {

		return new ThreadPoolExecutor(corePoolSize, maxPoolSize,
				keepAliveSeconds, TimeUnit.SECONDS, queue, threadFactory, rejectedExecutionHandler);
	}

	/**
	 * 创建BlockingQueue以用于ThreadPoolExecutor.
	 * <p>将为正容量值创建LinkedBlockingQueue实例; 否则SynchronousQueue.
	 * 
	 * @param queueCapacity 指定的队列容量
	 * 
	 * @return BlockingQueue实例
	 */
	protected BlockingQueue<Runnable> createQueue(int queueCapacity) {
		if (queueCapacity > 0) {
			return new LinkedBlockingQueue<Runnable>(queueCapacity);
		}
		else {
			return new SynchronousQueue<Runnable>();
		}
	}


	@Override
	public ExecutorService getObject() {
		return this.exposedExecutor;
	}

	@Override
	public Class<? extends ExecutorService> getObjectType() {
		return (this.exposedExecutor != null ? this.exposedExecutor.getClass() : ExecutorService.class);
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

}
