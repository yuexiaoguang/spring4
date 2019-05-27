package org.springframework.scheduling.config;

import java.util.concurrent.RejectedExecutionHandler;

import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.StringUtils;

/**
 * {@link FactoryBean}用于创建{@link ThreadPoolTask​​Executor}实例, 主要用于XML任务命名空间后面.
 */
public class TaskExecutorFactoryBean implements
		FactoryBean<TaskExecutor>, BeanNameAware, InitializingBean, DisposableBean {

	private String poolSize;

	private Integer queueCapacity;

	private RejectedExecutionHandler rejectedExecutionHandler;

	private Integer keepAliveSeconds;

	private String beanName;

	private ThreadPoolTaskExecutor target;


	public void setPoolSize(String poolSize) {
		this.poolSize = poolSize;
	}

	public void setQueueCapacity(int queueCapacity) {
		this.queueCapacity = queueCapacity;
	}

	public void setRejectedExecutionHandler(RejectedExecutionHandler rejectedExecutionHandler) {
		this.rejectedExecutionHandler = rejectedExecutionHandler;
	}

	public void setKeepAliveSeconds(int keepAliveSeconds) {
		this.keepAliveSeconds = keepAliveSeconds;
	}

	@Override
	public void setBeanName(String beanName) {
		this.beanName = beanName;
	}


	@Override
	public void afterPropertiesSet() {
		this.target = new ThreadPoolTaskExecutor();
		determinePoolSizeRange();
		if (this.queueCapacity != null) {
			this.target.setQueueCapacity(this.queueCapacity);
		}
		if (this.keepAliveSeconds != null) {
			this.target.setKeepAliveSeconds(this.keepAliveSeconds);
		}
		if (this.rejectedExecutionHandler != null) {
			this.target.setRejectedExecutionHandler(this.rejectedExecutionHandler);
		}
		if (this.beanName != null) {
			this.target.setThreadNamePrefix(this.beanName + "-");
		}
		this.target.afterPropertiesSet();
	}

	private void determinePoolSizeRange() {
		if (StringUtils.hasText(this.poolSize)) {
			try {
				int corePoolSize;
				int maxPoolSize;
				int separatorIndex = this.poolSize.indexOf('-');
				if (separatorIndex != -1) {
					corePoolSize = Integer.valueOf(this.poolSize.substring(0, separatorIndex));
					maxPoolSize = Integer.valueOf(this.poolSize.substring(separatorIndex + 1, this.poolSize.length()));
					if (corePoolSize > maxPoolSize) {
						throw new IllegalArgumentException(
								"Lower bound of pool-size range must not exceed the upper bound");
					}
					if (this.queueCapacity == null) {
						// 没有提供队列容量, 因此无限制
						if (corePoolSize == 0) {
							// 实际上将'corePoolSize'设置为范围的上限, 但允许核心线程超时...
							this.target.setAllowCoreThreadTimeOut(true);
							corePoolSize = maxPoolSize;
						}
						else {
							// 非零下限意味着核心最大尺寸范围...
							throw new IllegalArgumentException(
									"A non-zero lower bound for the size range requires a queue-capacity value");
						}
					}
				}
				else {
					Integer value = Integer.valueOf(this.poolSize);
					corePoolSize = value;
					maxPoolSize = value;
				}
				this.target.setCorePoolSize(corePoolSize);
				this.target.setMaxPoolSize(maxPoolSize);
			}
			catch (NumberFormatException ex) {
				throw new IllegalArgumentException("Invalid pool-size value [" + this.poolSize + "]: only single " +
						"maximum integer (e.g. \"5\") and minimum-maximum range (e.g. \"3-5\") are supported", ex);
			}
		}
	}


	@Override
	public TaskExecutor getObject() {
		return this.target;
	}

	@Override
	public Class<? extends TaskExecutor> getObjectType() {
		return (this.target != null ? this.target.getClass() : ThreadPoolTaskExecutor.class);
	}

	@Override
	public boolean isSingleton() {
		return true;
	}


	@Override
	public void destroy() {
		this.target.destroy();
	}

}
