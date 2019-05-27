package org.springframework.web.portlet.context;

import java.util.LinkedHashMap;
import java.util.Map;
import javax.portlet.PortletContext;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.config.Scope;
import org.springframework.util.Assert;

/**
 * {@link Scope} wrapper for a PortletContext, i.e. for global web application attributes.
 *
 * <p>This differs from traditional Spring singletons in that it exposes attributes in the
 * PortletContext. Those attributes will get destroyed whenever the entire application
 * shuts down, which might be earlier or later than the shutdown of the containing Spring
 * ApplicationContext.
 *
 * <p>The associated destruction mechanism relies on a
 * {@link org.springframework.web.context.ContextCleanupListener} being registered in
 * {@code web.xml}. Note that {@link org.springframework.web.context.ContextLoaderListener}
 * includes ContextCleanupListener's functionality.
 *
 * <p>This scope is registered as default scope with key
 * {@link org.springframework.web.context.WebApplicationContext#SCOPE_APPLICATION "application"}.
 */
public class PortletContextScope implements Scope, DisposableBean {

	private final PortletContext portletContext;

	private final Map<String, Runnable> destructionCallbacks = new LinkedHashMap<String, Runnable>();


	/**
	 * Create a new Scope wrapper for the given PortletContext.
	 * @param portletContext the PortletContext to wrap
	 */
	public PortletContextScope(PortletContext portletContext) {
		Assert.notNull(portletContext, "PortletContext must not be null");
		this.portletContext = portletContext;
	}


	@Override
	public Object get(String name, ObjectFactory<?> objectFactory) {
		Object scopedObject = this.portletContext.getAttribute(name);
		if (scopedObject == null) {
			scopedObject = objectFactory.getObject();
			this.portletContext.setAttribute(name, scopedObject);
		}
		return scopedObject;
	}

	@Override
	public Object remove(String name) {
		Object scopedObject = this.portletContext.getAttribute(name);
		if (scopedObject != null) {
			this.portletContext.removeAttribute(name);
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
	 * Invoke all registered destruction callbacks.
	 * To be called on ServletContext shutdown.
	 * @see org.springframework.web.context.ContextCleanupListener
	 */
	@Override
	public void destroy() {
		for (Runnable runnable : this.destructionCallbacks.values()) {
			runnable.run();
		}
		this.destructionCallbacks.clear();
	}

}
