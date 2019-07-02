package org.springframework.test.context.support;

import org.springframework.test.context.TestPropertySource;
import org.springframework.util.Assert;

/**
 * {@code MergedTestPropertySources}通过{@link TestPropertySource @TestPropertySource}
 * 封装在测试类及其所有超类上声明的<em>合并</em>属性源.
 */
class MergedTestPropertySources {

	private static final String[] EMPTY_STRING_ARRAY = new String[0];

	private final String[] locations;

	private final String[] properties;


	MergedTestPropertySources() {
		this(EMPTY_STRING_ARRAY, EMPTY_STRING_ARRAY);
	}

	/**
	 * @param locations 属性文件的资源位置; 可能为空, 但不能是{@code null}
	 * @param properties {@code key=value}对形式的属性; 可能为空, 但不能是{@code null}
	 */
	MergedTestPropertySources(String[] locations, String[] properties) {
		Assert.notNull(locations, "The locations array must not be null");
		Assert.notNull(properties, "The properties array must not be null");
		this.locations = locations;
		this.properties = properties;
	}

	/**
	 * 获取属性文件的资源位置.
	 */
	String[] getLocations() {
		return this.locations;
	}

	/**
	 * 获取<em>key-value</em>对的形式的属性.
	 */
	String[] getProperties() {
		return this.properties;
	}

}
