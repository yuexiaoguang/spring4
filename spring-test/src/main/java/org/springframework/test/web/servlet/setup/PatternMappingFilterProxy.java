package org.springframework.test.web.servlet.setup;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.springframework.util.Assert;
import org.springframework.web.util.UrlPathHelper;

/**
 * 仅当请求URL匹配使用Servlet规范中定义的模式匹配映射到的模式时, 才调用委托{@link Filter}的过滤器.
 */
final class PatternMappingFilterProxy implements Filter {

	private static final String EXTENSION_MAPPING_PATTERN = "*.";

	private static final String PATH_MAPPING_PATTERN = "/*";

	private static final UrlPathHelper urlPathHelper = new UrlPathHelper();

	private final Filter delegate;

	/** 需要完全匹配的模式, e.g. "/test" */
	private final List<String> exactMatches = new ArrayList<String>();

	/** 需要URL具有特定前缀的模式, e.g. "/test/*" */
	private final List<String> startsWithMatches = new ArrayList<String>();

	/** 需要请求URL具有特定后缀的模式, e.g. "*.html" */
	private final List<String> endsWithMatches = new ArrayList<String>();


	public PatternMappingFilterProxy(Filter delegate, String... urlPatterns) {
		Assert.notNull(delegate, "A delegate Filter is required");
		this.delegate = delegate;
		for (String urlPattern : urlPatterns) {
			addUrlPattern(urlPattern);
		}
	}

	private void addUrlPattern(String urlPattern) {
		Assert.notNull(urlPattern, "Found null URL Pattern");
		if (urlPattern.startsWith(EXTENSION_MAPPING_PATTERN)) {
			this.endsWithMatches.add(urlPattern.substring(1, urlPattern.length()));
		}
		else if (urlPattern.equals(PATH_MAPPING_PATTERN)) {
			this.startsWithMatches.add("");
		}
		else if (urlPattern.endsWith(PATH_MAPPING_PATTERN)) {
			this.startsWithMatches.add(urlPattern.substring(0, urlPattern.length() - 1));
			this.exactMatches.add(urlPattern.substring(0, urlPattern.length() - 2));
		}
		else {
			if ("".equals(urlPattern)) {
				urlPattern = "/";
			}
			this.exactMatches.add(urlPattern);
		}
	}


	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain)
			throws IOException, ServletException {

		HttpServletRequest httpRequest = (HttpServletRequest) request;
		String requestPath = urlPathHelper.getPathWithinApplication(httpRequest);

		if (matches(requestPath)) {
			this.delegate.doFilter(request, response, filterChain);
		}
		else {
			filterChain.doFilter(request, response);
		}
	}

	private boolean matches(String requestPath) {
		for (String pattern : this.exactMatches) {
			if (pattern.equals(requestPath)) {
				return true;
			}
		}
		if (!requestPath.startsWith("/")) {
			return false;
		}
		for (String pattern : this.endsWithMatches) {
			if (requestPath.endsWith(pattern)) {
				return true;
			}
		}
		for (String pattern : this.startsWithMatches) {
			if (requestPath.startsWith(pattern)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		this.delegate.init(filterConfig);
	}

	@Override
	public void destroy() {
		this.delegate.destroy();
	}

}
