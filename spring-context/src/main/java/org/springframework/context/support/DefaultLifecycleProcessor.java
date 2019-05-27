package org.springframework.context.support;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContextException;
import org.springframework.context.Lifecycle;
import org.springframework.context.LifecycleProcessor;
import org.springframework.context.Phased;
import org.springframework.context.SmartLifecycle;

/**
 * {@link LifecycleProcessor}策略的默认实现.
 */
public class DefaultLifecycleProcessor implements LifecycleProcessor, BeanFactoryAware {

	private final Log logger = LogFactory.getLog(getClass());

	private volatile long timeoutPerShutdownPhase = 30000;

	private volatile boolean running;

	private volatile ConfigurableListableBeanFactory beanFactory;


	/**
	 * 指定关闭任何阶段所分配的最长时间, 以毫秒为单位 (具有相同 '阶段'值的SmartLifecycle bean组).
	 * <p>默认值是 30 秒.
	 */
	public void setTimeoutPerShutdownPhase(long timeoutPerShutdownPhase) {
		this.timeoutPerShutdownPhase = timeoutPerShutdownPhase;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		if (!(beanFactory instanceof ConfigurableListableBeanFactory)) {
			throw new IllegalArgumentException(
					"DefaultLifecycleProcessor requires a ConfigurableListableBeanFactory: " + beanFactory);
		}
		this.beanFactory = (ConfigurableListableBeanFactory) beanFactory;
	}


	// Lifecycle implementation

	/**
	 * 启动所有实现{@link Lifecycle}并且尚未运行的已注册的bean.
	 * 实现{@link SmartLifecycle}的任何bean都将在其“阶段”内启动, 所有阶段将从最低到最高值进行排序.
	 * 所有未实现{@link SmartLifecycle}的bean都将在默认阶段0中启动.
	 * 声明为另一个bean的依赖项的bean将在依赖bean之前启动, 而不管声明的阶段如何.
	 */
	@Override
	public void start() {
		startBeans(false);
		this.running = true;
	}

	/**
	 * 停止所有实现{@link Lifecycle}并且当前正在运行的已注册的bean.
	 * 任何实现{@link SmartLifecycle}的bean都将在其“阶段”内停止, 并且所有阶段将从最高到最低值排序.
	 * 未实现{@link SmartLifecycle}的所有bean将在默认阶段0中停止.
	 * 声明为依赖于另一个bean的bean将在依赖项bean之前停止, 而不管声明的阶段如何.
	 */
	@Override
	public void stop() {
		stopBeans();
		this.running = false;
	}

	@Override
	public void onRefresh() {
		startBeans(true);
		this.running = true;
	}

	@Override
	public void onClose() {
		stopBeans();
		this.running = false;
	}

	@Override
	public boolean isRunning() {
		return this.running;
	}


	// Internal helpers

	private void startBeans(boolean autoStartupOnly) {
		Map<String, Lifecycle> lifecycleBeans = getLifecycleBeans();
		Map<Integer, LifecycleGroup> phases = new HashMap<Integer, LifecycleGroup>();
		for (Map.Entry<String, ? extends Lifecycle> entry : lifecycleBeans.entrySet()) {
			Lifecycle bean = entry.getValue();
			if (!autoStartupOnly || (bean instanceof SmartLifecycle && ((SmartLifecycle) bean).isAutoStartup())) {
				int phase = getPhase(bean);
				LifecycleGroup group = phases.get(phase);
				if (group == null) {
					group = new LifecycleGroup(phase, this.timeoutPerShutdownPhase, lifecycleBeans, autoStartupOnly);
					phases.put(phase, group);
				}
				group.add(entry.getKey(), bean);
			}
		}
		if (!phases.isEmpty()) {
			List<Integer> keys = new ArrayList<Integer>(phases.keySet());
			Collections.sort(keys);
			for (Integer key : keys) {
				phases.get(key).start();
			}
		}
	}

