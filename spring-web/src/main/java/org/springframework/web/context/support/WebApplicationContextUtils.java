package org.springframework.web.context.support;

import java.io.Serializable;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource.StubPropertySource;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.web.context.ConfigurableWebApplicationContext;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.RequestScope;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.SessionScope;
import org.springframework.web.context.request.WebRequest;

/**
 * 检索给定{@link ServletContext}的根{@link WebApplicationContext}的便捷方法.
 * 这对于从自定义Web视图或MVC操作中以编程方式访问Spring应用程序上下文非常有用.
 *
 * <p>请注意, 有许多方便的方法可以访问许多Web框架的根上下文, 无论是Spring的一部分还是外部库.
 * 这个帮助器类只是访问根上下文的最通用方法.
 */
public abstract class WebApplicationContextUtils {

	private static final boolean jsfPresent =
			ClassUtils.isPresent("javax.faces.context.FacesContext", RequestContextHolder.class.getClassLoader());


	/**
	 * 查找此Web应用程序的根{@code WebApplicationContext},
	 * 通常通过{@link org.springframework.web.context.ContextLoaderListener}加载.
	 * <p>将重新抛出在根上下文启动时发生的异常, 以区分失败的上下文启动和根本没有上下文.
	 * 
	 * @param sc 查找Web应用程序上下文的ServletContext
	 * 
	 * @return 此Web应用程序的根WebApplicationContext
	 * @throws IllegalStateException 如果找不到根WebApplicationContext
	 */
	public static WebApplicationContext getRequiredWebApplicationContext(ServletContext sc) throws IllegalStateException {
		WebApplicationContext wac = getWebApplicationContext(sc);
		if (wac == null) {
			throw new IllegalStateException("No WebApplicationContext found: no ContextLoaderListener registered?");
		}
		return wac;
	}

	/**
	 * 查找此Web应用程序的根{@code WebApplicationContext},
	 * 通常通过{@link org.springframework.web.context.ContextLoaderListener}加载.
	 * <p>将重新抛出在根上下文启动时发生的异常, 以区分失败的上下文启动和根本没有上下文.
	 * 
	 * @param sc 查找Web应用程序上下文的ServletContext
	 * 
	 * @return 此Web应用程序的根WebApplicationContext, 或{@code null}
	 */
	public static WebApplicationContext getWebApplicationContext(ServletContext sc) {
		return getWebApplicationContext(sc, WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);
	}

	/**
	 * 为此Web应用程序查找自定义{@code WebApplicationContext}.
	 * 
	 * @param sc 查找Web应用程序上下文的ServletContext
	 * @param attrName 要查找的ServletContext属性的名称
	 * 
	 * @return 此Web应用程序所需的WebApplicationContext, 或{@code null}
	 */
	public static WebApplicationContext getWebApplicationContext(ServletContext sc, String attrName) {
		Assert.notNull(sc, "ServletContext must not be null");
		Object attr = sc.getAttribute(attrName);
		if (attr == null) {
			return null;
		}
		if (attr instanceof RuntimeException) {
			throw (RuntimeException) attr;
		}
		if (attr instanceof Error) {
			throw (Error) attr;
		}
		if (attr instanceof Exception) {
			throw new IllegalStateException((Exception) attr);
		}
		if (!(attr instanceof WebApplicationContext)) {
			throw new IllegalStateException("Context attribute is not of type WebApplicationContext: " + attr);
		}
		return (WebApplicationContext) attr;
	}

