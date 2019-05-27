package org.springframework.jms.config;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.jms.listener.MessageListenerContainer;
import org.springframework.util.Assert;

/**
 * 为已注册的{@linkplain JmsListenerEndpoint 端点}创建必要的{@link MessageListenerContainer}实例.
 * 还管理监听器容器的生命周期, 特别是在应用程序上下文的生命周期内.
 *
 * <p>与手动创建的{@link MessageListenerContainer}相反, 注册表管理的监听器容器不是应用程序上下文中的bean, 也不是自动装配的候选者.
 * 如果需要访问此注册表的监听器容器以进行管理, 使用{@link #getListenerContainers()}.
 * 如果需要访问特定的消息监听器容器, 使用端点的id作为参数的{@link #getListenerContainer(String)}.
 */
public class JmsListenerEndpointRegistry implements DisposableBean, SmartLifecycle,
		ApplicationContextAware, ApplicationListener<ContextRefreshedEvent> {

	protected final Log logger = LogFactory.getLog(getClass());

	private final Map<String, MessageListenerContainer> listenerContainers =
			new ConcurrentHashMap<String, MessageListenerContainer>();

	private int phase = Integer.MAX_VALUE;

	private ApplicationContext applicationContext;

	private boolean contextRefreshed;


	@Override
	public void setApplicationContext(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	@Override
	public void onApplicationEvent(ContextRefreshedEvent event) {
		if (event.getApplicationContext() == this.applicationContext) {
			this.contextRefreshed = true;
		}
	}


	/**
	 * 返回具有指定标识的{@link MessageListenerContainer}或{@code null}.
	 * 
	 * @param id 容器的id
	 * 
	 * @return 容器或 {@code null}
	 */
	public MessageListenerContainer getListenerContainer(String id) {
		Assert.notNull(id, "Container identifier must not be null");
		return this.listenerContainers.get(id);
	}

	/**
	 * 返回管理的{@link MessageListenerContainer}实例的ID.
	 */
	public Set<String> getListenerContainerIds() {
		return Collections.unmodifiableSet(this.listenerContainers.keySet());
	}

	/**
	 * 返回管理的{@link MessageListenerContainer}实例.
	 */
	public Collection<MessageListenerContainer> getListenerContainers() {
		return Collections.unmodifiableCollection(this.listenerContainers.values());
	}

	/**
	 * 为给定的{@link JmsListenerEndpoint}创建消息监听器容器.
	 * <p>这创建了必要的基础架构来支持该端点的配置.
	 * <p>{@code startImmediately}标志确定是否应立即启动容器.
	 * 
	 * @param endpoint 要添加的端点
	 * @param factory 要使用的监听器工厂
	 * @param startImmediately 必要时立即启动容器
	 */
	public void registerListenerContainer(JmsListenerEndpoint endpoint, JmsListenerContainerFactory<?> factory,
			boolean startImmediately) {

		Assert.notNull(endpoint, "Endpoint must not be null");
		Assert.notNull(factory, "Factory must not be null");

		String id = endpoint.getId();
		Assert.notNull(id, "Endpoint id must not be null");
		synchronized (this.listenerContainers) {
			if (this.listenerContainers.containsKey(id)) {
				throw new IllegalStateException("Another endpoint is already registered with id '" + id + "'");
			}
			MessageListenerContainer container = createListenerContainer(endpoint, factory);
			this.listenerContainers.put(id, container);
			if (startImmediately) {
				startIfNecessary(container);
			}
		}
	}

	/**
	 * 为给定的{@link JmsListenerEndpoint}创建消息监听器容器.
	 * <p>这创建了必要的基础架构来支持该端点的配置.
	 * 
	 * @param endpoint 要添加的端点
	 * @param factory 要使用的监听器工厂
	 */
	public void registerListenerContainer(JmsListenerEndpoint endpoint, JmsListenerContainerFactory<?> factory) {
		registerListenerContainer(endpoint, factory, false);
	}

	/**
	 * 使用指定的工厂创建并启动新容器.
	 */
	protected MessageListenerContainer createListenerContainer(JmsListenerEndpoint endpoint,
			JmsListenerContainerFactory<?> factory) {

		MessageListenerContainer listenerContainer = factory.createListenerContainer(endpoint);

		if (listenerContainer instanceof InitializingBean) {
			try {
				((InitializingBean) listenerContainer).afterPropertiesSet();
			}
			catch (Exception ex) {
				throw new BeanInitializationException("Failed to initialize message listener container", ex);
			}
		}

		int containerPhase = listenerContainer.getPhase();
		if (containerPhase < Integer.MAX_VALUE) {  // a custom phase value
			if (this.phase < Integer.MAX_VALUE && this.phase != containerPhase) {
				throw new IllegalStateException("Encountered phase mismatch between container factory definitions: " +
						this.phase + " vs " + containerPhase);
			}
			this.phase = listenerContainer.getPhase();
		}

		return listenerContainer;
	}


	// Delegating implementation of SmartLifecycle

	@Override
	public int getPhase() {
		return this.phase;
	}

	@Override
	public boolean isAutoStartup() {
		return true;
	}

	@Override
	public void start() {
		for (MessageListenerContainer listenerContainer : getListenerContainers()) {
			startIfNecessary(listenerContainer);
		}
	}

	@Override
	public void stop() {
		for (MessageListenerContainer listenerContainer : getListenerContainers()) {
			listenerContainer.stop();
		}
	}

	@Override
	public void stop(Runnable callback) {
		Collection<MessageListenerContainer> listenerContainers = getListenerContainers();
		AggregatingCallback aggregatingCallback = new AggregatingCallback(listenerContainers.size(), callback);
		for (MessageListenerContainer listenerContainer : listenerContainers) {
			listenerContainer.stop(aggregatingCallback);
		}
	}

	@Override
	public boolean isRunning() {
		for (MessageListenerContainer listenerContainer : getListenerContainers()) {
			if (listenerContainer.isRunning()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 如果应该在启动时启动或启动后显式调用start, 则启动指定的{@link MessageListenerContainer}.
	 */
	private void startIfNecessary(MessageListenerContainer listenerContainer) {
		if (this.contextRefreshed || listenerContainer.isAutoStartup()) {
			listenerContainer.start();
		}
	}

	@Override
	public void destroy() {
		for (MessageListenerContainer listenerContainer : getListenerContainers()) {
			if (listenerContainer instanceof DisposableBean) {
				try {
					((DisposableBean) listenerContainer).destroy();
				}
				catch (Throwable ex) {
					logger.warn("Failed to destroy message listener container", ex);
				}
			}
		}
	}


	private static class AggregatingCallback implements Runnable {

		private final AtomicInteger count;

		private final Runnable finishCallback;

		public AggregatingCallback(int count, Runnable finishCallback) {
			this.count = new AtomicInteger(count);
			this.finishCallback = finishCallback;
		}

		@Override
		public void run() {
			if (this.count.decrementAndGet() == 0) {
				this.finishCallback.run();
			}
		}
	}
}
