package org.springframework.web.method.annotation;

import java.util.Map;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.context.request.NativeWebRequest;

/**
 * 解析使用{@code @RequestHeader}注解的方法参数, 但{@link Map}参数除外.
 * 有关使用{@code @RequestHeader}注解的{@link Map}参数的详细信息, 请参阅{@link RequestHeaderMapMethodArgumentResolver}.
 *
 * <p>{@code @RequestHeader}是从请求header解析的命名值.
 * 它有一个必需的标志和一个默认值, 当请求header不存在时, 会使用默认值.
 *
 * <p>调用{@link WebDataBinder}以将类型转换应用于尚未与方法参数类型匹配的已解析的请求header值.
 */
public class RequestHeaderMethodArgumentResolver extends AbstractNamedValueMethodArgumentResolver {

	/**
	 * @param beanFactory 一个bean工厂, 用于在默认值中解析 ${...}占位符和 #{...} SpEL表达式;
	 * 如果预期默认值不具有表达式, 则为{@code null}
	 */
	public RequestHeaderMethodArgumentResolver(ConfigurableBeanFactory beanFactory) {
		super(beanFactory);
	}


	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return (parameter.hasParameterAnnotation(RequestHeader.class) &&
				!Map.class.isAssignableFrom(parameter.nestedIfOptional().getNestedParameterType()));
	}

	@Override
	protected NamedValueInfo createNamedValueInfo(MethodParameter parameter) {
		RequestHeader annotation = parameter.getParameterAnnotation(RequestHeader.class);
		return new RequestHeaderNamedValueInfo(annotation);
	}

	@Override
	protected Object resolveName(String name, MethodParameter parameter, NativeWebRequest request) throws Exception {
		String[] headerValues = request.getHeaderValues(name);
		if (headerValues != null) {
			return (headerValues.length == 1 ? headerValues[0] : headerValues);
		}
		else {
			return null;
		}
	}

	@Override
	protected void handleMissingValue(String name, MethodParameter parameter) throws ServletRequestBindingException {
		throw new ServletRequestBindingException("Missing request header '" + name +
				"' for method parameter of type " + parameter.getNestedParameterType().getSimpleName());
	}


	private static class RequestHeaderNamedValueInfo extends NamedValueInfo {

		private RequestHeaderNamedValueInfo(RequestHeader annotation) {
			super(annotation.name(), annotation.required(), annotation.defaultValue());
		}
	}

}