	/**
	 * 为此Web应用程序查找唯一的{@code WebApplicationContext}:
	 * 注册的{@code ServletContext}属性中的根Web应用程序上下文(首选)或唯一的{@code WebApplicationContext}
	 * (通常来自当前Web应用程序中的单个{@code DispatcherServlet}).
	 * <p>请注意, {@code DispatcherServlet}对其上下文的暴露可以通过其{@code publishContext}属性进行控制,
	 * 默认为{@code true}, 但可以选择性地切换为仅发布单个上下文, 尽管Web应用程序中注册了多个{@code DispatcherServlet}.
	 * 
	 * @param sc 查找Web应用程序上下文的ServletContext
	 * 
	 * @return 此Web应用程序所需的WebApplicationContext, 或{@code null}
	 */
	public static WebApplicationContext findWebApplicationContext(ServletContext sc) {
		WebApplicationContext wac = getWebApplicationContext(sc);
		if (wac == null) {
			Enumeration<String> attrNames = sc.getAttributeNames();
			while (attrNames.hasMoreElements()) {
				String attrName = attrNames.nextElement();
				Object attrValue = sc.getAttribute(attrName);
				if (attrValue instanceof WebApplicationContext) {
					if (wac != null) {
						throw new IllegalStateException("No unique WebApplicationContext found: more than one " +
								"DispatcherServlet registered with publishContext=true?");
					}
					wac = (WebApplicationContext) attrValue;
				}
			}
		}
		return wac;
	}


	/**
	 * 使用给定BeanFactory注册特定于Web的作用域 ("request", "session", "globalSession"), 如WebApplicationContext所使用的那样.
	 * 
	 * @param beanFactory 要配置的BeanFactory
	 */
	public static void registerWebApplicationScopes(ConfigurableListableBeanFactory beanFactory) {
		registerWebApplicationScopes(beanFactory, null);
	}

	/**
	 * 使用给定BeanFactory注册特定于Web的作用域 ("request", "session", "globalSession", "application"),
	 * 如WebApplicationContext所使用的那样.
	 * 
	 * @param beanFactory 要配置的BeanFactory
	 * @param sc 正在运行的ServletContext
	 */
	public static void registerWebApplicationScopes(ConfigurableListableBeanFactory beanFactory, ServletContext sc) {
		beanFactory.registerScope(WebApplicationContext.SCOPE_REQUEST, new RequestScope());
		beanFactory.registerScope(WebApplicationContext.SCOPE_SESSION, new SessionScope(false));
		beanFactory.registerScope(WebApplicationContext.SCOPE_GLOBAL_SESSION, new SessionScope(true));
		if (sc != null) {
			ServletContextScope appScope = new ServletContextScope(sc);
			beanFactory.registerScope(WebApplicationContext.SCOPE_APPLICATION, appScope);
			// 注册为ServletContext属性, 用于ContextCleanupListener来检测它.
			sc.setAttribute(ServletContextScope.class.getName(), appScope);
		}

		beanFactory.registerResolvableDependency(ServletRequest.class, new RequestObjectFactory());
		beanFactory.registerResolvableDependency(ServletResponse.class, new ResponseObjectFactory());
		beanFactory.registerResolvableDependency(HttpSession.class, new SessionObjectFactory());
		beanFactory.registerResolvableDependency(WebRequest.class, new WebRequestObjectFactory());
		if (jsfPresent) {
			FacesDependencyRegistrar.registerFacesDependencies(beanFactory);
		}
	}

	/**
	 * 使用给定的BeanFactory注册特定于Web的环境bean ("contextParameters", "contextAttributes"),
	 * 如WebApplicationContext所使用的那样.
	 * 
	 * @param bf 要配置的BeanFactory
	 * @param sc 正在运行的ServletContext
	 */
	public static void registerEnvironmentBeans(ConfigurableListableBeanFactory bf, ServletContext sc) {
		registerEnvironmentBeans(bf, sc, null);
	}

