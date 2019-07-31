package org.springframework.web.cors;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.util.CollectionUtils;
import org.springframework.web.util.WebUtils;

/**
 * {@link CorsProcessor}的默认实现, 根据<a href="http://www.w3.org/TR/cors/">CORS W3C recommendation</a>定义.
 *
 * <p>请注意, 当输入{@link CorsConfiguration}为{@code null}时, 此实现不会完全拒绝简单或实际请求,
 * 而只是避免将CORS header添加到响应中.
 * 如果响应已经包含CORS header, 或者如果检测到请求是同源请求, 则也会跳过CORS处理.
 */
public class DefaultCorsProcessor implements CorsProcessor {

	private static final Charset UTF8_CHARSET = Charset.forName("UTF-8");

	private static final Log logger = LogFactory.getLog(DefaultCorsProcessor.class);


	@Override
	@SuppressWarnings("resource")
	public boolean processRequest(CorsConfiguration config, HttpServletRequest request, HttpServletResponse response)
			throws IOException {

		if (!CorsUtils.isCorsRequest(request)) {
			return true;
		}

		ServletServerHttpResponse serverResponse = new ServletServerHttpResponse(response);
		if (responseHasCors(serverResponse)) {
			logger.debug("Skip CORS processing: response already contains \"Access-Control-Allow-Origin\" header");
			return true;
		}

		ServletServerHttpRequest serverRequest = new ServletServerHttpRequest(request);
		if (WebUtils.isSameOrigin(serverRequest)) {
			logger.debug("Skip CORS processing: request is from same origin");
			return true;
		}

		boolean preFlightRequest = CorsUtils.isPreFlightRequest(request);
		if (config == null) {
			if (preFlightRequest) {
				rejectRequest(serverResponse);
				return false;
			}
			else {
				return true;
			}
		}

		return handleInternal(serverRequest, serverResponse, config, preFlightRequest);
	}

	private boolean responseHasCors(ServerHttpResponse response) {
		try {
			return (response.getHeaders().getAccessControlAllowOrigin() != null);
		}
		catch (NullPointerException npe) {
			// SPR-11919 and https://issues.jboss.org/browse/WFLY-3474
			return false;
		}
	}

	/**
	 * 其中一个CORS检查失败时调用.
	 * 默认实现将响应状态设置为403, 并将"Invalid CORS request"写入响应.
	 */
	protected void rejectRequest(ServerHttpResponse response) throws IOException {
		response.setStatusCode(HttpStatus.FORBIDDEN);
		response.getBody().write("Invalid CORS request".getBytes(UTF8_CHARSET));
	}

	/**
	 * 处理给定的请求.
	 */
	protected boolean handleInternal(ServerHttpRequest request, ServerHttpResponse response,
			CorsConfiguration config, boolean preFlightRequest) throws IOException {

		String requestOrigin = request.getHeaders().getOrigin();
		String allowOrigin = checkOrigin(config, requestOrigin);

		HttpMethod requestMethod = getMethodToUse(request, preFlightRequest);
		List<HttpMethod> allowMethods = checkMethods(config, requestMethod);

		List<String> requestHeaders = getHeadersToUse(request, preFlightRequest);
		List<String> allowHeaders = checkHeaders(config, requestHeaders);

		if (allowOrigin == null || allowMethods == null || (preFlightRequest && allowHeaders == null)) {
			rejectRequest(response);
			return false;
		}

		HttpHeaders responseHeaders = response.getHeaders();
		responseHeaders.setAccessControlAllowOrigin(allowOrigin);
		responseHeaders.add(HttpHeaders.VARY, HttpHeaders.ORIGIN);

		if (preFlightRequest) {
			responseHeaders.setAccessControlAllowMethods(allowMethods);
		}

		if (preFlightRequest && !allowHeaders.isEmpty()) {
			responseHeaders.setAccessControlAllowHeaders(allowHeaders);
		}

		if (!CollectionUtils.isEmpty(config.getExposedHeaders())) {
			responseHeaders.setAccessControlExposeHeaders(config.getExposedHeaders());
		}

		if (Boolean.TRUE.equals(config.getAllowCredentials())) {
			responseHeaders.setAccessControlAllowCredentials(true);
		}

		if (preFlightRequest && config.getMaxAge() != null) {
			responseHeaders.setAccessControlMaxAge(config.getMaxAge());
		}

		response.flush();
		return true;
	}

	/**
	 * 检查来源并确定响应的来源.
	 * 默认实现委托给
	 * {@link org.springframework.web.cors.CorsConfiguration#checkOrigin(String)}.
	 */
	protected String checkOrigin(CorsConfiguration config, String requestOrigin) {
		return config.checkOrigin(requestOrigin);
	}

	/**
	 * 检查HTTP方法并确定pre-flight请求的响应的方法.
	 * 默认实现委托给
	 * {@link org.springframework.web.cors.CorsConfiguration#checkOrigin(String)}.
	 */
	protected List<HttpMethod> checkMethods(CorsConfiguration config, HttpMethod requestMethod) {
		return config.checkHttpMethod(requestMethod);
	}

	private HttpMethod getMethodToUse(ServerHttpRequest request, boolean isPreFlight) {
		return (isPreFlight ? request.getHeaders().getAccessControlRequestMethod() : request.getMethod());
	}

	/**
	 * 检查header并确定pre-flight请求的响应的header.
	 * 默认实现委托给
	 * {@link org.springframework.web.cors.CorsConfiguration#checkOrigin(String)}.
	 */
	protected List<String> checkHeaders(CorsConfiguration config, List<String> requestHeaders) {
		return config.checkHeaders(requestHeaders);
	}

	private List<String> getHeadersToUse(ServerHttpRequest request, boolean isPreFlight) {
		HttpHeaders headers = request.getHeaders();
		return (isPreFlight ? headers.getAccessControlRequestHeaders() : new ArrayList<String>(headers.keySet()));
	}

}
