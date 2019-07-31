package org.springframework.web.util;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

/**
 * URL路径匹配的工具类. 在RequestDispatcher中提供对URL路径的支持, 包括并支持一致的URL解码.
 *
 * <p>由{@link org.springframework.web.servlet.handler.AbstractUrlHandlerMapping},
 * {@link org.springframework.web.servlet.mvc.multiaction.AbstractUrlMethodNameResolver}
 * 和{@link org.springframework.web.servlet.support.RequestContext}用于路径匹配和/或URI确定.
 */
public class UrlPathHelper {

	/**
	 * 特殊的WebSphere请求属性, 指示原始请求URI.
	 * 优先于WebSphere上的标准Servlet 2.4 forward属性, 仅仅因为需要请求转发链中的第一个URI.
	 */
	private static final String WEBSPHERE_URI_ATTRIBUTE = "com.ibm.websphere.servlet.uri_non_decoded";

	private static final Log logger = LogFactory.getLog(UrlPathHelper.class);

	static volatile Boolean websphereComplianceFlag;


	private boolean alwaysUseFullPath = false;

	private boolean urlDecode = true;

	private boolean removeSemicolonContent = true;

	private String defaultEncoding = WebUtils.DEFAULT_CHARACTER_ENCODING;


	/**
	 * 设置URL查找是否应始终使用当前servlet上下文中的完整路径.
	 * 否则, 如果适用, 则使用当前servlet映射中的路径 (i.e. 在web.xml中的".../*" servlet映射的情况下).
	 * 默认"false".
	 */
	public void setAlwaysUseFullPath(boolean alwaysUseFullPath) {
		this.alwaysUseFullPath = alwaysUseFullPath;
	}

	/**
	 * 设置是否应对上下文路径和请求URI进行URL解码.
	 * 与servlet路径相比, Servlet API都返回<i>未解码</i>.
	 * <p>根据Servlet规范 (ISO-8859-1)使用请求编码或默认编码.
	 * <p>从Spring 2.5开始, 默认值为"true".
	 */
	public void setUrlDecode(boolean urlDecode) {
		this.urlDecode = urlDecode;
	}

	/**
	 * 在确定查找路径时是否解码请求URI.
	 */
	public boolean isUrlDecode() {
		return this.urlDecode;
	}

	/**
	 * 设置是否";" (分号) 内容应从请求URI中删除.
	 * <p>默认 "true".
	 */
	public void setRemoveSemicolonContent(boolean removeSemicolonContent) {
		this.removeSemicolonContent = removeSemicolonContent;
	}

	/**
	 * 是否从请求URI中删除 ";" (分号)内容.
	 */
	public boolean shouldRemoveSemicolonContent() {
		return this.removeSemicolonContent;
	}

	/**
	 * 设置用于URL解码的默认字符编码.
	 * 根据Servlet规范, 默认为ISO-8859-1.
	 * <p>如果请求指定了自身的字符编码, 请求编码将覆盖此设置.
	 * 这也允许在调用{@code ServletRequest.setCharacterEncoding}方法的过滤器中一般覆盖字符编码.
	 * 
	 * @param defaultEncoding 要使用的字符编码
	 */
	public void setDefaultEncoding(String defaultEncoding) {
		this.defaultEncoding = defaultEncoding;
	}

	/**
	 * 返回用于URL解码的默认字符编码.
	 */
	protected String getDefaultEncoding() {
		return this.defaultEncoding;
	}


	/**
	 * 返回给定请求的映射查找路径, 如果适用, 则在当前servlet映射中, 否则在Web应用程序中.
	 * <p>如果在RequestDispatcher include中调用, 则检测包括请求URL.
	 * 
	 * @param request 当前的HTTP请求
	 * 
	 * @return 查找路径
	 */
	public String getLookupPathForRequest(HttpServletRequest request) {
		// 始终在当前servlet上下文中使用完整路径?
		if (this.alwaysUseFullPath) {
			return getPathWithinApplication(request);
		}
		// 否则, 如果适用, 在当前servlet映射中使用path
		String rest = getPathWithinServletMapping(request);
		if (!"".equals(rest)) {
			return rest;
		}
		else {
			return getPathWithinApplication(request);
		}
	}