	/**
	 * 使用给定的BeanFactory注册特定于Web的环境bean ("contextParameters", "contextAttributes"),
	 * 如WebApplicationContext所使用的那样.
	 * 
	 * @param bf 要配置的BeanFactory
	 * @param servletContext 正在运行的ServletContext
	 * @param servletConfig 包含Portlet的ServletConfig
	 */
	public static void registerEnvironmentBeans(
			ConfigurableListableBeanFactory bf, ServletContext servletContext, ServletConfig servletConfig) {

		if (servletContext != null && !bf.containsBean(WebApplicationContext.SERVLET_CONTEXT_BEAN_NAME)) {
			bf.registerSingleton(WebApplicationContext.SERVLET_CONTEXT_BEAN_NAME, servletContext);
		}

		if (servletConfig != null && !bf.containsBean(ConfigurableWebApplicationContext.SERVLET_CONFIG_BEAN_NAME)) {
			bf.registerSingleton(ConfigurableWebApplicationContext.SERVLET_CONFIG_BEAN_NAME, servletConfig);
		}

		if (!bf.containsBean(WebApplicationContext.CONTEXT_PARAMETERS_BEAN_NAME)) {
			Map<String, String> parameterMap = new HashMap<String, String>();
			if (servletContext != null) {
				Enumeration<?> paramNameEnum = servletContext.getInitParameterNames();
				while (paramNameEnum.hasMoreElements()) {
					String paramName = (String) paramNameEnum.nextElement();
					parameterMap.put(paramName, servletContext.getInitParameter(paramName));
				}
			}
			if (servletConfig != null) {
				Enumeration<?> paramNameEnum = servletConfig.getInitParameterNames();
				while (paramNameEnum.hasMoreElements()) {
					String paramName = (String) paramNameEnum.nextElement();
					parameterMap.put(paramName, servletConfig.getInitParameter(paramName));
				}
			}
			bf.registerSingleton(WebApplicationContext.CONTEXT_PARAMETERS_BEAN_NAME,
					Collections.unmodifiableMap(parameterMap));
		}

		if (!bf.containsBean(WebApplicationContext.CONTEXT_ATTRIBUTES_BEAN_NAME)) {
			Map<String, Object> attributeMap = new HashMap<String, Object>();
			if (servletContext != null) {
				Enumeration<?> attrNameEnum = servletContext.getAttributeNames();
				while (attrNameEnum.hasMoreElements()) {
					String attrName = (String) attrNameEnum.nextElement();
					attributeMap.put(attrName, servletContext.getAttribute(attrName));
				}
			}
			bf.registerSingleton(WebApplicationContext.CONTEXT_ATTRIBUTES_BEAN_NAME,
					Collections.unmodifiableMap(attributeMap));
		}
	}

	/**
	 * {@link #initServletPropertySources(MutablePropertySources, ServletContext, ServletConfig)}的便捷变体,
	 * 它始终为{@link ServletConfig}参数提供{@code null}.
	 */
	public static void initServletPropertySources(MutablePropertySources propertySources, ServletContext servletContext) {
		initServletPropertySources(propertySources, servletContext, null);
	}

	/**
	 * 将基于{@code Servlet}的{@link StubPropertySource 存根属性源}替换为
	 * 使用给定的{@code servletContext}和{@code servletConfig}对象填充的实际实例.
	 * <p>这个方法是幂等的, 因为它可以被调用任意次, 但是会用它们相应的实际属性源替换存根属性源一次且仅有一次.
	 * 
	 * @param propertySources 要初始化的{@link MutablePropertySources} (不能是{@code null})
	 * @param servletContext 当前的{@link ServletContext}
	 * (如果为{@code null}, 或者如果{@link StandardServletEnvironment#SERVLET_CONTEXT_PROPERTY_SOURCE_NAME servlet上下文属性源}已经初始化, 则忽略)
	 * @param servletConfig 当前的{@link ServletConfig}
	 * (如果为{@code null}, 或者如果{@link StandardServletEnvironment#SERVLET_CONFIG_PROPERTY_SOURCE_NAME servlet上下文属性源}已经初始化, 则忽略)
	 */
	public static void initServletPropertySources(
			MutablePropertySources propertySources, ServletContext servletContext, ServletConfig servletConfig) {

		Assert.notNull(propertySources, "'propertySources' must not be null");
		if (servletContext != null && propertySources.contains(StandardServletEnvironment.SERVLET_CONTEXT_PROPERTY_SOURCE_NAME) &&
				propertySources.get(StandardServletEnvironment.SERVLET_CONTEXT_PROPERTY_SOURCE_NAME) instanceof StubPropertySource) {
			propertySources.replace(StandardServletEnvironment.SERVLET_CONTEXT_PROPERTY_SOURCE_NAME,
					new ServletContextPropertySource(StandardServletEnvironment.SERVLET_CONTEXT_PROPERTY_SOURCE_NAME, servletContext));
		}
		if (servletConfig != null && propertySources.contains(StandardServletEnvironment.SERVLET_CONFIG_PROPERTY_SOURCE_NAME) &&
				propertySources.get(StandardServletEnvironment.SERVLET_CONFIG_PROPERTY_SOURCE_NAME) instanceof StubPropertySource) {
			propertySources.replace(StandardServletEnvironment.SERVLET_CONFIG_PROPERTY_SOURCE_NAME,
					new ServletConfigPropertySource(StandardServletEnvironment.SERVLET_CONFIG_PROPERTY_SOURCE_NAME, servletConfig));
		}
	}

