package org.springframework.web.servlet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 由对象实现的接口, 该对象可以解析在处理器映射或执行期间抛出的异常, 通常解析为错误视图.
 * 实现者通常在应用程序上下文中注册为bean.
 *
 * <p>错误视图类似于JSP错误页面, 但可以与任何类型的异常一起使用, 包括受检异常, 以及特定处理器的潜在细粒度映射.
 */
public interface HandlerExceptionResolver {

	/**
	 * 尝试解析在处理器执行期间抛出的给定异常, 返回表示特定错误页面的{@link ModelAndView}.
	 * <p>返回的{@code ModelAndView}可能是{@linkplain ModelAndView#isEmpty() empty},
	 * 表示异常已成功解析, 但不应呈现任何视图, 例如通过设置状态码.
	 * 
	 * @param request 当前的HTTP请求
	 * @param response 当前的HTTP响应
	 * @param handler 执行的处理器, 或{@code null} 如果在异常时没有选择 (例如, 如果multipart解析失败)
	 * @param ex 在处理器执行期间抛出的异常
	 * 
	 * @return 要转发到的相应{@code ModelAndView}, 或{@code null}以进行解析链中的默认处理
	 */
	ModelAndView resolveException(
			HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex);

}
