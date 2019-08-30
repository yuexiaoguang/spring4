package org.springframework.web.portlet.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.PortletContext;
import javax.portlet.PortletException;
import javax.portlet.PortletRequest;
import javax.portlet.PortletRequestDispatcher;
import javax.portlet.PortletResponse;
import javax.portlet.PortletSession;
import javax.portlet.ResourceRequest;
import javax.portlet.ResourceResponse;
import javax.portlet.filter.PortletRequestWrapper;
import javax.portlet.filter.PortletResponseWrapper;
import javax.servlet.http.Cookie;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.util.WebUtils;

/**
 * 用于portlet应用程序的其他工具.
 * 由各种框架类使用.
 */
public abstract class PortletUtils {

	/**
	 * 返回当前Web应用程序的临时目录, 由portlet容器提供.
	 * 
	 * @param portletContext Web应用程序的portlet上下文
	 * 
	 * @return 表示临时目录的File
	 */
	public static File getTempDir(PortletContext portletContext) {
		Assert.notNull(portletContext, "PortletContext must not be null");
		return (File) portletContext.getAttribute(WebUtils.TEMP_DIR_CONTEXT_ATTRIBUTE);
	}

	/**
	 * 返回Web应用程序中给定路径的实际路径, 由portlet容器提供.
	 * <p>如果路径尚未以斜杠开头, 则预先设置斜杠, 如果路径无法解析为资源, 则抛出{@link java.io.FileNotFoundException}
	 * (与{@link javax.portlet.PortletContext#getRealPath PortletContext的 {@code getRealPath}}相反, 它只返回{@code null}).
	 * 
	 * @param portletContext Web应用程序的portlet上下文
	 * @param path Web应用程序中的相对路径
	 * 
	 * @return 相应的真实路径
	 * @throws FileNotFoundException 如果路径无法解析为资源
	 */
	public static String getRealPath(PortletContext portletContext, String path) throws FileNotFoundException {
		Assert.notNull(portletContext, "PortletContext must not be null");
		// 将位置解释为相对于Web应用程序根目录.
		if (!path.startsWith("/")) {
			path = "/" + path;
		}
		String realPath = portletContext.getRealPath(path);
		if (realPath == null) {
			throw new FileNotFoundException(
					"PortletContext resource [" + path + "] cannot be resolved to absolute file path - " +
					"web application archive not expanded?");
		}
		return realPath;
	}


	/**
	 * 在{@link javax.portlet.PortletSession#PORTLET_SCOPE}下检查给定请求中是否有给定名称的会话属性.
	 * 如果没有会话或者会话在该范围内没有此类属性, 则返回{@code null}.
	 * 如果之前不存在, 则不会创建新会话!
	 * 
	 * @param request 当前的portlet请求
	 * @param name 会话属性的名称
	 * 
	 * @return 会话属性的值, 或{@code null}
	 */
	public static Object getSessionAttribute(PortletRequest request, String name) {
		return getSessionAttribute(request, name, PortletSession.PORTLET_SCOPE);
	}

	/**
	 * 检查给定范围内给定请求中给定名称的会话属性.
	 * 如果没有会话或者会话在该范围内没有此类属性, 则返回{@code null}.
	 * 如果之前不存在, 则不会创建新会话!
	 * 
	 * @param request 当前的portlet请求
	 * @param name 会话属性的名称
	 * @param scope 此属性的会话范围
	 * 
	 * @return 会话属性的值, 或{@code null}
	 */
	public static Object getSessionAttribute(PortletRequest request, String name, int scope) {
		Assert.notNull(request, "Request must not be null");
		PortletSession session = request.getPortletSession(false);
		return (session != null ? session.getAttribute(name, scope) : null);
	}

	/**
	 * 在{@link javax.portlet.PortletSession#PORTLET_SCOPE}下检查给定请求中是否有给定名称的会话属性.
	 * 如果没有会话或会话在该范围内没有此类属性, 则引发异常.
	 * <p>如果之前不存在, 则不会创建新会话!
	 * 
	 * @param request 当前的portlet请求
	 * @param name 会话属性的名称
	 * 
	 * @return 会话属性的值
	 * @throws IllegalStateException 如果找不到会话属性
	 */
	public static Object getRequiredSessionAttribute(PortletRequest request, String name)
			throws IllegalStateException {

		return getRequiredSessionAttribute(request, name, PortletSession.PORTLET_SCOPE);
	}

