package org.springframework.jms.config;

import java.util.concurrent.Executor;

import org.springframework.jms.listener.DefaultMessageListenerContainer;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.util.backoff.BackOff;

/**
 * 用于构建常规{@link DefaultMessageListenerContainer}的{@link JmsListenerContainerFactory}实现.
 *
 * <p>这应该是大多数用户的默认设置, 对于手动构建此类容器定义的用户来说, 这应该是一个良好的转换路径.
 */
public class DefaultJmsListenerContainerFactory
		extends AbstractJmsListenerContainerFactory<DefaultMessageListenerContainer> {

	private Executor taskExecutor;

	private PlatformTransactionManager transactionManager;

	private Integer cacheLevel;

	private String cacheLevelName;

	private String concurrency;

	private Integer maxMessagesPerTask;

	private Long receiveTimeout;

	private Long recoveryInterval;

	private BackOff backOff;


	public void setTaskExecutor(Executor taskExecutor) {
		this.taskExecutor = taskExecutor;
	}

	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	public void setCacheLevel(Integer cacheLevel) {
		this.cacheLevel = cacheLevel;
	}

	public void setCacheLevelName(String cacheLevelName) {
		this.cacheLevelName = cacheLevelName;
	}

	public void setConcurrency(String concurrency) {
		this.concurrency = concurrency;
	}

	public void setMaxMessagesPerTask(Integer maxMessagesPerTask) {
		this.maxMessagesPerTask = maxMessagesPerTask;
	}

	public void setReceiveTimeout(Long receiveTimeout) {
		this.receiveTimeout = receiveTimeout;
	}

	public void setRecoveryInterval(Long recoveryInterval) {
		this.recoveryInterval = recoveryInterval;
	}

	public void setBackOff(BackOff backOff) {
		this.backOff = backOff;
	}


	@Override
	protected DefaultMessageListenerContainer createContainerInstance() {
		return new DefaultMessageListenerContainer();
	}

	@Override
	protected void initializeContainer(DefaultMessageListenerContainer container) {
		if (this.taskExecutor != null) {
			container.setTaskExecutor(this.taskExecutor);
		}
		if (this.transactionManager != null) {
			container.setTransactionManager(this.transactionManager);
		}

		if (this.cacheLevel != null) {
			container.setCacheLevel(this.cacheLevel);
		}
		else if (this.cacheLevelName != null) {
			container.setCacheLevelName(this.cacheLevelName);
		}

		if (this.concurrency != null) {
			container.setConcurrency(this.concurrency);
		}
		if (this.maxMessagesPerTask != null) {
			container.setMaxMessagesPerTask(this.maxMessagesPerTask);
		}
		if (this.receiveTimeout != null) {
			container.setReceiveTimeout(this.receiveTimeout);
		}

		if (this.backOff != null) {
			container.setBackOff(this.backOff);
			if (this.recoveryInterval != null) {
				logger.warn("Ignoring recovery interval in DefaultJmsListenerContainerFactory in favor of BackOff");
			}
		}
		else if (this.recoveryInterval != null) {
			container.setRecoveryInterval(this.recoveryInterval);
		}
	}
}
