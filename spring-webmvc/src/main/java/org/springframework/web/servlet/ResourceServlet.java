package org.springframework.web.servlet;

import java.io.IOException;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.context.support.ServletContextResource;

/**
 * 公开内部资源的简单servlet, 如果找不到指定的资源, 则包含默认URL.
 * 例如, 在使用JSP时尝试和捕获异常的替代方法.
 *
 * <p>此servlet的进一步用法是能够将 last-modified 时间戳应用于准静态资源 (通常是JSP).
 * 这可以作为参数指定资源的桥接, 或作为特定目标资源的代理 (或要组合的特定目标资源的列表).
 *
 * <p>典型用法会将像"/ResourceServlet"这样的URL映射到此servlet的实例上,
 * 并使用"JSP include"操作来包含此URL, 其中"resource"参数指示WAR中的实际目标路径.
 *
 * <p>可以将{@code defaultUrl}属性设置为默认URL的内部资源路径, 以便在找不到目标资源或首先未指定目标资源时进行呈现.
 *
 * <p>"resource"参数和{@code defaultUrl}属性还可以指定要组合的目标资源列表.
 * 这些资源将逐一包括在内, 以建立响应.
 * 如果last-modified 确定处于活动状态, 则将使用这些文件中的最新时间戳.
 *
 * <p>{@code allowedResources}属性可以设置为应该通过此servlet可用的资源的URL模式.
 * 如果未设置, 则可以请求任何目标资源, 包括WEB-INF目录中的资源!
 *
 * <p>如果使用此servlet进行直接访问而不是通过 includes, 则应指定{@code contentType}属性以应用适当的内容类型.
 * 请注意, 通过RequestDispatcher include包含资源时, 将忽略目标JSP中的内容类型header.
 *
 * <p>要为目标资源应用 last-modified时间戳, 将{@code applyLastModified}属性设置为true.
 * 然后, 此servlet将返回目标资源的文件时间戳作为 last-modified的值, 如果不可检索则返回此servlet的启动时间.
 *
 * <p>请注意, 如果目标资源不生成依赖于HttpSession或cookie的内容, 则以上述方式应用last-modified时间戳才有意义;
 * 它只是允许评估请求参数.
 *
 * <p>这种last-modified的用法的一个典型案例是JSP, 它只是最少使用基本手段, 如包含或消息解析来构建准静态内容.
 * 不必在每个请求上重新生成此类内容; 只要文件没有更改, 它就可以缓存.
 *
 * <p>请注意, 如果告诉它, 这个servlet将应用last-modified时间戳:
 * 决定是否可以以这种方式缓存目标资源的内容.
 * 典型用例是不是由控制器提供的辅助资源, 例如由JSP生成的JavaScript文件 (不依赖于HttpSession).
 *
 * @deprecated 从Spring 4.3.5开始, 支持
 * {@link org.springframework.web.servlet.resource.ResourceHttpRequestHandler}
 */
@SuppressWarnings("serial")
@Deprecated
public class ResourceServlet extends HttpServletBean {

	/**
	 * Any number of these characters are considered delimiters between multiple resource paths in a single String value.
	 */
	public static final String RESOURCE_URL_DELIMITERS = ",; \t\n";

	/**
	 * Name of the parameter that must contain the actual resource path.
	 */
	public static final String RESOURCE_PARAM_NAME = "resource";


	private String defaultUrl;

	private String allowedResources;

	private String contentType;

	private boolean applyLastModified = false;

	private PathMatcher pathMatcher;

	private long startupTime;


	/**
	 * Set the URL within the current web application from which to
	 * include content if the requested path isn't found, or if none
	 * is specified in the first place.
	 * <p>If specifying multiple URLs, they will be included one by one
	 * to build the response. If last-modified determination is active,
	 * the newest timestamp among those files will be used.
	 * @see #setApplyLastModified
	 */
	public void setDefaultUrl(String defaultUrl) {
		this.defaultUrl = defaultUrl;
	}

	/**
	 * Set allowed resources as URL pattern, e.g. "/WEB-INF/res/*.jsp",
	 * The parameter can be any Ant-style pattern parsable by AntPathMatcher.
	 * @see org.springframework.util.AntPathMatcher
	 */
	public void setAllowedResources(String allowedResources) {
		this.allowedResources = allowedResources;
	}

	/**
	 * Set the content type of the target resource (typically a JSP).
	 * Default is none, which is appropriate when including resources.
	 * <p>For directly accessing resources, for example to leverage this
	 * servlet's last-modified support, specify a content type here.
	 * Note that a content type header in the target JSP will be ignored
	 * when including the resource via a RequestDispatcher include.
	 */
	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	/**
	 * Set whether to apply the file timestamp of the target resource
	 * as last-modified value. Default is "false".
	 * <p>This is mainly intended for JSP targets that don't generate
	 * session-specific or database-driven content: Such files can be
	 * cached by the browser as long as the last-modified timestamp
	 * of the JSP file doesn't change.
	 * <p>This will only work correctly with expanded WAR files that
	 * allow access to the file timestamps. Else, the startup time
	 * of this servlet is returned.
	 */
	public void setApplyLastModified(boolean applyLastModified) {
		this.applyLastModified = applyLastModified;
	}


	/**
	 * Remember the startup time, using no last-modified time before it.
	 */
	@Override
	protected void initServletBean() {
		this.pathMatcher = getPathMatcher();
		this.startupTime = System.currentTimeMillis();
	}

	/**
	 * Return a {@link PathMatcher} to use for matching the "allowedResources" URL pattern.
	 * <p>The default is {@link AntPathMatcher}.
	 * @see #setAllowedResources
	 * @see org.springframework.util.AntPathMatcher
	 */
	protected PathMatcher getPathMatcher() {
		return new AntPathMatcher();
	}


