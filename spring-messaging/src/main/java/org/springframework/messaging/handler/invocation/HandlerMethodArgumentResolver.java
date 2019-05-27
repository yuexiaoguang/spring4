package org.springframework.messaging.handler.invocation;

import org.springframework.core.MethodParameter;
import org.springframework.messaging.Message;

/**
 * 用于在给定{@link Message}的上下文中将方法参数解析为参数值的策略接口.
 */
public interface HandlerMethodArgumentResolver {

	/**
	 * 此解析程序是否支持给定的{@linkplain MethodParameter 方法参数}.
	 * 
	 * @param parameter 要检查的方法参数
	 * 
	 * @return {@code true} 如果此解析器支持提供的参数; 否则{@code false}
	 */
	boolean supportsParameter(MethodParameter parameter);

	/**
	 * 将方法参数解析为给定消息的参数值.
	 * 
	 * @param parameter 要解析的方法参数.
	 * 此参数必须先前已传递给{@link #supportsParameter(org.springframework.core.MethodParameter)},
	 * 该方法必须已返回{@code true}.
	 * @param message 当前处理的消息
	 * 
	 * @return 已解析的参数值, 或{@code null}
	 * @throws Exception 如果参数值的准备有错误
	 */
	Object resolveArgument(MethodParameter parameter, Message<?> message) throws Exception;

}
