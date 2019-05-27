package org.springframework.jmx.export.naming;

import java.util.Hashtable;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.jmx.export.metadata.JmxAttributeSource;
import org.springframework.jmx.export.metadata.ManagedResource;
import org.springframework.jmx.support.ObjectNameManager;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * {@link ObjectNamingStrategy}接口的实现, 从source级元数据中读取{@code ObjectName}.
 * 如果在source级元数据中找不到{@code ObjectName}, 则回退到bean密钥 (bean名称).
 *
 * <p>使用{@link JmxAttributeSource}策略接口, 以便可以使用任何支持的实现读取元数据.
 * 开箱即用, {@link org.springframework.jmx.export.annotation.AnnotationJmxAttributeSource}
 * 内省了Spring提供的一组定义明确的Java 5注解.
 */
public class MetadataNamingStrategy implements ObjectNamingStrategy, InitializingBean {

	/**
	 * 用于读取元数据的{@code JmxAttributeSource}实现.
	 */
	private JmxAttributeSource attributeSource;

	private String defaultDomain;


	/**
	 * 创建一个新的{@code MetadataNamingStrategy}, 需要通过{@link #setAttributeSource}方法进行配置.
	 */
	public MetadataNamingStrategy() {
	}

	/**
	 * @param attributeSource 要使用的JmxAttributeSource
	 */
	public MetadataNamingStrategy(JmxAttributeSource attributeSource) {
		Assert.notNull(attributeSource, "JmxAttributeSource must not be null");
		this.attributeSource = attributeSource;
	}


	/**
	 * 设置{@code JmxAttributeSource}接口的实现, 以便在读取source级元数据时使用.
	 */
	public void setAttributeSource(JmxAttributeSource attributeSource) {
		Assert.notNull(attributeSource, "JmxAttributeSource must not be null");
		this.attributeSource = attributeSource;
	}

	/**
	 * 指定在未指定source级元数据时用于生成ObjectName的默认域.
	 * <p>默认使用bean名称中指定的域 (如果bean名称遵循JMX ObjectName语法);
	 * 否则托管bean类的包名称.
	 */
	public void setDefaultDomain(String defaultDomain) {
		this.defaultDomain = defaultDomain;
	}

	@Override
	public void afterPropertiesSet() {
		if (this.attributeSource == null) {
			throw new IllegalArgumentException("Property 'attributeSource' is required");
		}
	}


	/**
	 * 从与托管资源的{@code Class}关联的source级元数据中读取{@code ObjectName}.
	 */
	@Override
	public ObjectName getObjectName(Object managedBean, String beanKey) throws MalformedObjectNameException {
		Class<?> managedClass = AopUtils.getTargetClass(managedBean);
		ManagedResource mr = this.attributeSource.getManagedResource(managedClass);

		// 检查是否已指定对象名称.
		if (mr != null && StringUtils.hasText(mr.getObjectName())) {
			return ObjectNameManager.getInstance(mr.getObjectName());
		}
		else {
			try {
				return ObjectNameManager.getInstance(beanKey);
			}
			catch (MalformedObjectNameException ex) {
				String domain = this.defaultDomain;
				if (domain == null) {
					domain = ClassUtils.getPackageName(managedClass);
				}
				Hashtable<String, String> properties = new Hashtable<String, String>();
				properties.put("type", ClassUtils.getShortName(managedClass));
				properties.put("name", beanKey);
				return ObjectNameManager.getInstance(domain, properties);
			}
		}
	}

}
