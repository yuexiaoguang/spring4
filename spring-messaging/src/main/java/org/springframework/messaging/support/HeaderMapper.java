package org.springframework.messaging.support;

import org.springframework.messaging.MessageHeaders;

/**
 * 用于将{@link MessageHeaders}与其他类型的对象相互映射的通用策略接口.
 * 这通常由适配器使用, 其中"其他类型"具有header或属性 (HTTP, JMS, AMQP, etc).
 *
 * @param <T> 要与header相互映射的类型
 */
public interface HeaderMapper<T> {

	/**
	 * 从给定的{@link MessageHeaders}映射到指定的目标消息.
	 * 
	 * @param headers 抽象MessageHeaders
	 * @param target 本机目标消息
	 */
	void fromHeaders(MessageHeaders headers, T target);

	/**
	 * 从给定的目标消息映射到抽象的{@link MessageHeaders}.
	 * 
	 * @param source 本机目标消息
	 * 
	 * @return 抽象MessageHeaders
	 */
	MessageHeaders toHeaders(T source);

}
