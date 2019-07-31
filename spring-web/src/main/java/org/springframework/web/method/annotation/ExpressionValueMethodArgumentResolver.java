package org.springframework.web.method.annotation;

import javax.servlet.ServletException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.context.request.NativeWebRequest;

/**
 * 解析使用{@code @Value}注解的方法参数.
 *
 * <p>{@code @Value}没有名称, 但是从默认值字符串中解析,
 * 该字符串可能包含 ${...} 占位符或Spring Expression Language #{...}表达式.
 *
 * <p>可以调用{@link WebDataBinder}以将类型转换应用于已解析的参数值.
 */
public class ExpressionValueMethodArgumentResolver extends AbstractNamedValueMethodArgumentResolver {

	/**
	 * @param beanFactory 一个bean工厂, 用于在默认值中解析 ${...}占位符和 #{...} SpEL表达式;
	 * 如果预期默认值不包含表达式, 则为{@code null}
	 */
	public ExpressionValueMethodArgumentResolver(ConfigurableBeanFactory beanFactory) {
		super(beanFactory);
	}


	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return parameter.hasParameterAnnotation(Value.class);
	}

	@Override
	protected NamedValueInfo createNamedValueInfo(MethodParameter parameter) {
		Value annotation = parameter.getParameterAnnotation(Value.class);
		return new ExpressionValueNamedValueInfo(annotation);
	}

	@Override
	protected Object resolveName(String name, MethodParameter parameter, NativeWebRequest webRequest) throws Exception {
		// No name to resolve
		return null;
	}

	@Override
	protected void handleMissingValue(String name, MethodParameter parameter) throws ServletException {
		throw new UnsupportedOperationException("@Value is never required: " + parameter.getMethod());
	}


	private static class ExpressionValueNamedValueInfo extends NamedValueInfo {

		private ExpressionValueNamedValueInfo(Value annotation) {
			super("@Value", false, annotation.value());
		}
	}

}
