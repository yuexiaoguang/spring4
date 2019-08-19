package org.springframework.web.servlet.mvc.method.annotation;

import java.io.IOException;
import java.lang.reflect.Type;

import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;

/**
 * 使用默认方法实现实现
 * {@link org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice
 * ResponseBodyAdvice}的便捷起点.
 *
 * <p>子类需要实现{@link #supports}以返回true, 具体取决于增强何时适用.
 */
public abstract class RequestBodyAdviceAdapter implements RequestBodyAdvice {

	/**
	 * 默认实现返回传入的主体.
	 */
	@Override
	public Object handleEmptyBody(Object body, HttpInputMessage inputMessage,
			MethodParameter parameter, Type targetType,
			Class<? extends HttpMessageConverter<?>> converterType) {

		return body;
	}

	/**
	 * 默认实现返回传入的InputMessage.
	 */
	@Override
	public HttpInputMessage beforeBodyRead(HttpInputMessage inputMessage, MethodParameter parameter,
			Type targetType, Class<? extends HttpMessageConverter<?>> converterType)
			throws IOException {

		return inputMessage;
	}

	/**
	 * 默认实现返回传入的主体.
	 */
	@Override
	public Object afterBodyRead(Object body, HttpInputMessage inputMessage, MethodParameter parameter,
			Type targetType, Class<? extends HttpMessageConverter<?>> converterType) {

		return body;
	}
}
