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
	 * 返回给定会话的最佳可用互斥锁: 即, 用于同步给定会话的对象.
	 * <p>返回会话互斥锁属性, 如果可用; 通常, 这意味着需要在{@code web.xml}中定义HttpSessionMutexListener.
	 * 如果没有找到互斥锁属性, 则回退到HttpSession本身.
	 * <p>会话互斥锁在会话的整个生命周期内保证是同一个对象, 在{@code SESSION_MUTEX_ATTRIBUTE}常量定义的键下可用.
	 * 它用作同步锁定当前会话的安全引用.
	 * <p>在许多情况下, HttpSession引用本身也是一个安全的互斥锁, 因为它对于同一个活动逻辑会话始终是相同的对象引用.
	 * 但是, 不能在不同的servlet容器中保证这一点; 唯一 100% 安全的方式是会话互斥.
	 * 
	 * @param session 要为其查找互斥锁的HttpSession
	 * 
	 * @return 互斥锁对象 (never {@code null})
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
	 * 返回指定类型的适当请求对象, 如果可用, 根据需要解包给定请求.
	 * 
	 * @param request 要内省的servlet请求
	 * @param requiredType 请求对象的所需类型
	 * 
	 * @return 匹配的请求对象, 或{@code null} 如果没有该类型可用
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
	 * 返回指定类型的适当响应对象, 根据需要解包给定的响应.
	 * 
	 * @param response 要内省的servlet反应
	 * @param requiredType 响应对象的所需类型
	 * 
	 * @return 匹配的响应对象, 或{@code null} 如果没有该类型可用
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
	 * 确定给定请求是否为include请求, 即不是从外部进入的顶级HTTP请求.
	 * <p>检查是否存在"javax.servlet.include.request_uri"请求属性.
	 * 可以检查仅包含在include请求中的请求属性.
	 * 
	 * @param request 当前的servlet请求
	 * 
	 * @return 给定请求是否为include请求
	 */
	public static boolean isIncludeRequest(ServletRequest request) {
		return (request.getAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE) != null);
	}

	/**
	 * 将Servlet规范的错误属性公开为Servlet 2.3规范中定义的键下的{@link javax.servlet.http.HttpServletRequest}属性,
	 * 用于直接呈现的错误页面, 而不是通过Servlet容器的错误页面解析:
	 * {@code javax.servlet.error.status_code},
	 * {@code javax.servlet.error.exception_type},
	 * {@code javax.servlet.error.message},
	 * {@code javax.servlet.error.exception},
	 * {@code javax.servlet.error.request_uri},
	 * {@code javax.servlet.error.servlet_name}.
	 * <p>如果已经存在, 则不覆盖值, 以尊重之前已明确公开的属性值.
	 * <p>默认情况下显示状态码200. 显式设置"javax.servlet.error.status_code"属性 (之前或之后), 以显示不同的状态码.
	 * 
	 * @param request 当前的servlet请求
	 * @param ex 遇到的异常
	 * @param servletName 违规servlet的名称
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
	 * 如果尚未存在, 公开指定的请求属性.
	 * 
	 * @param request 当前的servlet请求
	 * @param name 属性的名称
	 * @param value 属性的建议值
	 */
	private static void exposeRequestAttributeIfNotPresent(ServletRequest request, String name, Object value) {
		if (request.getAttribute(name) == null) {
			request.setAttribute(name, value);
		}
	}

	/**
	 * 清除Servlet规范的错误属性, 作为Servlet 2.3规范中定义的键下的{@link javax.servlet.http.HttpServletRequest}属性:
	 * {@code javax.servlet.error.status_code},
	 * {@code javax.servlet.error.exception_type},
	 * {@code javax.servlet.error.message},
	 * {@code javax.servlet.error.exception},
	 * {@code javax.servlet.error.request_uri},
	 * {@code javax.servlet.error.servlet_name}.
	 * 
	 * @param request 当前的servlet请求
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
	 * 将给定的Map公开为请求属性, 使用键作为属性名称, 值作为相应的属性值.
	 * 键必须是字符串.
	 * 
	 * @param request 当前的HTTP请求
	 * @param attributes 属性Map
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
	 * 检索具有给定名称的第一个cookie.
	 * 请注意, 多个Cookie可以具有相同的名称, 但路径或域不同.
	 * 
	 * @param request 当前的servlet请求
	 * @param name cookie名称
	 * 
	 * @return 具有给定名称的第一个cookie, 或{@code null}
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
	 * 检查请求中是否通过按钮(直接使用名称)或通过图像(名称 + ".x" 或名称 + ".y")发送了特定输入类型为"submit"的参数.
	 * 
	 * @param request 当前的HTTP请求
	 * @param name 参数名称
	 * 
	 * @return 如果参数已发送
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
	 * 从给定的请求参数中获取命名参数.
	 * <p>有关查找算法的说明, 请参阅{@link #findParameterValue(java.util.Map, String)}.
	 * 
	 * @param request 当前的HTTP请求
	 * @param name 请求参数的<i>逻辑</i>名称
	 * 
	 * @return 参数的值, 或{@code null} 如果参数在给定请求中不存在
	 */
	public static String findParameterValue(ServletRequest request, String name) {
		return findParameterValue(request.getParameterMap(), name);
	}

	/**
	 * 从给定的请求参数中获取命名参数.
	 * <p>此方法将尝试使用以下算法获取参数值:
	 * <ol>
	 * <li>尝试仅使用给定的<i>逻辑</i>名称获取参数值.
	 * 这将处理<tt>logicalName = value</tt>形式的参数.
	 * 对于正常参数, e.g. 使用隐藏的HTML表单字段提交, 这将返回请求的值.</li>
	 * <li>尝试从参数名称中获取参数值, 其中请求中的参数名称的格式为<tt>logicalName_value = xyz</tt>, 其中 "_"是已配置的分隔符.
	 * 这涉及使用HTML表单提交按钮提交的参数值.</li>
	 * <li>如果在上一步中获得的值具有".x" 或 ".y"后缀, 将其删除.
	 * 这将处理使用HTML表单图像按钮提交的值的情况.
	 * 在这种情况下, 请求中的参数实际上是<tt>logicalName_value.x = 123</tt>的形式. </li>
	 * </ol>
	 * 
	 * @param parameters 可用的参数Map
	 * @param name 请求参数的<i>逻辑</i>名称
	 * 
	 * @return 参数的值, 或{@code null} 如果参数在给定请求中不存在
	 */
	public static String findParameterValue(Map<String, ?> parameters, String name) {
		// 首先尝试将其作为普通的 name=value 参数
		Object value = parameters.get(name);
		if (value instanceof String[]) {
			String[] values = (String[]) value;
			return (values.length > 0 ? values[0] : null);
		}
		else if (value != null) {
			return value.toString();
		}
		// 如果还没有值, 尝试将其作为 name_value=xyz 参数
		String prefix = name + "_";
		for (String paramName : parameters.keySet()) {
			if (paramName.startsWith(prefix)) {
				// 支持图像按钮, 提交参数为 name_value.x=123
				for (String suffix : SUBMIT_IMAGE_SUFFIXES) {
					if (paramName.endsWith(suffix)) {
						return paramName.substring(prefix.length(), paramName.length() - suffix.length());
					}
				}
				return paramName.substring(prefix.length());
			}
		}
		// 找不到参数值...
		return null;
	}

	/**
	 * 返回包含具有给定前缀的所有参数的Map.
	 * 将单个值映射到String, 将多个值映射到String数组.
	 * <p>例如, 使用前缀"spring_", "spring_param1" 和 "spring_param2"会产生一个带有"param1" 和 "param2"作为键的Map.
	 * 
	 * @param request 用于查找参数的HTTP请求
	 * @param prefix 参数名称的开头 (如果为null或空字符串, 则所有参数都匹配)
	 * 
	 * @return 包含请求参数<b>不带前缀</b>的Map, 包含String或String数组作为值
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
	 * 返回请求中指定的目标页面.
	 * 
	 * @param request 当前的servlet请求
	 * @param paramPrefix 要检查的参数前缀 (e.g. "_target"参数, 如"_target1"或"_target2")
	 * @param currentPage 当前页面, 如果没有指定目标页面, 则作为后备返回
	 * 
	 * @return 请求中指定的页面, 或当前页面
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
	 * 从给定的请求URL路径中提取URL文件名.
	 * 正确解析嵌套的路径, 例如"/products/view.html".
	 * 
	 * @param urlPath 请求URL路径 (e.g. "/index.html")
	 * 
	 * @return 提取的URI文件名 (e.g. "index")
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
	 * 从给定的请求URL路径中提取完整的URL文件名 (包括文件扩展名).
	 * 正确解析嵌套的路径, 例如 "/products/view.html", 并删除任何路径和/或查询参数.
	 * 
	 * @param urlPath 请求URL路径 (e.g. "/products/index.html")
	 * 
	 * @return 提取的URI文件名 (e.g. "index.html")
	 * @deprecated as of Spring 4.3.2, in favor of custom code for such purposes
	 * (或{@link UriUtils#extractFileExtension}用于文件扩展名用例)
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
	 * 使用矩阵变量解析给定的字符串. 示例字符串看起来像这样{@code "q1=a;q1=b;q2=a,b,c"}.
	 * 生成的Map将包含{@code "q1"}和{@code "q2"}, 其值为{@code ["a","b"]} 和 {@code ["a","b","c"]}.
	 * 
	 * @param matrixVariables 未解析的矩阵变量字符串
	 * 
	 * @return 带有矩阵变量名称和值的Map (never {@code null})
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
	 * 根据允许的来源列表检查给定的请求来源.
	 * 包含"*"的列表表示允许所有来源.
	 * 空列表表示只允许相同的来源.
	 * <p><strong>Note:</strong>此方法可以使用"Forwarded"
	 * (<a href="http://tools.ietf.org/html/rfc7239">RFC 7239</a>),
	 * "X-Forwarded-Host", "X-Forwarded-Port", 和"X-Forwarded-Proto" header中的值,
	 * 以反映客户端发起的地址.
	 * 考虑使用{@code ForwardedHeaderFilter}从中心位置选择是否提取和使用, 或丢弃此header.
	 * 有关此过滤器的更多信息, 请参阅Spring Framework参考.
	 * 
	 * @return {@code true} 如果请求来源有效, 否则{@code false}
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
	 * 根据{@code Origin}, {@code Host}, {@code Forwarded}, {@code X-Forwarded-Proto},
	 * {@code X-Forwarded-Host}和{@code X-Forwarded-Port} header检查请求是否是相同来源的请求.
	 * <p><strong>Note:</strong>此方法可以使用"Forwarded"
	 * (<a href="http://tools.ietf.org/html/rfc7239">RFC 7239</a>),
	 * "X-Forwarded-Host", "X-Forwarded-Port", and "X-Forwarded-Proto" header中的值,
	 * 以反映客户端发起的地址.
	 * 考虑使用{@code ForwardedHeaderFilter}从中心位置选择是否提取和使用, 或丢弃此header.
	 * 有关此过滤器的更多信息, 请参阅Spring Framework参考.
	 * 
	 * @return {@code true} 如果请求是相同来源的请求, {@code false} 如果是跨源请求
	 */
	public static boolean isSameOrigin(HttpRequest request) {
		String origin = request.getHeaders().getOrigin();
		if (origin == null) {
			return true;
		}
		UriComponentsBuilder urlBuilder;
		if (request instanceof ServletServerHttpRequest) {
			// 如果能够更有效地构建: 只需要 scheme, host, port进行来源比较
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