	/**
	 * Determine the URL of the target resource and include it.
	 * @see #determineResourceUrl
	 */
	@Override
	protected final void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		// Determine URL of resource to include...
		String resourceUrl = determineResourceUrl(request);

		if (resourceUrl != null) {
			try {
				doInclude(request, response, resourceUrl);
			}
			catch (ServletException ex) {
				if (logger.isWarnEnabled()) {
					logger.warn("Failed to include content of resource [" + resourceUrl + "]", ex);
				}
				// Try including default URL if appropriate.
				if (!includeDefaultUrl(request, response)) {
					throw ex;
				}
			}
			catch (IOException ex) {
				if (logger.isWarnEnabled()) {
					logger.warn("Failed to include content of resource [" + resourceUrl + "]", ex);
				}
				// Try including default URL if appropriate.
				if (!includeDefaultUrl(request, response)) {
					throw ex;
				}
			}
		}

		// No resource URL specified -> try to include default URL.
		else if (!includeDefaultUrl(request, response)) {
			throw new ServletException("No target resource URL found for request");
		}
	}

	/**
	 * Determine the URL of the target resource of this request.
	 * <p>Default implementation returns the value of the "resource" parameter.
	 * Can be overridden in subclasses.
	 * @param request current HTTP request
	 * @return the URL of the target resource, or {@code null} if none found
	 * @see #RESOURCE_PARAM_NAME
	 */
	protected String determineResourceUrl(HttpServletRequest request) {
		return request.getParameter(RESOURCE_PARAM_NAME);
	}

	/**
	 * Include the specified default URL, if appropriate.
	 * @param request current HTTP request
	 * @param response current HTTP response
	 * @return whether a default URL was included
	 * @throws ServletException if thrown by the RequestDispatcher
	 * @throws IOException if thrown by the RequestDispatcher
	 */
	private boolean includeDefaultUrl(HttpServletRequest request, HttpServletResponse response)
		throws ServletException, IOException {

		if (this.defaultUrl == null) {
			return false;
		}
		doInclude(request, response, this.defaultUrl);
		return true;
	}

	/**
	 * Include the specified resource via the RequestDispatcher.
	 * @param request current HTTP request
	 * @param response current HTTP response
	 * @param resourceUrl the URL of the target resource
	 * @throws ServletException if thrown by the RequestDispatcher
	 * @throws IOException if thrown by the RequestDispatcher
	 */
	private void doInclude(HttpServletRequest request, HttpServletResponse response, String resourceUrl)
			throws ServletException, IOException {

		if (this.contentType != null) {
			response.setContentType(this.contentType);
		}
		String[] resourceUrls = StringUtils.tokenizeToStringArray(resourceUrl, RESOURCE_URL_DELIMITERS);
		for (String url : resourceUrls) {
			String path = StringUtils.cleanPath(url);
			// Check whether URL matches allowed resources
			if (this.allowedResources != null && !this.pathMatcher.match(this.allowedResources, path)) {
				throw new ServletException("Resource [" + path +
						"] does not match allowed pattern [" + this.allowedResources + "]");
			}
			if (logger.isDebugEnabled()) {
				logger.debug("Including resource [" + path + "]");
			}
			RequestDispatcher rd = request.getRequestDispatcher(path);
			rd.include(request, response);
		}
	}

	/**
	 * Return the last-modified timestamp of the file that corresponds
	 * to the target resource URL (i.e. typically the request ".jsp" file).
	 * Will simply return -1 if "applyLastModified" is false (the default).
	 * <p>Returns no last-modified date before the startup time of this servlet,
	 * to allow for message resolution etc that influences JSP contents,
	 * assuming that those background resources might have changed on restart.
	 * <p>Returns the startup time of this servlet if the file that corresponds
	 * to the target resource URL couldn't be resolved (for example, because
	 * the WAR is not expanded).
	 * @see #determineResourceUrl
	 * @see #getFileTimestamp
	 */
	@Override
	protected final long getLastModified(HttpServletRequest request) {
		if (this.applyLastModified) {
			String resourceUrl = determineResourceUrl(request);
			if (resourceUrl == null) {
				resourceUrl = this.defaultUrl;
			}
			if (resourceUrl != null) {
				String[] resourceUrls = StringUtils.tokenizeToStringArray(resourceUrl, RESOURCE_URL_DELIMITERS);
				long latestTimestamp = -1;
				for (String url : resourceUrls) {
					long timestamp = getFileTimestamp(url);
					if (timestamp > latestTimestamp) {
						latestTimestamp = timestamp;
					}
				}
				return (latestTimestamp > this.startupTime ? latestTimestamp : this.startupTime);
			}
		}
		return -1;
	}

	/**
	 * Return the file timestamp for the given resource.
	 * @param resourceUrl the URL of the resource
	 * @return the file timestamp in milliseconds, or -1 if not determinable
	 */
	protected long getFileTimestamp(String resourceUrl) {
		ServletContextResource resource = new ServletContextResource(getServletContext(), resourceUrl);
		try {
			long lastModifiedTime = resource.lastModified();
			if (logger.isDebugEnabled()) {
				logger.debug("Last-modified timestamp of " + resource + " is " + lastModifiedTime);
			}
			return lastModifiedTime;
		}
		catch (IOException ex) {
			if (logger.isWarnEnabled()) {
				logger.warn("Couldn't retrieve last-modified timestamp of " + resource +
						" - using ResourceServlet startup time");
			}
			return -1;
		}
	}

}