	/**
	 * 返回给定请求的servlet映射中的路径,
	 * i.e. 请求URL的一部分超出调用servlet的部分, 或"" 如果整个URL已用于标识servlet.
	 * <p>如果在RequestDispatcher include中调用, 则检测包括请求URL.
	 * <p>E.g.: servlet mapping = "/*"; request URI = "/test/a" -> "/test/a".
	 * <p>E.g.: servlet mapping = "/"; request URI = "/test/a" -> "/test/a".
	 * <p>E.g.: servlet mapping = "/test/*"; request URI = "/test/a" -> "/a".
	 * <p>E.g.: servlet mapping = "/test"; request URI = "/test" -> "".
	 * <p>E.g.: servlet mapping = "/*.test"; request URI = "/a.test" -> "".
	 * 
	 * @param request 当前的HTTP请求
	 * 
	 * @return servlet映射中的路径, 或""
	 */
	public String getPathWithinServletMapping(HttpServletRequest request) {
		String pathWithinApp = getPathWithinApplication(request);
		String servletPath = getServletPath(request);
		String sanitizedPathWithinApp = getSanitizedPath(pathWithinApp);
		String path;

		// 如果app容器清理了servletPath, 检查已清理的版本
		if (servletPath.contains(sanitizedPathWithinApp)) {
			path = getRemainingPath(sanitizedPathWithinApp, servletPath, false);
		}
		else {
			path = getRemainingPath(pathWithinApp, servletPath, false);
		}

		if (path != null) {
			// 正常情况: URI包含servlet路径.
			return path;
		}
		else {
			// 特殊情况: URI与servlet路径不同.
			String pathInfo = request.getPathInfo();
			if (pathInfo != null) {
				// 使用路径信息. 表示servlet映射中的索引页?
				// e.g. 使用索引页: URI="/", servletPath="/index.html"
				return pathInfo;
			}
			if (!this.urlDecode) {
				// 没有路径信息... (不是由前缀映射, 也不是由扩展名映射, 也不是"/*")
				// 对于默认的servlet映射 (i.e. "/"), urlDecode=false 会导致问题, 因为getServletPath() 返回一个已解码的路径.
				// 如果解码pathWithinApp产生匹配, 则只使用pathWithinApp.
				path = getRemainingPath(decodeInternal(request, pathWithinApp), servletPath, false);
				if (path != null) {
					return pathWithinApp;
				}
			}
			// 否则, 使用完整的servlet路径.
			return servletPath;
		}
	}

	/**
	 * 返回Web应用程序中给定请求的路径.
	 * <p>如果在RequestDispatcher include中调用, 则检测包括请求URL.
	 * 
	 * @param request 当前的HTTP请求
	 * 
	 * @return Web应用程序中的路径
	 */
	public String getPathWithinApplication(HttpServletRequest request) {
		String contextPath = getContextPath(request);
		String requestUri = getRequestUri(request);
		String path = getRemainingPath(requestUri, contextPath, true);
		if (path != null) {
			// 正常情况: URI包含上下文路径.
			return (StringUtils.hasText(path) ? path : "/");
		}
		else {
			return requestUri;
		}
	}

	/**
	 * 将给定的"mapping"与"requestUri"的开头匹配, 如果匹配则返回额外的部分.
	 * 需要此方法, 因为与requestUri不同, HttpServletRequest返回的上下文路径和servlet路径被剥离分号内容.
	 */
	private String getRemainingPath(String requestUri, String mapping, boolean ignoreCase) {
		int index1 = 0;
		int index2 = 0;
		for (; (index1 < requestUri.length()) && (index2 < mapping.length()); index1++, index2++) {
			char c1 = requestUri.charAt(index1);
			char c2 = mapping.charAt(index2);
			if (c1 == ';') {
				index1 = requestUri.indexOf('/', index1);
				if (index1 == -1) {
					return null;
				}
				c1 = requestUri.charAt(index1);
			}
			if (c1 == c2 || (ignoreCase && (Character.toLowerCase(c1) == Character.toLowerCase(c2)))) {
				continue;
			}
			return null;
		}
		if (index2 != mapping.length()) {
			return null;
		}
		else if (index1 == requestUri.length()) {
			return "";
		}
		else if (requestUri.charAt(index1) == ';') {
			index1 = requestUri.indexOf('/', index1);
		}
		return (index1 != -1 ? requestUri.substring(index1) : "");
	}

