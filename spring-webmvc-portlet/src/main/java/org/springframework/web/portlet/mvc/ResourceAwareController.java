package org.springframework.web.portlet.mvc;

import javax.portlet.ResourceRequest;
import javax.portlet.ResourceResponse;

import org.springframework.web.portlet.ModelAndView;

/**
 * Extension of the Portlet {@link Controller} interface that allows
 * for handling Portlet 2.0 resource requests as well. Can also be
 * implemented by {@link AbstractController} subclasses.
 */
public interface ResourceAwareController {

	/**
	 * Process the resource request and return a ModelAndView object which the DispatcherPortlet
	 * will render. A {@code null} return value is not an error: It indicates that this
	 * object completed request processing itself, thus there is no ModelAndView to render.
	 * @param request current portlet resource request
	 * @param response current portlet resource response
	 * @return a ModelAndView to render, or null if handled directly
	 * @throws Exception in case of errors
	 */
	ModelAndView handleResourceRequest(ResourceRequest request, ResourceResponse response) throws Exception;

}
