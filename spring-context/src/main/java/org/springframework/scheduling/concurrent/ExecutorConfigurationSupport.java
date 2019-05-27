package org.springframework.scheduling.concurrent;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

/**
 * 设置{@code java.util.concurrent.ExecutorService}的类的基类
 * (通常是{@link java.util.concurrent.ThreadPoolExecutor}).
 * 定义常见配置设置和常见生命周期处理.
 */
@SuppressWarnings("serial")
public abstract class ExecutorConfigurationSupport extends CustomizableThreadFactory
		implements BeanNameAware, InitializingBean, DisposableBean {

	protected final Log logger = LogFactory.getLog(getClass());

	private ThreadFactory threadFactory = this;

	private boolean threadNamePrefixSet = false;

	private RejectedExecutionHandler rejectedExecutionHandler = new ThreadPoolExecutor.AbortPolicy();

	private boolean waitForTasksToCompleteOnShutdown = false;

	private int awaitTerminationSeconds = 0;

	private String beanName;

	private ExecutorService executor;


	/**
	 * 设置用于ExecutorService的线程池的ThreadFactory.
	 * 默认值是底层ExecutorService的默认线程工厂.
	 * <p>在具有JSR-236支持的Java EE 7或其他托管环境中, 请考虑指定位于JNDI的ManagedThreadFactory:
	 * 默认情况下, 在"java:comp/DefaultManagedThreadFactory"中找到.
	 * 使用XML中的"jee:jndi-lookup"命名空间元素或程序化{@link org.springframework.jndi.JndiLocatorDelegate}来方便查找.
	 * 或者, 如果没有找到托管线程工厂, 请考虑使用Spring的{@link DefaultManagedAwareThreadFactory}并回退到本地线程.
	 */
	public void setThreadFactory(ThreadFactory threadFactory) {
		this.threadFactory = (threadFactory != null ? threadFactory : this);
	}

	@Override
	public void setThreadNamePrefix(String threadNamePrefix) {
		super.setThreadNamePrefix(threadNamePrefix);
		this.threadNamePrefixSet = true;
	}

	/**
	 * 设置用于ExecutorService的RejectedExecutionHandler.
	 * 默认值是ExecutorService的默认中止策略.
	 */
	public void setRejectedExecutionHandler(RejectedExecutionHandler rejectedExecutionHandler) {
		this.rejectedExecutionHandler =
				(rejectedExecutionHandler != null ? rejectedExecutionHandler : new ThreadPoolExecutor.AbortPolicy());
	}

	/**
	 * 设置是否在关闭时等待计划任务完成, 不中断正在运行的任务并执行队列中的所有任务.
	 * <p>默认"false", 立即关闭, 中断正在进行的任务和清空队列.
	 * 如果您希望以更长的关闭阶段为代价完全完成任务, 请将此标志切换为"true".
	 * <p>请注意, 在正在进行的任务完成时, Spring的容器关闭会继续.
	 * 如果您希望此执行器在容器的其余部分继续关闭之前, 阻塞并等待任务终止
	 * - e.g. 为了保持您的任务可能需要的其他资源
	 * -, 设置{@link #setAwaitTerminationSeconds "awaitTerminationSeconds"}属性, 而不是此属性.
	 */
	public void setWaitForTasksToCompleteOnShutdown(boolean waitForJobsToCompleteOnShutdown) {
		this.waitForTasksToCompleteOnShutdown = waitForJobsToCompleteOnShutdown;
	}

	/**
	 * 设置此执行器在关闭时应阻塞的最大秒数, 以便在容器的其余部分继续关闭之前等待剩余任务完成执行.
	 * 如果剩余任务可能需要访问也由容器管理的其他资源, 则此功能尤其有用.
	 * <p>默认情况下, 此执行器将不会等待任务终止.
	 * 它将立即关闭, 中断正在进行的任务并清空剩余的任务队列
	 * - 或者, 如果
	 * {@link #setWaitForTasksToCompleteOnShutdown "waitForTasksToCompleteOnShutdown"}已经设置为{@code true},
	 * 它将继续完全执行所有正在进行的任务以及队列中的所有剩余任务, 与容器的其余部分并行关闭.
	 * <p>在任何一种情况下, 如果使用此属性指定等待终止周期, 则此执行器将等待给定时间(最大值)以终止任务.
	 * 根据经验, 如果同时将"waitForTasksToCompleteOnShutdown"设置为{@code true}, 请在此处指定明显更高的超时,
	 * 因为队列中的所有剩余任务仍将执行
	 * - 与默认关闭行为相反, 它只是在等待当前正在执行的对线程中断没有反应的任务.
	 */
	public void setAwaitTerminationSeconds(int awaitTerminationSeconds) {
		this.awaitTerminationSeconds = awaitTerminationSeconds;
	}

	@Override
	public void setBeanName(String name) {
		this.beanName = name;
	}


	/**
	 * 在容器应用所有属性值后调用{@code initialize()}.
	 */
	@Override
	public void afterPropertiesSet() {
		initialize();
	}

	/**
	 * 设置ExecutorService.
	 */
	public void initialize() {
		if (logger.isInfoEnabled()) {
			logger.info("Initializing ExecutorService " + (this.beanName != null ? " '" + this.beanName + "'" : ""));
		}
		if (!this.threadNamePrefixSet && this.beanName != null) {
			setThreadNamePrefix(this.beanName + "-");
		}
		this.executor = initializeExecutor(this.threadFactory, this.rejectedExecutionHandler);
	}

	/**
	 * 创建目标{@link java.util.concurrent.ExecutorService}实例.
	 * 由{@code afterPropertiesSet}调用.
	 * 
	 * @param threadFactory 要使用的ThreadFactory
	 * @param rejectedExecutionHandler 要使用的RejectedExecutionHandler
	 * 
	 * @return 新的ExecutorService实例
	 */
	protected abstract ExecutorService initializeExecutor(
			ThreadFactory threadFactory, RejectedExecutionHandler rejectedExecutionHandler);


	/**
	 * 当BeanFactory销毁任务执行器实例时调用{@code shutdown}.
	 */
	@Override
	public void destroy() {
		shutdown();
	}

	/**
	 * 在底层的ExecutorService上执行关闭.
	 */
	public void shutdown() {
		if (logger.isInfoEnabled()) {
			logger.info("Shutting down ExecutorService" + (this.beanName != null ? " '" + this.beanName + "'" : ""));
		}
		if (this.executor != null) {
			if (this.waitForTasksToCompleteOnShutdown) {
				this.executor.shutdown();
			}
			else {
				this.executor.shutdownNow();
			}
			awaitTerminationIfNecessary(this.executor);
		}
	}

	/**
	 * 等待执行器终止, 根据{@link #setAwaitTerminationSeconds "awaitTerminationSeconds"}属性的值.
	 */
	private void awaitTerminationIfNecessary(ExecutorService executor) {
		if (this.awaitTerminationSeconds > 0) {
			try {
				if (!executor.awaitTermination(this.awaitTerminationSeconds, TimeUnit.SECONDS)) {
					if (logger.isWarnEnabled()) {
						logger.warn("Timed out while waiting for executor" +
								(this.beanName != null ? " '" + this.beanName + "'" : "") + " to terminate");
					}
				}
			}
			catch (InterruptedException ex) {
				if (logger.isWarnEnabled()) {
					logger.warn("Interrupted while waiting for executor" +
							(this.beanName != null ? " '" + this.beanName + "'" : "") + " to terminate");
				}
				Thread.currentThread().interrupt();
			}
		}
	}
}
