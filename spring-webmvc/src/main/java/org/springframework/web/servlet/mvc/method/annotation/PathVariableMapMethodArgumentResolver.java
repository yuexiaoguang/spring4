package org.springframework.web.servlet.mvc.method.annotation;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.core.MethodParameter;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.HandlerMapping;

/**
 * 解析使用 @{@link PathVariable}注解的{@link Map}方法参数, 其中注解未指定路径变量名称.
 * 创建的{@link Map}包含所有URI模板名称/值对.
 */
public class PathVariableMapMethodArgumentResolver implements HandlerMethodArgumentResolver {

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		PathVariable ann = parameter.getParameterAnnotation(PathVariable.class);
		return (ann != null && (Map.class.isAssignableFrom(parameter.getParameterType()))
				&& !StringUtils.hasText(ann.value()));
	}

	/**
	 * 返回包含所有URI模板变量或空映射的Map.
	 */
	@Override
	public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
			NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {

		@SuppressWarnings("unchecked")
		Map<String, String> uriTemplateVars =
				(Map<String, String>) webRequest.getAttribute(
						HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);

		if (!CollectionUtils.isEmpty(uriTemplateVars)) {
			return new LinkedHashMap<String, String>(uriTemplateVars);
		}
		else {
			return Collections.emptyMap();
		}
	}

}
