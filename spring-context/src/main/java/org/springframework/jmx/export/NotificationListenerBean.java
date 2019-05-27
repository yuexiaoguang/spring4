package org.springframework.jmx.export;

import javax.management.NotificationListener;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.jmx.support.NotificationListenerHolder;
import org.springframework.util.Assert;

/**
 * 聚合{@link javax.management.NotificationListener}, {@link javax.management.NotificationFilter}, 和任意回传对象的Helper类.
 *
 * <p>还支持将封装的{@link javax.management.NotificationListener}
 * 与希望通过{@link #setMappedObjectNames mappedObjectNames}属性
 * 从中接收{@link javax.management.Notification Notifications}的任意数量的MBean相关联.
 *
 * <p>Note: 此类支持Spring bean名称为{@link #setMappedObjectNames "mappedObjectNames"}, 作为指定JMX对象名称的替代方法.
 * 请注意, 此类bean名称仅支持由相同{@link MBeanExporter}导出的bean.
 */
public class NotificationListenerBean extends NotificationListenerHolder implements InitializingBean {

	public NotificationListenerBean() {
	}

	/**
	 * @param notificationListener 封装的监听器
	 */
	public NotificationListenerBean(NotificationListener notificationListener) {
		Assert.notNull(notificationListener, "NotificationListener must not be null");
		setNotificationListener(notificationListener);
	}


	@Override
	public void afterPropertiesSet() {
		if (getNotificationListener() == null) {
			throw new IllegalArgumentException("Property 'notificationListener' is required");
		}
	}

	void replaceObjectName(Object originalName, Object newName) {
		if (this.mappedObjectNames != null && this.mappedObjectNames.contains(originalName)) {
			this.mappedObjectNames.remove(originalName);
			this.mappedObjectNames.add(newName);
		}
	}

}
