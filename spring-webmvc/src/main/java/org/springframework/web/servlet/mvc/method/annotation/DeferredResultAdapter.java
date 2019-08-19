package org.springframework.web.servlet.mvc.method.annotation;

import org.springframework.web.context.request.async.DeferredResult;

/**
 * 将单个异步返回值适配为{@code DeferredResult}的约定.
 *
 * @deprecated 4.3.8自5.0以来, 基于可插入的{@code ReactiveAdapterRegistry}机制,
 * 为Spring MVC控制器方法添加了对反应式返回值的一流支持.
 * 另一种替代方法是实现自定义
 * {@link org.springframework.web.method.support.AsyncHandlerMethodReturnValueHandler}.
 */
@Deprecated
public interface DeferredResultAdapter {

	/**
	 * 为给定的返回值创建一个{@code DeferredResult}.
	 * 
	 * @param returnValue 返回值 (never {@code null})
	 * 
	 * @return the DeferredResult
	 */
	DeferredResult<?> adaptToDeferredResult(Object returnValue);

}
