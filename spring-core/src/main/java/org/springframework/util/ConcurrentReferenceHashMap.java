package org.springframework.util;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.lang.reflect.Array;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A {@link ConcurrentHashMap}, 使用{@{@link ReferenceType#SOFT 软}
 * 或{@linkplain ReferenceType#WEAK 弱}引用的{@code keys}和{@code values}.
 *
 * <p>此类可用作
 * {@code Collections.synchronizedMap(new WeakHashMap<K, Reference<V>>())}的替代方法,
 * 以便在并发访问时支持更好的性能.
 * 此实现遵循与{@link ConcurrentHashMap}相同的设计约定, 但支持{@code null}值和{@code null}键.
 *
 * <p><b>NOTE:</b> 引用的使用意味着无法保证放入Map的项目随后可用.
 * 垃圾收集器可能随时丢弃引用, 因此可能看起来像未知线程正在静默删除条目.
 *
 * <p>如果没有明确指定, 此实现将使用{@linkplain SoftReference 软引用}.
 *
 * @param <K> 键的类型
 * @param <V> 值的类型
 */
public class ConcurrentReferenceHashMap<K, V> extends AbstractMap<K, V> implements ConcurrentMap<K, V> {

	private static final int DEFAULT_INITIAL_CAPACITY = 16;

	private static final float DEFAULT_LOAD_FACTOR = 0.75f;

	private static final int DEFAULT_CONCURRENCY_LEVEL = 16;

	private static final ReferenceType DEFAULT_REFERENCE_TYPE = ReferenceType.SOFT;

	private static final int MAXIMUM_CONCURRENCY_LEVEL = 1 << 16;

	private static final int MAXIMUM_SEGMENT_SIZE = 1 << 30;


	/**
	 * 使用散列中的高阶位索引的段数组.
	 */
	private final Segment[] segments;

	/**
	 * 当每个表的平均引用数超过此值时, 将尝试调整大小.
	 */
	private final float loadFactor;

	/**
	 * 引用类型: SOFT or WEAK.
	 */
	private final ReferenceType referenceType;

	/**
	 * 用于计算段数组大小的移位值和来自散列的索引.
	 */
	private final int shift;

	/**
	 * 后期绑定条目集.
	 */
	private volatile Set<Map.Entry<K, V>> entrySet;


	public ConcurrentReferenceHashMap() {
		this(DEFAULT_INITIAL_CAPACITY, DEFAULT_LOAD_FACTOR, DEFAULT_CONCURRENCY_LEVEL, DEFAULT_REFERENCE_TYPE);
	}

	/**
	 * @param initialCapacity Map的初始容量
	 */
	public ConcurrentReferenceHashMap(int initialCapacity) {
		this(initialCapacity, DEFAULT_LOAD_FACTOR, DEFAULT_CONCURRENCY_LEVEL, DEFAULT_REFERENCE_TYPE);
	}

	/**
	 * @param initialCapacity Map的初始容量
	 * @param loadFactor 加载因子. 当每个表的平均引用数超过此值时, 将尝试调整大小
	 */
	public ConcurrentReferenceHashMap(int initialCapacity, float loadFactor) {
		this(initialCapacity, loadFactor, DEFAULT_CONCURRENCY_LEVEL, DEFAULT_REFERENCE_TYPE);
	}

	/**
	 * @param initialCapacity Map的初始容量
	 * @param concurrencyLevel 并发写入Map的线程数
	 */
	public ConcurrentReferenceHashMap(int initialCapacity, int concurrencyLevel) {
		this(initialCapacity, DEFAULT_LOAD_FACTOR, concurrencyLevel, DEFAULT_REFERENCE_TYPE);
	}

	/**
	 * @param initialCapacity Map的初始容量
	 * @param referenceType 用于条目的引用类型 (软或弱)
	 */
	public ConcurrentReferenceHashMap(int initialCapacity, ReferenceType referenceType) {
		this(initialCapacity, DEFAULT_LOAD_FACTOR, DEFAULT_CONCURRENCY_LEVEL, referenceType);
	}

	/**
	 * @param initialCapacity Map的初始容量
	 * @param loadFactor 加载因子. 当每个表的平均引用数超过此值时, 将尝试调整大小.
	 * @param concurrencyLevel 并发写入Map的线程数
	 */
	public ConcurrentReferenceHashMap(int initialCapacity, float loadFactor, int concurrencyLevel) {
		this(initialCapacity, loadFactor, concurrencyLevel, DEFAULT_REFERENCE_TYPE);
	}

	/**
	 * @param initialCapacity Map的初始容量
	 * @param loadFactor 加载因子. 当每个表的平均引用数超过此值时, 将尝试调整大小.
	 * @param concurrencyLevel 并发写入Map的线程数
	 * @param referenceType 用于条目的引用类型 (软或弱)
	 */
	@SuppressWarnings("unchecked")
	public ConcurrentReferenceHashMap(
			int initialCapacity, float loadFactor, int concurrencyLevel, ReferenceType referenceType) {

		Assert.isTrue(initialCapacity >= 0, "Initial capacity must not be negative");
		Assert.isTrue(loadFactor > 0f, "Load factor must be positive");
		Assert.isTrue(concurrencyLevel > 0, "Concurrency level must be positive");
		Assert.notNull(referenceType, "Reference type must not be null");
		this.loadFactor = loadFactor;
		this.shift = calculateShift(concurrencyLevel, MAXIMUM_CONCURRENCY_LEVEL);
		int size = 1 << this.shift;
		this.referenceType = referenceType;
		int roundedUpSegmentCapacity = (int) ((initialCapacity + size - 1L) / size);
		this.segments = (Segment[]) Array.newInstance(Segment.class, size);
		for (int i = 0; i < this.segments.length; i++) {
			this.segments[i] = new Segment(roundedUpSegmentCapacity);
		}
	}


	protected final float getLoadFactor() {
		return this.loadFactor;
	}

	protected final int getSegmentsSize() {
		return this.segments.length;
	}

	protected final Segment getSegment(int index) {
		return this.segments[index];
	}

	/**
	 * 返回{@link ReferenceManager}的工厂方法.
	 * 每个{@link Segment}都会调用一次此方法.
	 * 
	 * @return 一个新的引用管理器
	 */
	protected ReferenceManager createReferenceManager() {
		return new ReferenceManager();
	}

	/**
	 * 获取给定对象的哈希值, 应用其他哈希函数以减少冲突.
	 * 此实现使用与{@link ConcurrentHashMap}相同的 Wang/Jenkins算法.
	 * 子类可以覆盖以提供替代散列.
	 * 
	 * @param o 要哈希的对象 (may be null)
	 * 
	 * @return 生成的哈希码
	 */
	protected int getHash(Object o) {
		int hash = (o != null ? o.hashCode() : 0);
		hash += (hash << 15) ^ 0xffffcd7d;
		hash ^= (hash >>> 10);
		hash += (hash << 3);
		hash ^= (hash >>> 6);
		hash += (hash << 2) + (hash << 14);
		hash ^= (hash >>> 16);
		return hash;
	}

	@Override
	public V get(Object key) {
		Entry<K, V> entry = getEntryIfAvailable(key);
		return (entry != null ? entry.getValue() : null);
	}

	@Override
	public V getOrDefault(Object key, V defaultValue) {
		Entry<K, V> entry = getEntryIfAvailable(key);
		return (entry != null ? entry.getValue() : defaultValue);
	}

	@Override
	public boolean containsKey(Object key) {
		Entry<K, V> entry = getEntryIfAvailable(key);
		return (entry != null && ObjectUtils.nullSafeEquals(entry.getKey(), key));
	}

	private Entry<K, V> getEntryIfAvailable(Object key) {
		Reference<K, V> ref = getReference(key, Restructure.WHEN_NECESSARY);
		return (ref != null ? ref.get() : null);
	}

	/**
	 * 返回指定{@code key}的{@link Entry}的{@link Reference}, 或{@code null}.
	 * 
	 * @param key 键 (can be {@code null})
	 * @param restructure 此次调用期间允许的重组类型
	 * 
	 * @return 引用, 或{@code null}
	 */
	protected final Reference<K, V> getReference(Object key, Restructure restructure) {
		int hash = getHash(key);
		return getSegmentForHash(hash).getReference(key, hash, restructure);
	}

	@Override
	public V put(K key, V value) {
		return put(key, value, true);
	}

	@Override
	public V putIfAbsent(K key, V value) {
		return put(key, value, false);
	}

	private V put(final K key, final V value, final boolean overwriteExisting) {
		return doTask(key, new Task<V>(TaskOption.RESTRUCTURE_BEFORE, TaskOption.RESIZE) {
			@Override
			protected V execute(Reference<K, V> ref, Entry<K, V> entry, Entries entries) {
				if (entry != null) {
					V oldValue = entry.getValue();
					if (overwriteExisting) {
						entry.setValue(value);
					}
					return oldValue;
				}
				entries.add(value);
				return null;
			}
		});
	}

	@Override
	public V remove(Object key) {
		return doTask(key, new Task<V>(TaskOption.RESTRUCTURE_AFTER, TaskOption.SKIP_IF_EMPTY) {
			@Override
			protected V execute(Reference<K, V> ref, Entry<K, V> entry) {
				if (entry != null) {
					ref.release();
					return entry.value;
				}
				return null;
			}
		});
	}

	@Override
	public boolean remove(Object key, final Object value) {
		return doTask(key, new Task<Boolean>(TaskOption.RESTRUCTURE_AFTER, TaskOption.SKIP_IF_EMPTY) {
			@Override
			protected Boolean execute(Reference<K, V> ref, Entry<K, V> entry) {
				if (entry != null && ObjectUtils.nullSafeEquals(entry.getValue(), value)) {
					ref.release();
					return true;
				}
				return false;
			}
		});
	}

	@Override
	public boolean replace(K key, final V oldValue, final V newValue) {
		return doTask(key, new Task<Boolean>(TaskOption.RESTRUCTURE_BEFORE, TaskOption.SKIP_IF_EMPTY) {
			@Override
			protected Boolean execute(Reference<K, V> ref, Entry<K, V> entry) {
				if (entry != null && ObjectUtils.nullSafeEquals(entry.getValue(), oldValue)) {
					entry.setValue(newValue);
					return true;
				}
				return false;
			}
		});
	}

	@Override
	public V replace(K key, final V value) {
		return doTask(key, new Task<V>(TaskOption.RESTRUCTURE_BEFORE, TaskOption.SKIP_IF_EMPTY) {
			@Override
			protected V execute(Reference<K, V> ref, Entry<K, V> entry) {
				if (entry != null) {
					V oldValue = entry.getValue();
					entry.setValue(value);
					return oldValue;
				}
				return null;
			}
		});
	}

	@Override
	public void clear() {
		for (Segment segment : this.segments) {
			segment.clear();
		}
	}

	/**
	 * 删除任何已被垃圾收集且不再引用的条目.
	 * 在正常情况下, 垃圾收集的条目会在向Map添加或删除项目时自动清除.
	 * 此方法可用于强制清除, 并且在频繁读取Map但不经常更新时非常有用.
	 */
	public void purgeUnreferencedEntries() {
		for (Segment segment : this.segments) {
			segment.restructureIfNecessary(false);
		}
	}


	@Override
	public int size() {
		int size = 0;
		for (Segment segment : this.segments) {
			size += segment.getCount();
		}
		return size;
	}

	@Override
	public boolean isEmpty() {
		for (Segment segment : this.segments) {
			if (segment.getCount() > 0) {
				return false;
			}
		}
		return true;
	}

	@Override
	public Set<Map.Entry<K, V>> entrySet() {
		Set<Map.Entry<K, V>> entrySet = this.entrySet;
		if (entrySet == null) {
			entrySet = new EntrySet();
			this.entrySet = entrySet;
		}
		return entrySet;
	}

	private <T> T doTask(Object key, Task<T> task) {
		int hash = getHash(key);
		return getSegmentForHash(hash).doTask(hash, key, task);
	}

	private Segment getSegmentForHash(int hash) {
		return this.segments[(hash >>> (32 - this.shift)) & (this.segments.length - 1)];
	}

	/**
	 * 计算可用于在指定的最大值和最小值之间创建二次幂值的移位值.
	 * 
	 * @param minimumValue 最小值
	 * @param maximumValue 最大值
	 * 
	 * @return 计算出的移位 (使用{@code 1 << shift}来获取值)
	 */
	protected static int calculateShift(int minimumValue, int maximumValue) {
		int shift = 0;
		int value = 1;
		while (value < minimumValue && value < maximumValue) {
			value <<= 1;
			shift++;
		}
		return shift;
	}


	/**
	 * 此Map支持的各种引用类型.
	 */
	public enum ReferenceType {

		/** Use {@link SoftReference}s */
		SOFT,

		/** Use {@link WeakReference}s */
		WEAK
	}


	/**
	 * 用于划分Map的单个段, 以实现更好的并发性能.
	 */
	@SuppressWarnings("serial")
	protected final class Segment extends ReentrantLock {

		private final ReferenceManager referenceManager;

		private final int initialSize;

		/**
		 * 使用散列中的低位比特索引的引用数组.
		 * 此属性应仅与{@code resizeThreshold}一起设置.
		 */
		private volatile Reference<K, V>[] references;

		/**
		 * 此段中包含的引用总数.
		 * 包括链接引用和已被垃圾收集但未清除的引用.
		 */
		private volatile int count = 0;

		/**
		 * 应该调整引用大小时的阈值.
		 * 当{@code count}超过此值时, 将调整引用大小.
		 */
		private int resizeThreshold;

		public Segment(int initialCapacity) {
			this.referenceManager = createReferenceManager();
			this.initialSize = 1 << calculateShift(initialCapacity, MAXIMUM_SEGMENT_SIZE);
			setReferences(createReferenceArray(this.initialSize));
		}

		public Reference<K, V> getReference(Object key, int hash, Restructure restructure) {
			if (restructure == Restructure.WHEN_NECESSARY) {
				restructureIfNecessary(false);
			}
			if (this.count == 0) {
				return null;
			}
			// 使用本地副本来防止其他线程写入
			Reference<K, V>[] references = this.references;
			int index = getIndex(hash, references);
			Reference<K, V> head = references[index];
			return findInChain(head, key, hash);
		}

		/**
		 * 对此段应用更新操作.
		 * 该段将在更新期间锁定.
		 * 
		 * @param hash 键的哈希值
		 * @param key 键
		 * @param task 更新操作
		 * 
		 * @return 操作的结果
		 */
		public <T> T doTask(final int hash, final Object key, final Task<T> task) {
			boolean resize = task.hasOption(TaskOption.RESIZE);
			if (task.hasOption(TaskOption.RESTRUCTURE_BEFORE)) {
				restructureIfNecessary(resize);
			}
			if (task.hasOption(TaskOption.SKIP_IF_EMPTY) && this.count == 0) {
				return task.execute(null, null, null);
			}
			lock();
			try {
				final int index = getIndex(hash, this.references);
				final Reference<K, V> head = this.references[index];
				Reference<K, V> ref = findInChain(head, key, hash);
				Entry<K, V> entry = (ref != null ? ref.get() : null);
				Entries entries = new Entries() {
					@Override
					public void add(V value) {
						@SuppressWarnings("unchecked")
						Entry<K, V> newEntry = new Entry<K, V>((K) key, value);
						Reference<K, V> newReference = Segment.this.referenceManager.createReference(newEntry, hash, head);
						Segment.this.references[index] = newReference;
						Segment.this.count++;
					}
				};
				return task.execute(ref, entry, entries);
			}
			finally {
				unlock();
				if (task.hasOption(TaskOption.RESTRUCTURE_AFTER)) {
					restructureIfNecessary(resize);
				}
			}
		}

		/**
		 * 清除此段中的所有条目.
		 */
		public void clear() {
			if (this.count == 0) {
				return;
			}
			lock();
			try {
				setReferences(createReferenceArray(this.initialSize));
				this.count = 0;
			}
			finally {
				unlock();
			}
		}

		/**
		 * 必要时重构底层数据结构.
		 * 此方法可以增加引用表的大小以及清除任何已被垃圾回收的引用.
		 * 
		 * @param allowResize 如果允许调整大小
		 */
		protected final void restructureIfNecessary(boolean allowResize) {
			boolean needsResize = (this.count > 0 && this.count >= this.resizeThreshold);
			Reference<K, V> ref = this.referenceManager.pollForPurge();
			if (ref != null || (needsResize && allowResize)) {
				lock();
				try {
					int countAfterRestructure = this.count;
					Set<Reference<K, V>> toPurge = Collections.emptySet();
					if (ref != null) {
						toPurge = new HashSet<Reference<K, V>>();
						while (ref != null) {
							toPurge.add(ref);
							ref = this.referenceManager.pollForPurge();
						}
					}
					countAfterRestructure -= toPurge.size();

					// 重新计算考虑锁内部的计数和将被清除的项目
					needsResize = (countAfterRestructure > 0 && countAfterRestructure >= this.resizeThreshold);
					boolean resizing = false;
					int restructureSize = this.references.length;
					if (allowResize && needsResize && restructureSize < MAXIMUM_SEGMENT_SIZE) {
						restructureSize <<= 1;
						resizing = true;
					}

					// 创建新表或重用现有表
					Reference<K, V>[] restructured =
							(resizing ? createReferenceArray(restructureSize) : this.references);

					// Restructure
					for (int i = 0; i < this.references.length; i++) {
						ref = this.references[i];
						if (!resizing) {
							restructured[i] = null;
						}
						while (ref != null) {
							if (!toPurge.contains(ref) && (ref.get() != null)) {
								int index = getIndex(ref.getHash(), restructured);
								restructured[index] = this.referenceManager.createReference(
										ref.get(), ref.getHash(), restructured[index]);
							}
							ref = ref.getNext();
						}
					}

					// Replace volatile members
					if (resizing) {
						setReferences(restructured);
					}
					this.count = Math.max(countAfterRestructure, 0);
				}
				finally {
					unlock();
				}
			}
		}

		private Reference<K, V> findInChain(Reference<K, V> ref, Object key, int hash) {
			Reference<K, V> currRef = ref;
			while (currRef != null) {
				if (currRef.getHash() == hash) {
					Entry<K, V> entry = currRef.get();
					if (entry != null) {
						K entryKey = entry.getKey();
						if (ObjectUtils.nullSafeEquals(entryKey, key)) {
							return currRef;
						}
					}
				}
				currRef = currRef.getNext();
			}
			return null;
		}

		@SuppressWarnings({"rawtypes", "unchecked"})
		private Reference<K, V>[] createReferenceArray(int size) {
			return new Reference[size];
		}

		private int getIndex(int hash, Reference<K, V>[] references) {
			return (hash & (references.length - 1));
		}

		/**
		 * 用新值替换引用, 重新计算resizeThreshold.
		 * 
		 * @param references 新的引用
		 */
		private void setReferences(Reference<K, V>[] references) {
			this.references = references;
			this.resizeThreshold = (int) (references.length * getLoadFactor());
		}

		/**
		 * 返回当前引用数组的大小.
		 */
		public final int getSize() {
			return this.references.length;
		}

		/**
		 * 返回此段中的引用总数.
		 */
		public final int getCount() {
			return this.count;
		}
	}


	/**
	 * Map中包含的{@link Entry}的引用.
	 * 实现通常是围绕特定Java引用实现的包装器 (e.g., {@link SoftReference}).
	 */
	protected interface Reference<K, V> {

		/**
		 * 返回引用的条目, 或{@code null} 如果条目不再可用.
		 */
		Entry<K, V> get();

		/**
		 * 返回引用的哈希值.
		 */
		int getHash();

		/**
		 * 返回链中的下一个引用, 或{@code null}.
		 */
		Reference<K, V> getNext();

		/**
		 * 发布此条目并确保它将从{@code ReferenceManager#pollForPurge()}返回.
		 */
		void release();
	}


	/**
	 * Map条目.
	 */
	protected static final class Entry<K, V> implements Map.Entry<K, V> {

		private final K key;

		private volatile V value;

		public Entry(K key, V value) {
			this.key = key;
			this.value = value;
		}

		@Override
		public K getKey() {
			return this.key;
		}

		@Override
		public V getValue() {
			return this.value;
		}

		@Override
		public V setValue(V value) {
			V previous = this.value;
			this.value = value;
			return previous;
		}

		@Override
		public String toString() {
			return (this.key + "=" + this.value);
		}

		@Override
		@SuppressWarnings("rawtypes")
		public final boolean equals(Object other) {
			if (this == other) {
				return true;
			}
			if (!(other instanceof Map.Entry)) {
				return false;
			}
			Map.Entry otherEntry = (Map.Entry) other;
			return (ObjectUtils.nullSafeEquals(getKey(), otherEntry.getKey()) &&
					ObjectUtils.nullSafeEquals(getValue(), otherEntry.getValue()));
		}

		@Override
		public final int hashCode() {
			return (ObjectUtils.nullSafeHashCode(this.key) ^ ObjectUtils.nullSafeHashCode(this.value));
		}
	}


	/**
	 * 针对{@link Segment}的{@link Segment#doTask run}任务.
	 */
	private abstract class Task<T> {

		private final EnumSet<TaskOption> options;

		public Task(TaskOption... options) {
			this.options = (options.length == 0 ? EnumSet.noneOf(TaskOption.class) : EnumSet.of(options[0], options));
		}

		public boolean hasOption(TaskOption option) {
			return this.options.contains(option);
		}

		/**
		 * 执行任务.
		 * 
		 * @param ref 找到的引用 (or {@code null})
		 * @param entry 找到的条目 (or {@code null})
		 * @param entries 访问底层条目
		 * 
		 * @return 任务的结果
		 */
		protected T execute(Reference<K, V> ref, Entry<K, V> entry, Entries entries) {
			return execute(ref, entry);
		}

		/**
		 * 可用于不需要访问{@link Entries}的任务的便捷方法.
		 * 
		 * @param ref 找到的引用 (or {@code null})
		 * @param entry 找到的条目 (or {@code null})
		 * 
		 * @return 任务的结果
		 */
		protected T execute(Reference<K, V> ref, Entry<K, V> entry) {
			return null;
		}
	}


	/**
	 * {@code Task}支持的各种选项.
	 */
	private enum TaskOption {

		RESTRUCTURE_BEFORE, RESTRUCTURE_AFTER, SKIP_IF_EMPTY, RESIZE
	}


	/**
	 * 允许任务访问{@link Segment}条目.
	 */
	private abstract class Entries {

		/**
		 * 添加具有指定值的新条目.
		 * 
		 * @param value 要添加的值
		 */
		public abstract void add(V value);
	}


	/**
	 * 内部条目集合的实现.
	 */
	private class EntrySet extends AbstractSet<Map.Entry<K, V>> {

		@Override
		public Iterator<Map.Entry<K, V>> iterator() {
			return new EntryIterator();
		}

		@Override
		public boolean contains(Object o) {
			if (o instanceof Map.Entry<?, ?>) {
				Map.Entry<?, ?> entry = (Map.Entry<?, ?>) o;
				Reference<K, V> ref = ConcurrentReferenceHashMap.this.getReference(entry.getKey(), Restructure.NEVER);
				Entry<K, V> otherEntry = (ref != null ? ref.get() : null);
				if (otherEntry != null) {
					return ObjectUtils.nullSafeEquals(otherEntry.getValue(), otherEntry.getValue());
				}
			}
			return false;
		}

		@Override
		public boolean remove(Object o) {
			if (o instanceof Map.Entry<?, ?>) {
				Map.Entry<?, ?> entry = (Map.Entry<?, ?>) o;
				return ConcurrentReferenceHashMap.this.remove(entry.getKey(), entry.getValue());
			}
			return false;
		}

		@Override
		public int size() {
			return ConcurrentReferenceHashMap.this.size();
		}

		@Override
		public void clear() {
			ConcurrentReferenceHashMap.this.clear();
		}
	}


	/**
	 * 内部条目迭代器的实现.
	 */
	private class EntryIterator implements Iterator<Map.Entry<K, V>> {

		private int segmentIndex;

		private int referenceIndex;

		private Reference<K, V>[] references;

		private Reference<K, V> reference;

		private Entry<K, V> next;

		private Entry<K, V> last;

		public EntryIterator() {
			moveToNextSegment();
		}

		@Override
		public boolean hasNext() {
			getNextIfNecessary();
			return (this.next != null);
		}

		@Override
		public Entry<K, V> next() {
			getNextIfNecessary();
			if (this.next == null) {
				throw new NoSuchElementException();
			}
			this.last = this.next;
			this.next = null;
			return this.last;
		}

		private void getNextIfNecessary() {
			while (this.next == null) {
				moveToNextReference();
				if (this.reference == null) {
					return;
				}
				this.next = this.reference.get();
			}
		}

		private void moveToNextReference() {
			if (this.reference != null) {
				this.reference = this.reference.getNext();
			}
			while (this.reference == null && this.references != null) {
				if (this.referenceIndex >= this.references.length) {
					moveToNextSegment();
					this.referenceIndex = 0;
				}
				else {
					this.reference = this.references[this.referenceIndex];
					this.referenceIndex++;
				}
			}
		}

		private void moveToNextSegment() {
			this.reference = null;
			this.references = null;
			if (this.segmentIndex < ConcurrentReferenceHashMap.this.segments.length) {
				this.references = ConcurrentReferenceHashMap.this.segments[this.segmentIndex].references;
				this.segmentIndex++;
			}
		}

		@Override
		public void remove() {
			Assert.state(this.last != null, "No element to remove");
			ConcurrentReferenceHashMap.this.remove(this.last.getKey());
		}
	}


	/**
	 * 可以执行的重组类型.
	 */
	protected enum Restructure {

		WHEN_NECESSARY, NEVER
	}


	/**
	 * 用于管理{@link Reference}的策略类.
	 * 如果需要支持其他引用类型, 则可以重写此类.
	 */
	protected class ReferenceManager {

		private final ReferenceQueue<Entry<K, V>> queue = new ReferenceQueue<Entry<K, V>>();

		/**
		 * 用于创建新{@link Reference}的工厂方法.
		 * 
		 * @param entry 引用中包含的条目
		 * @param hash 哈希值
		 * @param next 链中的下一个引用, 或{@code null}
		 * 
		 * @return 新的{@link Reference}
		 */
		public Reference<K, V> createReference(Entry<K, V> entry, int hash, Reference<K, V> next) {
			if (ConcurrentReferenceHashMap.this.referenceType == ReferenceType.WEAK) {
				return new WeakEntryReference<K, V>(entry, hash, next, this.queue);
			}
			return new SoftEntryReference<K, V>(entry, hash, next, this.queue);
		}

		/**
		 * 返回任何已被垃圾收集的引用, 并可以从底层结构中清除它; 如果没有引用需要清除, 则为 {@code null}.
		 * 这个方法必须是线程安全的, 理想情况下不应该在返回{@code null}时阻塞. 引用应该只返回一次.
		 * 
		 * @return 要清除的引用, 或 {@code null}
		 */
		@SuppressWarnings("unchecked")
		public Reference<K, V> pollForPurge() {
			return (Reference<K, V>) this.queue.poll();
		}
	}


	/**
	 * {@link SoftReference}的内部{@link Reference}实现.
	 */
	private static final class SoftEntryReference<K, V> extends SoftReference<Entry<K, V>> implements Reference<K, V> {

		private final int hash;

		private final Reference<K, V> nextReference;

		public SoftEntryReference(Entry<K, V> entry, int hash, Reference<K, V> next, ReferenceQueue<Entry<K, V>> queue) {
			super(entry, queue);
			this.hash = hash;
			this.nextReference = next;
		}

		@Override
		public int getHash() {
			return this.hash;
		}

		@Override
		public Reference<K, V> getNext() {
			return this.nextReference;
		}

		@Override
		public void release() {
			enqueue();
			clear();
		}
	}


	/**
	 * {@link WeakReference}的内部{@link Reference}实现.
	 */
	private static final class WeakEntryReference<K, V> extends WeakReference<Entry<K, V>> implements Reference<K, V> {

		private final int hash;

		private final Reference<K, V> nextReference;

		public WeakEntryReference(Entry<K, V> entry, int hash, Reference<K, V> next, ReferenceQueue<Entry<K, V>> queue) {
			super(entry, queue);
			this.hash = hash;
			this.nextReference = next;
		}

		@Override
		public int getHash() {
			return this.hash;
		}

		@Override
		public Reference<K, V> getNext() {
			return this.nextReference;
		}

		@Override
		public void release() {
			enqueue();
			clear();
		}
	}
}
