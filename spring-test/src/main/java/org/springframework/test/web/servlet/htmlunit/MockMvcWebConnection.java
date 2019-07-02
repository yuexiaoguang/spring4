package org.springframework.test.web.servlet.htmlunit;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.gargoylesoftware.htmlunit.CookieManager;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebConnection;
import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.WebResponse;
import com.gargoylesoftware.htmlunit.util.Cookie;

import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.util.Assert;

/**
 * {@code MockMvcWebConnection}使{@link MockMvc}能够将{@link WebRequest}转换为{@link WebResponse}.
 * <p>这是与<a href="http://htmlunit.sourceforge.net/">HtmlUnit</a>的核心集成.
 * <p>示例用法如下所示.
 *
 * <pre class="code">
 * WebClient webClient = new WebClient();
 * MockMvc mockMvc = ...
 * MockMvcWebConnection webConnection = new MockMvcWebConnection(mockMvc, webClient);
 * webClient.setWebConnection(webConnection);
 *
 * // Use webClient as normal ...
 * </pre>
 */
public final class MockMvcWebConnection implements WebConnection {

	private final Map<String, MockHttpSession> sessions = new HashMap<String, MockHttpSession>();

	private final MockMvc mockMvc;

	private final String contextPath;

	private WebClient webClient;


	/**
	 * 创建一个新实例, 假定应用程序的上下文路径为{@code ""} (i.e., 根上下文).
	 * <p>例如, URL {@code http://localhost/test/this}将使用{@code ""}作为上下文路径.
	 * 
	 * @param mockMvc 要使用的{@code MockMvc}实例; never {@code null}
	 * @param webClient 要使用的{@link WebClient}. never {@code null}
	 */
	public MockMvcWebConnection(MockMvc mockMvc, WebClient webClient) {
		this(mockMvc, webClient, "");
	}

	/**
	 * <p>路径可以是{@code null}, 在这种情况下, URL的第一个路径段将变为contextPath.
	 * 否则它必须符合{@link javax.servlet.http.HttpServletRequest#getContextPath()}
	 * 它可以是一个空字符串, 否则必须以"/"字符开头而不是以"/"字符结尾.
	 * 
	 * @param mockMvc 要使用的{@code MockMvc}实例 (never {@code null})
	 * @param webClient 要使用的{@link WebClient} (never {@code null})
	 * @param contextPath 要使用的contextPath
	 */
	public MockMvcWebConnection(MockMvc mockMvc, WebClient webClient, String contextPath) {
		Assert.notNull(mockMvc, "MockMvc must not be null");
		Assert.notNull(webClient, "WebClient must not be null");
		validateContextPath(contextPath);

		this.webClient = webClient;
		this.mockMvc = mockMvc;
		this.contextPath = contextPath;
	}

	/**
	 * 创建一个新实例, 假定应用程序的上下文路径为 {@code ""} (i.e., 根上下文).
	 * <p>例如, URL {@code http://localhost/test/this}将使用{@code ""}作为上下文路径.
	 * 
	 * @param mockMvc 要使用的{@code MockMvc}实例; never {@code null}
	 * @deprecated Use {@link #MockMvcWebConnection(MockMvc, WebClient)}
	 */
	@Deprecated
	public MockMvcWebConnection(MockMvc mockMvc) {
		this(mockMvc, "");
	}

	/**
	 * <p>路径可以是{@code null}, 在这种情况下, URL的第一个路径段将变为contextPath.
	 * 否则它必须符合{@link javax.servlet.http.HttpServletRequest#getContextPath()},
	 * 它可以是一个空字符串, 否则必须以 "/"字符开头而不是以 "/"字符结尾.
	 * 
	 * @param mockMvc 要使用的{@code MockMvc}实例; never {@code null}
	 * @param contextPath 要使用的contextPath
	 * 
	 * @deprecated use {@link #MockMvcWebConnection(MockMvc, WebClient, String)}
	 */
	@Deprecated
	public MockMvcWebConnection(MockMvc mockMvc, String contextPath) {
		this(mockMvc, new WebClient(), contextPath);
	}

	/**
	 * 验证提供的{@code contextPath}.
	 * <p>如果该值不是{@code null}, 则它必须符合{@link javax.servlet.http.HttpServletRequest#getContextPath()},
	 * 它可以是空字符串, 否则必须以"/"字符开头而不是以 "/"字符结尾.
	 * 
	 * @param contextPath 要验证的路径
	 */
	static void validateContextPath(String contextPath) {
		if (contextPath == null || "".equals(contextPath)) {
			return;
		}
		if (!contextPath.startsWith("/")) {
			throw new IllegalArgumentException("contextPath '" + contextPath + "' must start with '/'.");
		}
		if (contextPath.endsWith("/")) {
			throw new IllegalArgumentException("contextPath '" + contextPath + "' must not end with '/'.");
		}
	}


	public void setWebClient(WebClient webClient) {
		Assert.notNull(webClient, "WebClient must not be null");
		this.webClient = webClient;
	}


	public WebResponse getResponse(WebRequest webRequest) throws IOException {
		long startTime = System.currentTimeMillis();
		HtmlUnitRequestBuilder requestBuilder = new HtmlUnitRequestBuilder(this.sessions, this.webClient, webRequest);
		requestBuilder.setContextPath(this.contextPath);

		MockHttpServletResponse httpServletResponse = getResponse(requestBuilder);
		String forwardedUrl = httpServletResponse.getForwardedUrl();
		while (forwardedUrl != null) {
			requestBuilder.setForwardPostProcessor(new ForwardRequestPostProcessor(forwardedUrl));
			httpServletResponse = getResponse(requestBuilder);
			forwardedUrl = httpServletResponse.getForwardedUrl();
		}
		storeCookies(webRequest, httpServletResponse.getCookies());

		return new MockWebResponseBuilder(startTime, webRequest, httpServletResponse).build();
	}

	private MockHttpServletResponse getResponse(RequestBuilder requestBuilder) throws IOException {
		ResultActions resultActions;
		try {
			resultActions = this.mockMvc.perform(requestBuilder);
		}
		catch (Exception ex) {
			throw new IOException(ex);
		}

		return resultActions.andReturn().getResponse();
	}

	private void storeCookies(WebRequest webRequest, javax.servlet.http.Cookie[] cookies) {
		if (cookies == null) {
			return;
		}
		Date now = new Date();
		CookieManager cookieManager = this.webClient.getCookieManager();
		for (javax.servlet.http.Cookie cookie : cookies) {
			if (cookie.getDomain() == null) {
				cookie.setDomain(webRequest.getUrl().getHost());
			}
			Cookie toManage = MockWebResponseBuilder.createCookie(cookie);
			Date expires = toManage.getExpires();
			if (expires == null || expires.after(now)) {
				cookieManager.addCookie(toManage);
			}
			else {
				cookieManager.removeCookie(toManage);
			}
		}
	}

	@Override
	public void close() {
	}
}
