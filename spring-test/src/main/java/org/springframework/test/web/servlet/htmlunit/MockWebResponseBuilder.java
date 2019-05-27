package org.springframework.test.web.servlet.htmlunit;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import javax.servlet.http.Cookie;

import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.WebResponse;
import com.gargoylesoftware.htmlunit.WebResponseData;
import com.gargoylesoftware.htmlunit.util.NameValuePair;

import org.apache.http.impl.cookie.BasicClientCookie;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

final class MockWebResponseBuilder {

	private static final String DEFAULT_STATUS_MESSAGE = "N/A";


	private final long startTime;

	private final WebRequest webRequest;

	private final MockHttpServletResponse response;


	public MockWebResponseBuilder(long startTime, WebRequest webRequest, MockHttpServletResponse response) {
		Assert.notNull(webRequest, "WebRequest must not be null");
		Assert.notNull(response, "HttpServletResponse must not be null");
		this.startTime = startTime;
		this.webRequest = webRequest;
		this.response = response;
	}


	public WebResponse build() throws IOException {
		WebResponseData webResponseData = webResponseData();
		long endTime = System.currentTimeMillis();
		return new WebResponse(webResponseData, webRequest, endTime - startTime);
	}

	private WebResponseData webResponseData() throws IOException {
		List<NameValuePair> responseHeaders = responseHeaders();
		int statusCode = (this.response.getRedirectedUrl() != null ?
				HttpStatus.MOVED_PERMANENTLY.value() : this.response.getStatus());
		String statusMessage = statusMessage(statusCode);
		return new WebResponseData(this.response.getContentAsByteArray(), statusCode, statusMessage, responseHeaders);
	}

	private String statusMessage(int statusCode) {
		String errorMessage = this.response.getErrorMessage();
		if (StringUtils.hasText(errorMessage)) {
			return errorMessage;
		}

		try {
			return HttpStatus.valueOf(statusCode).getReasonPhrase();
		}
		catch (IllegalArgumentException ex) {
			// ignore
		}

		return DEFAULT_STATUS_MESSAGE;
	}

	private List<NameValuePair> responseHeaders() {
		Collection<String> headerNames = this.response.getHeaderNames();
		List<NameValuePair> responseHeaders = new ArrayList<NameValuePair>(headerNames.size());
		for (String headerName : headerNames) {
			List<Object> headerValues = this.response.getHeaderValues(headerName);
			for (Object value : headerValues) {
				responseHeaders.add(new NameValuePair(headerName, String.valueOf(value)));
			}
		}
		String location = this.response.getRedirectedUrl();
		if (location != null) {
			responseHeaders.add(new NameValuePair("Location", location));
		}
		for (Cookie cookie : this.response.getCookies()) {
			responseHeaders.add(new NameValuePair("Set-Cookie", valueOfCookie(cookie)));
		}
		return responseHeaders;
	}

	private String valueOfCookie(Cookie cookie) {
		return createCookie(cookie).toString();
	}

	static com.gargoylesoftware.htmlunit.util.Cookie createCookie(Cookie cookie) {
		Date expires = null;
		if (cookie.getMaxAge() > -1) {
			expires = new Date(System.currentTimeMillis() + cookie.getMaxAge() * 1000);
		}
		BasicClientCookie result = new BasicClientCookie(cookie.getName(), cookie.getValue());
		result.setDomain(cookie.getDomain());
		result.setComment(cookie.getComment());
		result.setExpiryDate(expires);
		result.setPath(cookie.getPath());
		result.setSecure(cookie.getSecure());
		if (cookie.isHttpOnly()) {
			result.setAttribute("httponly", "true");
		}
		return new com.gargoylesoftware.htmlunit.util.Cookie(result);
	}
}