	/**
	 * 使用以下规则清理给定路径:
	 * <ul>
	 *     <li>由"/"替换所有"//"</li>
	 * </ul>
	 */
	private String getSanitizedPath(final String path) {
		String sanitized = path;
		while (true) {
			int index = sanitized.indexOf("//");
			if (index < 0) {
				break;
			}
			else {
				sanitized = sanitized.substring(0, index) + sanitized.substring(index + 1);
			}
		}
		return sanitized;
	}

	/**
	 * 返回给定请求的请求URI, 如果在RequestDispatcher include中调用, 则检测include请求URL.
	 * <p>由{@code request.getRequestURI()}返回的值是<i>不</i>由servlet容器解码, 此方法将解码它.
	 * <p>Web容器解析的URI应该是正确的, 但是像JBoss/Jetty这样的容器错误地包含 ";" 字符串, 例如URI中的字符串如";jsessionid".
	 * 这种方法切断了这些不正确的附录.
	 * 
	 * @param request 当前的HTTP请求
	 * 
	 * @return 请求URI
	 */
	public String getRequestUri(HttpServletRequest request) {
		String uri = (String) request.getAttribute(WebUtils.INCLUDE_REQUEST_URI_ATTRIBUTE);
		if (uri == null) {
			uri = request.getRequestURI();
		}
		return decodeAndCleanUriString(request, uri);
	}

	/**
	 * 返回给定请求的上下文路径, 如果在RequestDispatcher include中调用, 则检测include请求URL.
	 * <p>由{@code request.getContextPath()}返回的值<i>不</i>由servlet容器解码, 此方法将解码它.
	 * 
	 * @param request 当前的HTTP请求
	 * 
	 * @return 上下文路径
	 */
	public String getContextPath(HttpServletRequest request) {
		String contextPath = (String) request.getAttribute(WebUtils.INCLUDE_CONTEXT_PATH_ATTRIBUTE);
		if (contextPath == null) {
			contextPath = request.getContextPath();
		}
		if ("/".equals(contextPath)) {
			// 无效的情况, 但发生在Jetty上的包含: 默默地适配它.
			contextPath = "";
		}
		return decodeRequestString(request, contextPath);
	}

	/**
	 * 如果在RequestDispatcher include中调用了include请求URL, 则返回给定请求的servlet路径.
	 * <p>由于{@code request.getServletPath()}返回的值已由servlet容器解码, 因此此方法不会尝试对其进行解码.
	 * 
	 * @param request 当前的HTTP请求
	 * 
	 * @return 上下文路径
	 */
	public String getServletPath(HttpServletRequest request) {
		String servletPath = (String) request.getAttribute(WebUtils.INCLUDE_SERVLET_PATH_ATTRIBUTE);
		if (servletPath == null) {
			servletPath = request.getServletPath();
		}
		if (servletPath.length() > 1 && servletPath.endsWith("/") && shouldRemoveTrailingServletPathSlash(request)) {
			// 在WebSphere上, 在非兼容模式下, 对于在所有其他servlet容器上"/foo/"将为 "/foo"的情况:
			// 删除尾部斜杠, 继续使用剩余的斜杠作为最终查找路径...
			servletPath = servletPath.substring(0, servletPath.length() - 1);
		}
		return servletPath;
	}


	/**
	 * 返回给定请求的请求URI. 如果这是转发请求, 正确解析为原始请求的请求URI.
	 */
	public String getOriginatingRequestUri(HttpServletRequest request) {
		String uri = (String) request.getAttribute(WEBSPHERE_URI_ATTRIBUTE);
		if (uri == null) {
			uri = (String) request.getAttribute(WebUtils.FORWARD_REQUEST_URI_ATTRIBUTE);
			if (uri == null) {
				uri = request.getRequestURI();
			}
		}
		return decodeAndCleanUriString(request, uri);
	}

