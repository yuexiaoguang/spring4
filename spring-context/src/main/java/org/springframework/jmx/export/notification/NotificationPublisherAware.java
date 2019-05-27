package org.springframework.jmx.export.notification;

import org.springframework.beans.factory.Aware;

/**
 * 由任何Spring管理的资源实现的接口, 该资源将在{@link javax.management.MBeanServer}中注册,
 * 并希望发送JMX {@link javax.management.Notification javax.management.Notifications}.
 *
 * <p>只要在{@link javax.management.MBeanServer}中注册, 就会为Spring创建的托管资源提供{@link NotificationPublisher}.
 *
 * <p><b>NOTE:</b> 这个接口只适用于简单的Spring管理的bean, 它恰好通过Spring的{@link org.springframework.jmx.export.MBeanExporter}导出.
 * 它不适用于任何未导出的bean; 它也不适用于Spring导出的标准MBean.
 * 对于标准JMX MBean, 请考虑实现{@link javax.management.modelmbean.ModelMBeanNotificationBroadcaster}接口
 * (或实现完整的{@link javax.management.modelmbean.ModelMBean}).
 */
public interface NotificationPublisherAware extends Aware {

	/**
	 * 为当前管理的资源实例设置{@link NotificationPublisher}实例.
	 */
	void setNotificationPublisher(NotificationPublisher notificationPublisher);

}
