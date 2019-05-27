package org.springframework.jmx.export.naming;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

/**
 * 策略接口, 封装{@code ObjectName}实例的创建.
 *
 * <p>在注册bean时, {@code MBeanExporter}使用它来获取{@code ObjectName}.
 */
public interface ObjectNamingStrategy {

	/**
	 * 获取所提供bean的{@code ObjectName}.
	 * 
	 * @param managedBean 将在返回的{@code ObjectName}下公开的bean
	 * @param beanKey 传递给{@code MBeanExporter}的bean Map中与此bean关联的键
	 * 
	 * @return the {@code ObjectName} instance
	 * @throws MalformedObjectNameException 如果生成的{@code ObjectName}无效
	 */
	ObjectName getObjectName(Object managedBean, String beanKey) throws MalformedObjectNameException;

}
