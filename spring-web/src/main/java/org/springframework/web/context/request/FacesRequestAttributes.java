package org.springframework.web.context.request;

import java.lang.reflect.Method;
import java.util.Map;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.portlet.PortletSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.util.WebUtils;

/**
 * {@link RequestAttributes}适配器, 用于JSF {@link javax.faces.context.FacesContext}.
 * 在JSF环境中用作默认值, 包装当前的FacesContext.
 *
 * <p><b>NOTE:</b> 与{@link ServletRequestAttributes}相反, 此变体<i>不</i>支持范围属性的销毁回调,
 * 既不支持请求范围也不支持会话范围.
 * 如果依赖于此类隐式销毁回调, 考虑在{@code web.xml}中定义Spring {@link RequestContextListener}.
 *
 * <p>从Spring 4.0开始, 需要JSF 2.0或更高版本.
 */
public class FacesRequestAttributes implements RequestAttributes {

	private static final boolean portletApiPresent =
			ClassUtils.isPresent("javax.portlet.PortletSession", FacesRequestAttributes.class.getClassLoader());

	/**
	 * 将创建很多这些对象, 因此不希望每次都有新的记录器.
	 */
	private static final Log logger = LogFactory.getLog(FacesRequestAttributes.class);

	private final FacesContext facesContext;


	/**
	 * @param facesContext 当前FacesContext
	 */
	public FacesRequestAttributes(FacesContext facesContext) {
		Assert.notNull(facesContext, "FacesContext must not be null");
		this.facesContext = facesContext;
	}


	/**
	 * 返回此适配器操作的JSF FacesContext.
	 */
	protected final FacesContext getFacesContext() {
		return this.facesContext;
	}

	/**
	 * 返回此适配器操作的JSF ExternalContext.
	 */
	protected final ExternalContext getExternalContext() {
		return getFacesContext().getExternalContext();
	}

	/**
	 * 返回指定范围的JSF属性Map
	 * 
	 * @param scope 常量指示请求或会话范围
	 * 
	 * @return 指定范围内属性的Map表示
	 */
	protected Map<String, Object> getAttributeMap(int scope) {
		if (scope == SCOPE_REQUEST) {
			return getExternalContext().getRequestMap();
		}
		else {
			return getExternalContext().getSessionMap();
		}
	}


	@Override
	public Object getAttribute(String name, int scope) {
		if (scope == SCOPE_GLOBAL_SESSION && portletApiPresent) {
			return PortletSessionAccessor.getAttribute(name, getExternalContext());
		}
		else {
			return getAttributeMap(scope).get(name);
		}
	}

	@Override
	public void setAttribute(String name, Object value, int scope) {
		if (scope == SCOPE_GLOBAL_SESSION && portletApiPresent) {
			PortletSessionAccessor.setAttribute(name, value, getExternalContext());
		}
		else {
			getAttributeMap(scope).put(name, value);
		}
	}

	@Override
	public void removeAttribute(String name, int scope) {
		if (scope == SCOPE_GLOBAL_SESSION && portletApiPresent) {
			PortletSessionAccessor.removeAttribute(name, getExternalContext());
		}
		else {
			getAttributeMap(scope).remove(name);
		}
	}

	@Override
	public String[] getAttributeNames(int scope) {
		if (scope == SCOPE_GLOBAL_SESSION && portletApiPresent) {
			return PortletSessionAccessor.getAttributeNames(getExternalContext());
		}
		else {
			return StringUtils.toStringArray(getAttributeMap(scope).keySet());
		}
	}

	@Override
	public void registerDestructionCallback(String name, Runnable callback, int scope) {
		if (logger.isWarnEnabled()) {
			logger.warn("Could not register destruction callback [" + callback + "] for attribute '" + name +
					"' because FacesRequestAttributes does not support such callbacks");
		}
	}

