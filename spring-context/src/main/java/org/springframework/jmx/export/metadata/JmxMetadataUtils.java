package org.springframework.jmx.export.metadata;

import javax.management.modelmbean.ModelMBeanNotificationInfo;

import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * 用于将Spring JMX元数据转换为其普通JMX等效项的实用程序方法.
 */
public abstract class JmxMetadataUtils {

	/**
	 * 将提供的{@link ManagedNotification}转换为相应的{@link javax.management.modelmbean.ModelMBeanNotificationInfo}.
	 */
	public static ModelMBeanNotificationInfo convertToModelMBeanNotificationInfo(ManagedNotification notificationInfo) {
		String[] notifTypes = notificationInfo.getNotificationTypes();
		if (ObjectUtils.isEmpty(notifTypes)) {
			throw new IllegalArgumentException("Must specify at least one notification type");
		}

		String name = notificationInfo.getName();
		if (!StringUtils.hasText(name)) {
			throw new IllegalArgumentException("Must specify notification name");
		}

		String description = notificationInfo.getDescription();
		return new ModelMBeanNotificationInfo(notifTypes, name, description);
	}

}
