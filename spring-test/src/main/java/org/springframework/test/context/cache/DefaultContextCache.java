package org.springframework.test.context.cache;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.style.ToStringCreator;
import org.springframework.test.annotation.DirtiesContext.HierarchyMode;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.util.Assert;

/**
 * {@link ContextCache} API的默认实现.
 *
 * <p>使用配置了最大大小的同步{@link Map}和<em>最近最少使用的</em> (LRU) 逐出策略来缓存{@link ApplicationContext}实例.
 *
 * <p>最大大小可以作为{@linkplain #DefaultContextCache(int) 构造函数参数}提供,
 * 或者通过系统属性或名为{@code spring.test.context.cache.maxSize}的Spring属性设置.
 */
public class DefaultContextCache implements ContextCache {

	private static final Log statsLogger = LogFactory.getLog(CONTEXT_CACHE_LOGGING_CATEGORY);

	/**
	 * 上下文键到{@code ApplicationContext}实例的Map.
	 */
	private final Map<MergedContextConfiguration, ApplicationContext> contextMap =
			Collections.synchronizedMap(new LruCache(32, 0.75f));

	/**
	 * 父键到子键集合的Map, 表示上下文层次结构的自上而下<em>树</ em>.
	 * 此信息用于确定在删除作为其他上下文的父级的上下文时, 需要递归删除和关闭的子树.
	 */
	private final Map<MergedContextConfiguration, Set<MergedContextConfiguration>> hierarchyMap =
			new ConcurrentHashMap<MergedContextConfiguration, Set<MergedContextConfiguration>>(32);

	private final int maxSize;

	private final AtomicInteger hitCount = new AtomicInteger();

	private final AtomicInteger missCount = new AtomicInteger();


	/**
	 * 使用通过{@link ContextCacheUtils#retrieveMaxCacheSize()}获得的最大缓存大小.
	 */
	public DefaultContextCache() {
		this(ContextCacheUtils.retrieveMaxCacheSize());
	}

