package org.springframework.web.servlet.mvc.method.annotation;

import org.springframework.core.MethodParameter;
import org.springframework.util.PatternMatchUtils;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.SmartView;
import org.springframework.web.servlet.View;

/**
 * 处理{@link ModelAndView}类型的返回值, 将视图和模型信息复制到{@link ModelAndViewContainer}.
 *
 * <p>如果返回值为{@code null}, {@link ModelAndViewContainer#setRequestHandled(boolean)}标志设置为{@code true},
 * 以指示请求已直接处理.
 *
 * <p>{@link ModelAndView}返回类型具有设置目的.
 * 因此, 应该在处理器之前配置此处理器, 该处理器支持使用{@code @ModelAttribute}
 * 或{@code @ResponseBody}注解的返回值类型, 以确保它们不会接管.
 */
public class ModelAndViewMethodReturnValueHandler implements HandlerMethodReturnValueHandler {

	private String[] redirectPatterns;


	/**
	 * 配置一个更简单的模式 (如{@link PatternMatchUtils#simpleMatch}中所述),
	 * 以便识别除"redirect:"之外的自定义重定向前缀.
	 * <p>请注意, 仅配置此属性不会使自定义重定向前缀起作用.
	 * 必须有一个自定义{@link View}来识别前缀.
	 */
	public void setRedirectPatterns(String... redirectPatterns) {
		this.redirectPatterns = redirectPatterns;
	}

	/**
	 * 返回配置的重定向模式.
	 */
	public String[] getRedirectPatterns() {
		return this.redirectPatterns;
	}


	@Override
	public boolean supportsReturnType(MethodParameter returnType) {
		return ModelAndView.class.isAssignableFrom(returnType.getParameterType());
	}

	@Override
	public void handleReturnValue(Object returnValue, MethodParameter returnType,
			ModelAndViewContainer mavContainer, NativeWebRequest webRequest) throws Exception {

		if (returnValue == null) {
			mavContainer.setRequestHandled(true);
			return;
		}

		ModelAndView mav = (ModelAndView) returnValue;
		if (mav.isReference()) {
			String viewName = mav.getViewName();
			mavContainer.setViewName(viewName);
			if (viewName != null && isRedirectViewName(viewName)) {
				mavContainer.setRedirectModelScenario(true);
			}
		}
		else {
			View view = mav.getView();
			mavContainer.setView(view);
			if (view instanceof SmartView) {
				if (((SmartView) view).isRedirectView()) {
					mavContainer.setRedirectModelScenario(true);
				}
			}
		}
		mavContainer.setStatus(mav.getStatus());
		mavContainer.addAllAttributes(mav.getModel());
	}

	/**
	 * 给定的视图名称是否为重定向视图引用.
	 * 默认实现检查配置的重定向模式, 以及视图名称是否以"redirect:"前缀开头.
	 * 
	 * @param viewName 要检查的视图名称, never {@code null}
	 * 
	 * @return "true" 如果给定的视图名称被识别为重定向视图引用; 否则"false"
	 */
	protected boolean isRedirectViewName(String viewName) {
		if (PatternMatchUtils.simpleMatch(this.redirectPatterns, viewName)) {
			return true;
		}
		return viewName.startsWith("redirect:");
	}

}
