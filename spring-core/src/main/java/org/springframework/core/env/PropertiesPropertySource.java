package org.springframework.core.env;

import java.util.Map;
import java.util.Properties;

/**
 * 从{@link java.util.Properties}对象中提取属性的{@link PropertySource}实现.
 *
 * <p>请注意, 因为{@code Properties}对象在技术上是{@code <Object, Object>} {@link java.util.Hashtable Hashtable},
 * 所以可能包含非{@code String}键或值.
 * 但是，此实现仅限于以{@link Properties#getProperty}和{@link Properties#setProperty}的相同方式
 * 访问基于{@code String}的键和值.
 */
public class PropertiesPropertySource extends MapPropertySource {

	@SuppressWarnings({"unchecked", "rawtypes"})
	public PropertiesPropertySource(String name, Properties source) {
		super(name, (Map) source);
	}

	protected PropertiesPropertySource(String name, Map<String, Object> source) {
		super(name, source);
	}
}
