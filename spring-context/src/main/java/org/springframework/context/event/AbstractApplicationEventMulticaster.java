package org.springframework.context.event;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;

/**
 * {@link ApplicationEventMulticaster}接口的抽象实现, 提供基本的监听器注册工具.
 *
 * <p>默认情况下不允许同一监听器的多个实例, 因为它会将监听器保留在linked Set.
 * 用于保存ApplicationListener对象的集合类可以通过"collectionClass" bean属性重写.
 *
 * <p>实际实现ApplicationEventMulticaster的 {@link #multicastEvent} 方法留给了子类.
 * {@link SimpleApplicationEventMulticaster} 简单地将所有事件多播到所有已注册的监听器, 并在调用线程中调用它们.
 * 在这些方面, 替代实现可能更复杂.
 */
public abstract class AbstractApplicationEventMulticaster
		implements ApplicationEventMulticaster, BeanClassLoaderAware, BeanFactoryAware {

	private final ListenerRetriever defaultRetriever = new ListenerRetriever(false);

	final Map<ListenerCacheKey, ListenerRetriever> retrieverCache =
			new ConcurrentHashMap<ListenerCacheKey, ListenerRetriever>(64);

	private ClassLoader beanClassLoader;

	private BeanFactory beanFactory;

	private Object retrievalMutex = this.defaultRetriever;


	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.beanClassLoader = classLoader;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
		if (beanFactory instanceof ConfigurableBeanFactory) {
			ConfigurableBeanFactory cbf = (ConfigurableBeanFactory) beanFactory;
			if (this.beanClassLoader == null) {
				this.beanClassLoader = cbf.getBeanClassLoader();
			}
			this.retrievalMutex = cbf.getSingletonMutex();
		}
	}

	private BeanFactory getBeanFactory() {
		if (this.beanFactory == null) {
			throw new IllegalStateException("ApplicationEventMulticaster cannot retrieve listener beans " +
					"because it is not associated with a BeanFactory");
		}
		return this.beanFactory;
	}


	@Override
	public void addApplicationListener(ApplicationListener<?> listener) {
		synchronized (this.retrievalMutex) {
			// 如果已经注册, 则显式删除代理的目标, 以避免对同一个监听器进行双重调用.
			Object singletonTarget = AopProxyUtils.getSingletonTarget(listener);
			if (singletonTarget instanceof ApplicationListener) {
				this.defaultRetriever.applicationListeners.remove(singletonTarget);
			}
			this.defaultRetriever.applicationListeners.add(listener);
			this.retrieverCache.clear();
		}
	}

	@Override
	public void addApplicationListenerBean(String listenerBeanName) {
		synchronized (this.retrievalMutex) {
			this.defaultRetriever.applicationListenerBeans.add(listenerBeanName);
			this.retrieverCache.clear();
		}
	}

	@Override
	public void removeApplicationListener(ApplicationListener<?> listener) {
		synchronized (this.retrievalMutex) {
			this.defaultRetriever.applicationListeners.remove(listener);
			this.retrieverCache.clear();
		}
	}

	@Override
	public void removeApplicationListenerBean(String listenerBeanName) {
		synchronized (this.retrievalMutex) {
			this.defaultRetriever.applicationListenerBeans.remove(listenerBeanName);
			this.retrieverCache.clear();
		}
	}

	@Override
	public void removeAllListeners() {
		synchronized (this.retrievalMutex) {
			this.defaultRetriever.applicationListeners.clear();
			this.defaultRetriever.applicationListenerBeans.clear();
			this.retrieverCache.clear();
		}
	}


	/**
	 * 返回包含的所有ApplicationListeners.
	 * 
	 * @return ApplicationListeners集合
	 */
	protected Collection<ApplicationListener<?>> getApplicationListeners() {
		synchronized (this.retrievalMutex) {
			return this.defaultRetriever.getApplicationListeners();
		}
	}

	/**
	 * 返回与给定事件类型匹配的ApplicationListeners集合. 不匹配的监听器会尽早的被排除在外.
	 * 
	 * @param event 要传播的事件. 允许根据缓存的匹配信息尽早排除不匹配的监听器.
	 * @param eventType 事件类型
	 * 
	 * @return ApplicationListeners集合
	 */
	protected Collection<ApplicationListener<?>> getApplicationListeners(
			ApplicationEvent event, ResolvableType eventType) {

		Object source = event.getSource();
		Class<?> sourceType = (source != null ? source.getClass() : null);
		ListenerCacheKey cacheKey = new ListenerCacheKey(eventType, sourceType);

		// 快速检查ConcurrentHashMap上的现有条目...
		ListenerRetriever retriever = this.retrieverCache.get(cacheKey);
		if (retriever != null) {
			return retriever.getApplicationListeners();
		}

		if (this.beanClassLoader == null ||
				(ClassUtils.isCacheSafe(event.getClass(), this.beanClassLoader) &&
						(sourceType == null || ClassUtils.isCacheSafe(sourceType, this.beanClassLoader)))) {
			// 完全同步构建和缓存ListenerRetriever
			synchronized (this.retrievalMutex) {
				retriever = this.retrieverCache.get(cacheKey);
				if (retriever != null) {
					return retriever.getApplicationListeners();
				}
				retriever = new ListenerRetriever(true);
				Collection<ApplicationListener<?>> listeners =
						retrieveApplicationListeners(eventType, sourceType, retriever);
				this.retrieverCache.put(cacheKey, retriever);
				return listeners;
			}
		}
		else {
			// No ListenerRetriever caching -> no synchronization necessary
			return retrieveApplicationListeners(eventType, sourceType, null);
		}
	}

	/**
	 * 实际检索给定事件和源类型的应用程序监听器.
	 * 
	 * @param eventType 事件类型
	 * @param sourceType 事件源类型
	 * @param retriever ListenerRetriever, 如果应该填充一个 (用于缓存目的)
	 * 
	 * @return 给定事件和源类型的预过滤的应用程序监听器列表
	 */
	private Collection<ApplicationListener<?>> retrieveApplicationListeners(
			ResolvableType eventType, Class<?> sourceType, ListenerRetriever retriever) {

		LinkedList<ApplicationListener<?>> allListeners = new LinkedList<ApplicationListener<?>>();
		Set<ApplicationListener<?>> listeners;
		Set<String> listenerBeans;
		synchronized (this.retrievalMutex) {
			listeners = new LinkedHashSet<ApplicationListener<?>>(this.defaultRetriever.applicationListeners);
			listenerBeans = new LinkedHashSet<String>(this.defaultRetriever.applicationListenerBeans);
		}
		for (ApplicationListener<?> listener : listeners) {
			if (supportsEvent(listener, eventType, sourceType)) {
				if (retriever != null) {
					retriever.applicationListeners.add(listener);
				}
				allListeners.add(listener);
			}
		}
		if (!listenerBeans.isEmpty()) {
			BeanFactory beanFactory = getBeanFactory();
			for (String listenerBeanName : listenerBeans) {
				try {
					Class<?> listenerType = beanFactory.getType(listenerBeanName);
					if (listenerType == null || supportsEvent(listenerType, eventType)) {
						ApplicationListener<?> listener =
								beanFactory.getBean(listenerBeanName, ApplicationListener.class);
						if (!allListeners.contains(listener) && supportsEvent(listener, eventType, sourceType)) {
							if (retriever != null) {
								retriever.applicationListenerBeans.add(listenerBeanName);
							}
							allListeners.add(listener);
						}
					}
				}
				catch (NoSuchBeanDefinitionException ex) {
					// 单例监听器实例 (没有支持bean定义) 消失 - 可能在销毁阶段的中间
				}
			}
		}
		AnnotationAwareOrderComparator.sort(allListeners);
		return allListeners;
	}

	/**
	 * 在尝试实例化之前, 通过检查其通常声明的事件类型来提前过滤监听器.
	 * <p>如果此方法返回给定监听器的{@code true}作为第一遍,
	 * 之后将通过 {@link #supportsEvent(ApplicationListener,ResolvableType, Class)}调用检索并完全评估监听器实例.
	 * 
	 * @param listenerType 由BeanFactory确定的监听器类型
	 * @param eventType 要检查的事件类型
	 * 
	 * @return 是否应将给定的监听器包含在给定事件类型的候选者中
	 */
	protected boolean supportsEvent(Class<?> listenerType, ResolvableType eventType) {
		if (GenericApplicationListener.class.isAssignableFrom(listenerType) ||
				SmartApplicationListener.class.isAssignableFrom(listenerType)) {
			return true;
		}
		ResolvableType declaredEventType = GenericApplicationListenerAdapter.resolveDeclaredEventType(listenerType);
		return (declaredEventType == null || declaredEventType.isAssignableFrom(eventType));
	}

	/**
	 * 确定给定监听器是否支持给定事件.
	 * <p>默认实现检测 {@link SmartApplicationListener} 和 {@link GenericApplicationListener}接口.
	 * 对于标准{@link ApplicationListener}, 将使用{@link GenericApplicationListenerAdapter}来内省一般声明的目标监听器类型.
	 * 
	 * @param listener 要检查的目标监听器
	 * @param eventType 要检查的事件类型
	 * @param sourceType 要检查的源类型
	 * 
	 * @return 是否应将给定的监听器包含在给定事件类型的候选者中
	 */
	protected boolean supportsEvent(ApplicationListener<?> listener, ResolvableType eventType, Class<?> sourceType) {
		GenericApplicationListener smartListener = (listener instanceof GenericApplicationListener ?
				(GenericApplicationListener) listener : new GenericApplicationListenerAdapter(listener));
		return (smartListener.supportsEventType(eventType) && smartListener.supportsSourceType(sourceType));
	}


	/**
	 * ListenerRetrievers的缓存键, 基于事件类型和源类型.
	 */
	private static final class ListenerCacheKey implements Comparable<ListenerCacheKey> {

		private final ResolvableType eventType;

		private final Class<?> sourceType;

		public ListenerCacheKey(ResolvableType eventType, Class<?> sourceType) {
			this.eventType = eventType;
			this.sourceType = sourceType;
		}

		@Override
		public boolean equals(Object other) {
			if (this == other) {
				return true;
			}
			ListenerCacheKey otherKey = (ListenerCacheKey) other;
			return (ObjectUtils.nullSafeEquals(this.eventType, otherKey.eventType) &&
					ObjectUtils.nullSafeEquals(this.sourceType, otherKey.sourceType));
		}

		@Override
		public int hashCode() {
			return (ObjectUtils.nullSafeHashCode(this.eventType) * 29 + ObjectUtils.nullSafeHashCode(this.sourceType));
		}

		@Override
		public String toString() {
			return "ListenerCacheKey [eventType = " + this.eventType + ", sourceType = " + this.sourceType.getName() + "]";
		}

		@Override
		public int compareTo(ListenerCacheKey other) {
			int result = 0;
			if (this.eventType != null) {
				result = this.eventType.toString().compareTo(other.eventType.toString());
			}
			if (result == 0 && this.sourceType != null) {
				result = this.sourceType.getName().compareTo(other.sourceType.getName());
			}
			return result;
		}
	}


	/**
	 * Helper类, 它封装了一组特定的目标监听器, 允许有效检索预过滤的监听器.
	 * <p>每个事件类型和源类型都会缓存此帮助程序的实例.
	 */
	private class ListenerRetriever {

		public final Set<ApplicationListener<?>> applicationListeners;

		public final Set<String> applicationListenerBeans;

		private final boolean preFiltered;

		public ListenerRetriever(boolean preFiltered) {
			this.applicationListeners = new LinkedHashSet<ApplicationListener<?>>();
			this.applicationListenerBeans = new LinkedHashSet<String>();
			this.preFiltered = preFiltered;
		}

		public Collection<ApplicationListener<?>> getApplicationListeners() {
			LinkedList<ApplicationListener<?>> allListeners = new LinkedList<ApplicationListener<?>>();
			for (ApplicationListener<?> listener : this.applicationListeners) {
				allListeners.add(listener);
			}
			if (!this.applicationListenerBeans.isEmpty()) {
				BeanFactory beanFactory = getBeanFactory();
				for (String listenerBeanName : this.applicationListenerBeans) {
					try {
						ApplicationListener<?> listener = beanFactory.getBean(listenerBeanName, ApplicationListener.class);
						if (this.preFiltered || !allListeners.contains(listener)) {
							allListeners.add(listener);
						}
					}
					catch (NoSuchBeanDefinitionException ex) {
						// 单例监听器实例 (没有支持bean定义) 消失 - 可能在销毁阶段的中间
					}
				}
			}
			AnnotationAwareOrderComparator.sort(allListeners);
			return allListeners;
		}
	}
}
