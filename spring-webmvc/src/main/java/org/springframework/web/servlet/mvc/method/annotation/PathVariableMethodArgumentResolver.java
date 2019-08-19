package org.springframework.web.servlet.mvc.method.annotation;

import java.beans.PropertyEditor;
import java.util.HashMap;
import java.util.Map;

import org.springframework.core.MethodParameter;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ValueConstants;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.method.annotation.AbstractNamedValueMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.method.support.UriComponentsContributor;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.View;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * 解析使用 @{@link PathVariable}注解的方法参数.
 *
 * <p> @{@link PathVariable} 是一个从URI模板变量解析的命名值.
 * 它始终是必需的, 并且没有默认值可供使用.
 * 有关如何处理命名值的更多信息, 请参阅基类
 * {@link org.springframework.web.method.annotation.AbstractNamedValueMethodArgumentResolver}.
 *
 * <p>如果方法参数类型为{@link Map}, 则注解中指定的名称用于解析URI变量String值.
 * 然后, 通过类型转换将值转换为{@link Map}, 假设已注册了合适的{@link Converter}或{@link PropertyEditor}.
 *
 * <p>调用{@link WebDataBinder}以将类型转换应用于尚未与方法参数类型匹配的已解析的路径变量值.
 */
public class PathVariableMethodArgumentResolver extends AbstractNamedValueMethodArgumentResolver
		implements UriComponentsContributor {

	private static final TypeDescriptor STRING_TYPE_DESCRIPTOR = TypeDescriptor.valueOf(String.class);


	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		if (!parameter.hasParameterAnnotation(PathVariable.class)) {
			return false;
		}
		if (Map.class.isAssignableFrom(parameter.nestedIfOptional().getNestedParameterType())) {
			String paramName = parameter.getParameterAnnotation(PathVariable.class).value();
			return StringUtils.hasText(paramName);
		}
		return true;
	}

	@Override
	protected NamedValueInfo createNamedValueInfo(MethodParameter parameter) {
		PathVariable annotation = parameter.getParameterAnnotation(PathVariable.class);
		return new PathVariableNamedValueInfo(annotation);
	}

	@Override
	@SuppressWarnings("unchecked")
	protected Object resolveName(String name, MethodParameter parameter, NativeWebRequest request) throws Exception {
		Map<String, String> uriTemplateVars = (Map<String, String>) request.getAttribute(
				HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);
		return (uriTemplateVars != null ? uriTemplateVars.get(name) : null);
	}

	@Override
	protected void handleMissingValue(String name, MethodParameter parameter) throws ServletRequestBindingException {
		throw new MissingPathVariableException(name, parameter);
	}

	@Override
	@SuppressWarnings("unchecked")
	protected void handleResolvedValue(Object arg, String name, MethodParameter parameter,
			ModelAndViewContainer mavContainer, NativeWebRequest request) {

		String key = View.PATH_VARIABLES;
		int scope = RequestAttributes.SCOPE_REQUEST;
		Map<String, Object> pathVars = (Map<String, Object>) request.getAttribute(key, scope);
		if (pathVars == null) {
			pathVars = new HashMap<String, Object>();
			request.setAttribute(key, pathVars, scope);
		}
		pathVars.put(name, arg);
	}

	@Override
	public void contributeMethodArgument(MethodParameter parameter, Object value,
			UriComponentsBuilder builder, Map<String, Object> uriVariables, ConversionService conversionService) {

		if (Map.class.isAssignableFrom(parameter.nestedIfOptional().getNestedParameterType())) {
			return;
		}

		PathVariable ann = parameter.getParameterAnnotation(PathVariable.class);
		String name = (ann != null && !StringUtils.isEmpty(ann.value()) ? ann.value() : parameter.getParameterName());
		value = formatUriValue(conversionService, new TypeDescriptor(parameter.nestedIfOptional()), value);
		uriVariables.put(name, value);
	}

	protected String formatUriValue(ConversionService cs, TypeDescriptor sourceType, Object value) {
		if (value == null) {
			return null;
		}
		else if (value instanceof String) {
			return (String) value;
		}
		else if (cs != null) {
			return (String) cs.convert(value, sourceType, STRING_TYPE_DESCRIPTOR);
		}
		else {
			return value.toString();
		}
	}


	private static class PathVariableNamedValueInfo extends NamedValueInfo {

		public PathVariableNamedValueInfo(PathVariable annotation) {
			super(annotation.name(), annotation.required(), ValueConstants.DEFAULT_NONE);
		}
	}

}
