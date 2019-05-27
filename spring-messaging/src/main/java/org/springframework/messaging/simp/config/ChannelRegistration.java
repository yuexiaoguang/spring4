package org.springframework.messaging.simp.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * 用于自定义{@link org.springframework.messaging.MessageChannel}配置的注册类.
 */
public class ChannelRegistration {

	private TaskExecutorRegistration registration;

	private final List<ChannelInterceptor> interceptors = new ArrayList<ChannelInterceptor>();


	/**
	 * 配置支持此消息通道的线程池.
	 */
	public TaskExecutorRegistration taskExecutor() {
		return taskExecutor(null);
	}

	/**
	 * 使用自定义ThreadPoolTask​​Executor配置支持此消息通道的线程池.
	 * 
	 * @param taskExecutor 要使用的执行器 (或{@code null}作为默认执行器)
	 */
	public TaskExecutorRegistration taskExecutor(ThreadPoolTaskExecutor taskExecutor) {
		if (this.registration == null) {
			this.registration = (taskExecutor != null ? new TaskExecutorRegistration(taskExecutor) :
					new TaskExecutorRegistration());
		}
		return this.registration;
	}

	/**
	 * 为此消息通道配置给定的拦截器, 将它们添加到通道的当前拦截器列表中.
	 */
	public ChannelRegistration interceptors(ChannelInterceptor... interceptors) {
		this.interceptors.addAll(Arrays.asList(interceptors));
		return this;
	}

	/**
	 * 配置消息通道的拦截器.
	 * @deprecated as of 4.3.12, in favor of {@link #interceptors(ChannelInterceptor...)}
	 */
	@Deprecated
	public ChannelRegistration setInterceptors(ChannelInterceptor... interceptors) {
		if (interceptors != null) {
			this.interceptors.addAll(Arrays.asList(interceptors));
		}
		return this;
	}


	protected boolean hasTaskExecutor() {
		return (this.registration != null);
	}

	/**
	 * @deprecated as of 4.3.12 since it's not used anymore
	 */
	@Deprecated
	protected TaskExecutorRegistration getTaskExecRegistration() {
		return this.registration;
	}

	/**
	 * @deprecated as of 4.3.12 since it's not used anymore
	 */
	@Deprecated
	protected TaskExecutorRegistration getOrCreateTaskExecRegistration() {
		return taskExecutor();
	}

	protected boolean hasInterceptors() {
		return !this.interceptors.isEmpty();
	}

	protected List<ChannelInterceptor> getInterceptors() {
		return this.interceptors;
	}

}
