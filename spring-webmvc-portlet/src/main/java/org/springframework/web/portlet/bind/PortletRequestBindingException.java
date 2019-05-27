package org.springframework.web.portlet.bind;

import javax.portlet.PortletException;

/**
 * Fatal binding exception, thrown when we want to
 * treat binding exceptions as unrecoverable.
 */
@SuppressWarnings("serial")
public class PortletRequestBindingException extends PortletException {

	/**
	 * Constructor for PortletRequestBindingException.
	 * @param msg the detail message
	 */
	public PortletRequestBindingException(String msg) {
		super(msg);
	}

	/**
	 * Constructor for PortletRequestBindingException.
	 * @param msg the detail message
	 * @param cause the root cause
	 */
	public PortletRequestBindingException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
