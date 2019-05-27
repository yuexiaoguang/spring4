package org.springframework.jmx.support;

import java.util.Hashtable;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

/**
 * 用于创建{@link javax.management.ObjectName}实例的Helper类.
 */
public class ObjectNameManager {

	/**
	 * 检索与提供的名称对应的{@code ObjectName}实例.
	 * 
	 * @param objectName {@code ObjectName}或{@code String}格式的{@code ObjectName}
	 * 
	 * @return the {@code ObjectName} instance
	 * @throws MalformedObjectNameException 如果对象名称规范无效
	 */
	public static ObjectName getInstance(Object objectName) throws MalformedObjectNameException {
		if (objectName instanceof ObjectName) {
			return (ObjectName) objectName;
		}
		if (!(objectName instanceof String)) {
			throw new MalformedObjectNameException("Invalid ObjectName value type [" +
					objectName.getClass().getName() + "]: only ObjectName and String supported.");
		}
		return getInstance((String) objectName);
	}

	/**
	 * 检索与提供的名称对应的{@code ObjectName}实例.
	 * 
	 * @param objectName {@code String}格式的{@code ObjectName}
	 * 
	 * @return the {@code ObjectName} instance
	 * @throws MalformedObjectNameException 如果对象名称规范无效
	 */
	public static ObjectName getInstance(String objectName) throws MalformedObjectNameException {
		return ObjectName.getInstance(objectName);
	}

	/**
	 * 使用提供的键和值检索{@code ObjectName}实例, 用于指定的域和单个属性.
	 * 
	 * @param domainName {@code ObjectName}的域名
	 * @param key {@code ObjectName}中单个属性的Key
	 * @param value {@code ObjectName}中单个属性的值
	 * 
	 * @return the {@code ObjectName} instance
	 * @throws MalformedObjectNameException 如果对象名称规范无效
	 */
	public static ObjectName getInstance(String domainName, String key, String value)
			throws MalformedObjectNameException {

		return ObjectName.getInstance(domainName, key, value);
	}

	/**
	 * 使用指定的域名和提供的键/名称属性检索{@code ObjectName}实例.
	 * 
	 * @param domainName {@code ObjectName}的域名
	 * @param properties {@code ObjectName}的属性
	 * 
	 * @return the {@code ObjectName} instance
	 * @throws MalformedObjectNameException 如果对象名称规范无效
	 */
	public static ObjectName getInstance(String domainName, Hashtable<String, String> properties)
			throws MalformedObjectNameException {

		return ObjectName.getInstance(domainName, properties);
	}

}
