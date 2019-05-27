package org.springframework.scheduling.concurrent;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.lang.UsesJava7;
import org.springframework.scheduling.support.DelegatingErrorHandlingRunnable;
import org.springframework.scheduling.support.TaskUtils;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;

/**
 * {@link org.springframework.beans.factory.FactoryBean}, 设置{@link java.util.concurrent.ScheduledExecutorService}
 * (默认: {@link java.util.concurrent.ScheduledThreadPoolExecutor})
 * 并公开它用于bean引用.
 *
 * <p>允许注册{@link ScheduledExecutorTask ScheduledExecutorTasks},
 * 在初始化时自动启动{@link ScheduledExecutorService}并在销毁上下文时取消它.
 * 在只需要在启动时静态注册任务的场景中, 根本不需要在应用程序代码中访问{@link ScheduledExecutorService}实例本身;
 * 然后, {@code ScheduledExecutorFactoryBean}仅用于生命周期集成.
 *
 * <p>或者, 可以使用构造函数注入直接设置{@link ScheduledThreadPoolExecutor}实例,
 * 或使用指向{@link java.util.concurrent.Executors}类的工厂方法定义.
 * <b>强烈建议特别是配置类中的普通{@code @Bean}方法, 其中此{@code FactoryBean}变体会强制您返回{@code FactoryBean}类型,
 * 而不是{@code ScheduledExecutorService}.</b>
 *
 * <p>请注意, 在重复执行之间, {@link java.util.concurrent.ScheduledExecutorService}使用共享的{@link Runnable}实例,
 * 而Quartz则为每次执行实例化一个新Job.
 *
 * <p><b>WARNING:</b> 通过本机{@link java.util.concurrent.ScheduledExecutorService}
 * 提交的{@link Runnable Runnables}一旦抛出异常就会从执行计划中删除.
 * 如果希望在此类异常后继续执行, 将这个FactoryBean的
 * {@link #setContinueScheduledExecutionAfterException "continueScheduledExecutionAfterException"}属性设置为"true".
 */
