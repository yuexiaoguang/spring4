package org.springframework.messaging.handler.invocation;

import org.springframework.core.MethodParameter;
import org.springframework.messaging.Message;

/**
 * 用于处理从处理{@link Message}的方法调用返回的值的策略接口.
 */
public interface HandlerMethodReturnValueHandler {

	/**
	 * 此处理器是否支持给定的{@linkplain MethodParameter 方法返回类型}.
	 * 
	 * @param returnType 要检查的方法返回类型
	 * 
	 * @return {@code true} 如果此处理器支持提供的返回类型; 否则{@code false}
	 */
	boolean supportsReturnType(MethodParameter returnType);

	/**
	 * 处理给定的返回值.
	 * 
	 * @param returnValue 处理器方法返回的值
	 * @param returnType 返回值的类型.
	 * 此类型必须先前已传递给{@link #supportsReturnType(org.springframework.core.MethodParameter)}, 并且必须已返回{@code true}.
	 * @param message 导致调用此方法的消息
	 * 
	 * @throws Exception 如果返回值处理导致错误
	 */
	void handleReturnValue(Object returnValue, MethodParameter returnType, Message<?> message)
			throws Exception;

}
