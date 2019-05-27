package org.springframework.cache;

import java.util.concurrent.Callable;

/**
 * 定义公共缓存操作的接口.
 *
 * <b>Note:</b> 由于缓存的一般用法, 建议实现允许存储null值
 * (例如, 缓存返回{@code null}的方法).
 */
public interface Cache {

	/**
	 * 返回缓存的名称.
	 */
	String getName();

	/**
	 * 返回底层本机缓存提供程序.
	 */
	Object getNativeCache();

	/**
	 * 返回此缓存映射指定键的值.
	 * <p>如果缓存不包含此键的映射, 则返回{@code null};
	 * 否则, 缓存的值 (可能是{@code null}本身)将在{@link ValueWrapper}中返回.
	 * 
	 * @param key 要返回其关联值的键
	 * 
	 * @return 此缓存映射指定键的值, 包含在{@link ValueWrapper}中, 该值还可以包含缓存的{@code null}值.
	 * 直接返回{@code null}意味着缓存不包含此键的映射.
	 */
	ValueWrapper get(Object key);

	/**
	 * 返回此缓存映射指定键的值, 通常指定返回值的类型.
	 * <p>Note: {@code get}的这种变体不允许区分缓存的{@code null}值和根本找不到的缓存条目.
	 * 为此目的使用标准 {@link #get(Object)}变体.
	 * 
	 * @param key 要返回其关联值的键
	 * @param type 返回值的必需类型
	 * (可能是{@code null}以绕过类型检查; 如果在缓存中找到{@code null}值, 则指定的类型无关紧要)
	 * 
	 * @return 此缓存映射指定键的值 (可能是{@code null}本身); 如果缓存不包含此键的映射, 也可以是{@code null}
	 * @throws IllegalStateException 如果找到了缓存条目, 但未能匹配指定的类型
	 */
	<T> T get(Object key, Class<T> type);

	/**
	 * 返回此缓存映射指定键的值, 如果需要, 从{@code valueLoader}获取该值.
	 * 此方法提供了传统 "如果缓存了, 返回; 否则创建, 缓存并返回"模式的简单替代.
	 * <p>如果可能, 实现应该确保加载操作是同步的, 以便只有在对同一个键进行并发访问时才调用指定的{@code valueLoader}.
	 * <p>如果{@code valueLoader}抛出异常, 它将被包装在{@link ValueRetrievalException}中
	 * 
	 * @param key 要返回其关联值的键
	 * 
	 * @return 此缓存映射指定键的值
	 * @throws ValueRetrievalException 如果{@code valueLoader}抛出异常
	 */
	<T> T get(Object key, Callable<T> valueLoader);

	/**
	 * 将指定的值与此缓存中的指定键相关联.
	 * <p>如果缓存先前包含此键的映射, 则旧值将替换为指定的值.
	 * 
	 * @param key 与指定值关联的键
	 * @param value 与指定键关联的值
	 */
	void put(Object key, Object value);

	/**
	 * 如果尚未设置, 则将指定值与此缓存中的指定键进行原子关联.
	 * <p>等效于:
	 * <pre><code>
	 * Object existingValue = cache.get(key);
	 * if (existingValue == null) {
	 *     cache.put(key, value);
	 *     return null;
	 * } else {
	 *     return existingValue;
	 * }
	 * </code></pre>
	 * 除了动作以原子方式执行.
	 * 虽然所有开箱即用的{@link CacheManager}实现都能够以原子方式执行put,
	 * 该操作也可以分两步实现, e.g. 以非原子方式检查存在和随后的放置.
	 * 有关更多详细信息, 请查看您正在使用的本机缓存实现的文档.
	 * 
	 * @param key 与指定值关联的键
	 * @param value 与指定键关联的值
	 * 
	 * @return 此缓存映射指定键的值 (这可能是{@code null}本身),
	 * 如果缓存在此调用之前不包含该键的任何映射, 则也可以{@code null}.
	 * 因此，返回{@code null}表示给定的{@code value}已与键相关联.
	 */
	ValueWrapper putIfAbsent(Object key, Object value);

	/**
	 * 如果该键存在, 则从该缓存中删除该键的映射.
	 * 
	 * @param key 要从缓存中删除其映射的键
	 */
	void evict(Object key);

	/**
	 * 从缓存中删除所有映射.
	 */
	void clear();


	/**
	 * 表示缓存值的(包装器)对象.
	 */
	interface ValueWrapper {

		/**
		 * 返回缓存中的实际值.
		 */
		Object get();
	}


	/**
	 * 如果值加载器回调失败并出现异常, 则从{@link #get(Object, Callable)}抛出包装器异常.
	 */
	@SuppressWarnings("serial")
	class ValueRetrievalException extends RuntimeException {

		private final Object key;

		public ValueRetrievalException(Object key, Callable<?> loader, Throwable ex) {
			super(String.format("Value for key '%s' could not be loaded using '%s'", key, loader), ex);
			this.key = key;
		}

		public Object getKey() {
			return this.key;
		}
	}

}