	/**
	 * 返回给定请求的上下文路径, 如果在RequestDispatcher include中调用, 则检测include请求URL.
	 * <p>由{@code request.getContextPath()}返回的值<i>不</i>由servlet容器解码, 此方法将解码它.
	 * 
	 * @param request 当前的HTTP请求
	 * 
	 * @return 上下文路径
	 */
	public String getOriginatingContextPath(HttpServletRequest request) {
		String contextPath = (String) request.getAttribute(WebUtils.FORWARD_CONTEXT_PATH_ATTRIBUTE);
		if (contextPath == null) {
			contextPath = request.getContextPath();
		}
		return decodeRequestString(request, contextPath);
	}

	/**
	 * 返回给定请求的servlet路径, 如果在RequestDispatcher include中调用, 则检测include请求URL.
	 * 
	 * @param request 当前的HTTP请求
	 * 
	 * @return servlet路径
	 */
	public String getOriginatingServletPath(HttpServletRequest request) {
		String servletPath = (String) request.getAttribute(WebUtils.FORWARD_SERVLET_PATH_ATTRIBUTE);
		if (servletPath == null) {
			servletPath = request.getServletPath();
		}
		return servletPath;
	}

	/**
	 * 返回给定请求的URL的查询字符串部分.
	 * 如果这是转发请求, 请正确解析为原始请求的查询字符串.
	 * 
	 * @param request 当前的HTTP请求
	 * 
	 * @return 查询字符串
	 */
	public String getOriginatingQueryString(HttpServletRequest request) {
		if ((request.getAttribute(WebUtils.FORWARD_REQUEST_URI_ATTRIBUTE) != null) ||
			(request.getAttribute(WebUtils.ERROR_REQUEST_URI_ATTRIBUTE) != null)) {
			return (String) request.getAttribute(WebUtils.FORWARD_QUERY_STRING_ATTRIBUTE);
		}
		else {
			return request.getQueryString();
		}
	}

	/**
	 * 解码提供的URI字符串并删除';'之后任何无关的部分.
	 */
	private String decodeAndCleanUriString(HttpServletRequest request, String uri) {
		uri = removeSemicolonContent(uri);
		uri = decodeRequestString(request, uri);
		uri = getSanitizedPath(uri);
		return uri;
	}

	/**
	 * 使用URLDecoder解码给定的源字符串.
	 * 编码将从请求中获取, 并回退到默认的"ISO-8859-1".
	 * <p>默认实现使用{@code URLDecoder.decode(input, enc)}.
	 * 
	 * @param request 当前的HTTP请求
	 * @param source 要解码的字符串
	 * 
	 * @return 解码后的字符串
	 */
	public String decodeRequestString(HttpServletRequest request, String source) {
		if (this.urlDecode && source != null) {
			return decodeInternal(request, source);
		}
		return source;
	}

	@SuppressWarnings("deprecation")
	private String decodeInternal(HttpServletRequest request, String source) {
		String enc = determineEncoding(request);
		try {
			return UriUtils.decode(source, enc);
		}
		catch (UnsupportedEncodingException ex) {
			if (logger.isWarnEnabled()) {
				logger.warn("Could not decode request string [" + source + "] with encoding '" + enc +
						"': falling back to platform default encoding; exception message: " + ex.getMessage());
			}
			return URLDecoder.decode(source);
		}
	}

	/**
	 * 确定给定请求的编码.
	 * 可以在子类中重写.
	 * <p>默认实现检查请求编码, 然后回退到为此解析器指定的默认编码.
	 * 
	 * @param request 当前的HTTP请求
	 * 
	 * @return 请求的编码 (never {@code null})
	 */
	protected String determineEncoding(HttpServletRequest request) {
		String enc = request.getCharacterEncoding();
		if (enc == null) {
			enc = getDefaultEncoding();
		}
		return enc;
	}

	/**
	 * 从给定请求URI删除";" (分号)内容, 如果
	 * {@linkplain #setRemoveSemicolonContent(boolean) removeSemicolonContent}
	 * 属性为"true". 将始终删除"jssessionid".
	 * 
	 * @param requestUri 要删除";"的请求URI字符串
	 * 
	 * @return 更新的URI字符串
	 */
	public String removeSemicolonContent(String requestUri) {
		return (this.removeSemicolonContent ?
				removeSemicolonContentInternal(requestUri) : removeJsessionid(requestUri));
	}

