package org.springframework.web.bind;

import org.springframework.web.util.NestedServletException;

/**
 * 当想要将绑定异常视为不可恢复的绑定异常时, 抛出的致命的绑定异常.
 *
 * <p>扩展ServletException, 以方便在任何Servlet资源中抛出 (例如Filter),
 * 并使用NestedServletException进行正确的根本原因处理 (因为普通的ServletException根本不暴露其根本原因).
 */
@SuppressWarnings("serial")
public class ServletRequestBindingException extends NestedServletException {

	public ServletRequestBindingException(String msg) {
		super(msg);
	}

	public ServletRequestBindingException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
