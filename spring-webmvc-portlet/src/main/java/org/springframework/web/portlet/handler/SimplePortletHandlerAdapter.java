package org.springframework.web.portlet.handler;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.EventPortlet;
import javax.portlet.EventRequest;
import javax.portlet.EventResponse;
import javax.portlet.Portlet;
import javax.portlet.PortletContext;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
import javax.portlet.ResourceRequest;
import javax.portlet.ResourceResponse;
import javax.portlet.ResourceServingPortlet;

import org.springframework.web.portlet.HandlerAdapter;
import org.springframework.web.portlet.ModelAndView;
import org.springframework.web.portlet.context.PortletContextAware;
import org.springframework.web.portlet.util.PortletUtils;

/**
 * 将Portlet接口与通用DispatcherPortlet一起使用的适配器.
 * 调用Portlet的{@code render}和{@code processAction}方法来处理请求.
 *
 * <p>默认不会激活此适配器; 它需要在DispatcherPortlet上下文中定义为bean.
 * 它将自动应用于实现Portlet接口的映射的处理器bean.
 *
 * <p>请注意, 定义为bean的Portlet实例不会接收初始化和销毁​​回调,
 * 除非在DispatcherPortlet上下文中定义了一个特殊的后处理器, 如SimplePortletPostProcessor.
 *
 * <p><b>或者, 考虑使用Spring的PortletWrappingController包装Portlet.</b>
 * 这特别适用于现有的Portlet类, 允许指定Portlet初始化参数等.
 */
public class SimplePortletHandlerAdapter implements HandlerAdapter, PortletContextAware {

	private PortletContext portletContext;


	@Override
	public void setPortletContext(PortletContext portletContext) {
		this.portletContext = portletContext;
	}


	@Override
	public boolean supports(Object handler) {
		return (handler instanceof Portlet);
	}

	@Override
	public void handleAction(ActionRequest request, ActionResponse response, Object handler)
			throws Exception {

		((Portlet) handler).processAction(request, response);
	}

	@Override
	public ModelAndView handleRender(RenderRequest request, RenderResponse response, Object handler)
			throws Exception {

		((Portlet) handler).render(request, response);
		return null;
	}

	@Override
	public ModelAndView handleResource(ResourceRequest request, ResourceResponse response, Object handler)
			throws Exception {

		if (handler instanceof ResourceServingPortlet) {
			((ResourceServingPortlet) handler).serveResource(request, response);
		}
		else {
			// 大致相当于Portlet 2.0 GenericPortlet
			PortletUtils.serveResource(request, response, this.portletContext);
		}
		return null;
	}

	@Override
	public void handleEvent(EventRequest request, EventResponse response, Object handler) throws Exception {
		if (handler instanceof EventPortlet) {
			((EventPortlet) handler).processEvent(request, response);
		}
		else {
			// 如果没有找到事件处理方法, 则保持渲染参数
			response.setRenderParameters(request);
		}
	}
}
