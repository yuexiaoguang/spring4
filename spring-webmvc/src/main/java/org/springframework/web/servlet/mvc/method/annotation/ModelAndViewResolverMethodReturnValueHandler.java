package org.springframework.web.servlet.mvc.method.annotation;

import java.lang.reflect.Method;
import java.util.List;

import org.springframework.core.MethodParameter;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.annotation.ModelAttributeMethodProcessor;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.annotation.ModelAndViewResolver;

/**
 * 此返回值处理器旨在在所有其他处理器之后进行排序, 因为它尝试处理 _any_ 返回值类型 (i.e. 为所有返回类型返回{@code true}).
 *
 * <p>返回值可以使用{@link ModelAndViewResolver}处理, 也可以通过将其视为模型属性(如果它是非简单类型)来处理.
 * 如果这些都没有成功 (基本上是除String之外的简单类型), 则会引发{@link UnsupportedOperationException}.
 *
 * <p><strong>Note:</strong> 这个类主要用于支持{@link ModelAndViewResolver},
 * 遗憾的是, 由于无法实现{@link HandlerMethodReturnValueHandler#supportsReturnType}方法,
 * 因此无法正确地适配{@link HandlerMethodReturnValueHandler}约定.
 * 因此, {@code ModelAndViewResolver}仅限于在所有其他返回值处理器之后在最后调用.
 * 建议将{@code ModelAndViewResolver}重新实现为{@code HandlerMethodReturnValueHandler},
 * 它还可以更好地访问返回类型和方法信息.
 */
public class ModelAndViewResolverMethodReturnValueHandler implements HandlerMethodReturnValueHandler {

	private final List<ModelAndViewResolver> mavResolvers;

	private final ModelAttributeMethodProcessor modelAttributeProcessor = new ModelAttributeMethodProcessor(true);


	public ModelAndViewResolverMethodReturnValueHandler(List<ModelAndViewResolver> mavResolvers) {
		this.mavResolvers = mavResolvers;
	}


	/**
	 * Always returns {@code true}. See class-level note.
	 */
	@Override
	public boolean supportsReturnType(MethodParameter returnType) {
		return true;
	}

	@Override
	public void handleReturnValue(Object returnValue, MethodParameter returnType,
			ModelAndViewContainer mavContainer, NativeWebRequest webRequest) throws Exception {

		if (this.mavResolvers != null) {
			for (ModelAndViewResolver mavResolver : this.mavResolvers) {
				Class<?> handlerType = returnType.getContainingClass();
				Method method = returnType.getMethod();
				ExtendedModelMap model = (ExtendedModelMap) mavContainer.getModel();
				ModelAndView mav = mavResolver.resolveModelAndView(method, handlerType, returnValue, model, webRequest);
				if (mav != ModelAndViewResolver.UNRESOLVED) {
					mavContainer.addAllAttributes(mav.getModel());
					mavContainer.setViewName(mav.getViewName());
					if (!mav.isReference()) {
						mavContainer.setView(mav.getView());
					}
					return;
				}
			}
		}

		// No suitable ModelAndViewResolver...
		if (this.modelAttributeProcessor.supportsReturnType(returnType)) {
			this.modelAttributeProcessor.handleReturnValue(returnValue, returnType, mavContainer, webRequest);
		}
		else {
			throw new UnsupportedOperationException("Unexpected return type: " +
					returnType.getParameterType().getName() + " in method: " + returnType.getMethod());
		}
	}

}
