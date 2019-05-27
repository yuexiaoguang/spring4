package org.springframework.web.portlet;

import javax.portlet.PortletException;
import javax.portlet.PortletRequest;

import org.springframework.core.style.StylerUtils;

/**
 * Exception to be thrown if DispatcherPortlet is unable to determine
 * a corresponding handler for an incoming portlet request.
 */
@SuppressWarnings("serial")
public class NoHandlerFoundException extends PortletException {

	/**
	 * Constructor for NoHandlerFoundException.
	 * @param msg the detail message
	 */
	public NoHandlerFoundException(String msg) {
		super(msg);
	}

	/**
	 * Constructor for NoHandlerFoundException.
	 * @param msg the detail message
	 * @param request the current portlet request,
	 * for further context to be included in the exception message
	 */
	public NoHandlerFoundException(String msg, PortletRequest request) {
		super(msg + ": mode '" + request.getPortletMode() +
				"', phase '" + request.getAttribute(PortletRequest.LIFECYCLE_PHASE) +
				"', parameters " + StylerUtils.style(request.getParameterMap()));
	}

}
