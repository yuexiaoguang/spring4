package org.springframework.jms.support.converter;

/**
 * 指示要转换为的目标消息类型的常量:
 * {@link javax.jms.TextMessage}, {@link javax.jms.BytesMessage},
 * {@link javax.jms.MapMessage}, {@link javax.jms.ObjectMessage}.
 */
public enum MessageType {

	TEXT, BYTES, MAP, OBJECT

}
