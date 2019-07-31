package org.springframework.web.method.support;

import org.springframework.core.MethodParameter;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;

/**
 * 用于在给定请求的上下文中将方法参数解析为参数值的策略接口.
 */
public interface HandlerMethodArgumentResolver {

	/**
	 * 此解析器是否支持给定的{@linkplain MethodParameter 方法参数}.
	 * 
	 * @param parameter 要检查的方法参数
	 * 
	 * @return {@code true} 如果此解析器支持提供的参数; 否则{@code false}
	 */
	boolean supportsParameter(MethodParameter parameter);

	/**
	 * 将方法参数解析为来自给定请求的参数值.
	 * {@link ModelAndViewContainer}提供对请求模型的访问.
	 * {@link WebDataBinderFactory}提供了一种在需要进行数据绑定和类型转换时创建{@link WebDataBinder}实例的方法.
	 * 
	 * @param parameter 要解析的方法参数.
	 * 此参数必须先前已经过{{@link #supportsParameter}验证, 且必须已返回{@code true}.
	 * @param mavContainer 当前请求的ModelAndViewContainer
	 * @param webRequest 当前的请求
	 * @param binderFactory 用于创建{@link WebDataBinder}实例的工厂
	 * 
	 * @return 解析后的参数值, 或{@code null}
	 * @throws Exception 如果参数值的准备有错误
	 */
	Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
			NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception;

}
