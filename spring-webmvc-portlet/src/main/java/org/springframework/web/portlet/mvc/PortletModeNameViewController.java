package org.springframework.web.portlet.mvc;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.PortletException;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;

import org.springframework.web.portlet.ModelAndView;

/**
 * <p>Trivial controller that transforms the PortletMode to a view name.
 * The advantage here is that the client is not exposed to
 * the concrete view technology but rather just to the controller URL;
 * the concrete view will be determined by the ViewResolver.</p>
 *
 * <p>Example: PortletMode.VIEW -> "view"</p>
 *
 * <p>This controller does not handle action requests.</p>
 */
public class PortletModeNameViewController implements Controller {

	@Override
	public void handleActionRequest(ActionRequest request, ActionResponse response) throws Exception {
		throw new PortletException("PortletModeNameViewController does not handle action requests");
	}

	@Override
	public ModelAndView handleRenderRequest(RenderRequest request, RenderResponse response) {
		return new ModelAndView(request.getPortletMode().toString());
	}

}
