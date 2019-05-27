package org.springframework.messaging.handler.annotation.support;

import java.lang.reflect.Method;

import org.springframework.messaging.handler.invocation.InvocableHandlerMethod;

/**
 * {@link InvocableHandlerMethod}的工厂, 适合处理传入的{@link org.springframework.messaging.Message}
 *
 * <p>通常由需要灵活方法签名的监听器端点使用.
 */
public interface MessageHandlerMethodFactory {

	/**
	 * 创建能够处理指定方法端点的{@link InvocableHandlerMethod}.
	 * 
	 * @param bean bean实例
	 * @param method 要调用的方法
	 * 
	 * @return 适用于该方法的{@link InvocableHandlerMethod}
	 */
	InvocableHandlerMethod createInvocableHandlerMethod(Object bean, Method method);

}
