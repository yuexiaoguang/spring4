package org.springframework.jmx.export.naming;

import java.io.IOException;
import java.util.Properties;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.jmx.support.ObjectNameManager;
import org.springframework.util.CollectionUtils;

/**
 * {@code ObjectNamingStrategy}实现, 它从传递给{@code MBeanExporter}的"beans" Map中使用的Key构建{@code ObjectName}实例.
 *
 * <p>还可以检查对象名称映射, 以{@code Properties}或属性文件的{@code mappingLocations}给出.
 * 用于查找的Key是{@code MBeanExporter}的 "beans" Map中使用的Key.
 * 如果没有找到给定Key的映射, 则Key本身用于构建 {@code ObjectName}.
 */
public class KeyNamingStrategy implements ObjectNamingStrategy, InitializingBean {

	/**
	 * {@code Log} instance for this class.
	 */
	protected final Log logger = LogFactory.getLog(getClass());

	/**
	 * 存储bean Key到{@code ObjectName}的映射.
	 */
	private Properties mappings;

	/**
	 * 存储{@code Resource}, 包含应加载到用于{@code ObjectName}解析的{@code Properties}的最终合并的集合的属性.
	 */
	private Resource[] mappingLocations;

	/**
	 * 存储将{@code mappings} {@code Properties}与存储在{@code mappingLocations}定义的资源中的属性合并的结果.
	 */
	private Properties mergedMappings;


	/**
	 * 设置包含对象名称映射的本地属性, e.g. 通过XML bean定义中的"props"标签.
	 * 这些可以被视为默认值, 由从文件加载的属性覆盖.
	 */
	public void setMappings(Properties mappings) {
		this.mappings = mappings;
	}

	/**
	 * 设置要加载的属性文件的位置, 包含对象名称映射.
	 */
	public void setMappingLocation(Resource location) {
		this.mappingLocations = new Resource[] {location};
	}

	/**
	 * 设置要加载的属性文件的位置, 包含对象名称映射.
	 */
	public void setMappingLocations(Resource... mappingLocations) {
		this.mappingLocations = mappingLocations;
	}


	/**
	 * 将{@code mappings}和{@code mappingLocations}中配置的{@code Properties}合并到用于{@code ObjectName}解析的最终{@code Properties}实例中.
	 */
	@Override
	public void afterPropertiesSet() throws IOException {
		this.mergedMappings = new Properties();
		CollectionUtils.mergePropertiesIntoMap(this.mappings, this.mergedMappings);

		if (this.mappingLocations != null) {
			for (Resource location : this.mappingLocations) {
				if (logger.isInfoEnabled()) {
					logger.info("Loading JMX object name mappings file from " + location);
				}
				PropertiesLoaderUtils.fillProperties(this.mergedMappings, location);
			}
		}
	}


	/**
	 * 尝试通过给定的Key检索{@code ObjectName}, 尝试首先在映射中查找映射的值.
	 */
	@Override
	public ObjectName getObjectName(Object managedBean, String beanKey) throws MalformedObjectNameException {
		String objectName = null;
		if (this.mergedMappings != null) {
			objectName = this.mergedMappings.getProperty(beanKey);
		}
		if (objectName == null) {
			objectName = beanKey;
		}
		return ObjectNameManager.getInstance(objectName);
	}

}
