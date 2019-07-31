package org.springframework.web;

import javax.servlet.ServletException;

/**
 * 当HTTP请求处理器需要预先存在的会话时抛出的异常.
 */
@SuppressWarnings("serial")
public class HttpSessionRequiredException extends ServletException {

	private String expectedAttribute;


	public HttpSessionRequiredException(String msg) {
		super(msg);
	}

	/**
	 * @param msg 详细信息
	 * @param expectedAttribute 预期会话属性的名称
	 */
	public HttpSessionRequiredException(String msg, String expectedAttribute) {
		super(msg);
		this.expectedAttribute = expectedAttribute;
	}


	/**
	 * 返回预期的会话属性的名称.
	 */
	public String getExpectedAttribute() {
		return this.expectedAttribute;
	}

}
