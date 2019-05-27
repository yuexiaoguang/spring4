package org.springframework.jms.support;

import javax.jms.Message;

import org.springframework.messaging.support.HeaderMapper;

/**
 * 用于将{@link org.springframework.messaging.Message} header映射到出站JMS {@link javax.jms.Message} (e.g. 配置JMS属性),
 * 或从入站JMS消息中提取消息header值的策略接口.
 */
public interface JmsHeaderMapper extends HeaderMapper<Message> {

	/**
	 * 符合JMS的{@code content_type}属性.
	 */
	String CONTENT_TYPE_PROPERTY = "content_type";

}

