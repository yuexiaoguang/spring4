package org.springframework.jmx.export.notification;

import javax.management.Notification;

/**
 * 简单的接口, 允许Spring管理的MBean发布JMX通知, 而不知道这些通知如何传输到{@link javax.management.MBeanServer}.
 *
 * <p>托管资源可以通过实现{@link NotificationPublisherAware}接口访问{@code NotificationPublisher}.
 * 在使用{@link javax.management.MBeanServer}注册特定托管资源实例后,
 * 如果该资源实现{@link NotificationPublisherAware}接口, Spring将向其注入{@code NotificationPublisher}实例.
 *
 * <p>每个受管资源实例都将具有{@code NotificationPublisher}实现的不同实例.
 * 此实例将跟踪为特定管理资源注册的所有{@link javax.management.NotificationListener NotificationListeners}.
 *
 * <p>任何现有的用户定义的MBean都应使用标准JMX API进行通知发布;
 * 此接口仅供Spring创建的MBean使用.
 */
public interface NotificationPublisher {

	/**
	 * 将指定的{@link javax.management.Notification}发送到所有已注册的{@link javax.management.NotificationListener NotificationListeners}.
	 * 托管资源不负责管理已注册的{@link javax.management.NotificationListener NotificationListeners}列表;
	 * 这是自动执行的.
	 * 
	 * @param notification 要发送的JMX通知
	 * 
	 * @throws UnableToSendNotificationException 如果发送失败
	 */
	void sendNotification(Notification notification) throws UnableToSendNotificationException;

}
