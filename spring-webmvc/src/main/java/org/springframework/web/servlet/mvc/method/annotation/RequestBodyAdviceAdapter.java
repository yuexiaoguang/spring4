package org.springframework.web.servlet.mvc.method.annotation;

import java.io.IOException;
import java.lang.reflect.Type;

import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;

/**
 * A convenient starting point for implementing
 * {@link org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice
 * ResponseBodyAdvice} with default method implementations.
 *
 * <p>Sub-classes are required to implement {@link #supports} to return true
 * depending on when the advice applies.
 */
public abstract class RequestBodyAdviceAdapter implements RequestBodyAdvice {

	/**
	 * The default implementation returns the body that was passed in.
	 */
	@Override
	public Object handleEmptyBody(Object body, HttpInputMessage inputMessage,
			MethodParameter parameter, Type targetType,
			Class<? extends HttpMessageConverter<?>> converterType) {

		return body;
	}

	/**
	 * The default implementation returns the InputMessage that was passed in.
	 */
	@Override
	public HttpInputMessage beforeBodyRead(HttpInputMessage inputMessage, MethodParameter parameter,
			Type targetType, Class<? extends HttpMessageConverter<?>> converterType)
			throws IOException {

		return inputMessage;
	}

	/**
	 * The default implementation returns the body that was passed in.
	 */
	@Override
	public Object afterBodyRead(Object body, HttpInputMessage inputMessage, MethodParameter parameter,
			Type targetType, Class<? extends HttpMessageConverter<?>> converterType) {

		return body;
	}

}
