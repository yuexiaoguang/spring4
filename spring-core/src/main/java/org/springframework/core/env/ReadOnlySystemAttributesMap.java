package org.springframework.core.env;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * 由系统属性或环境变量支持的只读{@code Map<String, String>}实现.
 *
 * <p>{@link SecurityManager}禁止访问{@link System#getProperties()}或 {@link System#getenv()}时,
 * 由{@link AbstractApplicationContext}使用.
 * 正是由于这个原因, {@link #keySet()}, {@link #entrySet()}, 和{@link #values()}的实现总是返回空,
 * 即使{@link #get(Object)}实际上可以返回非null, 如果当前安全管理器允许访问各个键.
 */
abstract class ReadOnlySystemAttributesMap implements Map<String, String> {

	@Override
	public boolean containsKey(Object key) {
		return (get(key) != null);
	}

	/**
	 * @param key 要检索的系统属性的名称
	 * 
	 * @throws IllegalArgumentException 如果给定键是非String
	 */
	@Override
	public String get(Object key) {
		if (!(key instanceof String)) {
			throw new IllegalArgumentException(
					"Type of key [" + key.getClass().getName() + "] must be java.lang.String");
		}
		return getSystemAttribute((String) key);
	}

	@Override
	public boolean isEmpty() {
		return false;
	}

	/**
	 * 返回底层系统属性的模板方法.
	 * <p>实现通常在此处调用{@link System#getProperty(String)}或{@link System#getenv(String)}.
	 */
	protected abstract String getSystemAttribute(String attributeName);


	// Unsupported

	@Override
	public int size() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String put(String key, String value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean containsValue(Object value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String remove(Object key) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void clear() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Set<String> keySet() {
		return Collections.emptySet();
	}

	@Override
	public void putAll(Map<? extends String, ? extends String> map) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Collection<String> values() {
		return Collections.emptySet();
	}

	@Override
	public Set<Entry<String, String>> entrySet() {
		return Collections.emptySet();
	}

}
