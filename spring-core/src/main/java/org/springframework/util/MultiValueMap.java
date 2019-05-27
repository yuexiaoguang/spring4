package org.springframework.util;

import java.util.List;
import java.util.Map;

/**
 * 存储多个值的{@code Map}接口的扩展.
 */
public interface MultiValueMap<K, V> extends Map<K, List<V>> {

	/**
	 * 返回给定键的第一个值.
	 * 
	 * @param key 键
	 * 
	 * @return 指定键的第一个值, 或{@code null}
	 */
	V getFirst(K key);

	/**
	 * 将给定的单个值添加到给定键的当前值列表中.
	 * 
	 * @param key 键
	 * @param value 要添加的值
	 */
	void add(K key, V value);

	/**
	 * 在给定键下设置给定的单个值.
	 * 
	 * @param key 键
	 * @param value 要添加的值
	 */
	void set(K key, V value);

	/**
	 * 在下面设置给定的值.
	 * 
	 * @param values 值.
	 */
	void setAll(Map<K, V> values);

	/**
	 * 返回此{@code MultiValueMap}中包含的第一个值.
	 * 
	 * @return 单个值
	 */
	Map<K, V> toSingleValueMap();

}
