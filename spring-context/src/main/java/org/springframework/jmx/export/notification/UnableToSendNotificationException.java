package org.springframework.jmx.export.notification;

import org.springframework.jmx.JmxException;

/**
 * 当无法发送JMX {@link javax.management.Notification}时抛出.
 *
 * <p>通常可以通过{@link #getCause()}属性获取无法发送特定通知的原因的根异常.
 */
@SuppressWarnings("serial")
public class UnableToSendNotificationException extends JmxException {

	public UnableToSendNotificationException(String msg) {
		super(msg);
	}

	public UnableToSendNotificationException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
