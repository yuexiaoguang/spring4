package org.springframework.jmx.export.notification;

import javax.management.AttributeChangeNotification;
import javax.management.MBeanException;
import javax.management.Notification;
import javax.management.ObjectName;
import javax.management.modelmbean.ModelMBean;
import javax.management.modelmbean.ModelMBeanNotificationBroadcaster;

import org.springframework.util.Assert;

/**
 * {@link NotificationPublisher}实现, 它使用{@link ModelMBean}接口提供的基础结构,
 * 跟踪{@link javax.management.NotificationListener javax.management.NotificationListeners},
 * 并发送{@link Notification Notifications}到这些监听器.
 */
public class ModelMBeanNotificationPublisher implements NotificationPublisher {

	/**
	 * {@link ModelMBean}实例包装将注入此{@code NotificationPublisher}的托管资源.
	 */
	private final ModelMBeanNotificationBroadcaster modelMBean;

	/**
	 * 与{@link ModelMBean modelMBean}关联的{@link ObjectName}.
	 */
	private final ObjectName objectName;

	/**
	 * 与{@link ModelMBean modelMBean}关联的受管资源.
	 */
	private final Object managedResource;


	/**
	 * 将所有{@link javax.management.Notification Notifications}发布到提供的{@link ModelMBean}.
	 * 
	 * @param modelMBean 目标{@link ModelMBean}; must not be {@code null}
	 * @param objectName 源{@link ModelMBean}的{@link ObjectName}
	 * @param managedResource 提供的{@link ModelMBean}公开的托管资源
	 * 
	 * @throws IllegalArgumentException 如果有参数是{@code null}
	 */
	public ModelMBeanNotificationPublisher(
			ModelMBeanNotificationBroadcaster modelMBean, ObjectName objectName, Object managedResource) {

		Assert.notNull(modelMBean, "'modelMBean' must not be null");
		Assert.notNull(objectName, "'objectName' must not be null");
		Assert.notNull(managedResource, "'managedResource' must not be null");
		this.modelMBean = modelMBean;
		this.objectName = objectName;
		this.managedResource = managedResource;
	}


	/**
	 * 使用包装的{@link ModelMBean}实例发送提供的{@link Notification}.
	 * 
	 * @param notification 要发送的{@link Notification}
	 * 
	 * @throws IllegalArgumentException 如果提供的{@code notification}是{@code null}
	 * @throws UnableToSendNotificationException 如果提供的{@code notification}无法发送
	 */
	@Override
	public void sendNotification(Notification notification) {
		Assert.notNull(notification, "Notification must not be null");
		replaceNotificationSourceIfNecessary(notification);
		try {
			if (notification instanceof AttributeChangeNotification) {
				this.modelMBean.sendAttributeChangeNotification((AttributeChangeNotification) notification);
			}
			else {
				this.modelMBean.sendNotification(notification);
			}
		}
		catch (MBeanException ex) {
			throw new UnableToSendNotificationException("Unable to send notification [" + notification + "]", ex);
		}
	}

	/**
	 * 从{@link Notification javadoc}:
	 * <p><i>"强烈建议通知发件者使用对象名称, 而不是对MBean对象的引用作为源."</i>
	 * 
	 * @param notification {@link Notification}, 其{@link javax.management.Notification#getSource()}可能需要修改
	 */
	private void replaceNotificationSourceIfNecessary(Notification notification) {
		if (notification.getSource() == null || notification.getSource().equals(this.managedResource)) {
			notification.setSource(this.objectName);
		}
	}

}
