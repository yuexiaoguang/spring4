package org.springframework.beans.factory.config;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

/**
 * 用于从YAML源读取的{@code Map}的工厂，保留YAML声明的值类型及其结构.
 *
 * <p>YAML是一种很好的人类可读的配置格式, 它有一些有用的分层属性.
 * 它或多或少是JSON的超集, 因此它具有许多类似的功能.
 *
 * <p>如果提供了多个资源, 则后者将按层次覆盖先前的条目;
 * 也就是说, 合并了在任何深度的{@code Map}类型中具有相同嵌套Key的所有条目. For example:
 *
 * <pre class="code">
 * foo:
 *   bar:
 *    one: two
 * three: four
 * </pre>
 *
 * plus (later in the list)
 *
 * <pre class="code">
 * foo:
 *   bar:
 *    one: 2
 * five: six
 * </pre>
 *
 * results in an effective input of
 *
 * <pre class="code">
 * foo:
 *   bar:
 *    one: 2
 * three: four
 * five: six
 * </pre>
 *
 * 请注意, 第一个文档中“foo”的值不是简单地替换为第二个文档中的值, 而是将其嵌套值合并.
 */
public class YamlMapFactoryBean extends YamlProcessor implements FactoryBean<Map<String, Object>>, InitializingBean {

	private boolean singleton = true;

	private Map<String, Object> map;


	/**
	 * 设置是否应创建单例, 否则每个请求返回一个新对象.
	 * 默认 {@code true} (单例).
	 */
	public void setSingleton(boolean singleton) {
		this.singleton = singleton;
	}

	@Override
	public boolean isSingleton() {
		return this.singleton;
	}

	@Override
	public void afterPropertiesSet() {
		if (isSingleton()) {
			this.map = createMap();
		}
	}

	@Override
	public Map<String, Object> getObject() {
		return (this.map != null ? this.map : createMap());
	}

	@Override
	public Class<?> getObjectType() {
		return Map.class;
	}


	/**
	 * 子类可以重写的模板方法, 用于构造此工厂返回的对象.
	 * <p>在共享单例的情况下, 第一次调用 {@link #getObject()}时延迟地调用; 否则每次调用{@link #getObject()}时调用.
	 * <p>默认实现返回合并的{@code Map}实例.
	 * 
	 * @return 该工厂返回的对象
	 */
	protected Map<String, Object> createMap() {
		final Map<String, Object> result = new LinkedHashMap<String, Object>();
		process(new MatchCallback() {
			@Override
			public void process(Properties properties, Map<String, Object> map) {
				merge(result, map);
			}
		});
		return result;
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	private void merge(Map<String, Object> output, Map<String, Object> map) {
		for (Entry<String, Object> entry : map.entrySet()) {
			String key = entry.getKey();
			Object value = entry.getValue();
			Object existing = output.get(key);
			if (value instanceof Map && existing instanceof Map) {
				Map<String, Object> result = new LinkedHashMap<String, Object>((Map) existing);
				merge(result, (Map) value);
				output.put(key, result);
			}
			else {
				output.put(key, value);
			}
		}
	}

}
