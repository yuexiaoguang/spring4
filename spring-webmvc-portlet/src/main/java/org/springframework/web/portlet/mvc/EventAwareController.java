package org.springframework.web.portlet.mvc;

import javax.portlet.EventRequest;
import javax.portlet.EventResponse;

/**
 * Extension of the Portlet {@link Controller} interface that allows
 * for handling Portlet 2.0 event requests as well. Can also be
 * implemented by {@link AbstractController} subclasses.
 */
public interface EventAwareController {

	/**
	 * Process the event request. There is nothing to return.
	 * @param request current portlet event request
	 * @param response current portlet event response
	 * @throws Exception in case of errors
	 */
	void handleEventRequest(EventRequest request, EventResponse response) throws Exception;

}
