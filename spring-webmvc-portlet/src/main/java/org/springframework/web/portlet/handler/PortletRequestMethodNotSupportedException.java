package org.springframework.web.portlet.handler;

import javax.portlet.PortletException;

import org.springframework.util.StringUtils;

/**
 * 当请求处理器不支持特定请求的方法时抛出的异常.
 */
@SuppressWarnings("serial")
public class PortletRequestMethodNotSupportedException extends PortletException {

	private String method;

	private String[] supportedMethods;


	/**
	 * @param method 不受支持的HTTP请求方法
	 */
	public PortletRequestMethodNotSupportedException(String method) {
		this(method, null);
	}

	/**
	 * @param method 不受支持的HTTP请求方法
	 * @param supportedMethods 实际支持的HTTP方法
	 */
	public PortletRequestMethodNotSupportedException(String method, String[] supportedMethods) {
		super("Request method '" + method + "' not supported by mapped handler");
		this.method = method;
		this.supportedMethods = supportedMethods;
	}

	/**
	 * @param supportedMethods 实际支持的HTTP方法
	 */
	public PortletRequestMethodNotSupportedException(String[] supportedMethods) {
		super("Mapped handler only supports client data requests with methods " +
				StringUtils.arrayToCommaDelimitedString(supportedMethods));
		this.supportedMethods = supportedMethods;
	}


	/**
	 * 返回导致失败的HTTP请求方法.
	 */
	public String getMethod() {
		return this.method;
	}

	/**
	 * 返回实际支持的HTTP方法.
	 */
	public String[] getSupportedMethods() {
		return this.supportedMethods;
	}

}
