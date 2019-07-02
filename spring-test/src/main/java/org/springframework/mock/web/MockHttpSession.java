package org.springframework.mock.web;

import java.io.Serializable;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionBindingListener;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * {@link javax.servlet.http.HttpSession}接口的模拟实现.
 *
 * <p>从Spring 4.0开始, 这组模拟是在Servlet 3.0基线上设计的.
 *
 * <p>用于测试Web框架; 也适用于测试应用程序控制器.
 */
@SuppressWarnings("deprecation")
public class MockHttpSession implements HttpSession {

	public static final String SESSION_COOKIE_NAME = "JSESSION";


	private static int nextId = 1;

	private String id;

	private final long creationTime = System.currentTimeMillis();

	private int maxInactiveInterval;

	private long lastAccessedTime = System.currentTimeMillis();

	private final ServletContext servletContext;

	private final Map<String, Object> attributes = new LinkedHashMap<String, Object>();

	private boolean invalid = false;

	private boolean isNew = true;


	/**
	 * 使用默认的{@link MockServletContext}.
	 */
	public MockHttpSession() {
		this(null);
	}

	/**
	 * @param servletContext 运行会话的ServletContext
	 */
	public MockHttpSession(ServletContext servletContext) {
		this(servletContext, null);
	}

	/**
	 * @param servletContext 运行会话的ServletContext
	 * @param id 此会话的唯一标识符
	 */
	public MockHttpSession(ServletContext servletContext, String id) {
		this.servletContext = (servletContext != null ? servletContext : new MockServletContext());
		this.id = (id != null ? id : Integer.toString(nextId++));
	}


	@Override
	public long getCreationTime() {
		assertIsValid();
		return this.creationTime;
	}

	@Override
	public String getId() {
		return this.id;
	}

	/**
	 * 从Servlet 3.1开始, 可以更改会话的ID.
	 * 
	 * @return 新会话id
	 */
	public String changeSessionId() {
		this.id = Integer.toString(nextId++);
		return this.id;
	}

	public void access() {
		this.lastAccessedTime = System.currentTimeMillis();
		this.isNew = false;
	}

	@Override
	public long getLastAccessedTime() {
		assertIsValid();
		return this.lastAccessedTime;
	}

	@Override
	public ServletContext getServletContext() {
		return this.servletContext;
	}

	@Override
	public void setMaxInactiveInterval(int interval) {
		this.maxInactiveInterval = interval;
	}

	@Override
	public int getMaxInactiveInterval() {
		return this.maxInactiveInterval;
	}

	@Override
	public javax.servlet.http.HttpSessionContext getSessionContext() {
		throw new UnsupportedOperationException("getSessionContext");
	}

	@Override
	public Object getAttribute(String name) {
		assertIsValid();
		Assert.notNull(name, "Attribute name must not be null");
		return this.attributes.get(name);
	}

	@Override
	public Object getValue(String name) {
		return getAttribute(name);
	}

	@Override
	public Enumeration<String> getAttributeNames() {
		assertIsValid();
		return Collections.enumeration(new LinkedHashSet<String>(this.attributes.keySet()));
	}

	@Override
	public String[] getValueNames() {
		assertIsValid();
		return StringUtils.toStringArray(this.attributes.keySet());
	}

	@Override
	public void setAttribute(String name, Object value) {
		assertIsValid();
		Assert.notNull(name, "Attribute name must not be null");
		if (value != null) {
			this.attributes.put(name, value);
			if (value instanceof HttpSessionBindingListener) {
				((HttpSessionBindingListener) value).valueBound(new HttpSessionBindingEvent(this, name, value));
			}
		}
		else {
			removeAttribute(name);
		}
	}

	@Override
	public void putValue(String name, Object value) {
		setAttribute(name, value);
	}

	@Override
	public void removeAttribute(String name) {
		assertIsValid();
		Assert.notNull(name, "Attribute name must not be null");
		Object value = this.attributes.remove(name);
		if (value instanceof HttpSessionBindingListener) {
			((HttpSessionBindingListener) value).valueUnbound(new HttpSessionBindingEvent(this, name, value));
		}
	}

	@Override
	public void removeValue(String name) {
		removeAttribute(name);
	}

	/**
	 * 清除此会话的所有属性.
	 */
	public void clearAttributes() {
		for (Iterator<Map.Entry<String, Object>> it = this.attributes.entrySet().iterator(); it.hasNext();) {
			Map.Entry<String, Object> entry = it.next();
			String name = entry.getKey();
			Object value = entry.getValue();
			it.remove();
			if (value instanceof HttpSessionBindingListener) {
				((HttpSessionBindingListener) value).valueUnbound(new HttpSessionBindingEvent(this, name, value));
			}
		}
	}

	/**
	 * 使此会话无效, 然后解绑绑定到它的任何对象.
	 * 
	 * @throws IllegalStateException 如果在已经失效的会话上调用此方法
	 */
	@Override
	public void invalidate() {
		assertIsValid();
		this.invalid = true;
		clearAttributes();
	}

	public boolean isInvalid() {
		return this.invalid;
	}

	/**
	 * 断言此会话未被{@linkplain #invalidate() 失效}的便捷方法.
	 * 
	 * @throws IllegalStateException 如果此会话已失效
	 */
	private void assertIsValid() {
		Assert.state(!isInvalid(), "The session has already been invalidated");
	}

	public void setNew(boolean value) {
		this.isNew = value;
	}

	@Override
	public boolean isNew() {
		assertIsValid();
		return this.isNew;
	}

	/**
	 * 将此会话的属性序列化为一个对象, 该对象可以转换为具有标准Java序列化的字节数组.
	 * 
	 * @return 表示此会话的序列化状态
	 */
	public Serializable serializeState() {
		HashMap<String, Serializable> state = new HashMap<String, Serializable>();
		for (Iterator<Map.Entry<String, Object>> it = this.attributes.entrySet().iterator(); it.hasNext();) {
			Map.Entry<String, Object> entry = it.next();
			String name = entry.getKey();
			Object value = entry.getValue();
			it.remove();
			if (value instanceof Serializable) {
				state.put(name, (Serializable) value);
			}
			else {
				// 不可序列化... 在这种情况下, Servlet容器通常会自动解绑属性.
				if (value instanceof HttpSessionBindingListener) {
					((HttpSessionBindingListener) value).valueUnbound(new HttpSessionBindingEvent(this, name, value));
				}
			}
		}
		return state;
	}

	/**
	 * 从{@link #serializeState()}创建的状态对象反序列化此会话的属性.
	 * 
	 * @param state 表示此会话的序列化状态
	 */
	@SuppressWarnings("unchecked")
	public void deserializeState(Serializable state) {
		Assert.isTrue(state instanceof Map, "Serialized state needs to be of type [java.util.Map]");
		this.attributes.putAll((Map<String, Object>) state);
	}
}
