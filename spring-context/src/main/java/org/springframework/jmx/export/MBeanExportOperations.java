package org.springframework.jmx.export;

import javax.management.ObjectName;

/**
 * 定义应用程序开发人员在应用程序运行时访问的一组MBean导出操作的接口.
 *
 * <p>该接口应该用于使用Spring的管理接口生成功能, 将应用程序资源导出到JMX, 并且可以选择使用它的{@link ObjectName}生成功能.
 */
public interface MBeanExportOperations {

	/**
	 * 使用JMX注册提供的资源. 如果资源已经不是有效的MBean, Spring将为它生成一个管理接口.
	 * 生成的确切接口将取决于实现及其配置.
	 * 此调用还为受管资源生成{@link ObjectName}, 并将其返回给调用者.
	 * 
	 * @param managedResource 通过JMX公开的资源
	 * 
	 * @return 资源暴露所用的{@link ObjectName}
	 * @throws MBeanExportException 如果Spring无法生成{@link ObjectName}或注册MBean
	 */
	ObjectName registerManagedResource(Object managedResource) throws MBeanExportException;

	/**
	 * 使用JMX注册提供的资源. 如果资源已经不是有效的MBean, Spring将为它生成一个管理接口.
	 * 生成的确切接口将取决于实现及其配置.
	 * 
	 * @param managedResource 通过JMX公开的资源
	 * @param objectName 资源暴露所用的{@link ObjectName}
	 * 
	 * @throws MBeanExportException 如果Spring无法注册MBean
	 */
	void registerManagedResource(Object managedResource, ObjectName objectName) throws MBeanExportException;

	/**
	 * 从底层MBeanServer注册表中删除指定的MBean.
	 * 
	 * @param objectName 要删除的资源的{@link ObjectName}
	 */
	void unregisterManagedResource(ObjectName objectName);

}
