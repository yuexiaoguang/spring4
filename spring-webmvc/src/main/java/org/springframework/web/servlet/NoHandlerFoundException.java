package org.springframework.web.servlet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.HttpHeaders;

/**
 * 默认情况下, 当DispatcherServlet无法找到请求的处理器时, 它会发送404响应.
 * 但是, 如果其属性"throwExceptionIfNoHandlerFound"设置为{@code true},
 * 则会引发此异常, 并且可以使用已配置的HandlerExceptionResolver进行处理.
 */
@SuppressWarnings("serial")
public class NoHandlerFoundException extends ServletException {

	private final String httpMethod;

	private final String requestURL;

	private final HttpHeaders headers;


	/**
	 * @param httpMethod HTTP方法
	 * @param requestURL HTTP请求URL
	 * @param headers HTTP请求header
	 */
	public NoHandlerFoundException(String httpMethod, String requestURL, HttpHeaders headers) {
		super("No handler found for " + httpMethod + " " + requestURL);
		this.httpMethod = httpMethod;
		this.requestURL = requestURL;
		this.headers = headers;
	}


	public String getHttpMethod() {
		return this.httpMethod;
	}

	public String getRequestURL() {
		return this.requestURL;
	}

	public HttpHeaders getHeaders() {
		return this.headers;
	}

}
