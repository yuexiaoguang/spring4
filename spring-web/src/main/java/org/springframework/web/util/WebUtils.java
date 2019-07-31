package org.springframework.web.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TreeMap;
import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import javax.servlet.ServletRequestWrapper;
import javax.servlet.ServletResponse;
import javax.servlet.ServletResponseWrapper;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.http.HttpRequest;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * Web应用程序的工具类.
 * 由各种框架类使用.
 */
public abstract class WebUtils {

	/**
	 * 包含URI和路径的标准Servlet 2.3+规范请求属性.
	 * <p>如果通过RequestDispatcher包含, 则当前资源将看到原始请求. 它自己的URI和路径作为请求属性公开.
	 */
	public static final String INCLUDE_REQUEST_URI_ATTRIBUTE = "javax.servlet.include.request_uri";
	public static final String INCLUDE_CONTEXT_PATH_ATTRIBUTE = "javax.servlet.include.context_path";
	public static final String INCLUDE_SERVLET_PATH_ATTRIBUTE = "javax.servlet.include.servlet_path";
	public static final String INCLUDE_PATH_INFO_ATTRIBUTE = "javax.servlet.include.path_info";
	public static final String INCLUDE_QUERY_STRING_ATTRIBUTE = "javax.servlet.include.query_string";

	/**
	 * 转发URI和路径的标准Servlet 2.4+规范请求属性.
	 * <p>如果通过RequestDispatcher转发, 则当前资源将看到自己的URI和路径. 原始URI和路径作为请求属性公开.
	 */
	public static final String FORWARD_REQUEST_URI_ATTRIBUTE = "javax.servlet.forward.request_uri";
	public static final String FORWARD_CONTEXT_PATH_ATTRIBUTE = "javax.servlet.forward.context_path";
	public static final String FORWARD_SERVLET_PATH_ATTRIBUTE = "javax.servlet.forward.servlet_path";
	public static final String FORWARD_PATH_INFO_ATTRIBUTE = "javax.servlet.forward.path_info";
	public static final String FORWARD_QUERY_STRING_ATTRIBUTE = "javax.servlet.forward.query_string";

	/**
	 * 错误页面的标准Servlet 2.3+规范请求属性.
	 * <p>暴露给标记为错误页面的JSP, 直接转发给它们而不是通过servlet容器的错误页面解析机制.
	 */
	public static final String ERROR_STATUS_CODE_ATTRIBUTE = "javax.servlet.error.status_code";
	public static final String ERROR_EXCEPTION_TYPE_ATTRIBUTE = "javax.servlet.error.exception_type";
	public static final String ERROR_MESSAGE_ATTRIBUTE = "javax.servlet.error.message";
	public static final String ERROR_EXCEPTION_ATTRIBUTE = "javax.servlet.error.exception";
	public static final String ERROR_REQUEST_URI_ATTRIBUTE = "javax.servlet.error.request_uri";
	public static final String ERROR_SERVLET_NAME_ATTRIBUTE = "javax.servlet.error.servlet_name";


	/**
	 * 内容类型String中charset子句的前缀: ";charset="
	 */
	public static final String CONTENT_TYPE_CHARSET_PREFIX = ";charset=";

	/**
	 * 根据Servlet规范, {@code request.getCharacterEncoding}返回{@code null}时要使用的默认字符编码.
	 */
	public static final String DEFAULT_CHARACTER_ENCODING = "ISO-8859-1";

	/**
	 * 标准Servlet规范上下文属性, 指定当前Web应用程序的临时目录, 类型为{@code java.io.File}.
	 */
	public static final String TEMP_DIR_CONTEXT_ATTRIBUTE = "javax.servlet.context.tempdir";

	/**
	 * servlet上下文级别的HTML转义参数 (i.e. {@code web.xml}中的上下文参数): "defaultHtmlEscape".
	 */
	public static final String HTML_ESCAPE_CONTEXT_PARAM = "defaultHtmlEscape";

	/**
	 * 在servlet上下文级别使用响应编码的HTML转义参数 (i.e. {@code web.xml}中的上下文参数): "responseEncodedHtmlEscape".
	 */
	public static final String RESPONSE_ENCODED_HTML_ESCAPE_CONTEXT_PARAM = "responseEncodedHtmlEscape";

