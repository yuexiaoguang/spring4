package org.springframework.web.servlet.mvc.annotation;

import java.lang.reflect.Method;

import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.servlet.ModelAndView;

/**
 * 用于解析特定处理器方法的自定义返回值的SPI.
 * 通常用于检测特殊返回类型, 为它们解析已知的结果值.
 *
 * <p>典型的实现可能如下所示:
 *
 * <pre class="code">
 * public class MyModelAndViewResolver implements ModelAndViewResolver {
 *
 *     public ModelAndView resolveModelAndView(Method handlerMethod, Class handlerType,
 *             Object returnValue, ExtendedModelMap implicitModel, NativeWebRequest webRequest) {
 *         if (returnValue instanceof MySpecialRetVal.class)) {
 *             return new MySpecialRetVal(returnValue);
 *         }
 *         return UNRESOLVED;
 *     }
 * }</pre>
 */
public interface ModelAndViewResolver {

	/**
	 * 当解析器不知道如何处理给定的方法参数时, 返回的标记.
	 */
	ModelAndView UNRESOLVED = new ModelAndView();


	ModelAndView resolveModelAndView(Method handlerMethod, Class<?> handlerType, Object returnValue,
			ExtendedModelMap implicitModel, NativeWebRequest webRequest);

}
