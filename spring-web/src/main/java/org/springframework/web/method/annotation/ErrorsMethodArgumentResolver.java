package org.springframework.web.method.annotation;

import java.util.ArrayList;

import org.springframework.core.MethodParameter;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * 解析{@link Errors}方法参数.
 *
 * <p>预期{@code Errors}方法参数会立即出现在方法签名中的model属性之后.
 * 通过期望将添加到模型的最后两个属性作为模型属性及其{@link BindingResult}来解决此问题.
 */
public class ErrorsMethodArgumentResolver implements HandlerMethodArgumentResolver {

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		Class<?> paramType = parameter.getParameterType();
		return Errors.class.isAssignableFrom(paramType);
	}

	@Override
	public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
			NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {

		ModelMap model = mavContainer.getModel();
		if (model.size() > 0) {
			int lastIndex = model.size()-1;
			String lastKey = new ArrayList<String>(model.keySet()).get(lastIndex);
			if (lastKey.startsWith(BindingResult.MODEL_KEY_PREFIX)) {
				return model.get(lastKey);
			}
		}

		throw new IllegalStateException(
				"An Errors/BindingResult argument is expected to be declared immediately after the model attribute, " +
				"the @RequestBody or the @RequestPart arguments to which they apply: " + parameter.getMethod());
	}

}
