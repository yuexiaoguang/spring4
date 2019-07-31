package org.springframework.web.cors;

import javax.servlet.http.HttpServletRequest;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;

/**
 * 基于<a href="http://www.w3.org/TR/cors/">CORS W3C recommendation</a>的CORS请求处理的工具类.
 */
public abstract class CorsUtils {

	/**
	 * 如果请求是有效的CORS请求, 则返回{@code true}.
	 */
	public static boolean isCorsRequest(HttpServletRequest request) {
		return (request.getHeader(HttpHeaders.ORIGIN) != null);
	}

	/**
	 * 如果请求是有效的CORS pre-flight请求, 则返回{@code true}.
	 */
	public static boolean isPreFlightRequest(HttpServletRequest request) {
		return (isCorsRequest(request) && HttpMethod.OPTIONS.matches(request.getMethod()) &&
				request.getHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD) != null);
	}

}
