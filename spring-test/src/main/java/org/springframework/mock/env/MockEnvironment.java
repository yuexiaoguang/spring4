package org.springframework.mock.env;

import org.springframework.core.env.AbstractEnvironment;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * 简单{@link ConfigurableEnvironment}实现, 公开
 * {@link #setProperty(String, String)}和{@link #withProperty(String, String)}方法以进行测试.
 */
public class MockEnvironment extends AbstractEnvironment {

	private MockPropertySource propertySource = new MockPropertySource();

	public MockEnvironment() {
		getPropertySources().addLast(propertySource);
	}

	/**
	 * 设置此环境在底层{@link MockPropertySource}上的属性.
	 */
	public void setProperty(String key, String value) {
		propertySource.setProperty(key, value);
	}

	/**
	 * 返回当前实例的{@link #setProperty}的便捷同义方法.
	 * 用于方法链接和流式使用.
	 * 
	 * @return 此{@link MockEnvironment}实例
	 */
	public MockEnvironment withProperty(String key, String value) {
		this.setProperty(key, value);
		return this;
	}

}
