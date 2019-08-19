package org.springframework.web.servlet.mvc.method.annotation;

import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.AbstractJackson2HttpMessageConverter;
import org.springframework.http.converter.json.MappingJacksonValue;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;

/**
 * {@code ResponseBodyAdvice}实现的便捷基类,
 * 可以在使用{@link AbstractJackson2HttpMessageConverter}的具体子类进行JSON序列化之前自定义响应.
 */
public abstract class AbstractMappingJacksonResponseBodyAdvice implements ResponseBodyAdvice<Object> {

	@Override
	public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
		return AbstractJackson2HttpMessageConverter.class.isAssignableFrom(converterType);
	}

	@Override
	public final Object beforeBodyWrite(Object body, MethodParameter returnType,
			MediaType contentType, Class<? extends HttpMessageConverter<?>> converterType,
			ServerHttpRequest request, ServerHttpResponse response) {

		MappingJacksonValue container = getOrCreateContainer(body);
		beforeBodyWriteInternal(container, contentType, returnType, request, response);
		return container;
	}

	/**
	 * 将主体包装在{@link MappingJacksonValue}值容器中 (用于提供其他序列化指令), 或者只是在已经包装的情况下将其强制转换.
	 */
	protected MappingJacksonValue getOrCreateContainer(Object body) {
		return (body instanceof MappingJacksonValue ? (MappingJacksonValue) body : new MappingJacksonValue(body));
	}

	/**
	 * 仅在转换器类型为{@code MappingJackson2HttpMessageConverter}时调用.
	 */
	protected abstract void beforeBodyWriteInternal(MappingJacksonValue bodyContainer, MediaType contentType,
			MethodParameter returnType, ServerHttpRequest request, ServerHttpResponse response);

}
