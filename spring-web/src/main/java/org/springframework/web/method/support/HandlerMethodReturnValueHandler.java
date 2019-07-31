package org.springframework.web.method.support;

import org.springframework.core.MethodParameter;
import org.springframework.web.context.request.NativeWebRequest;

/**
 * 用于处理从处理器方法调用返回的值的策略接口.
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
	 * 通过向模型添加属性和设置视图,
	 * 或将{@link ModelAndViewContainer#setRequestHandled}标志设置为{@code true}来处理给定的返回值,
	 * 以指示响应已直接处理.
	 * 
	 * @param returnValue 处理器方法返回的值
	 * @param returnType 返回值的类型. 此类型必须先前已经过{@link #supportsReturnType}验证, 且必须已返回{@code true}.
	 * @param mavContainer 当前请求的ModelAndViewContainer
	 * @param webRequest 当前的要求
	 * 
	 * @throws Exception 如果返回值处理导致错误
	 */
	void handleReturnValue(Object returnValue, MethodParameter returnType,
			ModelAndViewContainer mavContainer, NativeWebRequest webRequest) throws Exception;

}
