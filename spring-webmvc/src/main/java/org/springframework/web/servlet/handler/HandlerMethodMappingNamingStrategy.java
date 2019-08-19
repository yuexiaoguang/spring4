package org.springframework.web.servlet.handler;

import org.springframework.web.method.HandlerMethod;

/**
 * 为处理器方法的映射指定名称的策略.
 *
 * <p>可以在
 * {@link org.springframework.web.servlet.handler.AbstractHandlerMethodMapping
 * AbstractHandlerMethodMapping} 上配置策略.
 * 它用于为每个已注册的处理器方法的映射指定名称.
 * 然后可以通过
 * {@link org.springframework.web.servlet.handler.AbstractHandlerMethodMapping#getHandlerMethodsForMappingName(String)
 * AbstractHandlerMethodMapping#getHandlerMethodsForMappingName}查询名称.
 *
 * <p>应用程序可以借助于静态方法
 * {@link org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder#fromMappingName(String)
 * MvcUriComponentsBuilder#fromMappingName}或在JSP中通过Spring标记库注册的"mvcUrl"函数按名称构建控制器方法的URL.
 */
public interface HandlerMethodMappingNamingStrategy<T> {

	/**
	 * 确定给定HandlerMethod和映射的名称.
	 * 
	 * @param handlerMethod 处理器方法
	 * @param mapping 映射
	 * 
	 * @return 名称
	 */
	String getName(HandlerMethod handlerMethod, T mapping);

}