	/**
	 * servlet上下文级别的Web应用程序根的键名参数 (i.e. {@code web.xml}中的上下文参数): "webAppRootKey".
	 */
	public static final String WEB_APP_ROOT_KEY_PARAM = "webAppRootKey";

	/** 默认的Web应用程序根的键: "webapp.root" */
	public static final String DEFAULT_WEB_APP_ROOT_KEY = "webapp.root";

	/** 图像按钮的名称后缀 */
	public static final String[] SUBMIT_IMAGE_SUFFIXES = {".x", ".y"};

	/** 互斥锁会话属性的键 */
	public static final String SESSION_MUTEX_ATTRIBUTE = WebUtils.class.getName() + ".MUTEX";


	/**
	 * 将系统属性设置为Web应用程序根目录.
	 * 可以使用{@code web.xml}中的"webAppRootKey" context-param定义系统属性的键. 默认为"webapp.root".
	 * <p>可用于支持使用{@code System.getProperty}值替换的工具, 例如日志文件位置中的log4j的"${key}"语法.
	 * 
	 * @param servletContext Web应用程序的servlet上下文
	 * 
	 * @throws IllegalStateException 如果已设置系统属性, 或者未展开WAR文件
	 */
	public static void setWebAppRootSystemProperty(ServletContext servletContext) throws IllegalStateException {
		Assert.notNull(servletContext, "ServletContext must not be null");
		String root = servletContext.getRealPath("/");
		if (root == null) {
			throw new IllegalStateException(
					"Cannot set web app root system property when WAR file is not expanded");
		}
		String param = servletContext.getInitParameter(WEB_APP_ROOT_KEY_PARAM);
		String key = (param != null ? param : DEFAULT_WEB_APP_ROOT_KEY);
		String oldValue = System.getProperty(key);
		if (oldValue != null && !StringUtils.pathEquals(oldValue, root)) {
			throw new IllegalStateException("Web app root system property already set to different value: '" +
					key + "' = [" + oldValue + "] instead of [" + root + "] - " +
					"Choose unique values for the 'webAppRootKey' context-param in your web.xml files!");
		}
		System.setProperty(key, root);
		servletContext.log("Set web app root system property: '" + key + "' = [" + root + "]");
	}

	/**
	 * 删除指向Web应用程序根目录的系统属性.
	 * 在关闭Web应用程序时调用.
	 * 
	 * @param servletContext Web应用程序的servlet上下文
	 */
	public static void removeWebAppRootSystemProperty(ServletContext servletContext) {
		Assert.notNull(servletContext, "ServletContext must not be null");
		String param = servletContext.getInitParameter(WEB_APP_ROOT_KEY_PARAM);
		String key = (param != null ? param : DEFAULT_WEB_APP_ROOT_KEY);
		System.getProperties().remove(key);
	}

	/**
	 * 返回是否为Web应用程序启用了默认的HTML转义, i.e. {@code web.xml}中的"defaultHtmlEscape" context-param的值.
	 * 如果没有给出明确的默认值, 则回退到{@code false}.
	 * 
	 * @param servletContext Web应用程序的servlet上下文
	 * 
	 * @return 是否启用了默认的HTML转义 (默认为{@code false})
	 * @deprecated as of Spring 4.1, in favor of {@link #getDefaultHtmlEscape}
	 */
	@Deprecated
	public static boolean isDefaultHtmlEscape(ServletContext servletContext) {
		if (servletContext == null) {
			return false;
		}
		String param = servletContext.getInitParameter(HTML_ESCAPE_CONTEXT_PARAM);
		return Boolean.valueOf(param);
	}

	/**
	 * 返回是否为Web应用程序启用了默认的HTML转义, i.e. {@code web.xml}中的"defaultHtmlEscape" context-param的值.
	 * <p>此方法区分了根本没有指定的参数和指定的实际boolean值, 允许在全局级别没有设置的情况下具有特定于上下文的默认值.
	 * 
	 * @param servletContext Web应用程序的servlet上下文
	 * 
	 * @return 是否为给定的应用程序启用了默认的HTML转义 ({@code null} = 没有显式默认值)
	 */
	public static Boolean getDefaultHtmlEscape(ServletContext servletContext) {
		if (servletContext == null) {
			return null;
		}
		String param = servletContext.getInitParameter(HTML_ESCAPE_CONTEXT_PARAM);
		return (StringUtils.hasText(param) ? Boolean.valueOf(param) : null);
	}

