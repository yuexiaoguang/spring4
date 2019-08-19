package org.springframework.web.servlet.mvc.method.annotation;

import org.springframework.core.MethodParameter;
import org.springframework.util.PatternMatchUtils;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.RequestToViewNameTranslator;

/**
 * 处理类型为{@code void}和{@code String}的返回值, 将它们解释为视图名称引用.
 * 从4.2开始, 它还处理一般的{@code CharSequence}类型,
 * e.g. {@code StringBuilder} 或Groovy的{@code GString}, 作为视图名称.
 *
 * <p>{@code null}返回值, 由于{@code void}返回类型或保留为原样的实际返回值,
 * 允许配置的{@link RequestToViewNameTranslator}按惯例选择视图名称.
 *
 * <p>字符串返回值可以通过多种方式解释, 具体取决于{@code @ModelAttribute}或{@code @ResponseBody}等注解的存在.
 * 因此, 应在支持这些注解的处理器之后配置此处理器.
 */
public class ViewNameMethodReturnValueHandler implements HandlerMethodReturnValueHandler {

	private String[] redirectPatterns;


	/**
	 * 配置一个更简单的模式 (如{@link PatternMatchUtils#simpleMatch}中所述),
	 * 以便识别除"redirect:"之外的自定义重定向前缀.
	 * <p>请注意, 仅配置此属性不会使自定义重定向前缀起作用.
	 * 必须有一个自定义视图, 它也可以识别前缀.
	 */
	public void setRedirectPatterns(String... redirectPatterns) {
		this.redirectPatterns = redirectPatterns;
	}

	/**
	 * 配置的重定向模式.
	 */
	public String[] getRedirectPatterns() {
		return this.redirectPatterns;
	}


	@Override
	public boolean supportsReturnType(MethodParameter returnType) {
		Class<?> paramType = returnType.getParameterType();
		return (void.class == paramType || CharSequence.class.isAssignableFrom(paramType));
	}

	@Override
	public void handleReturnValue(Object returnValue, MethodParameter returnType,
			ModelAndViewContainer mavContainer, NativeWebRequest webRequest) throws Exception {

		if (returnValue instanceof CharSequence) {
			String viewName = returnValue.toString();
			mavContainer.setViewName(viewName);
			if (isRedirectViewName(viewName)) {
				mavContainer.setRedirectModelScenario(true);
			}
		}
		else if (returnValue != null){
			// should not happen
			throw new UnsupportedOperationException("Unexpected return type: " +
					returnType.getParameterType().getName() + " in method: " + returnType.getMethod());
		}
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
		return (PatternMatchUtils.simpleMatch(this.redirectPatterns, viewName) || viewName.startsWith("redirect:"));
	}

}
