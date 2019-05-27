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
 * Adapter to use the Portlet interface with the generic DispatcherPortlet.
 * Calls the Portlet's {@code render} and {@code processAction}
 * methods to handle a request.
 *
 * <p>This adapter is not activated by default; it needs to be defined as a
 * bean in the DispatcherPortlet context. It will automatically apply to
 * mapped handler beans that implement the Portlet interface then.
 *
 * <p>Note that Portlet instances defined as bean will not receive initialization
 * and destruction callbacks, unless a special post-processor such as
 * SimplePortletPostProcessor is defined in the DispatcherPortlet context.
 *
 * <p><b>Alternatively, consider wrapping a Portlet with Spring's
 * PortletWrappingController.</b> This is particularly appropriate for
 * existing Portlet classes, allowing to specify Portlet initialization
 * parameters, etc.
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
			// roughly equivalent to Portlet 2.0 GenericPortlet
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
			// if no event processing method was found just keep render params
			response.setRenderParameters(request);
		}
	}

}
