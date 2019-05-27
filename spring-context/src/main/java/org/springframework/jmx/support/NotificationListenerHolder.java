package org.springframework.jmx.support;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.management.MalformedObjectNameException;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectName;

import org.springframework.util.ObjectUtils;

/**
 * 聚合{@link javax.management.NotificationListener}, {@link javax.management.NotificationFilter},
 * 和任意回传对象, 以及监听器希望从中接收{@link javax.management.Notification Notifications}的MBean的名称, 的Helper类.
 */
public class NotificationListenerHolder {

	private NotificationListener notificationListener;

	private NotificationFilter notificationFilter;

	private Object handback;

	protected Set<Object> mappedObjectNames;


	/**
	 * Set the {@link javax.management.NotificationListener}.
	 */
	public void setNotificationListener(NotificationListener notificationListener) {
		this.notificationListener = notificationListener;
	}

	/**
	 * Get the {@link javax.management.NotificationListener}.
	 */
	public NotificationListener getNotificationListener() {
		return this.notificationListener;
	}

	/**
	 * 设置与封装的{@link #getNotificationFilter() NotificationFilter}关联的{@link javax.management.NotificationFilter}.
	 * <p>May be {@code null}.
	 */
	public void setNotificationFilter(NotificationFilter notificationFilter) {
		this.notificationFilter = notificationFilter;
	}

	/**
	 * 返回与封装的{@link #getNotificationFilter() NotificationFilter}关联的{@link javax.management.NotificationFilter}.
	 * <p>May be {@code null}.
	 */
	public NotificationFilter getNotificationFilter() {
		return this.notificationFilter;
	}

	/**
	 * 在通知任何{@link javax.management.NotificationListener}时,
	 * 设置{@link javax.management.NotificationBroadcaster}将原样'回传'的对象.
	 * 
	 * @param handback 要回传的对象 (可以是{@code null})
	 */
	public void setHandback(Object handback) {
		this.handback = handback;
	}

	/**
	 * 在通知任何{@link javax.management.NotificationListener}时,
	 * {@link javax.management.NotificationBroadcaster}将原样'回传'的对象.
	 * 
	 * @return 要回传的对象 (may be {@code null})
	 */
	public Object getHandback() {
		return this.handback;
	}

	/**
	 * 设置封装的{@link #getNotificationFilter() NotificationFilter}将注册的单个MBean的{@link javax.management.ObjectName}样式名称,
	 * 以监听{@link javax.management.Notification Notifications}.
	 * 可以指定为{@code ObjectName}实例或{@code String}.
	 */
	public void setMappedObjectName(Object mappedObjectName) {
		setMappedObjectNames(mappedObjectName != null ? new Object[] {mappedObjectName} : null);
	}

	/**
	 * 设置封装的{@link #getNotificationFilter() NotificationFilter}将注册的单个MBean的{@link javax.management.ObjectName}样式名称,
	 * 以监听{@link javax.management.Notification Notifications}.
	 * 可以指定为{@code ObjectName}实例或{@code String}.
	 */
	public void setMappedObjectNames(Object[] mappedObjectNames) {
		this.mappedObjectNames = (mappedObjectNames != null ?
				new LinkedHashSet<Object>(Arrays.asList(mappedObjectNames)) : null);
	}

	/**
	 * 返回{@link javax.management.ObjectName}列表的字符串表示形式,
	 * 其中封装的{@link #getNotificationFilter() NotificationFilter}将被注册为
	 * {@link javax.management.Notification Notifications}的监听器.
	 * 
	 * @throws MalformedObjectNameException 如果{@code ObjectName}格式错误
	 */
	public ObjectName[] getResolvedObjectNames() throws MalformedObjectNameException {
		if (this.mappedObjectNames == null) {
			return null;
		}
		ObjectName[] resolved = new ObjectName[this.mappedObjectNames.size()];
		int i = 0;
		for (Object objectName : this.mappedObjectNames) {
			resolved[i] = ObjectNameManager.getInstance(objectName);
			i++;
		}
		return resolved;
	}


	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof NotificationListenerHolder)) {
			return false;
		}
		NotificationListenerHolder otherNlh = (NotificationListenerHolder) other;
		return (ObjectUtils.nullSafeEquals(this.notificationListener, otherNlh.notificationListener) &&
				ObjectUtils.nullSafeEquals(this.notificationFilter, otherNlh.notificationFilter) &&
				ObjectUtils.nullSafeEquals(this.handback, otherNlh.handback) &&
				ObjectUtils.nullSafeEquals(this.mappedObjectNames, otherNlh.mappedObjectNames));
	}

	@Override
	public int hashCode() {
		int hashCode = ObjectUtils.nullSafeHashCode(this.notificationListener);
		hashCode = 29 * hashCode + ObjectUtils.nullSafeHashCode(this.notificationFilter);
		hashCode = 29 * hashCode + ObjectUtils.nullSafeHashCode(this.handback);
		hashCode = 29 * hashCode + ObjectUtils.nullSafeHashCode(this.mappedObjectNames);
		return hashCode;
	}

}
