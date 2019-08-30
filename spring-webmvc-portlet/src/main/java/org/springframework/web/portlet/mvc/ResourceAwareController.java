package org.springframework.web.portlet.mvc;

import javax.portlet.ResourceRequest;
import javax.portlet.ResourceResponse;

import org.springframework.web.portlet.ModelAndView;

/**
 * Portlet {@link Controller}接口的扩展, 允许处理Portlet 2.0资源请求.
 * 也可以由{@link AbstractController}子类实现.
 */
public interface ResourceAwareController {

	/**
	 * 处理资源请求, 并返回DispatcherPortlet将渲染的ModelAndView对象.
	 * {@code null}返回值不是错误: 它表示此对象已完成请求处理本身, 因此没有要渲染的ModelAndView.
	 * 
	 * @param request 当前portlet资源请求
	 * @param response 当前的portlet资源响应
	 * 
	 * @return 要渲染的ModelAndView, 如果直接处理则为null
	 * @throws Exception
	 */
	ModelAndView handleResourceRequest(ResourceRequest request, ResourceResponse response) throws Exception;

}
