package org.springframework.web.method.annotation;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.CookieValue;

/**
 * 一个基本抽象类, 用于解析带{@code @CookieValue}注释的方法参数. 子类从请求中提取cookie值.
 *
 * <p>{@code @CookieValue}是从cookie解析的命名值.
 * 当cookie不存在时, 它具有必需的标志和默认值.
 *
 * <p>可以调用{@link WebDataBinder}将类型转换应用于已解析的cookie值.
 */
public abstract class AbstractCookieValueMethodArgumentResolver extends AbstractNamedValueMethodArgumentResolver {

	/**
	 * @param beanFactory 一个bean工厂, 用于在默认值中解析 ${...}占位符和 #{...} SpEL表达式;
	 * 如果预期默认值不包含表达式, 则为{@code null}
	 */
	public AbstractCookieValueMethodArgumentResolver(ConfigurableBeanFactory beanFactory) {
		super(beanFactory);
	}


	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return parameter.hasParameterAnnotation(CookieValue.class);
	}

	@Override
	protected NamedValueInfo createNamedValueInfo(MethodParameter parameter) {
		CookieValue annotation = parameter.getParameterAnnotation(CookieValue.class);
		return new CookieValueNamedValueInfo(annotation);
	}

	@Override
	protected void handleMissingValue(String name, MethodParameter parameter) throws ServletRequestBindingException {
		throw new ServletRequestBindingException("Missing cookie '" + name +
				"' for method parameter of type " + parameter.getNestedParameterType().getSimpleName());
	}


	private static class CookieValueNamedValueInfo extends NamedValueInfo {

		private CookieValueNamedValueInfo(CookieValue annotation) {
			super(annotation.name(), annotation.required(), annotation.defaultValue());
		}
	}

}
