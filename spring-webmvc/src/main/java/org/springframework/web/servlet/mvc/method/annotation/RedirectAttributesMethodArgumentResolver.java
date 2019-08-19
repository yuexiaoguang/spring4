package org.springframework.web.servlet.mvc.method.annotation;

import java.util.Map;

import org.springframework.core.MethodParameter;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.validation.DataBinder;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

/**
 * 解析{@link RedirectAttributes}类型的方法参数.
 *
 * <p>This resolver must be listed ahead of
 * {@link org.springframework.web.method.annotation.ModelMethodProcessor} and
 * {@link org.springframework.web.method.annotation.MapMethodProcessor},
 * 此解析器必须在ModelMethodProcessor之前列出, 它支持{@link Map}和{@link Model}参数,
 * 这两个参数都是{@code RedirectAttributes}的"super"类型, 并且还会尝试解析{@code RedirectAttributes}参数.
 */
public class RedirectAttributesMethodArgumentResolver implements HandlerMethodArgumentResolver {

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return RedirectAttributes.class.isAssignableFrom(parameter.getParameterType());
	}

	@Override
	public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
			NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {

		ModelMap redirectAttributes;
		if(binderFactory != null) {
			DataBinder dataBinder = binderFactory.createBinder(webRequest, null, null);
			redirectAttributes  = new RedirectAttributesModelMap(dataBinder);
		}
		else {
			redirectAttributes  = new RedirectAttributesModelMap();
		}
		mavContainer.setRedirectModel(redirectAttributes);
		return redirectAttributes;
	}

}