	/**
	 * @param maxSize 最大缓存大小
	 * 
	 * @throws IllegalArgumentException 如果提供的{@code maxSize}值不是正值
	 */
	public DefaultContextCache(int maxSize) {
		Assert.isTrue(maxSize > 0, "'maxSize' must be positive");
		this.maxSize = maxSize;
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean contains(MergedContextConfiguration key) {
		Assert.notNull(key, "Key must not be null");
		return this.contextMap.containsKey(key);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ApplicationContext get(MergedContextConfiguration key) {
		Assert.notNull(key, "Key must not be null");
		ApplicationContext context = this.contextMap.get(key);
		if (context == null) {
			this.missCount.incrementAndGet();
		}
		else {
			this.hitCount.incrementAndGet();
		}
		return context;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void put(MergedContextConfiguration key, ApplicationContext context) {
		Assert.notNull(key, "Key must not be null");
		Assert.notNull(context, "ApplicationContext must not be null");

		this.contextMap.put(key, context);
		MergedContextConfiguration child = key;
		MergedContextConfiguration parent = child.getParent();
		while (parent != null) {
			Set<MergedContextConfiguration> list = this.hierarchyMap.get(parent);
			if (list == null) {
				list = new HashSet<MergedContextConfiguration>();
				this.hierarchyMap.put(parent, list);
			}
			list.add(child);
			child = parent;
			parent = child.getParent();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void remove(MergedContextConfiguration key, HierarchyMode hierarchyMode) {
		Assert.notNull(key, "Key must not be null");

		// startKey是开始清除缓存的级别, 具体取决于配置的层次结构模式.
		MergedContextConfiguration startKey = key;
		if (hierarchyMode == HierarchyMode.EXHAUSTIVE) {
			while (startKey.getParent() != null) {
				startKey = startKey.getParent();
			}
		}

		List<MergedContextConfiguration> removedContexts = new ArrayList<MergedContextConfiguration>();
		remove(removedContexts, startKey);

		// 从层次结构Map中删除对所有已删除上下文的所有剩余引用.
		for (MergedContextConfiguration currentKey : removedContexts) {
			for (Set<MergedContextConfiguration> children : this.hierarchyMap.values()) {
				children.remove(currentKey);
			}
		}

		// 从层次结构Map中删除空条目.
		for (MergedContextConfiguration currentKey : this.hierarchyMap.keySet()) {
			if (this.hierarchyMap.get(currentKey).isEmpty()) {
				this.hierarchyMap.remove(currentKey);
			}
		}
	}

	private void remove(List<MergedContextConfiguration> removedContexts, MergedContextConfiguration key) {
		Assert.notNull(key, "Key must not be null");

		Set<MergedContextConfiguration> children = this.hierarchyMap.get(key);
		if (children != null) {
			for (MergedContextConfiguration child : children) {
				// 通过较低级别递归
				remove(removedContexts, child);
			}
			// 从层次结构Map中删除当前上下文的子集.
			this.hierarchyMap.remove(key);
		}

		// 首先物理地删除和关闭叶子节点 (i.e., 在返回堆栈的路上, 而不是在递归调用之前).
		ApplicationContext context = this.contextMap.remove(key);
		if (context instanceof ConfigurableApplicationContext) {
			((ConfigurableApplicationContext) context).close();
		}
		removedContexts.add(key);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int size() {
		return this.contextMap.size();
	}

	/**
	 * 获取此缓存的最大大小.
	 */
	public int getMaxSize() {
		return this.maxSize;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getParentContextCount() {
		return this.hierarchyMap.size();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getHitCount() {
		return this.hitCount.get();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getMissCount() {
		return this.missCount.get();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void reset() {
		synchronized (this.contextMap) {
			clear();
			clearStatistics();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void clear() {
		synchronized (this.contextMap) {
			this.contextMap.clear();
			this.hierarchyMap.clear();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void clearStatistics() {
		synchronized (this.contextMap) {
			this.hitCount.set(0);
			this.missCount.set(0);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void logStatistics() {
		if (statsLogger.isDebugEnabled()) {
			statsLogger.debug("Spring test ApplicationContext cache statistics: " + this);
		}
	}

	/**
	 * 生成包含此缓存的实现类型及其统计信息的文本字符串.
	 * <p>此方法返回的字符串包含符合{@link #logStatistics()}约定所需的所有信息.
	 * 
	 * @return 此缓存的字符串表示形式, 包括统计信息
	 */
	@Override
	public String toString() {
		return new ToStringCreator(this)
				.append("size", size())
				.append("maxSize", getMaxSize())
				.append("parentContextCount", getParentContextCount())
				.append("hitCount", getHitCount())
				.append("missCount", getMissCount())
				.toString();
	}


	/**
	 * 基于使用最大大小的{@link LinkedHashMap}和<em>最近最少使用的</em> (LRU)驱逐策略的简单缓存实现, 可以正确关闭应用程序上下文.
	 */
	@SuppressWarnings("serial")
	private class LruCache extends LinkedHashMap<MergedContextConfiguration, ApplicationContext> {

		/**
		 * 使用提供的初始容量和加载因子创建一个新的{@code LruCache}.
		 * 
		 * @param initialCapacity 初始容量
		 * @param loadFactor 加载因子
		 */
		LruCache(int initialCapacity, float loadFactor) {
			super(initialCapacity, loadFactor, true);
		}

		@Override
		protected boolean removeEldestEntry(Map.Entry<MergedContextConfiguration, ApplicationContext> eldest) {
			if (this.size() > DefaultContextCache.this.getMaxSize()) {
				// 不要删除"DefaultContextCache.this."; 否则, 不小心调用了 java.util.Map.remove(Object, Object).
				DefaultContextCache.this.remove(eldest.getKey(), HierarchyMode.CURRENT_LEVEL);
			}

			// 因为调用自定义驱逐算法, 所以返回false.
			return false;
		}
	}

}
