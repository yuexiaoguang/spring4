package org.springframework.web.servlet.mvc.method.annotation;

import javax.servlet.http.HttpServletRequest;

import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponentsBuilder;


/**
 * {@link UriComponentsBuilder}类型的参数值的解析器.
 *
 * <p>返回的实例通过
 * {@link ServletUriComponentsBuilder#fromServletMapping(HttpServletRequest)}初始化.
 */
public class UriComponentsBuilderMethodArgumentResolver implements HandlerMethodArgumentResolver {

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		Class<?> type = parameter.getParameterType();
		return (UriComponentsBuilder.class == type || ServletUriComponentsBuilder.class == type);
	}

	@Override
	public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
			NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {

		HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
		return ServletUriComponentsBuilder.fromServletMapping(request);
	}

}
