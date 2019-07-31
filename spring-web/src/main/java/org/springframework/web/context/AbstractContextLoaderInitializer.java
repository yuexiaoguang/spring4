package org.springframework.web.context;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.web.WebApplicationInitializer;

/**
 * {@link WebApplicationInitializer}实现的便捷基类, 用于在servlet上下文中注册{@link ContextLoaderListener}.
 *
 * <p>子类需要实现的唯一方法是{@link #createRootApplicationContext()},
 * 从{@link #registerContextLoaderListener(ServletContext)}调用.
 */
public abstract class AbstractContextLoaderInitializer implements WebApplicationInitializer {

	/** Logger available to subclasses */
	protected final Log logger = LogFactory.getLog(getClass());


	@Override
	public void onStartup(ServletContext servletContext) throws ServletException {
		registerContextLoaderListener(servletContext);
	}

	/**
	 * 针对给定的servlet上下文注册{@link ContextLoaderListener}.
	 * 使用从{@link #createRootApplicationContext()}模板方法返回的应用程序上下文初始化{@code ContextLoaderListener}.
	 * 
	 * @param servletContext 用于注册监听器的servlet上下文
	 */
	protected void registerContextLoaderListener(ServletContext servletContext) {
		WebApplicationContext rootAppContext = createRootApplicationContext();
		if (rootAppContext != null) {
			ContextLoaderListener listener = new ContextLoaderListener(rootAppContext);
			listener.setContextInitializers(getRootApplicationContextInitializers());
			servletContext.addListener(listener);
		}
		else {
			logger.debug("No ContextLoaderListener registered, as " +
					"createRootApplicationContext() did not return an application context");
		}
	}

	/**
	 * 创建要提供给{@code ContextLoaderListener}的"<strong>root</strong>"应用程序上下文.
	 * <p>返回的上下文被委托给
	 * {@link ContextLoaderListener#ContextLoaderListener(WebApplicationContext)},
	 * 并将被建立为任何{@code DispatcherServlet}应用程序上下文的父上下文.
	 * 因此, 它通常包含中间层服务, 数据源等.
	 * 
	 * @return 根应用程序上下文, 或{@code null} 如果不需要根上下文
	 */
	protected abstract WebApplicationContext createRootApplicationContext();

	/**
	 * 指定要应用于正在创建{@code ContextLoaderListener}的根应用程序上下文的应用程序上下文初始值设定项.
	 */
	protected ApplicationContextInitializer<?>[] getRootApplicationContextInitializers() {
		return null;
	}

}
