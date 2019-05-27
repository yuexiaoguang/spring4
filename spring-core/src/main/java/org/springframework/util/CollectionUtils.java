package org.springframework.util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * 主要供框架内部使用.
 */
public abstract class CollectionUtils {

	/**
	 * 如果提供的Collection为{@code null}或为空, 则返回{@code true}.
	 * 否则返回{@code false}.
	 * 
	 * @param collection 要检查的Collection
	 * 
	 * @return 给定的Collection是否为空
	 */
	public static boolean isEmpty(Collection<?> collection) {
		return (collection == null || collection.isEmpty());
	}

	/**
	 * 如果提供的Map为{@code null}或为空, 则返回{@code true}.
	 * 否则返回{@code false}.
	 * 
	 * @param map 要检查的Map
	 * 
	 * @return 给定的Map是否为空
	 */
	public static boolean isEmpty(Map<?, ?> map) {
		return (map == null || map.isEmpty());
	}

	/**
	 * 将提供的数组转换为List. 基本类型数组被转换为适当包装类型的List.
	 * <p><b>NOTE:</b> 通常更喜欢标准的{@link Arrays#asList}方法.
	 * 这个{@code arrayToList}方法只是为了在运行时处理可能是{@code Object[]}或基本类型数组的传入Object值.
	 * <p>{@code null}源值将转换为空List.
	 * 
	 * @param source 数组(可能是基本类型的)
	 * 
	 * @return 转换后的List结果
	 */
	@SuppressWarnings("rawtypes")
	public static List arrayToList(Object source) {
		return Arrays.asList(ObjectUtils.toObjectArray(source));
	}

	/**
	 * 将给定数组合并到给定的Collection中.
	 * 
	 * @param array 要合并的数组 (may be {@code null})
	 * @param collection 将数组合并到的目标Collection
	 */
	@SuppressWarnings("unchecked")
	public static <E> void mergeArrayIntoCollection(Object array, Collection<E> collection) {
		if (collection == null) {
			throw new IllegalArgumentException("Collection must not be null");
		}
		Object[] arr = ObjectUtils.toObjectArray(array);
		for (Object elem : arr) {
			collection.add((E) elem);
		}
	}

	/**
	 * 将给定的Properties实例合并到给定的Map中, 复制所有属性(键值对).
	 * <p>使用{@code Properties.propertyNames()}来捕获链接到原始Properties实例的默认属性.
	 * 
	 * @param props 要合并的Properties实例 (may be {@code null})
	 * @param map 将属性合并到的目标Map
	 */
	@SuppressWarnings("unchecked")
	public static <K, V> void mergePropertiesIntoMap(Properties props, Map<K, V> map) {
		if (map == null) {
			throw new IllegalArgumentException("Map must not be null");
		}
		if (props != null) {
			for (Enumeration<?> en = props.propertyNames(); en.hasMoreElements();) {
				String key = (String) en.nextElement();
				Object value = props.get(key);
				if (value == null) {
					// Allow for defaults fallback or potentially overridden accessor...
					value = props.getProperty(key);
				}
				map.put((K) key, (V) value);
			}
		}
	}