	@Override
	public Object resolveReference(String key) {
		if (REFERENCE_REQUEST.equals(key)) {
			return getExternalContext().getRequest();
		}
		else if (REFERENCE_SESSION.equals(key)) {
			return getExternalContext().getSession(true);
		}
		else if ("application".equals(key)) {
			return getExternalContext().getContext();
		}
		else if ("requestScope".equals(key)) {
			return getExternalContext().getRequestMap();
		}
		else if ("sessionScope".equals(key)) {
			return getExternalContext().getSessionMap();
		}
		else if ("applicationScope".equals(key)) {
			return getExternalContext().getApplicationMap();
		}
		else if ("facesContext".equals(key)) {
			return getFacesContext();
		}
		else if ("cookie".equals(key)) {
			return getExternalContext().getRequestCookieMap();
		}
		else if ("header".equals(key)) {
			return getExternalContext().getRequestHeaderMap();
		}
		else if ("headerValues".equals(key)) {
			return getExternalContext().getRequestHeaderValuesMap();
		}
		else if ("param".equals(key)) {
			return getExternalContext().getRequestParameterMap();
		}
		else if ("paramValues".equals(key)) {
			return getExternalContext().getRequestParameterValuesMap();
		}
		else if ("initParam".equals(key)) {
			return getExternalContext().getInitParameterMap();
		}
		else if ("view".equals(key)) {
			return getFacesContext().getViewRoot();
		}
		else if ("viewScope".equals(key)) {
			return getFacesContext().getViewRoot().getViewMap();
		}
		else if ("flash".equals(key)) {
			return getExternalContext().getFlash();
		}
		else if ("resource".equals(key)) {
			return getFacesContext().getApplication().getResourceHandler();
		}
		else {
			return null;
		}
	}

	@Override
	public String getSessionId() {
		Object session = getExternalContext().getSession(true);
		try {
			// HttpSession 和 PortletSession都有一个getId()方法.
			Method getIdMethod = session.getClass().getMethod("getId");
			return ReflectionUtils.invokeMethod(getIdMethod, session).toString();
		}
		catch (NoSuchMethodException ex) {
			throw new IllegalStateException("Session object [" + session + "] does not have a getId() method");
		}
	}

	@Override
	public Object getSessionMutex() {
		// 首先会话必须存在, 以允许监听器创建互斥属性
		ExternalContext externalContext = getExternalContext();
		Object session = externalContext.getSession(true);
		Object mutex = externalContext.getSessionMap().get(WebUtils.SESSION_MUTEX_ATTRIBUTE);
		if (mutex == null) {
			mutex = (session != null ? session : externalContext);
		}
		return mutex;
	}


	/**
	 * 内部类, 以避免硬编码Portlet API依赖.
 	 */
	private static class PortletSessionAccessor {

		public static Object getAttribute(String name, ExternalContext externalContext) {
			Object session = externalContext.getSession(false);
			if (session instanceof PortletSession) {
				return ((PortletSession) session).getAttribute(name, PortletSession.APPLICATION_SCOPE);
			}
			else if (session != null) {
				return externalContext.getSessionMap().get(name);
			}
			else {
				return null;
			}
		}

		public static void setAttribute(String name, Object value, ExternalContext externalContext) {
			Object session = externalContext.getSession(true);
			if (session instanceof PortletSession) {
				((PortletSession) session).setAttribute(name, value, PortletSession.APPLICATION_SCOPE);
			}
			else {
				externalContext.getSessionMap().put(name, value);
			}
		}

		public static void removeAttribute(String name, ExternalContext externalContext) {
			Object session = externalContext.getSession(false);
			if (session instanceof PortletSession) {
				((PortletSession) session).removeAttribute(name, PortletSession.APPLICATION_SCOPE);
			}
			else if (session != null) {
				externalContext.getSessionMap().remove(name);
			}
		}

		public static String[] getAttributeNames(ExternalContext externalContext) {
			Object session = externalContext.getSession(false);
			if (session instanceof PortletSession) {
				return StringUtils.toStringArray(
						((PortletSession) session).getAttributeNames(PortletSession.APPLICATION_SCOPE));
			}
			else if (session != null) {
				return StringUtils.toStringArray(externalContext.getSessionMap().keySet());
			}
			else {
				return new String[0];
			}
		}
	}
}
