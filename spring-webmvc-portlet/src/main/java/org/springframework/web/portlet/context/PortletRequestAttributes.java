package org.springframework.web.portlet.context;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.portlet.PortletRequest;
import javax.portlet.PortletResponse;
import javax.portlet.PortletSession;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.AbstractRequestAttributes;
import org.springframework.web.context.request.DestructionCallbackBindingListener;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.portlet.util.PortletUtils;

/**
 * {@link org.springframework.web.context.request.RequestAttributes}接口基于Portlet的实现.
 *
 * <p>访问来自portlet请求和portlet会话范围的对象,
 * 区分"session" (PortletSession的"portlet scope")和"global session" (PortletSession的"application scope").
 */
public class PortletRequestAttributes extends AbstractRequestAttributes {

	/**
	 * 当存储在{@link PortletSession}中时, 销毁回调名称的前缀.
	 */
	public static final String DESTRUCTION_CALLBACK_NAME_PREFIX =
			PortletRequestAttributes.class.getName() + ".DESTRUCTION_CALLBACK.";


	private final PortletRequest request;

	private PortletResponse response;

	private volatile PortletSession session;

	private final Map<String, Object> sessionAttributesToUpdate = new ConcurrentHashMap<String, Object>(1);

	private final Map<String, Object> globalSessionAttributesToUpdate = new ConcurrentHashMap<String, Object>(1);


	/**
	 * @param request 当前的portlet请求
	 */
	public PortletRequestAttributes(PortletRequest request) {
		Assert.notNull(request, "Request must not be null");
		this.request = request;
	}

	/**
	 * @param request 当前的portlet请求
	 * @param response 当前portlet响应 (可选)
	 */
	public PortletRequestAttributes(PortletRequest request, PortletResponse response) {
		this(request);
		this.response = response;
	}


	/**
	 * 公开包装的原生{@link PortletRequest}.
	 */
	public final PortletRequest getRequest() {
		return this.request;
	}

	/**
	 * 公开包装的原生{@link PortletResponse}.
	 */
	public final PortletResponse getResponse() {
		return this.response;
	}

	/**
	 * 公开包装的{@link PortletSession}.
	 * 
	 * @param allowCreate 是否允许创建新会话
	 */
	protected final PortletSession getSession(boolean allowCreate) {
		if (isRequestActive()) {
			PortletSession session = this.request.getPortletSession(allowCreate);
			this.session = session;
			return session;
		}
		else {
			// 通过存储的会话引用访问...
			PortletSession session = this.session;
			if (session == null) {
				if (allowCreate) {
					throw new IllegalStateException(
							"No session found and request already completed - cannot create new session!");
				}
				else {
					session = this.request.getPortletSession(false);
					this.session = session;
				}
			}
			return session;
		}
	}


	@Override
	public Object getAttribute(String name, int scope) {
		if (scope == SCOPE_REQUEST) {
			if (!isRequestActive()) {
				throw new IllegalStateException(
						"Cannot ask for request attribute - request is not active anymore!");
			}
			return this.request.getAttribute(name);
		}
		else {
			PortletSession session = getSession(false);
			if (session != null) {
				if (scope == SCOPE_GLOBAL_SESSION) {
					Object value = session.getAttribute(name, PortletSession.APPLICATION_SCOPE);
					if (value != null) {
						this.globalSessionAttributesToUpdate.put(name, value);
					}
					return value;
				}
				else {
					Object value = session.getAttribute(name);
					if (value != null) {
						this.sessionAttributesToUpdate.put(name, value);
					}
					return value;
				}
			}
			return null;
		}
	}

	@Override
	public void setAttribute(String name, Object value, int scope) {
		if (scope == SCOPE_REQUEST) {
			if (!isRequestActive()) {
				throw new IllegalStateException(
						"Cannot set request attribute - request is not active anymore!");
			}
			this.request.setAttribute(name, value);
		}
		else {
			PortletSession session = getSession(true);
			if (scope == SCOPE_GLOBAL_SESSION) {
				session.setAttribute(name, value, PortletSession.APPLICATION_SCOPE);
				this.globalSessionAttributesToUpdate.remove(name);
			}
			else {
				session.setAttribute(name, value);
				this.sessionAttributesToUpdate.remove(name);
			}
		}
	}