	/**
	 * 检查给定的迭代器是否包含给定元素.
	 * 
	 * @param iterator 要检查的Iterator
	 * @param element 要查找的元素
	 * 
	 * @return {@code true}如果找到, 否则{@code false}
	 */
	public static boolean contains(Iterator<?> iterator, Object element) {
		if (iterator != null) {
			while (iterator.hasNext()) {
				Object candidate = iterator.next();
				if (ObjectUtils.nullSafeEquals(candidate, element)) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * 检查给定的Enumeration是否包含给定元素.
	 * 
	 * @param enumeration 要检查的Enumeration
	 * @param element 要查找的元素
	 * 
	 * @return {@code true}如果找到, 否则{@code false}
	 */
	public static boolean contains(Enumeration<?> enumeration, Object element) {
		if (enumeration != null) {
			while (enumeration.hasMoreElements()) {
				Object candidate = enumeration.nextElement();
				if (ObjectUtils.nullSafeEquals(candidate, element)) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * 检查给定的Collection是否包含给定的元素实例.
	 * <p>强制给定实例存在, 而不是为相等元素返回{@code true}.
	 * 
	 * @param collection 要检查的Collection
	 * @param element 要查找的元素
	 * 
	 * @return {@code true}如果找到, 否则{@code false}
	 */
	public static boolean containsInstance(Collection<?> collection, Object element) {
		if (collection != null) {
			for (Object candidate : collection) {
				if (candidate == element) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * 如果'{@code candidates}'中的任何元素包含在'{@code source}'中, 则返回{@code true}; 否则返回{@code false}.
	 * 
	 * @param source 源Collection
	 * @param candidates 要搜索的候选者
	 * 
	 * @return 是否找到了任何候选者
	 */
	public static boolean containsAny(Collection<?> source, Collection<?> candidates) {
		if (isEmpty(source) || isEmpty(candidates)) {
			return false;
		}
		for (Object candidate : candidates) {
			if (source.contains(candidate)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 返回'{@code source}'中包含的'{@code candidates}'中的第一个元素.
	 * 如果'{@code source}'中没有'{@code candidates}'中的元素, 则返回{@code null}.
	 * 迭代顺序是{@​​link Collection}实现特定的.
	 * 
	 * @param source 源Collection
	 * @param candidates 要搜索的候选者
	 * 
	 * @return 第一个当前对象, 或{@code null}
	 */
	@SuppressWarnings("unchecked")
	public static <E> E findFirstMatch(Collection<?> source, Collection<E> candidates) {
		if (isEmpty(source) || isEmpty(candidates)) {
			return null;
		}
		for (Object candidate : candidates) {
			if (source.contains(candidate)) {
				return (E) candidate;
			}
		}
		return null;
	}

	/**
	 * 在给定的Collection中查找给定类型的单个值.
	 * 
	 * @param collection 要搜索的Collection
	 * @param type 要查找的类型
	 * 
	 * @return 如果存在明确匹配, 则找到给定类型的值; 如果找不到或多于一个这样的值, 则找到{@code null}
	 */
	@SuppressWarnings("unchecked")
	public static <T> T findValueOfType(Collection<?> collection, Class<T> type) {
		if (isEmpty(collection)) {
			return null;
		}
		T value = null;
		for (Object element : collection) {
			if (type == null || type.isInstance(element)) {
				if (value != null) {
					// More than one value found... no clear single value.
					return null;
				}
				value = (T) element;
			}
		}
		return value;
	}

	/**
	 * 查找给定Collection中给定类型之一的单个值:
	 * 在Collection中搜索第一种类型的值, 然后搜索第二种类型的值, 等等.
	 * 
	 * @param collection 要搜索的集合
	 * @param types 要按优先顺序查找的类型
	 * 
	 * @return 如果存在明确匹配, 则为找到的给定类型之一的值; 如果没有找到或超过一个这样的值, 则为找到{@code null}
	 */
	public static Object findValueOfType(Collection<?> collection, Class<?>[] types) {
		if (isEmpty(collection) || ObjectUtils.isEmpty(types)) {
			return null;
		}
		for (Class<?> type : types) {
			Object value = findValueOfType(collection, type);
			if (value != null) {
				return value;
			}
		}
		return null;
	}

	/**
	 * 确定给定Collection是否仅包含单个唯一对象.
	 * 
	 * @param collection 要检查的集合
	 * 
	 * @return {@code true}如果集合包含对同一实例的单个引用或多个引用, 或{@code false}
	 */
	public static boolean hasUniqueObject(Collection<?> collection) {
		if (isEmpty(collection)) {
			return false;
		}
		boolean hasCandidate = false;
		Object candidate = null;
		for (Object elem : collection) {
			if (!hasCandidate) {
				hasCandidate = true;
				candidate = elem;
			}
			else if (candidate != elem) {
				return false;
			}
		}
		return true;
	}

	/**
	 * 找到给定Collection的公共元素类型.
	 * 
	 * @param collection 要检查的Collection
	 * 
	 * @return 公共元素类型, 或{@code null}如果没有找到明确的公共类型 (或者集合为空)
	 */
	public static Class<?> findCommonElementType(Collection<?> collection) {
		if (isEmpty(collection)) {
			return null;
		}
		Class<?> candidate = null;
		for (Object val : collection) {
			if (val != null) {
				if (candidate == null) {
					candidate = val.getClass();
				}
				else if (candidate != val.getClass()) {
					return null;
				}
			}
		}
		return candidate;
	}

	/**
	 * 将给定枚举中的元素编组为给定类型的数组.
	 * 枚举元素必须可分配给给定数组的类型.
	 * 返回的数组将是与给定数组不同的实例.
	 */
	public static <A, E extends A> A[] toArray(Enumeration<E> enumeration, A[] array) {
		ArrayList<A> elements = new ArrayList<A>();
		while (enumeration.hasMoreElements()) {
			elements.add(enumeration.nextElement());
		}
		return elements.toArray(array);
	}

	/**
	 * 将枚举适配为迭代器.
	 * 
	 * @param enumeration 枚举
	 * 
	 * @return 迭代器
	 */
	public static <E> Iterator<E> toIterator(Enumeration<E> enumeration) {
		return new EnumerationIterator<E>(enumeration);
	}

	/**
	 * 将{@code Map<K, List<V>>}适配为{@code MultiValueMap<K, V>}.
	 * 
	 * @param map 原始Map
	 * 
	 * @return the multi-value map
	 */
	public static <K, V> MultiValueMap<K, V> toMultiValueMap(Map<K, List<V>> map) {
		return new MultiValueMapAdapter<K, V>(map);
	}

	/**
	 * 返回指定多值Map的不可修改视图.
	 * 
	 * @param  map 要返回不可修改视图的Map.
	 * 
	 * @return 不可修改视图.
	 */
	@SuppressWarnings("unchecked")
	public static <K, V> MultiValueMap<K, V> unmodifiableMultiValueMap(MultiValueMap<? extends K, ? extends V> map) {
		Assert.notNull(map, "'map' must not be null");
		Map<K, List<V>> result = new LinkedHashMap<K, List<V>>(map.size());
		for (Map.Entry<? extends K, ? extends List<? extends V>> entry : map.entrySet()) {
			List<? extends V> values = Collections.unmodifiableList(entry.getValue());
			result.put(entry.getKey(), (List<V>) values);
		}
		Map<K, List<V>> unmodifiableMap = Collections.unmodifiableMap(result);
		return toMultiValueMap(unmodifiableMap);
	}


	/**
	 * 包装枚举的迭代器.
	 */
	private static class EnumerationIterator<E> implements Iterator<E> {

		private final Enumeration<E> enumeration;

		public EnumerationIterator(Enumeration<E> enumeration) {
			this.enumeration = enumeration;
		}

		@Override
		public boolean hasNext() {
			return this.enumeration.hasMoreElements();
		}

		@Override
		public E next() {
			return this.enumeration.nextElement();
		}

		@Override
		public void remove() throws UnsupportedOperationException {
			throw new UnsupportedOperationException("Not supported");
		}
	}


	/**
	 * 将Map适配为MultiValueMap.
	 */
	@SuppressWarnings("serial")
	private static class MultiValueMapAdapter<K, V> implements MultiValueMap<K, V>, Serializable {

		private final Map<K, List<V>> map;

		public MultiValueMapAdapter(Map<K, List<V>> map) {
			Assert.notNull(map, "'map' must not be null");
			this.map = map;
		}

		@Override
		public void add(K key, V value) {
			List<V> values = this.map.get(key);
			if (values == null) {
				values = new LinkedList<V>();
				this.map.put(key, values);
			}
			values.add(value);
		}

		@Override
		public V getFirst(K key) {
			List<V> values = this.map.get(key);
			return (values != null ? values.get(0) : null);
		}

		@Override
		public void set(K key, V value) {
			List<V> values = new LinkedList<V>();
			values.add(value);
			this.map.put(key, values);
		}

		@Override
		public void setAll(Map<K, V> values) {
			for (Entry<K, V> entry : values.entrySet()) {
				set(entry.getKey(), entry.getValue());
			}
		}

		@Override
		public Map<K, V> toSingleValueMap() {
			LinkedHashMap<K, V> singleValueMap = new LinkedHashMap<K,V>(this.map.size());
			for (Entry<K, List<V>> entry : map.entrySet()) {
				singleValueMap.put(entry.getKey(), entry.getValue().get(0));
			}
			return singleValueMap;
		}

		@Override
		public int size() {
			return this.map.size();
		}

		@Override
		public boolean isEmpty() {
			return this.map.isEmpty();
		}

		@Override
		public boolean containsKey(Object key) {
			return this.map.containsKey(key);
		}

		@Override
		public boolean containsValue(Object value) {
			return this.map.containsValue(value);
		}

		@Override
		public List<V> get(Object key) {
			return this.map.get(key);
		}

		@Override
		public List<V> put(K key, List<V> value) {
			return this.map.put(key, value);
		}

		@Override
		public List<V> remove(Object key) {
			return this.map.remove(key);
		}

		@Override
		public void putAll(Map<? extends K, ? extends List<V>> map) {
			this.map.putAll(map);
		}

		@Override
		public void clear() {
			this.map.clear();
		}

		@Override
		public Set<K> keySet() {
			return this.map.keySet();
		}

		@Override
		public Collection<List<V>> values() {
			return this.map.values();
		}

		@Override
		public Set<Entry<K, List<V>>> entrySet() {
			return this.map.entrySet();
		}

		@Override
		public boolean equals(Object other) {
			if (this == other) {
				return true;
			}
			return map.equals(other);
		}

		@Override
		public int hashCode() {
			return this.map.hashCode();
		}

		@Override
		public String toString() {
			return this.map.toString();
		}
	}
}
