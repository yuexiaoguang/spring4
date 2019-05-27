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
 * Adapter to use the Controller workflow interface with the generic DispatcherPortlet.
 *
 * <p>This is an SPI class, not used directly by application code.
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
			// equivalent to Portlet 2.0 GenericPortlet
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
			// if no event processing method was found just keep render params
			response.setRenderParameters(request);
		}
	}

}