@SuppressWarnings("serial")
public class ScheduledExecutorFactoryBean extends ExecutorConfigurationSupport
		implements FactoryBean<ScheduledExecutorService> {

	// ScheduledThreadPoolExecutor.setRemoveOnCancelPolicy(boolean) only available on JDK 7+
	private static final boolean setRemoveOnCancelPolicyAvailable =
			ClassUtils.hasMethod(ScheduledThreadPoolExecutor.class, "setRemoveOnCancelPolicy", boolean.class);


	private int poolSize = 1;

	private ScheduledExecutorTask[] scheduledExecutorTasks;

	private boolean removeOnCancelPolicy = false;

	private boolean continueScheduledExecutionAfterException = false;

	private boolean exposeUnconfigurableExecutor = false;

	private ScheduledExecutorService exposedExecutor;


	/**
	 * 设置ScheduledExecutorService的池大小.
	 * 默认 1.
	 */
	public void setPoolSize(int poolSize) {
		Assert.isTrue(poolSize > 0, "'poolSize' must be 1 or higher");
		this.poolSize = poolSize;
	}

	/**
	 * 使用此FactoryBean创建的ScheduledExecutorService注册ScheduledExecutorTask对象.
	 * 根据每个ScheduledExecutorTask的设置, 它将通过ScheduledExecutorService的调度方法之一进行注册.
	 */
	public void setScheduledExecutorTasks(ScheduledExecutorTask... scheduledExecutorTasks) {
		this.scheduledExecutorTasks = scheduledExecutorTasks;
	}

	/**
	 * 在{@link ScheduledThreadPoolExecutor}上设置remove-on-cancel模式 (JDK 7+).
	 * <p>默认{@code false}.
	 * 如果设置为{@code true}, 目标执行器将切换到 remove-on-cancel模式 (如果可能, 否则将使用软回退).
	 */
	public void setRemoveOnCancelPolicy(boolean removeOnCancelPolicy) {
		this.removeOnCancelPolicy = removeOnCancelPolicy;
	}

	/**
	 * 指定在抛出异常后是否继续执行计划任务.
	 * <p>默认 "false", 匹配{@link java.util.concurrent.ScheduledExecutorService}的本机行为.
	 * 将此标志切换为"true" 以执行每个任务的异常执行, 并在成功执行的情况下继续执行计划.
	 */
	public void setContinueScheduledExecutionAfterException(boolean continueScheduledExecutionAfterException) {
		this.continueScheduledExecutionAfterException = continueScheduledExecutionAfterException;
	}

	/**
	 * 指定此FactoryBean是否应为创建的执行器公开不可配置的装饰器.
	 * <p>默认"false", 将原始执行器公开为bean引用.
	 * 将此标志切换为"true" 以严格阻止客户端修改执行器的配置.
	 */
	public void setExposeUnconfigurableExecutor(boolean exposeUnconfigurableExecutor) {
		this.exposeUnconfigurableExecutor = exposeUnconfigurableExecutor;
	}


	@Override
	@UsesJava7
	protected ExecutorService initializeExecutor(
			ThreadFactory threadFactory, RejectedExecutionHandler rejectedExecutionHandler) {

		ScheduledExecutorService executor =
				createExecutor(this.poolSize, threadFactory, rejectedExecutionHandler);

		if (this.removeOnCancelPolicy) {
			if (setRemoveOnCancelPolicyAvailable && executor instanceof ScheduledThreadPoolExecutor) {
				((ScheduledThreadPoolExecutor) executor).setRemoveOnCancelPolicy(true);
			}
			else {
				logger.info("Could not apply remove-on-cancel policy - not a Java 7+ ScheduledThreadPoolExecutor");
			}
		}

		// Register specified ScheduledExecutorTasks, if necessary.
		if (!ObjectUtils.isEmpty(this.scheduledExecutorTasks)) {
			registerTasks(this.scheduledExecutorTasks, executor);
		}

		// 使用不可配置的装饰器包装执行器.
		this.exposedExecutor = (this.exposeUnconfigurableExecutor ?
				Executors.unconfigurableScheduledExecutorService(executor) : executor);

		return executor;
	}

	/**
	 * 创建{@link ScheduledExecutorService}实例.
	 * <p>默认实现创建{@link ScheduledThreadPoolExecutor}.
	 * 可以在子类中重写以提供自定义的{@link ScheduledExecutorService}实例.
	 * 
	 * @param poolSize 指定的池大小
	 * @param threadFactory 要使用的ThreadFactory
	 * @param rejectedExecutionHandler 要使用的RejectedExecutionHandler
	 * 
	 * @return 新的ScheduledExecutorService实例
	 */
	protected ScheduledExecutorService createExecutor(
			int poolSize, ThreadFactory threadFactory, RejectedExecutionHandler rejectedExecutionHandler) {

		return new ScheduledThreadPoolExecutor(poolSize, threadFactory, rejectedExecutionHandler);
	}

	/**
	 * 在给定的{@link ScheduledExecutorService}上注册指定的{@link ScheduledExecutorTask ScheduledExecutorTasks}.
	 * 
	 * @param tasks 指定的ScheduledExecutorTasks (never empty)
	 * @param executor 用于注册任务的ScheduledExecutorService.
	 */
	protected void registerTasks(ScheduledExecutorTask[] tasks, ScheduledExecutorService executor) {
		for (ScheduledExecutorTask task : tasks) {
			Runnable runnable = getRunnableToSchedule(task);
			if (task.isOneTimeTask()) {
				executor.schedule(runnable, task.getDelay(), task.getTimeUnit());
			}
			else {
				if (task.isFixedRate()) {
					executor.scheduleAtFixedRate(runnable, task.getDelay(), task.getPeriod(), task.getTimeUnit());
				}
				else {
					executor.scheduleWithFixedDelay(runnable, task.getDelay(), task.getPeriod(), task.getTimeUnit());
				}
			}
		}
	}

	/**
	 * 确定要为给定任务调度的实际Runnable.
	 * <p>在{@link org.springframework.scheduling.support.DelegatingErrorHandlingRunnable}中包装任务的Runnable,
	 * 它将捕获并记录异常.
	 * 如果有必要，它会根据
	 * {@link #setContinueScheduledExecutionAfterException "continueScheduledExecutionAfterException"}标志抑制异常.
	 * 
	 * @param task 要调度的ScheduledExecutorTask
	 * 
	 * @return 要调度的实际Runnable (可能是装饰器)
	 */
	protected Runnable getRunnableToSchedule(ScheduledExecutorTask task) {
		return (this.continueScheduledExecutionAfterException ?
				new DelegatingErrorHandlingRunnable(task.getRunnable(), TaskUtils.LOG_AND_SUPPRESS_ERROR_HANDLER) :
				new DelegatingErrorHandlingRunnable(task.getRunnable(), TaskUtils.LOG_AND_PROPAGATE_ERROR_HANDLER));
	}


	@Override
	public ScheduledExecutorService getObject() {
		return this.exposedExecutor;
	}

	@Override
	public Class<? extends ScheduledExecutorService> getObjectType() {
		return (this.exposedExecutor != null ? this.exposedExecutor.getClass() : ScheduledExecutorService.class);
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

}
