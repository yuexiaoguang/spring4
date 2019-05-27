package org.springframework.web.portlet.handler;

import javax.portlet.PortletException;

import org.springframework.util.StringUtils;

/**
 * Exception thrown when a request handler does not support a
 * specific request method.
 */
@SuppressWarnings("serial")
public class PortletRequestMethodNotSupportedException extends PortletException {

	private String method;

	private String[] supportedMethods;


	/**
	 * Create a new PortletRequestMethodNotSupportedException.
	 * @param method the unsupported HTTP request method
	 */
	public PortletRequestMethodNotSupportedException(String method) {
		this(method, null);
	}

	/**
	 * Create a new PortletRequestMethodNotSupportedException.
	 * @param method the unsupported HTTP request method
	 * @param supportedMethods the actually supported HTTP methods
	 */
	public PortletRequestMethodNotSupportedException(String method, String[] supportedMethods) {
		super("Request method '" + method + "' not supported by mapped handler");
		this.method = method;
		this.supportedMethods = supportedMethods;
	}

	/**
	 * Create a new PortletRequestMethodNotSupportedException.
	 * @param supportedMethods the actually supported HTTP methods
	 */
	public PortletRequestMethodNotSupportedException(String[] supportedMethods) {
		super("Mapped handler only supports client data requests with methods " +
				StringUtils.arrayToCommaDelimitedString(supportedMethods));
		this.supportedMethods = supportedMethods;
	}


	/**
	 * Return the HTTP request method that caused the failure.
	 */
	public String getMethod() {
		return this.method;
	}

	/**
	 * Return the actually supported HTTP methods, if known.
	 */
	public String[] getSupportedMethods() {
		return this.supportedMethods;
	}

}
