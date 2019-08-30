package org.springframework.web.portlet.mvc;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.EventRequest;
import javax.portlet.EventResponse;
import javax.portlet.PortletContext;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
import javax.portlet.ResourceRequest;
import javax.portlet.ResourceResponse;

import org.springframework.web.portlet.HandlerAdapter;
import org.springframework.web.portlet.ModelAndView;
import org.springframework.web.portlet.context.PortletContextAware;
import org.springframework.web.portlet.util.PortletUtils;

/**
 * 将Controller工作流接口与通用DispatcherPortlet一起使用的适配器.
 *
 * <p>这是一个SPI类, 不由应用程序代码直接使用.
  */
public class SimpleControllerHandlerAdapter implements HandlerAdapter, PortletContextAware {

	private PortletContext portletContext;


	@Override
	public void setPortletContext(PortletContext portletContext) {
		this.portletContext = portletContext;
	}


	@Override
	public boolean supports(Object handler) {
		return (handler instanceof Controller);
	}

	@Override
	public void handleAction(ActionRequest request, ActionResponse response, Object handler)
			throws Exception {

		((Controller) handler).handleActionRequest(request, response);
	}

	@Override
	public ModelAndView handleRender(RenderRequest request, RenderResponse response, Object handler)
			throws Exception {

		return ((Controller) handler).handleRenderRequest(request, response);
	}

	@Override
	public ModelAndView handleResource(ResourceRequest request, ResourceResponse response, Object handler)
			throws Exception {

		if (handler instanceof ResourceAwareController) {
			return ((ResourceAwareController) handler).handleResourceRequest(request, response);
		}
		else {
			// 等效于Portlet 2.0 GenericPortlet
			PortletUtils.serveResource(request, response, this.portletContext);
			return null;
		}
	}

	@Override
	public void handleEvent(EventRequest request, EventResponse response, Object handler) throws Exception {
		if (handler instanceof EventAwareController) {
			((EventAwareController) handler).handleEventRequest(request, response);
		}
		else {
			// 如果没有找到事件处理方法, 则保持渲染参数
			response.setRenderParameters(request);
		}
	}

}
