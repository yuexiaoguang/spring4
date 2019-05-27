package org.springframework.beans.factory.config;

import java.util.Map;
import java.util.Properties;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.CollectionFactory;

/**
 * 用于从YAML源读取的{@link java.util.Properties}的工厂, 公开String属性值的平面结构.
 *
 * <p>YAML是一种很好的人类可读的配置格式, 它有一些有用的分层属性.
 * 它或多或少是JSON的超集, 因此它具有许多类似的功能.
 *
 * <p><b>Note: 所有暴露的值都是{@code String}类型</b>通过常见的{@link Properties#getProperty}方法进行访问
 * (e.g. 通过 {@link PropertyResourceConfigurer#setProperties(Properties)}解析配置属性).
 * 如果不希望这样, 请改用{@link YamlMapFactoryBean}.
 *
 * <p>此工厂创建的属性具有分层对象的嵌套路径, 例如此YAML
 *
 * <pre class="code">
 * environments:
 *   dev:
 *     url: http://dev.bar.com
 *     name: Developer Setup
 *   prod:
 *     url: http://foo.bar.com
 *     name: My Cool App
 * </pre>
 *
 * 翻译成这些属性:
 *
 * <pre class="code">
 * environments.dev.url=http://dev.bar.com
 * environments.dev.name=Developer Setup
 * environments.prod.url=http://foo.bar.com
 * environments.prod.name=My Cool App
 * </pre>
 *
 * 使用<code>[]</code>解除引用将列表拆分为属性Key, 例如这个YAML:
 *
 * <pre class="code">
 * servers:
 * - dev.bar.com
 * - foo.bar.com
 * </pre>
 *
 * 成为这样的属性:
 *
 * <pre class="code">
 * servers[0]=dev.bar.com
 * servers[1]=foo.bar.com
 * </pre>
 */
public class YamlPropertiesFactoryBean extends YamlProcessor implements FactoryBean<Properties>, InitializingBean {

	private boolean singleton = true;

	private Properties properties;


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
			this.properties = createProperties();
		}
	}

	@Override
	public Properties getObject() {
		return (this.properties != null ? this.properties : createProperties());
	}

	@Override
	public Class<?> getObjectType() {
		return Properties.class;
	}


	/**
	 * 子类可以重写的模板方法, 用于构造此工厂返回的对象.
	 * 默认实现返回包含所有资源内容的属性.
	 * <p>在共享单例的情况下, 第一次调用 {@link #getObject()}时延迟地调用; 否则每次调用{@link #getObject()}时调用.
	 * 
	 * @return 该工厂返回的对象
	 */
	protected Properties createProperties() {
		final Properties result = CollectionFactory.createStringAdaptingProperties();
		process(new MatchCallback() {
			@Override
			public void process(Properties properties, Map<String, Object> map) {
				result.putAll(properties);
			}
		});
		return result;
	}

}
