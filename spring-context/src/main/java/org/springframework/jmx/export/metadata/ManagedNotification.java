package org.springframework.jmx.export.metadata;

import org.springframework.util.StringUtils;

/**
 * 元数据, 指示bean发出的JMX通知.
 */
public class ManagedNotification {

	private String[] notificationTypes;

	private String name;

	private String description;


	/**
	 * 将单个通知类型或通知类型列表设置为逗号分隔的String.
	 */
	public void setNotificationType(String notificationType) {
		this.notificationTypes = StringUtils.commaDelimitedListToStringArray(notificationType);
	}

	/**
	 * 设置通知类型列表.
	 */
	public void setNotificationTypes(String... notificationTypes) {
		this.notificationTypes = notificationTypes;
	}

	/**
	 * 返回通知类型列表.
	 */
	public String[] getNotificationTypes() {
		return this.notificationTypes;
	}

	/**
	 * 设置此通知的名称.
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * 返回此通知的名称.
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * 设置此通知的描述.
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * 返回此通知的描述.
	 */
	public String getDescription() {
		return this.description;
	}

}
