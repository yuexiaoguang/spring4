package org.springframework.web.context.support;

import javax.servlet.ServletContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.web.context.ContextLoader;
import org.springframework.web.context.WebApplicationContext;

/**
 * 方便的基类, 用于在基于Spring的Web应用程序中构建的自动装配类.
 * 在当前Spring根Web应用程序上下文中针对bean解析端点类中的{@code @Autowired}注解
 * (由当前线程的上下文ClassLoader确定, 它需要是Web应用程序的ClassLoader).
 * 或者可以用作委托而不是基类.
 *
 * <p>此基类的典型用法是JAX-WS端点类:
 * 这样一个基于Spring的JAX-WS端点实现将遵循端点类的标准JAX-WS约定, 但它更轻量级,
 * 因为它将实际工作委托给一个或多个Spring管理的服务bean - 通常使用{@code @Autowired}获取.
 * 这种端点实例的生命周期将由JAX-WS运行时管理, 因此需要此基类根据当前的Spring上下文提供{@code @Autowired}处理.
 *
 * <p><b>NOTE:</b> 如果有一种显式方式来访问ServletContext, 那么更喜欢使用这个类.
 * {@link WebApplicationContextUtils}类允许基于ServletContext轻松访问Spring根Web应用程序上下文.
 */
public abstract class SpringBeanAutowiringSupport {

	private static final Log logger = LogFactory.getLog(SpringBeanAutowiringSupport.class);


	/**
	 * 此构造函数基于当前Web应用程序上下文在此实例上执行注入.
	 * <p>打算用作基类.
	 */
	public SpringBeanAutowiringSupport() {
		processInjectionBasedOnCurrentContext(this);
	}


	/**
	 * 基于当前Web应用程序上下文, 为给定目标对象处理{@code @Autowired}注入.
	 * <p>打算用作委托.
	 * 
	 * @param target 要处理的目标对象
	 */
	public static void processInjectionBasedOnCurrentContext(Object target) {
		Assert.notNull(target, "Target object must not be null");
		WebApplicationContext cc = ContextLoader.getCurrentWebApplicationContext();
		if (cc != null) {
			AutowiredAnnotationBeanPostProcessor bpp = new AutowiredAnnotationBeanPostProcessor();
			bpp.setBeanFactory(cc.getAutowireCapableBeanFactory());
			bpp.processInjection(target);
		}
		else {
			if (logger.isDebugEnabled()) {
				logger.debug("Current WebApplicationContext is not available for processing of " +
						ClassUtils.getShortName(target.getClass()) + ": " +
						"Make sure this class gets constructed in a Spring web application. Proceeding without injection.");
			}
		}
	}


	/**
	 * 根据存储在ServletContext中的当前根Web应用程序上下文, 为给定目标对象处理{@code @Autowired}注入.
	 * <p>打算用作委托.
	 * 
	 * @param target 要处理的目标对象
	 * @param servletContext 用于查找Spring Web应用程序上下文的ServletContext
	 */
	public static void processInjectionBasedOnServletContext(Object target, ServletContext servletContext) {
		Assert.notNull(target, "Target object must not be null");
		WebApplicationContext cc = WebApplicationContextUtils.getRequiredWebApplicationContext(servletContext);
		AutowiredAnnotationBeanPostProcessor bpp = new AutowiredAnnotationBeanPostProcessor();
		bpp.setBeanFactory(cc.getAutowireCapableBeanFactory());
		bpp.processInjection(target);
	}

}
