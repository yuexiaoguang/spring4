package org.springframework.web.context.support;

import java.util.LinkedHashMap;
import java.util.Map;
import javax.servlet.ServletContext;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.config.Scope;
import org.springframework.util.Assert;

/**
 * 用于ServletContext的{@link Scope}包装器, i.e. 用于全局Web应用程序属性.
 *
 * <p>这与传统的Spring单例不同, 它在ServletContext中公开了属性.
 * 每当整个应用程序关闭时, 这些属性都将被销毁, 这可能比Spring ApplicationContext的关闭更早或更晚.
 *
 * <p>相关的销毁机制依赖于在{@code web.xml}中注册的{@link org.springframework.web.context.ContextCleanupListener}.
 * 请注意{@link org.springframework.web.context.ContextLoaderListener}包含ContextCleanupListener的功能.
 *
 * <p>此范围注册为键
 * {@link org.springframework.web.context.WebApplicationContext#SCOPE_APPLICATION "application"}的默认范围.
 */
public class ServletContextScope implements Scope, DisposableBean {

	private final ServletContext servletContext;

	private final Map<String, Runnable> destructionCallbacks = new LinkedHashMap<String, Runnable>();


	/**
	 * @param servletContext 要包装的ServletContext
	 */
	public ServletContextScope(ServletContext servletContext) {
		Assert.notNull(servletContext, "ServletContext must not be null");
		this.servletContext = servletContext;
	}


	@Override
	public Object get(String name, ObjectFactory<?> objectFactory) {
		Object scopedObject = this.servletContext.getAttribute(name);
		if (scopedObject == null) {
			scopedObject = objectFactory.getObject();
			this.servletContext.setAttribute(name, scopedObject);
		}
		return scopedObject;
	}

	@Override
	public Object remove(String name) {
		Object scopedObject = this.servletContext.getAttribute(name);
		if (scopedObject != null) {
			this.servletContext.removeAttribute(name);
			this.destructionCallbacks.remove(name);
			return scopedObject;
		}
		else {
			return null;
		}
	}

	@Override
	public void registerDestructionCallback(String name, Runnable callback) {
		this.destructionCallbacks.put(name, callback);
	}

	@Override
	public Object resolveContextualObject(String key) {
		return null;
	}

	@Override
	public String getConversationId() {
		return null;
	}


	/**
	 * 调用所有已注册的销毁回调.
	 * 要在ServletContext上调用shutdown.
	 */
	@Override
	public void destroy() {
		for (Runnable runnable : this.destructionCallbacks.values()) {
			runnable.run();
		}
		this.destructionCallbacks.clear();
	}

}
