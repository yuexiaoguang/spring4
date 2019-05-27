package org.springframework.util;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 用于创建新的{@link Thread}实例的自定义帮助类.
 * 提供各种bean属性: 线程名称前缀, 线程优先级等.
 *
 * <p>用作线程工厂的基类, 例如
 * {@link org.springframework.scheduling.concurrent.CustomizableThreadFactory}.
 */
@SuppressWarnings("serial")
public class CustomizableThreadCreator implements Serializable {

	private String threadNamePrefix;

	private int threadPriority = Thread.NORM_PRIORITY;

	private boolean daemon = false;

	private ThreadGroup threadGroup;

	private final AtomicInteger threadCount = new AtomicInteger(0);


	/**
	 * 使用默认线程名称前缀.
	 */
	public CustomizableThreadCreator() {
		this.threadNamePrefix = getDefaultThreadNamePrefix();
	}

	/**
	 * @param threadNamePrefix 用于新创建的线程名称的前缀
	 */
	public CustomizableThreadCreator(String threadNamePrefix) {
		this.threadNamePrefix = (threadNamePrefix != null ? threadNamePrefix : getDefaultThreadNamePrefix());
	}


	/**
	 * 指定用于新创建的线程名称的前缀.
	 * 默认 "SimpleAsyncTaskExecutor-".
	 */
	public void setThreadNamePrefix(String threadNamePrefix) {
		this.threadNamePrefix = (threadNamePrefix != null ? threadNamePrefix : getDefaultThreadNamePrefix());
	}

	/**
	 * 返回用于创建新的线程的名称的线程名称前缀.
	 */
	public String getThreadNamePrefix() {
		return this.threadNamePrefix;
	}

	/**
	 * 设置此工厂创建的线程的优先级.
	 * 默认 5.
	 */
	public void setThreadPriority(int threadPriority) {
		this.threadPriority = threadPriority;
	}

	/**
	 * 返回此工厂创建的线程的优先级.
	 */
	public int getThreadPriority() {
		return this.threadPriority;
	}

	/**
	 * 设置此工厂是否应该创建守护线程, 只要应用程序本身正在运行就执行.
	 * <p>默认 "false": 具体工厂通常支持明确取消.
	 * 因此, 如果应用程序关闭, Runnables将默认完成其执行.
	 * <p>指定"true"表示线程实时关闭, 该线程在应用程序自身关闭时仍然执行{@link Runnable}.
	 */
	public void setDaemon(boolean daemon) {
		this.daemon = daemon;
	}

	/**
	 * 返回此工厂是否应创建守护线程.
	 */
	public boolean isDaemon() {
		return this.daemon;
	}

	/**
	 * 指定应在其中创建线程的线程组的名称.
	 */
	public void setThreadGroupName(String name) {
		this.threadGroup = new ThreadGroup(name);
	}

	/**
	 * 指定应在其中创建线程的线程组.
	 */
	public void setThreadGroup(ThreadGroup threadGroup) {
		this.threadGroup = threadGroup;
	}

	/**
	 * 返回应在其中创建线程的线程组 (或{@code null}为默认组).
	 */
	public ThreadGroup getThreadGroup() {
		return this.threadGroup;
	}


	/**
	 * 用于创建新{@link Thread}的模板方法.
	 * <p>默认实现为给定的{@link Runnable}创建一个新的Thread, 并应用适当的线程名称.
	 * 
	 * @param runnable 要执行的Runnable
	 */
	public Thread createThread(Runnable runnable) {
		Thread thread = new Thread(getThreadGroup(), runnable, nextThreadName());
		thread.setPriority(getThreadPriority());
		thread.setDaemon(isDaemon());
		return thread;
	}

	/**
	 * 返回用于新创建的{@link Thread}的线程名称.
	 * <p>默认实现返回指定的线程名称前缀, 并附加增加的线程数: e.g. "SimpleAsyncTaskExecutor-0".
	 */
	protected String nextThreadName() {
		return getThreadNamePrefix() + this.threadCount.incrementAndGet();
	}

	/**
	 * 构建此工厂的默认线程名称前缀.
	 * 
	 * @return 默认线程名称前缀 (never {@code null})
	 */
	protected String getDefaultThreadNamePrefix() {
		return ClassUtils.getShortName(getClass()) + "-";
	}
}
