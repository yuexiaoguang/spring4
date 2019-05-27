package org.springframework.jmx.export;

import javax.management.ObjectName;

/**
 * 一个监听器, 允许在通过{@link MBeanExporter}注册和注销MBean时通知应用程序代码.
 */
public interface MBeanExporterListener {

	/**
	 * 从{@link javax.management.MBeanServer}成功注册MBean后, 由{@link MBeanExporter}调用.
	 * 
	 * @param objectName 要注册的MBean的{@code ObjectName}
	 */
	void mbeanRegistered(ObjectName objectName);

	/**
	 * 从{@link javax.management.MBeanServer}成功注销MBean后, 由{@link MBeanExporter}调用.
	 * 
	 * @param objectName 要注销的MBean的{@code ObjectName}
	 */
	void mbeanUnregistered(ObjectName objectName);

}