	@Override
	public void removeAttribute(String name, int scope) {
		if (scope == SCOPE_REQUEST) {
			if (isRequestActive()) {
				this.request.removeAttribute(name);
				removeRequestDestructionCallback(name);
			}
		}
		else {
			PortletSession session = getSession(false);
			if (session != null) {
				if (scope == SCOPE_GLOBAL_SESSION) {
					session.removeAttribute(name, PortletSession.APPLICATION_SCOPE);
					this.globalSessionAttributesToUpdate.remove(name);
				}
				else {
					session.removeAttribute(name);
					this.sessionAttributesToUpdate.remove(name);
				}
			}
		}
	}

	@Override
	public String[] getAttributeNames(int scope) {
		if (scope == SCOPE_REQUEST) {
			if (!isRequestActive()) {
				throw new IllegalStateException(
						"Cannot ask for request attributes - request is not active anymore!");
			}
			return StringUtils.toStringArray(this.request.getAttributeNames());
		}
		else {
			PortletSession session = getSession(false);
			if (session != null) {
				if (scope == SCOPE_GLOBAL_SESSION) {
					return StringUtils.toStringArray(session.getAttributeNames(PortletSession.APPLICATION_SCOPE));
				}
				else {
					return StringUtils.toStringArray(session.getAttributeNames());
				}
			}
			return new String[0];
		}
	}

	@Override
	public void registerDestructionCallback(String name, Runnable callback, int scope) {
		if (scope == SCOPE_REQUEST) {
			registerRequestDestructionCallback(name, callback);
		}
		else {
			registerSessionDestructionCallback(name, callback);
		}
	}

	@Override
	public Object resolveReference(String key) {
		if (REFERENCE_REQUEST.equals(key)) {
			return this.request;
		}
		else if (REFERENCE_SESSION.equals(key)) {
			return getSession(true);
		}
		else {
			return null;
		}
	}

	@Override
	public String getSessionId() {
		return getSession(true).getId();
	}

	@Override
	public Object getSessionMutex() {
		return PortletUtils.getSessionMutex(getSession(true));
	}


	/**
	 * 通过{@code session.setAttribute}调用更新所有访问的会话属性, 向容器明确指示它们可能已被修改.
	 */
	@Override
	protected void updateAccessedSessionAttributes() {
		if (!this.sessionAttributesToUpdate.isEmpty() || !this.globalSessionAttributesToUpdate.isEmpty()) {
			PortletSession session = getSession(false);
			if (session != null) {
				try {
					for (Map.Entry<String, Object> entry : this.sessionAttributesToUpdate.entrySet()) {
						String name = entry.getKey();
						Object newValue = entry.getValue();
						Object oldValue = session.getAttribute(name);
						if (oldValue == newValue) {
							session.setAttribute(name, newValue);
						}
					}
					for (Map.Entry<String, Object> entry : this.globalSessionAttributesToUpdate.entrySet()) {
						String name = entry.getKey();
						Object newValue = entry.getValue();
						Object oldValue = session.getAttribute(name, PortletSession.APPLICATION_SCOPE);
						if (oldValue == newValue) {
							session.setAttribute(name, newValue, PortletSession.APPLICATION_SCOPE);
						}
					}
				}
				catch (IllegalStateException ex) {
					// Session invalidated - shouldn't usually happen.
				}
			}
			this.sessionAttributesToUpdate.clear();
			this.globalSessionAttributesToUpdate.clear();
		}
	}

	/**
	 * 注册给定的回调, 以便在会话终止后执行.
	 * <p>Note: 回调对象应该是可序列化的, 以便在Web应用程序重新启动后继续运行.
	 * 
	 * @param name 注册回调的属性的名称
	 * @param callback 销毁时要执行的回调
	 */
	protected void registerSessionDestructionCallback(String name, Runnable callback) {
		PortletSession session = getSession(true);
		session.setAttribute(DESTRUCTION_CALLBACK_NAME_PREFIX + name,
				new DestructionCallbackBindingListener(callback));
	}


	@Override
	public String toString() {
		return this.request.toString();
	}
}