	/**
	 * 检查给定范围内给定请求中给定名称的会话属性.
	 * 如果没有会话或会话在该范围内没有此类属性, 则引发异常.
	 * <p>如果之前不存在, 则不会创建新会话!
	 * 
	 * @param request 当前的portlet请求
	 * @param name 会话属性的名称
	 * @param scope 此属性的会话范围
	 * 
	 * @return 会话属性的值
	 * @throws IllegalStateException 如果找不到会话属性
	 */
	public static Object getRequiredSessionAttribute(PortletRequest request, String name, int scope)
			throws IllegalStateException {
		Object attr = getSessionAttribute(request, name, scope);
		if (attr == null) {
			throw new IllegalStateException("No session attribute '" + name + "' found");
		}
		return attr;
	}

	/**
	 * 在{@link javax.portlet.PortletSession#PORTLET_SCOPE}下将具有给定名称的session属性设置为给定值.
	 * 如果值为{@code null}, 则会删除会话属性, 如果会话存在的话.
	 * 如果没有必要, 不会创建新会话!
	 * 
	 * @param request 当前的portlet请求
	 * @param name 会话属性的名称
	 * @param value session属性的值
	 */
	public static void setSessionAttribute(PortletRequest request, String name, Object value) {
		setSessionAttribute(request, name, value, PortletSession.PORTLET_SCOPE);
	}

	/**
	 * 在给定范围中将具有给定名称的session属性设置为给定的值.
	 * 如果值为{@code null}, 则会删除会话属性, 如果会话存在的话.
	 * 如果没有必要, 不会创建新会话!
	 * 
	 * @param request 当前的portlet请求
	 * @param name 会话属性的名称
	 * @param value session属性的值
	 * @param scope 此属性的会话范围
	 */
	public static void setSessionAttribute(PortletRequest request, String name, Object value, int scope) {
		Assert.notNull(request, "Request must not be null");
		if (value != null) {
			request.getPortletSession().setAttribute(name, value, scope);
		}
		else {
			PortletSession session = request.getPortletSession(false);
			if (session != null) {
				session.removeAttribute(name, scope);
			}
		}
	}

	/**
	 * 获取{@link javax.portlet.PortletSession#PORTLET_SCOPE}下的指定会话属性, 如果找不到现有属性, 则创建并设置新属性.
	 * 给定的类需要有一个公共的无参构造函数.
	 * 对于Web层中的按需状态对象(如购物车)非常有用.
	 * 
	 * @param session 当前的portlet会话
	 * @param name 会话属性的名称
	 * @param clazz 要为新属性实例化的类
	 * 
	 * @return 会话属性的值, 如果未找到则新创建
	 * @throws IllegalArgumentException 如果无法实例化会话属性
	 */
	public static Object getOrCreateSessionAttribute(PortletSession session, String name, Class<?> clazz)
			throws IllegalArgumentException {

		return getOrCreateSessionAttribute(session, name, clazz, PortletSession.PORTLET_SCOPE);
	}

	/**
	 * 获取指定范围内的指定的会话属性, 如果找不到现有属性, 则创建并设置新属性.
	 * 给定的类需要有一个公共的无参构造函数.
	 * 对于Web层中的按需状态对象(如购物车)非常有用.
	 * 
	 * @param session 当前的portlet会话
	 * @param name 会话属性的名称
	 * @param clazz 要为新属性实例化的类
	 * @param scope 此属性的会话范围
	 * 
	 * @return 会话属性的值, 如果未找到则新创建
	 * @throws IllegalArgumentException 如果无法实例化会话属性
	 */
	public static Object getOrCreateSessionAttribute(PortletSession session, String name, Class<?> clazz, int scope)
			throws IllegalArgumentException {

		Assert.notNull(session, "Session must not be null");
		Object sessionObject = session.getAttribute(name, scope);
		if (sessionObject == null) {
			Assert.notNull(clazz, "Class must not be null if attribute value is to be instantiated");
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
			session.setAttribute(name, sessionObject, scope);
		}
		return sessionObject;
	}

