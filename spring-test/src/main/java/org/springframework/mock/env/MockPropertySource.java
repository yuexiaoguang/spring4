package org.springframework.mock.env;

import java.util.Properties;

import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.PropertySource;

/**
 * 简单的{@link PropertySource}实现用于测试.
 * 接受用户提供的{@link Properties}对象, 或者如果在构造期间省略, 则实现将初始化它自己的对象.
 *
 * 为方便起见, 公开了{@link #setProperty}和{@link #withProperty}方法, 例如:
 * <pre class="code">
 * {@code
 *   PropertySource<?> source = new MockPropertySource().withProperty("foo", "bar");
 * }
 * </pre>
 */
public class MockPropertySource extends PropertiesPropertySource {

	/**
	 * {@value}是{@link MockPropertySource}实例的默认名称, 除非给定明确的名称.
	 */
	public static final String MOCK_PROPERTIES_PROPERTY_SOURCE_NAME = "mockProperties";

	/**
	 * 创建一个名为{@value #MOCK_PROPERTIES_PROPERTY_SOURCE_NAME}的新{@code MockPropertySource},
	 * 它将维护自己的内部{@link Properties}实例.
	 */
	public MockPropertySource() {
		this(new Properties());
	}

	/**
	 * 创建一个具有给定名称的新{@code MockPropertySource},
	 * 该名称将维护其自己的内部{@link Properties}实例.
	 * 
	 * @param name 属性源的{@linkplain #getName() name}
	 */
	public MockPropertySource(String name) {
		this(name, new Properties());
	}

	/**
	 * 创建一个名为{@value #MOCK_PROPERTIES_PROPERTY_SOURCE_NAME}的新{@code MockPropertySource},
	 * 并由给定的{@link Properties}对象提供支持.
	 * 
	 * @param properties 要使用的属性
	 */
	public MockPropertySource(Properties properties) {
		this(MOCK_PROPERTIES_PROPERTY_SOURCE_NAME, properties);
	}

	/**
	 * 使用给定名称创建一个新的{@code MockPropertySource},
	 * 并由给定的{@link Properties}对象提供支持.
	 * 
	 * @param name 属性源的{@linkplain #getName() name}
	 * @param properties 要使用的属性
	 */
	public MockPropertySource(String name, Properties properties) {
		super(name, properties);
	}

	/**
	 * 设置底层{@link Properties}对象上的给定属性.
	 */
	public void setProperty(String name, Object value) {
		this.source.put(name, value);
	}

	/**
	 * 返回当前实例的{@link #setProperty}的便捷同义方法.
	 * 用于方法链接和流式使用.
	 * 
	 * @return 此{@link MockPropertySource}实例
	 */
	public MockPropertySource withProperty(String name, Object value) {
		this.setProperty(name, value);
		return this;
	}

}