	private String removeSemicolonContentInternal(String requestUri) {
		int semicolonIndex = requestUri.indexOf(';');
		while (semicolonIndex != -1) {
			int slashIndex = requestUri.indexOf('/', semicolonIndex);
			String start = requestUri.substring(0, semicolonIndex);
			requestUri = (slashIndex != -1) ? start + requestUri.substring(slashIndex) : start;
			semicolonIndex = requestUri.indexOf(';', semicolonIndex);
		}
		return requestUri;
	}

	private String removeJsessionid(String requestUri) {
		int startIndex = requestUri.toLowerCase().indexOf(";jsessionid=");
		if (startIndex != -1) {
			int endIndex = requestUri.indexOf(';', startIndex + 12);
			String start = requestUri.substring(0, startIndex);
			requestUri = (endIndex != -1) ? start + requestUri.substring(endIndex) : start;
		}
		return requestUri;
	}

	/**
	 * 通过{@link #decodeRequestString(HttpServletRequest, String)}解码给定的URI路径变量,
	 * 除非{@link #setUrlDecode(boolean)}设置为{@code true},
	 * 在这种情况下, 假设从中提取变量的URL路径已经通过调用{@link #getLookupPathForRequest(HttpServletRequest)}来解码.
	 * 
	 * @param request 当前的HTTP请求
	 * @param vars 从URL路径中提取的URI变量
	 * 
	 * @return 相同的Map或新的Map实例
	 */
	public Map<String, String> decodePathVariables(HttpServletRequest request, Map<String, String> vars) {
		if (this.urlDecode) {
			return vars;
		}
		else {
			Map<String, String> decodedVars = new LinkedHashMap<String, String>(vars.size());
			for (Map.Entry<String, String> entry : vars.entrySet()) {
				decodedVars.put(entry.getKey(), decodeInternal(request, entry.getValue()));
			}
			return decodedVars;
		}
	}

	/**
	 * 通过{@link #decodeRequestString(HttpServletRequest, String)}解码给定的矩阵变量,
	 * 除非{@link #setUrlDecode(boolean)}设置为{@code true},
	 * 在这种情况下, 假设从中提取变量的URL路径已经通过调用{@link #getLookupPathForRequest(HttpServletRequest)}来解码.
	 * 
	 * @param request 当前的HTTP请求
	 * @param vars 从URL路径中提取的URI变量
	 * 
	 * @return 相同的Map或新的Map实例
	 */
	public MultiValueMap<String, String> decodeMatrixVariables(HttpServletRequest request, MultiValueMap<String, String> vars) {
		if (this.urlDecode) {
			return vars;
		}
		else {
			MultiValueMap<String, String> decodedVars = new LinkedMultiValueMap	<String, String>(vars.size());
			for (String key : vars.keySet()) {
				for (String value : vars.get(key)) {
					decodedVars.add(key, decodeInternal(request, value));
				}
			}
			return decodedVars;
		}
	}

	private boolean shouldRemoveTrailingServletPathSlash(HttpServletRequest request) {
		if (request.getAttribute(WEBSPHERE_URI_ATTRIBUTE) == null) {
			// 常规servlet容器: 在任何情况下都表现如预期, 因此尾部斜杠是 "/" url-pattern映射的结果.
			// 不要删除斜杠.
			return false;
		}
		if (websphereComplianceFlag == null) {
			ClassLoader classLoader = UrlPathHelper.class.getClassLoader();
			String className = "com.ibm.ws.webcontainer.WebContainer";
			String methodName = "getWebContainerProperties";
			String propName = "com.ibm.ws.webcontainer.removetrailingservletpathslash";
			boolean flag = false;
			try {
				Class<?> cl = classLoader.loadClass(className);
				Properties prop = (Properties) cl.getMethod(methodName).invoke(null);
				flag = Boolean.parseBoolean(prop.getProperty(propName));
			}
			catch (Throwable ex) {
				if (logger.isDebugEnabled()) {
					logger.debug("Could not introspect WebSphere web container properties: " + ex);
				}
			}
			websphereComplianceFlag = flag;
		}
		// 如果WebSphere配置为完全符合Servlet, 请不要理会.
		// 但是, 如果它不符合要求, 删除不正确的尾部斜杠!
		return !websphereComplianceFlag;
	}

}
