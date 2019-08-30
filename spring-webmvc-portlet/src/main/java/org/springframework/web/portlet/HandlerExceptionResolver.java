package org.springframework.web.portlet;

import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
import javax.portlet.ResourceRequest;
import javax.portlet.ResourceResponse;

/**
 * 由可以解析在处理器映射或执行期间抛出的异常的对象实现的接口, 在典型情况下是错误视图.
 * 实现者通常在应用程序上下文中注册为bean.
 *
 * <p>错误视图类似于错误页面JSP, 但可以与任何类型的异常一起使用, 包括任何受检异常, 以及针对特定处理器的潜在细粒度映射.
 */
public interface HandlerExceptionResolver {

	/**
	 * 尝试解析在处理器执行期间抛出的给定异常, 返回表示特定错误页面的ModelAndView.
	 * 
	 * @param request 当前的portlet请求
	 * @param response 当前的portlet响应
	 * @param handler 执行的处理器, 或null (例如, 如果multipart解析失败)
	 * @param ex 在处理器执行期间抛出的异常
	 * 
	 * @return 要转发到的相应的ModelAndView, 或{@code null}进行默认处理
	 */
	ModelAndView resolveException(
			RenderRequest request, RenderResponse response, Object handler, Exception ex);

	/**
	 * 尝试解析在处理器执行期间抛出的给定异常, 返回表示特定错误页面的ModelAndView.
	 * 
	 * @param request 当前的portlet请求
	 * @param response 当前的portlet响应
	 * @param handler 执行的处理器, 或null (例如, 如果multipart解析失败)
	 * @param ex 在处理器执行期间抛出的异常
	 * 
	 * @return 要转发到的相应的ModelAndView, 或{@code null}进行默认处理
	 */
	ModelAndView resolveException(
			ResourceRequest request, ResourceResponse response, Object handler, Exception ex);

}
