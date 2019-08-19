package org.springframework.web.servlet.mvc.method.annotation;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.MatrixVariable;
import org.springframework.web.bind.annotation.ValueConstants;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.HandlerMapping;

/**
 * 解析使用{@link MatrixVariable @MatrixVariable}注解的{@link Map}类型的参数, 其中注解未指定名称.
 * 换句话说, 此解析器的目的是提供对多个矩阵变量的访问, 这些变量全部或与特定路径变量相关联.
 *
 * <p>指定名称时, 类型为Map的参数将被视为具有Map值的单个属性,
 * 并由{@link MatrixVariableMethodArgumentResolver}解析.
 */
public class MatrixVariableMapMethodArgumentResolver implements HandlerMethodArgumentResolver {

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		MatrixVariable matrixVariable = parameter.getParameterAnnotation(MatrixVariable.class);
		if (matrixVariable != null) {
			if (Map.class.isAssignableFrom(parameter.getParameterType())) {
				return !StringUtils.hasText(matrixVariable.name());
			}
		}
		return false;
	}

	@Override
	public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
			NativeWebRequest request, WebDataBinderFactory binderFactory) throws Exception {

		@SuppressWarnings("unchecked")
		Map<String, MultiValueMap<String, String>> matrixVariables =
				(Map<String, MultiValueMap<String, String>>) request.getAttribute(
						HandlerMapping.MATRIX_VARIABLES_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);

		if (CollectionUtils.isEmpty(matrixVariables)) {
			return Collections.emptyMap();
		}

		MultiValueMap<String, String> map = new LinkedMultiValueMap<String, String>();
		String pathVariable = parameter.getParameterAnnotation(MatrixVariable.class).pathVar();

		if (!pathVariable.equals(ValueConstants.DEFAULT_NONE)) {
			MultiValueMap<String, String> mapForPathVariable = matrixVariables.get(pathVariable);
			if (mapForPathVariable == null) {
				return Collections.emptyMap();
			}
			map.putAll(mapForPathVariable);
		}
		else {
			for (MultiValueMap<String, String> vars : matrixVariables.values()) {
				for (String name : vars.keySet()) {
					for (String value : vars.get(name)) {
						map.add(name, value);
					}
				}
			}
		}

		return (isSingleValueMap(parameter) ? map.toSingleValueMap() : map);
	}

	private boolean isSingleValueMap(MethodParameter parameter) {
		if (!MultiValueMap.class.isAssignableFrom(parameter.getParameterType())) {
			ResolvableType[] genericTypes = ResolvableType.forMethodParameter(parameter).getGenerics();
			if (genericTypes.length == 2) {
				return !List.class.isAssignableFrom(genericTypes[1].getRawClass());
			}
		}
		return false;
	}

}
