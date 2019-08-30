package org.springframework.web.portlet.context;

import java.io.Serializable;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import javax.portlet.PortletConfig;
import javax.portlet.PortletContext;
import javax.portlet.PortletRequest;
import javax.portlet.PortletResponse;
import javax.portlet.PortletSession;
import javax.servlet.ServletContext;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.util.Assert;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.RequestScope;
import org.springframework.web.context.request.SessionScope;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.context.support.WebApplicationContextUtils;

/**
 * 检索给定{@link PortletContext}的根{@link WebApplicationContext}的便捷方法.
 * 这对于从自定义Portlet实现中以编程方式访问Spring应用程序上下文非常有用.
 */
public abstract class PortletApplicationContextUtils {

	/**
	 * 查找此Web应用程序的根{@link WebApplicationContext}, 通常通过{@link org.springframework.web.context.ContextLoaderListener}加载.
	 * <p>将重新抛出在根上下文启动时发生的异常, 以区分失败的上下文启动和根本没有上下文.
	 * 
	 * @param pc 用于查找Web应用程序上下文的PortletContext
	 * 
	 * @return 此Web应用程序的根WebApplicationContext, 或{@code null}
	 * (转换为ApplicationContext以避免Servlet API依赖; 通常可以转换为WebApplicationContext, 但不需要)
	 */
	public static ApplicationContext getWebApplicationContext(PortletContext pc) {
		Assert.notNull(pc, "PortletContext must not be null");
		Object attr = pc.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);
		if (attr == null) {
			return null;
		}
		if (attr instanceof RuntimeException) {
			throw (RuntimeException) attr;
		}
		if (attr instanceof Error) {
			throw (Error) attr;
		}
		if (!(attr instanceof ApplicationContext)) {
			throw new IllegalStateException("Root context attribute is not of type WebApplicationContext: " + attr);
		}
		return (ApplicationContext) attr;
	}

	/**
	 * 查找此Web应用程序的根{@link WebApplicationContext}, 通常通过{@link org.springframework.web.context.ContextLoaderListener}加载.
	 * <p>将重新抛出在根上下文启动时发生的异常, 以区分失败的上下文启动和根本没有上下文.
	 * 
	 * @param pc 用于查找Web应用程序上下文的PortletContext
	 * 
	 * @return 此Web应用程序的根WebApplicationContext
	 * (转换为ApplicationContext以避免Servlet API依赖; 通常可以转换为WebApplicationContext, 但不需要)
	 * @throws IllegalStateException 如果找不到根WebApplicationContext
	 */
	public static ApplicationContext getRequiredWebApplicationContext(PortletContext pc) throws IllegalStateException {
		ApplicationContext wac = getWebApplicationContext(pc);
		if (wac == null) {
			throw new IllegalStateException("No WebApplicationContext found: no ContextLoaderListener registered?");
		}
		return wac;
	}


	/**
	 * 使用的给定BeanFactory注册特定于Web的作用域 ("request", "session", "globalSession"), 由Portlet ApplicationContext使用.
	 * 
	 * @param bf 要配置的BeanFactory
	 * @param pc 正在运行的PortletContext
	 */
	static void registerPortletApplicationScopes(ConfigurableListableBeanFactory bf, PortletContext pc) {
		bf.registerScope(WebApplicationContext.SCOPE_REQUEST, new RequestScope());
		bf.registerScope(WebApplicationContext.SCOPE_SESSION, new SessionScope(false));
		bf.registerScope(WebApplicationContext.SCOPE_GLOBAL_SESSION, new SessionScope(true));
		if (pc != null) {
			PortletContextScope appScope = new PortletContextScope(pc);
			bf.registerScope(WebApplicationContext.SCOPE_APPLICATION, appScope);
			// 注册为PortletContext属性, 以便ContextCleanupListener检测它.
			pc.setAttribute(PortletContextScope.class.getName(), appScope);
		}

		bf.registerResolvableDependency(PortletRequest.class, new RequestObjectFactory());
		bf.registerResolvableDependency(PortletResponse.class, new ResponseObjectFactory());
		bf.registerResolvableDependency(PortletSession.class, new SessionObjectFactory());
		bf.registerResolvableDependency(WebRequest.class, new WebRequestObjectFactory());
	}

	/**
	 * 使用给定BeanFactory注册特定于Web的环境bean ("contextParameters", "contextAttributes"),
	 * 由Portlet ApplicationContext使用.
	 * 
	 * @param bf 要配置的BeanFactory
	 * @param servletContext 正在运行的ServletContext
	 * @param portletContext 正在运行的PortletContext
	 * @param portletConfig 包含Portlet的PortletConfig
	 */
	static void registerEnvironmentBeans(ConfigurableListableBeanFactory bf, ServletContext servletContext,
			PortletContext portletContext, PortletConfig portletConfig) {

		if (servletContext != null && !bf.containsBean(WebApplicationContext.SERVLET_CONTEXT_BEAN_NAME)) {
			bf.registerSingleton(WebApplicationContext.SERVLET_CONTEXT_BEAN_NAME, servletContext);
		}

		if (portletContext != null && !bf.containsBean(ConfigurablePortletApplicationContext.PORTLET_CONTEXT_BEAN_NAME)) {
			bf.registerSingleton(ConfigurablePortletApplicationContext.PORTLET_CONTEXT_BEAN_NAME, portletContext);
		}

		if (portletConfig != null && !bf.containsBean(ConfigurablePortletApplicationContext.PORTLET_CONFIG_BEAN_NAME)) {
			bf.registerSingleton(ConfigurablePortletApplicationContext.PORTLET_CONFIG_BEAN_NAME, portletConfig);
		}

		if (!bf.containsBean(WebApplicationContext.CONTEXT_PARAMETERS_BEAN_NAME)) {
			Map<String, String> parameterMap = new HashMap<String, String>();
			if (portletContext != null) {
				Enumeration<String> paramNameEnum = portletContext.getInitParameterNames();
				while (paramNameEnum.hasMoreElements()) {
					String paramName = paramNameEnum.nextElement();
					parameterMap.put(paramName, portletContext.getInitParameter(paramName));
				}
			}
			if (portletConfig != null) {
				Enumeration<String> paramNameEnum = portletConfig.getInitParameterNames();
				while (paramNameEnum.hasMoreElements()) {
					String paramName = paramNameEnum.nextElement();
					parameterMap.put(paramName, portletConfig.getInitParameter(paramName));
				}
			}
			bf.registerSingleton(WebApplicationContext.CONTEXT_PARAMETERS_BEAN_NAME,
					Collections.unmodifiableMap(parameterMap));
		}

		if (!bf.containsBean(WebApplicationContext.CONTEXT_ATTRIBUTES_BEAN_NAME)) {
			Map<String, Object> attributeMap = new HashMap<String, Object>();
			if (portletContext != null) {
				Enumeration<String> attrNameEnum = portletContext.getAttributeNames();
				while (attrNameEnum.hasMoreElements()) {
					String attrName = attrNameEnum.nextElement();
					attributeMap.put(attrName, portletContext.getAttribute(attrName));
				}
			}
			bf.registerSingleton(WebApplicationContext.CONTEXT_ATTRIBUTES_BEAN_NAME,
					Collections.unmodifiableMap(attributeMap));
		}
	}

	/**
	 * 将基于{@code Servlet}和基于{@code Portlet}的
	 * {@link org.springframework.core.env.PropertySource.StubPropertySource 存根属性源}
	 * 替换为使用给定的{@code servletContext}, {@code portletContext}和{@code portletConfig}对象填充的实际实例.
	 * <p>这个方法是幂等的, 因为它可以被调用任意次, 但是会用它们相应的实际属性源替换存根属性源一次且只能执行一次.
	 * 
	 * @param propertySources 要初始化的{@link MutablePropertySources} (不能是{@code null})
	 * @param servletContext 当前的{@link ServletContext}
	 * (忽略, 如果为{@code null}或者
	 * {@link org.springframework.web.context.support.StandardServletEnvironment#SERVLET_CONTEXT_PROPERTY_SOURCE_NAME servlet上下文属性源}已经初始化)
	 * @param portletContext 当前的{@link PortletContext}
	 * (忽略, 如果为{@code null}或者
	 * {@link StandardPortletEnvironment#PORTLET_CONTEXT_PROPERTY_SOURCE_NAME portlet上下文属性源}已经初始化)
	 * @param portletConfig 当前的{@link PortletConfig}
	 * (忽略, 如果为{@code null}或者
	 * {@link StandardPortletEnvironment#PORTLET_CONFIG_PROPERTY_SOURCE_NAME portlet配置属性源}已经初始化)
	 */
	public static void initPortletPropertySources(MutablePropertySources propertySources, ServletContext servletContext,
			PortletContext portletContext, PortletConfig portletConfig) {

		Assert.notNull(propertySources, "'propertySources' must not be null");
		WebApplicationContextUtils.initServletPropertySources(propertySources, servletContext);

		if (portletContext != null && propertySources.contains(StandardPortletEnvironment.PORTLET_CONTEXT_PROPERTY_SOURCE_NAME)) {
			propertySources.replace(StandardPortletEnvironment.PORTLET_CONTEXT_PROPERTY_SOURCE_NAME,
					new PortletContextPropertySource(StandardPortletEnvironment.PORTLET_CONTEXT_PROPERTY_SOURCE_NAME, portletContext));
		}
		if (portletConfig != null && propertySources.contains(StandardPortletEnvironment.PORTLET_CONFIG_PROPERTY_SOURCE_NAME)) {
			propertySources.replace(StandardPortletEnvironment.PORTLET_CONFIG_PROPERTY_SOURCE_NAME,
					new PortletConfigPropertySource(StandardPortletEnvironment.PORTLET_CONFIG_PROPERTY_SOURCE_NAME, portletConfig));
		}
	}

	/**
	 * 返回当前的RequestAttributes实例.
	 */
	private static PortletRequestAttributes currentRequestAttributes() {
		RequestAttributes requestAttr = RequestContextHolder.currentRequestAttributes();
		if (!(requestAttr instanceof PortletRequestAttributes)) {
			throw new IllegalStateException("Current request is not a portlet request");
		}
		return (PortletRequestAttributes) requestAttr;
	}


	/**
	 * 按需公开当前请求对象的工厂.
	 */
	@SuppressWarnings("serial")
	private static class RequestObjectFactory implements ObjectFactory<PortletRequest>, Serializable {

		@Override
		public PortletRequest getObject() {
			return currentRequestAttributes().getRequest();
		}

		@Override
		public String toString() {
			return "Current PortletRequest";
		}
	}


	/**
	 * 按需公开当前响应对象的工厂.
	 */
	@SuppressWarnings("serial")
	private static class ResponseObjectFactory implements ObjectFactory<PortletResponse>, Serializable {

		@Override
		public PortletResponse getObject() {
			PortletResponse response = currentRequestAttributes().getResponse();
			if (response == null) {
				throw new IllegalStateException("Current portlet response not available");
			}
			return response;
		}

		@Override
		public String toString() {
			return "Current PortletResponse";
		}
	}


	/**
	 * 按需公开当前会话对象的工厂.
	 */
	@SuppressWarnings("serial")
	private static class SessionObjectFactory implements ObjectFactory<PortletSession>, Serializable {

		@Override
		public PortletSession getObject() {
			return currentRequestAttributes().getRequest().getPortletSession();
		}

		@Override
		public String toString() {
			return "Current PortletSession";
		}
	}


	/**
	 * 按需公开当前WebRequest对象的工厂.
	 */
	@SuppressWarnings("serial")
	private static class WebRequestObjectFactory implements ObjectFactory<WebRequest>, Serializable {

		@Override
		public WebRequest getObject() {
			PortletRequestAttributes requestAttr = currentRequestAttributes();
			return new PortletWebRequest(requestAttr.getRequest(), requestAttr.getResponse());
		}

		@Override
		public String toString() {
			return "Current PortletWebRequest";
		}
	}
}
