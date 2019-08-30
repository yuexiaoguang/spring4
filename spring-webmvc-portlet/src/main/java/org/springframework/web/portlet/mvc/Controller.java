package org.springframework.web.portlet.mvc;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;

import org.springframework.web.portlet.ModelAndView;

/**
 * 基本portlet控制器接口, 表示接收 RenderRequest/RenderResponse和ActionRequest/ActionResponse的组件,
 * 如{@code Portlet}, 但能够参与MVC工作流程.
 *
 * <p>portlet Controller接口的任何实现都应该是<i>可重用的线程安全的</i>类,
 * 能够在应用程序的整个生命周期中处理多个portlet请求.
 * 为了能够以简单的方式配置Controller, 控制器通常是JavaBeans.</p>
 *
 * <p><b><a name="workflow">Workflow</a>:</b></p>
 *
 * <p>在DispatcherPortlet收到请求, 并完成解析区域设置, 主题等之后, 它尝试使用
 * {@link org.springframework.web.portlet.HandlerMapping HandlerMapping}解析Controller以处理该请求.
 * When a Controller has been found, the
 * {@link #handleRenderRequest handleRenderRequest} or {@link #handleActionRequest handleActionRequest}
 * 找到Controller后, 将调用handleRenderRequest方法, 该方法负责处理实际请求, 并且 - 如果适用 - 返回适当的ModelAndView.
 * 实际上, 这些方法是{@link org.springframework.web.portlet.DispatcherPortlet DispatcherPortlet}的主要入口点,
 * 它将请求委托给控制器.</p>
 *
 * <p>所以基本上任何Controller接口的<i>直接</i>实现都只处理RenderRequests/ActionRequests,
 * 并且应该返回一个ModelAndView, 以供DispatcherPortlet进一步使用.
 * 任何其他功能, 如可选验证, 表单处理等, 都应该通过扩展上面提到的一个抽象控制器类来获得.</p>
 */
public interface Controller {

	/**
	 * 处理操作请求.
	 * 
	 * @param request 当前的portlet操作请求
	 * @param response 当前的portlet操作响应
	 * 
	 * @throws Exception
	 */
	void handleActionRequest(ActionRequest request, ActionResponse response) throws Exception;

	/**
	 * 处理渲染请求, 并返回DispatcherPortlet将渲染的ModelAndView对象.
	 * {@code null}返回值不是错误: 它表示此对象已完成请求处理本身, 因此没有要渲染的ModelAndView.
	 * 
	 * @param request 当前portlet渲染请求
	 * @param response 当前portlet渲染响应
	 * 
	 * @return 要渲染的ModelAndView, 如果直接处理则为null
	 * @throws Exception
	 */
	ModelAndView handleRenderRequest(RenderRequest request, RenderResponse response) throws Exception;

}