	/**
	 * 将指定的bean作为给定Lifecycle bean集合的一部分启动, 确保首先启动它所依赖的任何bean.
	 * 
	 * @param lifecycleBeans 以bean名称为键, Lifecycle实例为value的Map
	 * @param beanName 要启动的bean的名称
	 */
	private void doStart(Map<String, ? extends Lifecycle> lifecycleBeans, String beanName, boolean autoStartupOnly) {
		Lifecycle bean = lifecycleBeans.remove(beanName);
		if (bean != null && bean != this) {
			String[] dependenciesForBean = this.beanFactory.getDependenciesForBean(beanName);
			for (String dependency : dependenciesForBean) {
				doStart(lifecycleBeans, dependency, autoStartupOnly);
			}
			if (!bean.isRunning() &&
					(!autoStartupOnly || !(bean instanceof SmartLifecycle) || ((SmartLifecycle) bean).isAutoStartup())) {
				if (logger.isDebugEnabled()) {
					logger.debug("Starting bean '" + beanName + "' of type [" + bean.getClass().getName() + "]");
				}
				try {
					bean.start();
				}
				catch (Throwable ex) {
					throw new ApplicationContextException("Failed to start bean '" + beanName + "'", ex);
				}
				if (logger.isDebugEnabled()) {
					logger.debug("Successfully started bean '" + beanName + "'");
				}
			}
		}
	}

	private void stopBeans() {
		Map<String, Lifecycle> lifecycleBeans = getLifecycleBeans();
		Map<Integer, LifecycleGroup> phases = new HashMap<Integer, LifecycleGroup>();
		for (Map.Entry<String, Lifecycle> entry : lifecycleBeans.entrySet()) {
			Lifecycle bean = entry.getValue();
			int shutdownOrder = getPhase(bean);
			LifecycleGroup group = phases.get(shutdownOrder);
			if (group == null) {
				group = new LifecycleGroup(shutdownOrder, this.timeoutPerShutdownPhase, lifecycleBeans, false);
				phases.put(shutdownOrder, group);
			}
			group.add(entry.getKey(), bean);
		}
		if (!phases.isEmpty()) {
			List<Integer> keys = new ArrayList<Integer>(phases.keySet());
			Collections.sort(keys, Collections.reverseOrder());
			for (Integer key : keys) {
				phases.get(key).stop();
			}
		}
	}

	/**
	 * 将指定的bean作为给定Lifecycle bean集合的一部分停止, 确保首先停止依赖于它的任何bean.
	 * 
	 * @param lifecycleBeans 以bean名称为键, Lifecycle实例为value的Map
	 * @param beanName 要停止的bean的名称
	 */
	private void doStop(Map<String, ? extends Lifecycle> lifecycleBeans, final String beanName,
			final CountDownLatch latch, final Set<String> countDownBeanNames) {

		Lifecycle bean = lifecycleBeans.remove(beanName);
		if (bean != null) {
			String[] dependentBeans = this.beanFactory.getDependentBeans(beanName);
			for (String dependentBean : dependentBeans) {
				doStop(lifecycleBeans, dependentBean, latch, countDownBeanNames);
			}
			try {
				if (bean.isRunning()) {
					if (bean instanceof SmartLifecycle) {
						if (logger.isDebugEnabled()) {
							logger.debug("Asking bean '" + beanName + "' of type [" +
									bean.getClass().getName() + "] to stop");
						}
						countDownBeanNames.add(beanName);
						((SmartLifecycle) bean).stop(new Runnable() {
							@Override
							public void run() {
								latch.countDown();
								countDownBeanNames.remove(beanName);
								if (logger.isDebugEnabled()) {
									logger.debug("Bean '" + beanName + "' completed its stop procedure");
								}
							}
						});
					}
					else {
						if (logger.isDebugEnabled()) {
							logger.debug("Stopping bean '" + beanName + "' of type [" +
									bean.getClass().getName() + "]");
						}
						bean.stop();
						if (logger.isDebugEnabled()) {
							logger.debug("Successfully stopped bean '" + beanName + "'");
						}
					}
				}
				else if (bean instanceof SmartLifecycle) {
					// 不要等待没有运行的bean...
					latch.countDown();
				}
			}
			catch (Throwable ex) {
				if (logger.isWarnEnabled()) {
					logger.warn("Failed to stop bean '" + beanName + "'", ex);
				}
			}
		}
	}


	// overridable hooks

	/**
	 * 检索所有适用的 Lifecycle beans: 所有已经创建的单例, 以及所有SmartLifecycle bean (即使它们被标记为lazy-init).
	 * 
	 * @return 适用的bean的Map, bean名称为键, bean实例为值
	 */
	protected Map<String, Lifecycle> getLifecycleBeans() {
		Map<String, Lifecycle> beans = new LinkedHashMap<String, Lifecycle>();
		String[] beanNames = this.beanFactory.getBeanNamesForType(Lifecycle.class, false, false);
		for (String beanName : beanNames) {
			String beanNameToRegister = BeanFactoryUtils.transformedBeanName(beanName);
			boolean isFactoryBean = this.beanFactory.isFactoryBean(beanNameToRegister);
			String beanNameToCheck = (isFactoryBean ? BeanFactory.FACTORY_BEAN_PREFIX + beanName : beanName);
			if ((this.beanFactory.containsSingleton(beanNameToRegister) &&
					(!isFactoryBean || Lifecycle.class.isAssignableFrom(this.beanFactory.getType(beanNameToCheck)))) ||
					SmartLifecycle.class.isAssignableFrom(this.beanFactory.getType(beanNameToCheck))) {
				Lifecycle bean = this.beanFactory.getBean(beanNameToCheck, Lifecycle.class);
				if (bean != this) {
					beans.put(beanNameToRegister, bean);
				}
			}
		}
		return beans;
	}