	/**
	 * 返回HTML转义字符时是否应该使用响应编码, 只能使用 UTF-* 编码转义XML标记重要字符.
	 * 使用ServletContext参数为Web应用程序启用此选项,
	 * i.e. {@code web.xml}中的"responseEncodedHtmlEscape" context-param的值.
	 * <p>此方法区分了根本没有指定的参数和指定的实际boolean值, 允许在全局级别没有设置的情况下具有特定于上下文的默认值.
	 * 
	 * @param servletContext Web应用程序的servlet上下文
	 * 
	 * @return 响应编码是否用于HTML转义 ({@code null} = 没有显式默认值)
	 */
	public static Boolean getResponseEncodedHtmlEscape(ServletContext servletContext) {
		if (servletContext == null) {
			return null;
		}
		String param = servletContext.getInitParameter(RESPONSE_ENCODED_HTML_ESCAPE_CONTEXT_PARAM);
		return (StringUtils.hasText(param) ? Boolean.valueOf(param) : null);
	}

	/**
	 * 返回当前Web应用程序的临时目录, 由servlet容器提供.
	 * 
	 * @param servletContext Web应用程序的servlet上下文
	 * 
	 * @return 表示临时目录的文件
	 */
	public static File getTempDir(ServletContext servletContext) {
		Assert.notNull(servletContext, "ServletContext must not be null");
		return (File) servletContext.getAttribute(TEMP_DIR_CONTEXT_ATTRIBUTE);
	}

	/**
	 * 返回Web应用程序中给定路径的实际路径, 由servlet容器提供.
	 * <p>如果路径尚未以斜杠开头, 则在前面添加斜杠,
	 * 如果路径无法解析为资源, 则抛出FileNotFoundException (与ServletContext的{@code getRealPath}相反, 返回null).
	 * 
	 * @param servletContext Web应用程序的servlet上下文
	 * @param path Web应用程序中的路径
	 * 
	 * @return 相应的真实路径
	 * @throws FileNotFoundException 如果路径无法解析为资源
	 */
	public static String getRealPath(ServletContext servletContext, String path) throws FileNotFoundException {
		Assert.notNull(servletContext, "ServletContext must not be null");
		// 将位置解释为相对于Web应用程序根目录.
		if (!path.startsWith("/")) {
			path = "/" + path;
		}
		String realPath = servletContext.getRealPath(path);
		if (realPath == null) {
			throw new FileNotFoundException(
					"ServletContext resource [" + path + "] cannot be resolved to absolute file path - " +
					"web application archive not expanded?");
		}
		return realPath;
	}

	/**
	 * 确定给定请求的会话ID.
	 * 
	 * @param request 当前的HTTP请求
	 * 
	 * @return 会话ID, 或{@code null}
	 */
	public static String getSessionId(HttpServletRequest request) {
		Assert.notNull(request, "Request must not be null");
		HttpSession session = request.getSession(false);
		return (session != null ? session.getId() : null);
	}

	/**
	 * 检查给定请求以获取给定名称的会话属性.
	 * 如果没有会话或者会话没有这样的属性, 则返回null.
	 * 如果之前不存在, 则不会创建新会话!
	 * 
	 * @param request 当前的HTTP请求
	 * @param name 会话属性的名称
	 * 
	 * @return 会话属性的值, 或{@code null}
	 */
	public static Object getSessionAttribute(HttpServletRequest request, String name) {
		Assert.notNull(request, "Request must not be null");
		HttpSession session = request.getSession(false);
		return (session != null ? session.getAttribute(name) : null);
	}

	/**
	 * 检查给定请求以获取给定名称的会话属性.
	 * 如果没有会话或者会话没有这样的属性, 则抛出异常. 如果之前不存在, 则不会创建新会话!
	 * 
	 * @param request 当前的HTTP请求
	 * @param 会话属性的名称
	 * 
	 * @return 会话属性的值, 或{@code null}
	 * @throws IllegalStateException 如果找不到会话属性
	 */
	public static Object getRequiredSessionAttribute(HttpServletRequest request, String name)
			throws IllegalStateException {

		Object attr = getSessionAttribute(request, name);
		if (attr == null) {
			throw new IllegalStateException("No session attribute '" + name + "' found");
		}
		return attr;
	}

