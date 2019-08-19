package org.springframework.web.servlet.mvc.method.annotation;

import org.springframework.core.MethodParameter;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.RequestToViewNameTranslator;
import org.springframework.web.servlet.SmartView;
import org.springframework.web.servlet.View;

/**
 * 处理{@link View}类型的返回值.
 *
 * <p>{@code null}返回值保持原样, 将其留给配置的{@link RequestToViewNameTranslator}以按惯例选择视图名称.
 *
 * <p>{@link View}返回类型具有设置目的.
 * 因此, 应该在处理器之前配置此处理器, 该处理程序支持使用{@code @ModelAttribute}
 * 或{@code @ResponseBody}注解的返回值类型, 以确保它们不会接管.
 */
public class ViewMethodReturnValueHandler implements HandlerMethodReturnValueHandler {

	@Override
	public boolean supportsReturnType(MethodParameter returnType) {
		return View.class.isAssignableFrom(returnType.getParameterType());
	}

	@Override
	public void handleReturnValue(Object returnValue, MethodParameter returnType,
			ModelAndViewContainer mavContainer, NativeWebRequest webRequest) throws Exception {

		if (returnValue == null) {
			return;
		}
		else if (returnValue instanceof View){
			View view = (View) returnValue;
			mavContainer.setView(view);
			if (view instanceof SmartView) {
				if (((SmartView) view).isRedirectView()) {
					mavContainer.setRedirectModelScenario(true);
				}
			}
		}
		else {
			// should not happen
			throw new UnsupportedOperationException("Unexpected return type: " +
					returnType.getParameterType().getName() + " in method: " + returnType.getMethod());
		}
	}

}
