package org.springframework.web.bind.support;

import org.springframework.core.MethodParameter;
import org.springframework.web.context.request.NativeWebRequest;

/**
 * 用于解析特定处理器方法参数的自定义参数的SPI.
 * 通常用于检测特殊参数类型, 为它们解析已知的参数值.
 *
 * <p>典型的实现可能如下所示:
 *
 * <pre class="code">
 * public class MySpecialArgumentResolver implements WebArgumentResolver {
 *
 *   public Object resolveArgument(MethodParameter methodParameter, NativeWebRequest webRequest) {
 *     if (methodParameter.getParameterType().equals(MySpecialArg.class)) {
 *       return new MySpecialArg("myValue");
 *     }
 *     return UNRESOLVED;
 *   }
 * }</pre>
 */
public interface WebArgumentResolver {

	/**
	 * 当解析器不知道如何处理给定的方法参数时, 返回的标记.
	 */
	Object UNRESOLVED = new Object();


	/**
	 * 解析给定Web请求中给定处理器方法参数的参数.
	 * 
	 * @param methodParameter 要解析的处理器方法参数
	 * @param webRequest 当前的Web请求, 也允许访问本机请求
	 * 
	 * @return 参数值, 或{@code UNRESOLVED}
	 * @throws Exception 如果解析失败
	 */
	Object resolveArgument(MethodParameter methodParameter, NativeWebRequest webRequest) throws Exception;

}