	/**
	 * 返回当前的RequestAttributes实例.
	 */
	private static ServletRequestAttributes currentRequestAttributes() {
		RequestAttributes requestAttr = RequestContextHolder.currentRequestAttributes();
		if (!(requestAttr instanceof ServletRequestAttributes)) {
			throw new IllegalStateException("Current request is not a servlet request");
		}
		return (ServletRequestAttributes) requestAttr;
	}


	/**
	 * 按需公开当前请求对象的工厂.
	 */
	@SuppressWarnings("serial")
	private static class RequestObjectFactory implements ObjectFactory<ServletRequest>, Serializable {

		@Override
		public ServletRequest getObject() {
			return currentRequestAttributes().getRequest();
		}

		@Override
		public String toString() {
			return "Current HttpServletRequest";
		}
	}


	/**
	 * 按需公开当前响应对象的工厂.
	 */
	@SuppressWarnings("serial")
	private static class ResponseObjectFactory implements ObjectFactory<ServletResponse>, Serializable {

		@Override
		public ServletResponse getObject() {
			ServletResponse response = currentRequestAttributes().getResponse();
			if (response == null) {
				throw new IllegalStateException("Current servlet response not available - " +
						"consider using RequestContextFilter instead of RequestContextListener");
			}
			return response;
		}

		@Override
		public String toString() {
			return "Current HttpServletResponse";
		}
	}


	/**
	 * 按需公开当前会话对象的工厂.
	 */
	@SuppressWarnings("serial")
	private static class SessionObjectFactory implements ObjectFactory<HttpSession>, Serializable {

		@Override
		public HttpSession getObject() {
			return currentRequestAttributes().getRequest().getSession();
		}

		@Override
		public String toString() {
			return "Current HttpSession";
		}
	}


	/**
	 * 根据需要公开当前WebRequest对象的工厂.
	 */
	@SuppressWarnings("serial")
	private static class WebRequestObjectFactory implements ObjectFactory<WebRequest>, Serializable {

		@Override
		public WebRequest getObject() {
			ServletRequestAttributes requestAttr = currentRequestAttributes();
			return new ServletWebRequest(requestAttr.getRequest(), requestAttr.getResponse());
		}

		@Override
		public String toString() {
			return "Current ServletWebRequest";
		}
	}


	/**
	 * 内部类, 以避免硬编码的JSF依赖.
 	 */
	private static class FacesDependencyRegistrar {

		public static void registerFacesDependencies(ConfigurableListableBeanFactory beanFactory) {
			beanFactory.registerResolvableDependency(FacesContext.class, new ObjectFactory<FacesContext>() {
				@Override
				public FacesContext getObject() {
					return FacesContext.getCurrentInstance();
				}
				@Override
				public String toString() {
					return "Current JSF FacesContext";
				}
			});
			beanFactory.registerResolvableDependency(ExternalContext.class, new ObjectFactory<ExternalContext>() {
				@Override
				public ExternalContext getObject() {
					return FacesContext.getCurrentInstance().getExternalContext();
				}
				@Override
				public String toString() {
					return "Current JSF ExternalContext";
				}
			});
		}
	}
}