	/**
	 * 确定给定bean的生命周期阶段.
	 * <p>默认实现检查{@link Phased}接口. 可以覆盖以应用其他/进一步的策略.
	 * 
	 * @param bean 要内省的bean
	 * 
	 * @return 阶段为整数值. 默认 0.
	 */
	protected int getPhase(Lifecycle bean) {
		return (bean instanceof Phased ? ((Phased) bean).getPhase() : 0);
	}


	/**
	 * Helper类, 用于维护一组Lifecycle bean, 这些bean应根据其 '阶段'值 (或默认值0)一起启动和停止.
	 */
	private class LifecycleGroup {

		private final int phase;

		private final long timeout;

		private final Map<String, ? extends Lifecycle> lifecycleBeans;

		private final boolean autoStartupOnly;

		private final List<LifecycleGroupMember> members = new ArrayList<LifecycleGroupMember>();

		private int smartMemberCount;

		public LifecycleGroup(
				int phase, long timeout, Map<String, ? extends Lifecycle> lifecycleBeans, boolean autoStartupOnly) {

			this.phase = phase;
			this.timeout = timeout;
			this.lifecycleBeans = lifecycleBeans;
			this.autoStartupOnly = autoStartupOnly;
		}

		public void add(String name, Lifecycle bean) {
			this.members.add(new LifecycleGroupMember(name, bean));
			if (bean instanceof SmartLifecycle) {
				this.smartMemberCount++;
			}
		}

		public void start() {
			if (this.members.isEmpty()) {
				return;
			}
			if (logger.isInfoEnabled()) {
				logger.info("Starting beans in phase " + this.phase);
			}
			Collections.sort(this.members);
			for (LifecycleGroupMember member : this.members) {
				if (this.lifecycleBeans.containsKey(member.name)) {
					doStart(this.lifecycleBeans, member.name, this.autoStartupOnly);
				}
			}
		}

		public void stop() {
			if (this.members.isEmpty()) {
				return;
			}
			if (logger.isInfoEnabled()) {
				logger.info("Stopping beans in phase " + this.phase);
			}
			Collections.sort(this.members, Collections.reverseOrder());
			CountDownLatch latch = new CountDownLatch(this.smartMemberCount);
			Set<String> countDownBeanNames = Collections.synchronizedSet(new LinkedHashSet<String>());
			for (LifecycleGroupMember member : this.members) {
				if (this.lifecycleBeans.containsKey(member.name)) {
					doStop(this.lifecycleBeans, member.name, latch, countDownBeanNames);
				}
				else if (member.bean instanceof SmartLifecycle) {
					// 已经删除: 必须是另一个阶段的依赖bean
					latch.countDown();
				}
			}
			try {
				latch.await(this.timeout, TimeUnit.MILLISECONDS);
				if (latch.getCount() > 0 && !countDownBeanNames.isEmpty() && logger.isWarnEnabled()) {
					logger.warn("Failed to shut down " + countDownBeanNames.size() + " bean" +
							(countDownBeanNames.size() > 1 ? "s" : "") + " with phase value " +
							this.phase + " within timeout of " + this.timeout + ": " + countDownBeanNames);
				}
			}
			catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
			}
		}
	}


	/**
	 * 使Comparable接口适应生命周期阶段模型.
	 */
	private class LifecycleGroupMember implements Comparable<LifecycleGroupMember> {

		private final String name;

		private final Lifecycle bean;

		LifecycleGroupMember(String name, Lifecycle bean) {
			this.name = name;
			this.bean = bean;
		}

		@Override
		public int compareTo(LifecycleGroupMember other) {
			int thisOrder = getPhase(this.bean);
			int otherOrder = getPhase(other.bean);
			return (thisOrder == otherOrder ? 0 : (thisOrder < otherOrder) ? -1 : 1);
		}
	}
}