	/**
	 * 设置session属性.
	 * 如果会话存在且值为null, 则删除session属性.
	 * 如果没有必要, 不会创建新会话!
	 * 
	 * @param request 当前的HTTP请求
	 * @param name 会话属性的名称
	 * @param value 会话属性的值
	 */
	public static void setSessionAttribute(HttpServletRequest request, String name, Object value) {
		Assert.notNull(request, "Request must not be null");
		if (value != null) {
			request.getSession().setAttribute(name, value);
		}
		else {
			HttpSession session = request.getSession(false);
			if (session != null) {
				session.removeAttribute(name);
			}
		}
	}

	/**
	 * 获取指定的会话属性, 如果找不到现有属性, 则创建并设置新属性.
	 * 给定的类需要有一个公共的无参数构造函数.
	 * 对于Web层中的按需状态对象(如购物车)非常有用.
	 * 
	 * @param session 当前的HTTP会话
	 * @param name 会话属性的名称
	 * @param clazz 要为新属性实例化的类
	 * 
	 * @return 会话属性的值, 如果未找到则新建
	 * @throws IllegalArgumentException 如果无法实例化会话属性
	 * @deprecated as of Spring 4.3.2, in favor of custom code for such purposes
	 */
	@Deprecated
	public static Object getOrCreateSessionAttribute(HttpSession session, String name, Class<?> clazz)
			throws IllegalArgumentException {

		Assert.notNull(session, "Session must not be null");
		Object sessionObject = session.getAttribute(name);
		if (sessionObject == null) {
			try {
				sessionObject = clazz.newInstance();
			}
			catch (InstantiationException ex) {
				throw new IllegalArgumentException(
					"Could not instantiate class [" + clazz.getName() +
					"] for session attribute '" + name + "': " + ex.getMessage());
			}
			catch (IllegalAccessException ex) {
				throw new IllegalArgumentException(
					"Could not access default constructor of class [" + clazz.getName() +
					"] for session attribute '" + name + "': " + ex.getMessage());
			}
			session.setAttribute(name, sessionObject);
		}
		return sessionObject;
	}

	/**
	 * Return the best available mutex for the given session: that is, an object to synchronize on for the given session.
	 * <p>Returns the session mutex attribute if available; usually, this means that the HttpSessionMutexListener needs to be defined in {@code web.xml}.
	 * Falls back to the HttpSession itself if no mutex attribute found.
	 * <p>The session mutex is guaranteed to be the same object during the entire lifetime of the session, available under the key defined by the {@code SESSION_MUTEX_ATTRIBUTE} constant.
	 * It serves as a safe reference to synchronize on for locking on the current session.
	 * <p>In many cases, the HttpSession reference itself is a safe mutex as well, since it will always be the same object reference for the same active logical session.
	 * However, this is not guaranteed across different servlet containers; the only 100% safe way is a session mutex.
	 * 
	 * @param session the HttpSession to find a mutex for
	 * 
	 * @return the mutex object (never {@code null})
	 */
	public static Object getSessionMutex(HttpSession session) {
		Assert.notNull(session, "Session must not be null");
		Object mutex = session.getAttribute(SESSION_MUTEX_ATTRIBUTE);
		if (mutex == null) {
			mutex = session;
		}
		return mutex;
	}


	/**
	 * Return an appropriate request object of the specified type, if available, unwrapping the given request as far as necessary.
	 * 
	 * @param request the servlet request to introspect
	 * @param requiredType the desired type of request object
	 * 
	 * @return the matching request object, or {@code null} if none of that type is available
	 */
	@SuppressWarnings("unchecked")
	public static <T> T getNativeRequest(ServletRequest request, Class<T> requiredType) {
		if (requiredType != null) {
			if (requiredType.isInstance(request)) {
				return (T) request;
			}
			else if (request instanceof ServletRequestWrapper) {
				return getNativeRequest(((ServletRequestWrapper) request).getRequest(), requiredType);
			}
		}
		return null;
	}

	/**
	 * Return an appropriate response object of the specified type, if available, unwrapping the given response as far as necessary.
	 * 
	 * @param response the servlet response to introspect
	 * @param requiredType the desired type of response object
	 * 
	 * @return the matching response object, or {@code null} if none of that type is available
	 */
	@SuppressWarnings("unchecked")
	public static <T> T getNativeResponse(ServletResponse response, Class<T> requiredType) {
		if (requiredType != null) {
			if (requiredType.isInstance(response)) {
				return (T) response;
			}
			else if (response instanceof ServletResponseWrapper) {
				return getNativeResponse(((ServletResponseWrapper) response).getResponse(), requiredType);
			}
		}
		return null;
	}

