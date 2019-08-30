package org.springframework.web.portlet.handler;

import javax.portlet.PortletException;

/**
 * 当portlet内容生成器需要预先存在的会话时抛出的异常.
 */
@SuppressWarnings("serial")
public class PortletSessionRequiredException extends PortletException {

	public PortletSessionRequiredException(String msg) {
		super(msg);
	}

}
