package org.springframework.web.servlet.mvc.method.annotation;

import org.springframework.http.server.ServerHttpResponse;

/**
 * 将流式异步类型适配为{@code ResponseBodyEmitter}的约定.
 *
 * @deprecated 4.3.8自5.0以来, 基于可插入的{@code ReactiveAdapterRegistry}机制,
 * 为Spring MVC控制器方法添加了对反应式返回值的一流支持.
 */
@Deprecated
public interface ResponseBodyEmitterAdapter {

	/**
	 * Obtain a {@code ResponseBodyEmitter} for the given return value.
	 * If the return is the body {@code ResponseEntity} then the given
	 * {@code ServerHttpResponse} contains its status and headers.
	 * @param returnValue the return value (never {@code null})
	 * @param response the response
	 * @return the return value adapted to a {@code ResponseBodyEmitter}
	 */
	ResponseBodyEmitter adaptToEmitter(Object returnValue, ServerHttpResponse response);

}