	/**
	 * 返回给定会话的最佳可用互斥锁: 即, 为给定会话同步的对象.
	 * <p>返回会话互斥锁属性; 通常, 这意味着需要在{@code web.xml}中定义{@link org.springframework.web.util.HttpSessionMutexListener}.
	 * 如果没有找到互斥锁属性, 则回退到{@link javax.portlet.PortletSession}本身.
	 * <p>在会话的整个生命周期内, 会话互斥锁保证是同一个对象,
	 * 可在{@link org.springframework.web.util.WebUtils#SESSION_MUTEX_ATTRIBUTE}常量定义的键下使用.
	 * 它用作锁定当前会话的同步的安全引用.
	 * <p>在许多情况下, {@link javax.portlet.PortletSession}引用本身也是一个安全的互斥锁, 因为它始终是同一个活动逻辑会话的相同对象引用.
	 * 但是, 不能在不同的servlet容器中保证这一点; 唯一 100% 安全的方式是会话互斥锁.
	 * 
	 * @param session 要查找互斥锁的HttpSession
	 * 
	 * @return 互斥锁对象 (never {@code null})
	 */
	public static Object getSessionMutex(PortletSession session) {
		Assert.notNull(session, "Session must not be null");
		Object mutex = session.getAttribute(WebUtils.SESSION_MUTEX_ATTRIBUTE, PortletSession.APPLICATION_SCOPE);
		if (mutex == null) {
			mutex = session;
		}
		return mutex;
	}


	/**
	 * 返回指定类型的适当请求对象, 根据需要解包给定请求.
	 * 
	 * @param request 要内省的portlet请求
	 * @param requiredType 请求对象的所需类型
	 * 
	 * @return 匹配的请求对象, 或{@code null}
	 */
	@SuppressWarnings("unchecked")
	public static <T> T getNativeRequest(PortletRequest request, Class<T> requiredType) {
		if (requiredType != null) {
			if (requiredType.isInstance(request)) {
				return (T) request;
			}
			else if (request instanceof PortletRequestWrapper) {
				return getNativeRequest(((PortletRequestWrapper) request).getRequest(), requiredType);
			}
		}
		return null;
	}

	/**
	 * 返回指定类型的适当响应对象, 根据需要解包给定的响应.
	 * 
	 * @param response 要响应的portlet反应
	 * @param requiredType 响应对象的所需类型
	 * 
	 * @return 匹配的响应对象, 或{@code null}
	 */
	@SuppressWarnings("unchecked")
	public static <T> T getNativeResponse(PortletResponse response, Class<T> requiredType) {
		if (requiredType != null) {
			if (requiredType.isInstance(response)) {
				return (T) response;
			}
			else if (response instanceof PortletResponseWrapper) {
				return getNativeResponse(((PortletResponseWrapper) response).getResponse(), requiredType);
			}
		}
		return null;
	}

