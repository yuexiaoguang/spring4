package org.springframework.web.portlet;

import javax.portlet.PortletException;
import javax.portlet.PortletRequest;

import org.springframework.core.style.StylerUtils;

/**
 * 如果DispatcherPortlet无法确定传入的portlet请求的相应处理器, 则抛出异常.
 */
@SuppressWarnings("serial")
public class NoHandlerFoundException extends PortletException {

	/**
	 * @param msg 详细信息
	 */
	public NoHandlerFoundException(String msg) {
		super(msg);
	}

	/**
	 * @param msg 详细信息
	 * @param request 当前的portlet请求, 以便将更多上下文包含在异常消息中
	 */
	public NoHandlerFoundException(String msg, PortletRequest request) {
		super(msg + ": mode '" + request.getPortletMode() +
				"', phase '" + request.getAttribute(PortletRequest.LIFECYCLE_PHASE) +
				"', parameters " + StylerUtils.style(request.getParameterMap()));
	}

}