	/**
	 * Determine whether the given request is an include request, that is, not a top-level HTTP request coming in from the outside.
	 * <p>Checks the presence of the "javax.servlet.include.request_uri" request attribute.
	 * Could check any request attribute that is only present in an include request.
	 * 
	 * @param request current servlet request
	 * 
	 * @return whether the given request is an include request
	 */
	public static boolean isIncludeRequest(ServletRequest request) {
		return (request.getAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE) != null);
	}

	/**
	 * Expose the Servlet spec's error attributes as {@link javax.servlet.http.HttpServletRequest} attributes under the keys defined in the Servlet 2.3 specification, for error pages that are rendered directly rather than through the Servlet container's error page resolution:
	 * {@code javax.servlet.error.status_code},
	 * {@code javax.servlet.error.exception_type},
	 * {@code javax.servlet.error.message},
	 * {@code javax.servlet.error.exception},
	 * {@code javax.servlet.error.request_uri},
	 * {@code javax.servlet.error.servlet_name}.
	 * <p>Does not override values if already present, to respect attribute values that have been exposed explicitly before.
	 * <p>Exposes status code 200 by default. Set the "javax.servlet.error.status_code" attribute explicitly (before or after) in order to expose a different status code.
	 * 
	 * @param request current servlet request
	 * @param ex the exception encountered
	 * @param servletName the name of the offending servlet
	 */
	public static void exposeErrorRequestAttributes(HttpServletRequest request, Throwable ex, String servletName) {
		exposeRequestAttributeIfNotPresent(request, ERROR_STATUS_CODE_ATTRIBUTE, HttpServletResponse.SC_OK);
		exposeRequestAttributeIfNotPresent(request, ERROR_EXCEPTION_TYPE_ATTRIBUTE, ex.getClass());
		exposeRequestAttributeIfNotPresent(request, ERROR_MESSAGE_ATTRIBUTE, ex.getMessage());
		exposeRequestAttributeIfNotPresent(request, ERROR_EXCEPTION_ATTRIBUTE, ex);
		exposeRequestAttributeIfNotPresent(request, ERROR_REQUEST_URI_ATTRIBUTE, request.getRequestURI());
		exposeRequestAttributeIfNotPresent(request, ERROR_SERVLET_NAME_ATTRIBUTE, servletName);
	}

	/**
	 * Expose the specified request attribute if not already present.
	 * 
	 * @param request current servlet request
	 * @param name the name of the attribute
	 * @param value the suggested value of the attribute
	 */
	private static void exposeRequestAttributeIfNotPresent(ServletRequest request, String name, Object value) {
		if (request.getAttribute(name) == null) {
			request.setAttribute(name, value);
		}
	}

	/**
	 * Clear the Servlet spec's error attributes as {@link javax.servlet.http.HttpServletRequest} attributes under the keys defined in the Servlet 2.3 specification:
	 * {@code javax.servlet.error.status_code},
	 * {@code javax.servlet.error.exception_type},
	 * {@code javax.servlet.error.message},
	 * {@code javax.servlet.error.exception},
	 * {@code javax.servlet.error.request_uri},
	 * {@code javax.servlet.error.servlet_name}.
	 * 
	 * @param request current servlet request
	 */
	public static void clearErrorRequestAttributes(HttpServletRequest request) {
		request.removeAttribute(ERROR_STATUS_CODE_ATTRIBUTE);
		request.removeAttribute(ERROR_EXCEPTION_TYPE_ATTRIBUTE);
		request.removeAttribute(ERROR_MESSAGE_ATTRIBUTE);
		request.removeAttribute(ERROR_EXCEPTION_ATTRIBUTE);
		request.removeAttribute(ERROR_REQUEST_URI_ATTRIBUTE);
		request.removeAttribute(ERROR_SERVLET_NAME_ATTRIBUTE);
	}

	/**
	 * Expose the given Map as request attributes, using the keys as attribute names and the values as corresponding attribute values. Keys need to be Strings.
	 * 
	 * @param request current HTTP request
	 * @param attributes the attributes Map
	 * 
	 * @deprecated as of Spring 4.3.2, in favor of custom code for such purposes
	 */
	@Deprecated
	public static void exposeRequestAttributes(ServletRequest request, Map<String, ?> attributes) {
		Assert.notNull(request, "Request must not be null");
		Assert.notNull(attributes, "Attributes Map must not be null");
		for (Map.Entry<String, ?> entry : attributes.entrySet()) {
			request.setAttribute(entry.getKey(), entry.getValue());
		}
	}

	/**
	 * Retrieve the first cookie with the given name.
	 * Note that multiple cookies can have the same name but different paths or domains.
	 * 
	 * @param request current servlet request
	 * @param name cookie name
	 * 
	 * @return the first cookie with the given name, or {@code null} if none is found
	 */
	public static Cookie getCookie(HttpServletRequest request, String name) {
		Assert.notNull(request, "Request must not be null");
		Cookie[] cookies = request.getCookies();
		if (cookies != null) {
			for (Cookie cookie : cookies) {
				if (name.equals(cookie.getName())) {
					return cookie;
				}
			}
		}
		return null;
	}

	/**
	 * Check if a specific input type="submit" parameter was sent in the request, either via a button (directly with name) or via an image (name + ".x" or name + ".y").
	 * 
	 * @param request current HTTP request
	 * @param name name of the parameter
	 * 
	 * @return if the parameter was sent
	 */
	public static boolean hasSubmitParameter(ServletRequest request, String name) {
		Assert.notNull(request, "Request must not be null");
		if (request.getParameter(name) != null) {
			return true;
		}
		for (String suffix : SUBMIT_IMAGE_SUFFIXES) {
			if (request.getParameter(name + suffix) != null) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Obtain a named parameter from the given request parameters.
	 * <p>See {@link #findParameterValue(java.util.Map, String)} for a description of the lookup algorithm.
	 * 
	 * @param request current HTTP request
	 * @param name the <i>logical</i> name of the request parameter
	 * 
	 * @return the value of the parameter, or {@code null} if the parameter does not exist in given request
	 */
	public static String findParameterValue(ServletRequest request, String name) {
		return findParameterValue(request.getParameterMap(), name);
	}

	/**
	 * Obtain a named parameter from the given request parameters.
	 * <p>This method will try to obtain a parameter value using the following algorithm:
	 * <ol>
	 * <li>Try to get the parameter value using just the given <i>logical</i> name.
	 * This handles parameters of the form <tt>logicalName = value</tt>.
	 * For normal parameters, e.g. submitted using a hidden HTML form field, this will return the requested value.</li>
	 * <li>Try to obtain the parameter value from the parameter name, where the parameter name in the request is of the form <tt>logicalName_value = xyz</tt> with "_" being the configured delimiter.
	 * This deals with parameter values submitted using an HTML form submit button.</li>
	 * <li>If the value obtained in the previous step has a ".x" or ".y" suffix, remove that.
	 * This handles cases where the value was submitted using an HTML form image button.
	 * In this case the parameter in the request would actually be of the form <tt>logicalName_value.x = 123</tt>. </li>
	 * </ol>
	 * 
	 * @param parameters the available parameter map
	 * @param name the <i>logical</i> name of the request parameter
	 * 
	 * @return the value of the parameter, or {@code null} if the parameter does not exist in given request
	 */
	public static String findParameterValue(Map<String, ?> parameters, String name) {
		// First try to get it as a normal name=value parameter
		Object value = parameters.get(name);
		if (value instanceof String[]) {
			String[] values = (String[]) value;
			return (values.length > 0 ? values[0] : null);
		}
		else if (value != null) {
			return value.toString();
		}
		// If no value yet, try to get it as a name_value=xyz parameter
		String prefix = name + "_";
		for (String paramName : parameters.keySet()) {
			if (paramName.startsWith(prefix)) {
				// Support images buttons, which would submit parameters as name_value.x=123
				for (String suffix : SUBMIT_IMAGE_SUFFIXES) {
					if (paramName.endsWith(suffix)) {
						return paramName.substring(prefix.length(), paramName.length() - suffix.length());
					}
				}
				return paramName.substring(prefix.length());
			}
		}
		// We couldn't find the parameter value...
		return null;
	}

	/**
	 * Return a map containing all parameters with the given prefix.
	 * Maps single values to String and multiple values to String array.
	 * <p>For example, with a prefix of "spring_", "spring_param1" and "spring_param2" result in a Map with "param1" and "param2" as keys.
	 * 
	 * @param request HTTP request in which to look for parameters
	 * @param prefix the beginning of parameter names (if this is null or the empty string, all parameters will match)
	 * 
	 * @return map containing request parameters <b>without the prefix</b>, containing either a String or a String array as values
	 */
	public static Map<String, Object> getParametersStartingWith(ServletRequest request, String prefix) {
		Assert.notNull(request, "Request must not be null");
		Enumeration<String> paramNames = request.getParameterNames();
		Map<String, Object> params = new TreeMap<String, Object>();
		if (prefix == null) {
			prefix = "";
		}
		while (paramNames != null && paramNames.hasMoreElements()) {
			String paramName = paramNames.nextElement();
			if ("".equals(prefix) || paramName.startsWith(prefix)) {
				String unprefixed = paramName.substring(prefix.length());
				String[] values = request.getParameterValues(paramName);
				if (values == null || values.length == 0) {
					// Do nothing, no values found at all.
				}
				else if (values.length > 1) {
					params.put(unprefixed, values);
				}
				else {
					params.put(unprefixed, values[0]);
				}
			}
		}
		return params;
	}

	/**
	 * Return the target page specified in the request.
	 * 
	 * @param request current servlet request
	 * @param paramPrefix the parameter prefix to check for (e.g. "_target" for parameters like "_target1" or "_target2")
	 * @param currentPage the current page, to be returned as fallback if no target page specified
	 * 
	 * @return the page specified in the request, or current page if not found
	 * @deprecated as of Spring 4.3.2, in favor of custom code for such purposes
	 */
	@Deprecated
	public static int getTargetPage(ServletRequest request, String paramPrefix, int currentPage) {
		Enumeration<String> paramNames = request.getParameterNames();
		while (paramNames.hasMoreElements()) {
			String paramName = paramNames.nextElement();
			if (paramName.startsWith(paramPrefix)) {
				for (int i = 0; i < WebUtils.SUBMIT_IMAGE_SUFFIXES.length; i++) {
					String suffix = WebUtils.SUBMIT_IMAGE_SUFFIXES[i];
					if (paramName.endsWith(suffix)) {
						paramName = paramName.substring(0, paramName.length() - suffix.length());
					}
				}
				return Integer.parseInt(paramName.substring(paramPrefix.length()));
			}
		}
		return currentPage;
	}


	/**
	 * Extract the URL filename from the given request URL path.
	 * Correctly resolves nested paths such as "/products/view.html" as well.
	 * 
	 * @param urlPath the request URL path (e.g. "/index.html")
	 * 
	 * @return the extracted URI filename (e.g. "index")
	 * @deprecated as of Spring 4.3.2, in favor of custom code for such purposes
	 */
	@Deprecated
	public static String extractFilenameFromUrlPath(String urlPath) {
		String filename = extractFullFilenameFromUrlPath(urlPath);
		int dotIndex = filename.lastIndexOf('.');
		if (dotIndex != -1) {
			filename = filename.substring(0, dotIndex);
		}
		return filename;
	}

	/**
	 * Extract the full URL filename (including file extension) from the given request URL path. Correctly resolve nested paths such as "/products/view.html" and remove any path and or query parameters.
	 * 
	 * @param urlPath the request URL path (e.g. "/products/index.html")
	 * 
	 * @return the extracted URI filename (e.g. "index.html")
	 * @deprecated as of Spring 4.3.2, in favor of custom code for such purposes
	 * (or {@link UriUtils#extractFileExtension} for the file extension use case)
	 */
	@Deprecated
	public static String extractFullFilenameFromUrlPath(String urlPath) {
		int end = urlPath.indexOf('?');
		if (end == -1) {
			end = urlPath.indexOf('#');
			if (end == -1) {
				end = urlPath.length();
			}
		}
		int begin = urlPath.lastIndexOf('/', end) + 1;
		int paramIndex = urlPath.indexOf(';', begin);
		end = (paramIndex != -1 && paramIndex < end ? paramIndex : end);
		return urlPath.substring(begin, end);
	}

	/**
	 * Parse the given string with matrix variables. An example string would look like this {@code "q1=a;q1=b;q2=a,b,c"}.
	 * The resulting map would contain keys {@code "q1"} and {@code "q2"} with values {@code ["a","b"]} and {@code ["a","b","c"]} respectively.
	 * 
	 * @param matrixVariables the unparsed matrix variables string
	 * 
	 * @return a map with matrix variable names and values (never {@code null})
	 */
	public static MultiValueMap<String, String> parseMatrixVariables(String matrixVariables) {
		MultiValueMap<String, String> result = new LinkedMultiValueMap<String, String>();
		if (!StringUtils.hasText(matrixVariables)) {
			return result;
		}
		StringTokenizer pairs = new StringTokenizer(matrixVariables, ";");
		while (pairs.hasMoreTokens()) {
			String pair = pairs.nextToken();
			int index = pair.indexOf('=');
			if (index != -1) {
				String name = pair.substring(0, index);
				String rawValue = pair.substring(index + 1);
				for (String value : StringUtils.commaDelimitedListToStringArray(rawValue)) {
					result.add(name, value);
				}
			}
			else {
				result.add(pair, "");
			}
		}
		return result;
	}

	/**
	 * Check the given request origin against a list of allowed origins.
	 * A list containing "*" means that all origins are allowed.
	 * An empty list means only same origin is allowed.
	 * <p><strong>Note:</strong> this method may use values from "Forwarded"
	 * (<a href="http://tools.ietf.org/html/rfc7239">RFC 7239</a>),
	 * "X-Forwarded-Host", "X-Forwarded-Port", and "X-Forwarded-Proto" headers,
	 * if present, in order to reflect the client-originated address.
	 * Consider using the {@code ForwardedHeaderFilter} in order to choose from a central place whether to extract and use, or to discard such headers.
	 * See the Spring Framework reference for more on this filter.
	 * 
	 * @return {@code true} if the request origin is valid, {@code false} otherwise
	 */
	public static boolean isValidOrigin(HttpRequest request, Collection<String> allowedOrigins) {
		Assert.notNull(request, "Request must not be null");
		Assert.notNull(allowedOrigins, "Allowed origins must not be null");

		String origin = request.getHeaders().getOrigin();
		if (origin == null || allowedOrigins.contains("*")) {
			return true;
		}
		else if (CollectionUtils.isEmpty(allowedOrigins)) {
			return isSameOrigin(request);
		}
		else {
			return allowedOrigins.contains(origin);
		}
	}

	/**
	 * Check if the request is a same-origin one, based on {@code Origin}, {@code Host},
	 * {@code Forwarded}, {@code X-Forwarded-Proto}, {@code X-Forwarded-Host} and {@code X-Forwarded-Port} headers.
	 * <p><strong>Note:</strong> this method uses values from "Forwarded"
	 * (<a href="http://tools.ietf.org/html/rfc7239">RFC 7239</a>),
	 * "X-Forwarded-Host", "X-Forwarded-Port", and "X-Forwarded-Proto" headers,
	 * if present, in order to reflect the client-originated address.
	 * Consider using the {@code ForwardedHeaderFilter} in order to choose from a central place whether to extract and use, or to discard such headers.
	 * See the Spring Framework reference for more on this filter.
	 * 
	 * @return {@code true} if the request is a same-origin one, {@code false} in case of cross-origin request
	 */
	public static boolean isSameOrigin(HttpRequest request) {
		String origin = request.getHeaders().getOrigin();
		if (origin == null) {
			return true;
		}
		UriComponentsBuilder urlBuilder;
		if (request instanceof ServletServerHttpRequest) {
			// Build more efficiently if we can: we only need scheme, host, port for origin comparison
			HttpServletRequest servletRequest = ((ServletServerHttpRequest) request).getServletRequest();
			urlBuilder = new UriComponentsBuilder().
					scheme(servletRequest.getScheme()).
					host(servletRequest.getServerName()).
					port(servletRequest.getServerPort()).
					adaptFromForwardedHeaders(request.getHeaders());
		}
		else {
			urlBuilder = UriComponentsBuilder.fromHttpRequest(request);
		}
		UriComponents actualUrl = urlBuilder.build();
		UriComponents originUrl = UriComponentsBuilder.fromOriginHeader(origin).build();
		return (ObjectUtils.nullSafeEquals(actualUrl.getHost(), originUrl.getHost()) &&
				getPort(actualUrl) == getPort(originUrl));
	}

	private static int getPort(UriComponents uri) {
		int port = uri.getPort();
		if (port == -1) {
			if ("http".equals(uri.getScheme()) || "ws".equals(uri.getScheme())) {
				port = 80;
			}
			else if ("https".equals(uri.getScheme()) || "wss".equals(uri.getScheme())) {
				port = 443;
			}
		}
		return port;
	}

}
