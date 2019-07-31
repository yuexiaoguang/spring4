package org.springframework.web.method.annotation;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * 解析使用{@code @RequestHeader}注解的{@link Map}方法参数.
 * 对于使用{@code @RequestHeader}注解的单个header值, 请参阅{@link RequestHeaderMethodArgumentResolver}.
 *
 * <p>创建的{@link Map}包含所有请求header名称/值对.
 * 方法参数类型可以是{@link MultiValueMap}以接收header的所有值, 而不仅是第一个.
 */
public class RequestHeaderMapMethodArgumentResolver implements HandlerMethodArgumentResolver {

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return (parameter.hasParameterAnnotation(RequestHeader.class) &&
				Map.class.isAssignableFrom(parameter.getParameterType()));
	}

	@Override
	public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
			NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {

		Class<?> paramType = parameter.getParameterType();
		if (MultiValueMap.class.isAssignableFrom(paramType)) {
			MultiValueMap<String, String> result;
			if (HttpHeaders.class.isAssignableFrom(paramType)) {
				result = new HttpHeaders();
			}
			else {
				result = new LinkedMultiValueMap<String, String>();
			}
			for (Iterator<String> iterator = webRequest.getHeaderNames(); iterator.hasNext();) {
				String headerName = iterator.next();
				String[] headerValues = webRequest.getHeaderValues(headerName);
				if (headerValues != null) {
					for (String headerValue : headerValues) {
						result.add(headerName, headerValue);
					}
				}
			}
			return result;
		}
		else {
			Map<String, String> result = new LinkedHashMap<String, String>();
			for (Iterator<String> iterator = webRequest.getHeaderNames(); iterator.hasNext();) {
				String headerName = iterator.next();
				String headerValue = webRequest.getHeader(headerName);
				if (headerValue != null) {
					result.put(headerName, headerValue);
				}
			}
			return result;
		}
	}

}
