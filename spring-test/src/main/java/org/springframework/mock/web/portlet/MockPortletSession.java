package org.springframework.mock.web.portlet;

import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import javax.portlet.PortletContext;
import javax.portlet.PortletSession;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionBindingListener;

import org.springframework.mock.web.MockHttpSession;

/**
 * {@link javax.portlet.PortletSession}接口的模拟实现.
 */
public class MockPortletSession implements PortletSession {

	private static int nextId = 1;


	private final String id = Integer.toString(nextId++);

	private final long creationTime = System.currentTimeMillis();

	private int maxInactiveInterval;

	private long lastAccessedTime = System.currentTimeMillis();

	private final PortletContext portletContext;

	private final Map<String, Object> portletAttributes = new LinkedHashMap<String, Object>();

	private final Map<String, Object> applicationAttributes = new LinkedHashMap<String, Object>();

	private boolean invalid = false;

	private boolean isNew = true;


	/**
	 * 使用默认的{@link MockPortletContext}.
	 */
	public MockPortletSession() {
		this(null);
	}

	/**
	 * @param portletContext 运行会话的PortletContext
	 */
	public MockPortletSession(PortletContext portletContext) {
		this.portletContext = (portletContext != null ? portletContext : new MockPortletContext());
	}


	@Override
	public Object getAttribute(String name) {
		return this.portletAttributes.get(name);
	}

	@Override
	public Object getAttribute(String name, int scope) {
		if (scope == PortletSession.PORTLET_SCOPE) {
			return this.portletAttributes.get(name);
		}
		else if (scope == PortletSession.APPLICATION_SCOPE) {
			return this.applicationAttributes.get(name);
		}
		return null;
	}

	@Override
	public Enumeration<String> getAttributeNames() {
		return Collections.enumeration(new LinkedHashSet<String>(this.portletAttributes.keySet()));
	}

	@Override
	public Enumeration<String> getAttributeNames(int scope) {
		if (scope == PortletSession.PORTLET_SCOPE) {
			return Collections.enumeration(new LinkedHashSet<String>(this.portletAttributes.keySet()));
		}
		else if (scope == PortletSession.APPLICATION_SCOPE) {
			return Collections.enumeration(new LinkedHashSet<String>(this.applicationAttributes.keySet()));
		}
		return null;
	}

	@Override
	public long getCreationTime() {
		return this.creationTime;
	}

	@Override
	public String getId() {
		return this.id;
	}

	public void access() {
		this.lastAccessedTime = System.currentTimeMillis();
		setNew(false);
	}

	@Override
	public long getLastAccessedTime() {
		return this.lastAccessedTime;
	}

	@Override
	public int getMaxInactiveInterval() {
		return this.maxInactiveInterval;
	}

	/**
	 * 清除此会话的所有属性.
	 */
	public void clearAttributes() {
		doClearAttributes(this.portletAttributes);
		doClearAttributes(this.applicationAttributes);
	}

	protected void doClearAttributes(Map<String, Object> attributes) {
		for (Iterator<Map.Entry<String, Object>> it = attributes.entrySet().iterator(); it.hasNext();) {
			Map.Entry<String, Object> entry = it.next();
			String name = entry.getKey();
			Object value = entry.getValue();
			it.remove();
			if (value instanceof HttpSessionBindingListener) {
				((HttpSessionBindingListener) value).valueUnbound(
						new HttpSessionBindingEvent(new MockHttpSession(), name, value));
			}
		}
	}

	@Override
	public void invalidate() {
		this.invalid = true;
		clearAttributes();
	}

	public boolean isInvalid() {
		return this.invalid;
	}

	public void setNew(boolean value) {
		this.isNew = value;
	}

	@Override
	public boolean isNew() {
		return this.isNew;
	}

	@Override
	public void removeAttribute(String name) {
		this.portletAttributes.remove(name);
	}

	@Override
	public void removeAttribute(String name, int scope) {
		if (scope == PortletSession.PORTLET_SCOPE) {
			this.portletAttributes.remove(name);
		}
		else if (scope == PortletSession.APPLICATION_SCOPE) {
			this.applicationAttributes.remove(name);
		}
	}

	@Override
	public void setAttribute(String name, Object value) {
		if (value != null) {
			this.portletAttributes.put(name, value);
		}
		else {
			this.portletAttributes.remove(name);
		}
	}

	@Override
	public void setAttribute(String name, Object value, int scope) {
		if (scope == PortletSession.PORTLET_SCOPE) {
			if (value != null) {
				this.portletAttributes.put(name, value);
			}
			else {
				this.portletAttributes.remove(name);
			}
		}
		else if (scope == PortletSession.APPLICATION_SCOPE) {
			if (value != null) {
				this.applicationAttributes.put(name, value);
			}
			else {
				this.applicationAttributes.remove(name);
			}
		}
	}

	@Override
	public void setMaxInactiveInterval(int interval) {
		this.maxInactiveInterval = interval;
	}

	@Override
	public PortletContext getPortletContext() {
		return this.portletContext;
	}

	@Override
	public Map<String, Object> getAttributeMap() {
		return Collections.unmodifiableMap(this.portletAttributes);
	}

	@Override
	public Map<String, Object> getAttributeMap(int scope) {
		if (scope == PortletSession.PORTLET_SCOPE) {
			return Collections.unmodifiableMap(this.portletAttributes);
		}
		else if (scope == PortletSession.APPLICATION_SCOPE) {
			return Collections.unmodifiableMap(this.applicationAttributes);
		}
		else {
			return Collections.emptyMap();
		}
	}

}
