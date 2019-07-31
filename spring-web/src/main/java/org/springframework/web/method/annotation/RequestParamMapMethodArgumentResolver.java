package org.springframework.web.method.annotation;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.core.MethodParameter;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * 解析使用 @{@link RequestParam}注解的{@link Map}方法参数, 其中注解未指定请求参数名称.
 * 请参阅 {@link RequestParamMethodArgumentResolver}以使用请求参数名称解​​析{@link Map}方法参数.
 *
 * <p>创建的{@link Map}包含所有请求参数名称/值对.
 * 如果方法参数类型是{@link MultiValueMap}, 则创建的Map包含所有请求参数, 以及请求参数具有多个值的情况下的所有值.
 */
public class RequestParamMapMethodArgumentResolver implements HandlerMethodArgumentResolver {

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		RequestParam requestParam = parameter.getParameterAnnotation(RequestParam.class);
		if (requestParam != null) {
			if (Map.class.isAssignableFrom(parameter.getParameterType())) {
				return !StringUtils.hasText(requestParam.name());
			}
		}
		return false;
	}

	@Override
	public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
			NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {

		Class<?> paramType = parameter.getParameterType();

		Map<String, String[]> parameterMap = webRequest.getParameterMap();
		if (MultiValueMap.class.isAssignableFrom(paramType)) {
			MultiValueMap<String, String> result = new LinkedMultiValueMap<String, String>(parameterMap.size());
			for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
				for (String value : entry.getValue()) {
					result.add(entry.getKey(), value);
				}
			}
			return result;
		}
		else {
			Map<String, String> result = new LinkedHashMap<String, String>(parameterMap.size());
			for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
				if (entry.getValue().length > 0) {
					result.put(entry.getKey(), entry.getValue()[0]);
				}
			}
			return result;
		}
	}
}