	/**
	 * 将给定的Map公开为请求属性, 使用键作为属性名称, 将值作为相应的属性值.
	 * 键必须是String.
	 * 
	 * @param request 当前的portlet请求
	 * @param attributes 属性Map
	 */
	public static void exposeRequestAttributes(PortletRequest request, Map<String, ?> attributes) {
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
	 * @param request 当前的portlet请求
	 * @param name cookie名称
	 * 
	 * @return 第一个具有给定名称的cookie, 或{@code null}
	 */
	public static Cookie getCookie(PortletRequest request, String name) {
		Assert.notNull(request, "Request must not be null");
		Cookie cookies[] = request.getCookies();
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
	 * 检查请求中是否通过按钮(直接使用名称)或通过图像(名称 + ".x" 或名称 + ".y")发送了特定 input type="submit"参数.
	 * 
	 * @param request 当前的portlet请求
	 * @param name 参数的名称
	 * 
	 * @return 如果参数已发送
	 */
	public static boolean hasSubmitParameter(PortletRequest request, String name) {
		return getSubmitParameter(request, name) != null;
	}

	/**
	 * 返回特定input type="submit"参数的全名, 如果在请求中发送了该参数, 通过按钮(直接使用名称)或通过图像(名称 + ".x" 或名称 + ".y").
	 * 
	 * @param request 当前的portlet请求
	 * @param name 参数的名称
	 * 
	 * @return 带有后缀的实际参数名称, 或null
	 */
	public static String getSubmitParameter(PortletRequest request, String name) {
		Assert.notNull(request, "Request must not be null");
		if (request.getParameter(name) != null) {
			return name;
		}
		for (int i = 0; i < WebUtils.SUBMIT_IMAGE_SUFFIXES.length; i++) {
			String suffix = WebUtils.SUBMIT_IMAGE_SUFFIXES[i];
			String parameter = name + suffix;
			if (request.getParameter(parameter) != null) {
				return parameter;
			}
		}
		return null;
	}

	/**
	 * 返回包含具有给定前缀的所有参数.
	 * 将单个值作为String, 将多个值作为String数组.
	 * <p>例如, 使用前缀"spring_", "spring_param1"和"spring_param2"会产生一个带有"param1"和"param2"作为键的Map.
	 * <p>类似于portlet {@link javax.portlet.PortletRequest#getParameterMap()}, 但更灵活.
	 * 
	 * @param request 要查找参数的portlet请求
	 * @param prefix 参数名称的开头 (如果这是{@code null}或空字符串, 则所有参数都匹配)
	 * 
	 * @return 包含请求参数<b>不带前缀</b>的Map, 包含String或String数组作为值
	 */
	public static Map<String, Object> getParametersStartingWith(PortletRequest request, String prefix) {
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
	 * @param request 当前的portlet请求
	 * @param paramPrefix 要检查的参数前缀 (e.g. "_target"参数, 如"_target1" 或 "_target2")
	 * @param currentPage 当前页面, 如果没有指定目标页面, 则作为后备返回
	 * 
	 * @return 请求中指定的页面, 如果未找到, 则为当前页面
	 */
	public static int getTargetPage(PortletRequest request, String paramPrefix, int currentPage) {
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
	 * 通过将所有操作请求参数放入操作响应对象, 将其传递到渲染阶段.
	 * 当操作调用{@link javax.portlet.ActionResponse#sendRedirect sendRedirect}时, 可能无法调用此方法.
	 * 
	 * @param request 当前的操作请求
	 * @param response 当前的操作响应
	 */
	public static void passAllParametersToRenderPhase(ActionRequest request, ActionResponse response) {
		try {
			Enumeration<String> en = request.getParameterNames();
			while (en.hasMoreElements()) {
				String param = en.nextElement();
				String values[] = request.getParameterValues(param);
				response.setRenderParameter(param, values);
			}
		}
		catch (IllegalStateException ex) {
			// Ignore in case sendRedirect was already set.
		}
	}

	/**
	 * 清除{@link javax.portlet.ActionResponse}中的所有渲染参数.
	 * 当操作调用{@link ActionResponse#sendRedirect sendRedirect}时, 可能无法调用此方法.
	 * 
	 * @param response 当前的操作响应
	 */
	public static void clearAllRenderParameters(ActionResponse response) {
		try {
			response.setRenderParameters(new HashMap<String, String[]>(0));
		}
		catch (IllegalStateException ex) {
			// Ignore in case sendRedirect was already set.
		}
	}

	/**
	 * 使用PortletContext的请求调度器将给定请求中指定的资源提供给给定的响应.
	 * <p>这大致相当于Portlet 2.0 GenericPortlet.
	 * 
	 * @param request 当前的资源请求
	 * @param response 当前的资源响应
	 * @param context 当前Portlet的PortletContext
	 * 
	 * @throws PortletException 从Portlet API的forward方法传播
	 * @throws IOException 从Portlet API的forward方法传播
	 */
	public static void serveResource(ResourceRequest request, ResourceResponse response, PortletContext context)
			throws PortletException, IOException {

		String id = request.getResourceID();
		if (id != null) {
			if (!PortletUtils.isProtectedResource(id)) {
				PortletRequestDispatcher rd = context.getRequestDispatcher(id);
				if (rd != null) {
					rd.forward(request, response);
					return;
				}
			}
			response.setProperty(ResourceResponse.HTTP_STATUS_CODE, "404");
		}
	}

	/**
	 * 检查指定的路径是否指示受保护的WEB-INF或META-INF目录中的资源.
	 * 
	 * @param path 要检查的路径
	 */
	private static boolean isProtectedResource(String path) {
		return (StringUtils.startsWithIgnoreCase(path, "/WEB-INF") ||
				StringUtils.startsWithIgnoreCase(path, "/META-INF"));
	}

}
