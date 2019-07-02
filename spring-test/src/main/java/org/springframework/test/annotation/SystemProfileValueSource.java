package org.springframework.test.annotation;

import org.springframework.util.Assert;

/**
 * {@link ProfileValueSource}的实现, 它使用系统属性作为底层源.
 */
public class SystemProfileValueSource implements ProfileValueSource {

	private static final SystemProfileValueSource INSTANCE = new SystemProfileValueSource();


	/**
	 * 获取此ProfileValueSource的规范实例.
	 */
	public static final SystemProfileValueSource getInstance() {
		return INSTANCE;
	}


	private SystemProfileValueSource() {
	}

	/**
	 * 从系统属性中获取指定键指示的<em>配置文件值</em>.
	 */
	@Override
	public String get(String key) {
		Assert.hasText(key, "'key' must not be empty");
		return System.getProperty(key);
	}

}
