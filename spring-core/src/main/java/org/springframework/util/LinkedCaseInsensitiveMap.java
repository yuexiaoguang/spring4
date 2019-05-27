package org.springframework.util;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * {@link LinkedHashMap}变体, 以不区分大小写的方式存储String键, 例如, 在结果表中进行基于键的访问.
 *
 * <p>保留原始顺序以及键的原始大小, 同时允许在任何键的情况下 contains, get 和 remove调用.
 *
 * <p><i>不</i>支持{@code null}键.
 */
@SuppressWarnings("serial")
public class LinkedCaseInsensitiveMap<V> implements Map<String, V>, Serializable, Cloneable {

	private final LinkedHashMap<String, V> targetMap;

	private final HashMap<String, String> caseInsensitiveKeys;

	private final Locale locale;


	/**
	 * 根据默认的Locale存储不区分大小写的键 (默认为小写).
	 */
	public LinkedCaseInsensitiveMap() {
		this((Locale) null);
	}

	/**
	 * 根据给定的Locale存储不区分大小写的键 (默认为小写).
	 * 
	 * @param locale 用于不区分大小写的键转换的Locale
	 */
	public LinkedCaseInsensitiveMap(Locale locale) {
		this(16, locale);
	}

	/**
	 * 包装具有给定初始容量的{@link LinkedHashMap}, 并根据默认的Locale存储不区分大小写的键 (默认为小写).
	 * 
	 * @param initialCapacity 初始容量
	 */
	public LinkedCaseInsensitiveMap(int initialCapacity) {
		this(initialCapacity, null);
	}

	/**
	 * 包装具有给定初始容量的{@link LinkedHashMap}, 并根据默认的Locale存储不区分大小写的键 (默认为小写).
	 * 
	 * @param initialCapacity 初始容量
	 * @param locale 用于不区分大小写的键转换的Locale
	 */
	public LinkedCaseInsensitiveMap(int initialCapacity, Locale locale) {
		this.targetMap = new LinkedHashMap<String, V>(initialCapacity) {
			@Override
			public boolean containsKey(Object key) {
				return LinkedCaseInsensitiveMap.this.containsKey(key);
			}
			@Override
			protected boolean removeEldestEntry(Map.Entry<String, V> eldest) {
				boolean doRemove = LinkedCaseInsensitiveMap.this.removeEldestEntry(eldest);
				if (doRemove) {
					caseInsensitiveKeys.remove(convertKey(eldest.getKey()));
				}
				return doRemove;
			}
		};
		this.caseInsensitiveKeys = new HashMap<String, String>(initialCapacity);
		this.locale = (locale != null ? locale : Locale.getDefault());
	}

	/**
	 * Copy constructor.
	 */
	@SuppressWarnings("unchecked")
	private LinkedCaseInsensitiveMap(LinkedCaseInsensitiveMap<V> other) {
		this.targetMap = (LinkedHashMap<String, V>) other.targetMap.clone();
		this.caseInsensitiveKeys = (HashMap<String, String>) other.caseInsensitiveKeys.clone();
		this.locale = other.locale;
	}


	// Implementation of java.util.Map

	@Override
	public int size() {
		return this.targetMap.size();
	}

	@Override
	public boolean isEmpty() {
		return this.targetMap.isEmpty();
	}

	@Override
	public boolean containsKey(Object key) {
		return (key instanceof String && this.caseInsensitiveKeys.containsKey(convertKey((String) key)));
	}

	@Override
	public boolean containsValue(Object value) {
		return this.targetMap.containsValue(value);
	}

	@Override
	public V get(Object key) {
		if (key instanceof String) {
			String caseInsensitiveKey = this.caseInsensitiveKeys.get(convertKey((String) key));
			if (caseInsensitiveKey != null) {
				return this.targetMap.get(caseInsensitiveKey);
			}
		}
		return null;
	}

	@Override
	public V getOrDefault(Object key, V defaultValue) {
		if (key instanceof String) {
			String caseInsensitiveKey = this.caseInsensitiveKeys.get(convertKey((String) key));
			if (caseInsensitiveKey != null) {
				return this.targetMap.get(caseInsensitiveKey);
			}
		}
		return defaultValue;
	}

	@Override
	public V put(String key, V value) {
		String oldKey = this.caseInsensitiveKeys.put(convertKey(key), key);
		if (oldKey != null && !oldKey.equals(key)) {
			this.targetMap.remove(oldKey);
		}
		return this.targetMap.put(key, value);
	}

	@Override
	public void putAll(Map<? extends String, ? extends V> map) {
		if (map.isEmpty()) {
			return;
		}
		for (Map.Entry<? extends String, ? extends V> entry : map.entrySet()) {
			put(entry.getKey(), entry.getValue());
		}
	}

	@Override
	public V remove(Object key) {
		if (key instanceof String) {
			String caseInsensitiveKey = this.caseInsensitiveKeys.remove(convertKey((String) key));
			if (caseInsensitiveKey != null) {
				return this.targetMap.remove(caseInsensitiveKey);
			}
		}
		return null;
	}

	@Override
	public void clear() {
		this.caseInsensitiveKeys.clear();
		this.targetMap.clear();
	}

	@Override
	public Set<String> keySet() {
		return this.targetMap.keySet();
	}

	@Override
	public Collection<V> values() {
		return this.targetMap.values();
	}

	@Override
	public Set<Entry<String, V>> entrySet() {
		return this.targetMap.entrySet();
	}

	@Override
	public LinkedCaseInsensitiveMap<V> clone() {
		return new LinkedCaseInsensitiveMap<V>(this);
	}

	@Override
	public boolean equals(Object obj) {
		return this.targetMap.equals(obj);
	}

	@Override
	public int hashCode() {
		return this.targetMap.hashCode();
	}

	@Override
	public String toString() {
		return this.targetMap.toString();
	}


	// Specific to LinkedCaseInsensitiveMap

	/**
	 * 返回此{@code LinkedCaseInsensitiveMap}使用的区域设置.
	 * 用于不区分大小写的键转换.
	 */
	public Locale getLocale() {
		return this.locale;
	}

	/**
	 * 将给定键转换为不区分大小写的键.
	 * <p>默认实现根据此Map的Locale将键转换为小写.
	 * 
	 * @param key 用户指定的键
	 * 
	 * @return 用于存储的键
	 */
	protected String convertKey(String key) {
		return key.toLowerCase(getLocale());
	}

	/**
	 * 确定此Map是否应删除给定的最旧条目.
	 * 
	 * @param eldest 候选条目
	 * 
	 * @return {@code true}删除, {@code false}保留
	 */
	protected boolean removeEldestEntry(Map.Entry<String, V> eldest) {
		return false;
	}

}
