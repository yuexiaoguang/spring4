package org.springframework.web.portlet.handler;

import javax.portlet.PortletException;

/**
 * Exception thrown when a portlet content generator requires a pre-existing session.
 */
@SuppressWarnings("serial")
public class PortletSessionRequiredException extends PortletException {

	/**
	 * Create a new PortletSessionRequiredException.
	 * @param msg the detail message
	 */
	public PortletSessionRequiredException(String msg) {
		super(msg);
	}

}
