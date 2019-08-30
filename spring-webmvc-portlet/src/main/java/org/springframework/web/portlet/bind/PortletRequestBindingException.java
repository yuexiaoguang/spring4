package org.springframework.web.portlet.bind;

import javax.portlet.PortletException;

/**
 * 当想要将绑定异常视为不可恢复时, 抛出的致命绑定异常.
 */
@SuppressWarnings("serial")
public class PortletRequestBindingException extends PortletException {

	public PortletRequestBindingException(String msg) {
		super(msg);
	}

	public PortletRequestBindingException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
