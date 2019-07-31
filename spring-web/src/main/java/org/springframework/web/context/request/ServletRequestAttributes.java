package org.springframework.web.context.request;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.util.Assert;
import org.springframework.util.NumberUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.util.WebUtils;

/**
 * 基于Servlet的{@link RequestAttributes}接口实现.
 *
 * <p>从servlet请求和HTTP会话范围访问对象, "会话"和"全局会话"之间没有区别.
 */
public class ServletRequestAttributes extends AbstractRequestAttributes {

	/**
	 * 在{@link HttpSession}中存储销毁回调名称时, 标识它的前缀{@link String}常量.
	 */
	public static final String DESTRUCTION_CALLBACK_NAME_PREFIX =
			ServletRequestAttributes.class.getName() + ".DESTRUCTION_CALLBACK.";

	protected static final Set<Class<?>> immutableValueTypes = new HashSet<Class<?>>(16);

	static {
		immutableValueTypes.addAll(NumberUtils.STANDARD_NUMBER_TYPES);
		immutableValueTypes.add(Boolean.class);
		immutableValueTypes.add(Character.class);
		immutableValueTypes.add(String.class);
	}


	private final HttpServletRequest request;

	private HttpServletResponse response;

	private volatile HttpSession session;

	private final Map<String, Object> sessionAttributesToUpdate = new ConcurrentHashMap<String, Object>(1);


	/**
	 * @param request 当前的HTTP请求
	 */
	public ServletRequestAttributes(HttpServletRequest request) {
		Assert.notNull(request, "Request must not be null");
		this.request = request;
	}

	/**
	 * @param request 当前的HTTP请求
	 * @param response 当前HTTP响应 (用于可选的公开)
	 */
	public ServletRequestAttributes(HttpServletRequest request, HttpServletResponse response) {
		this(request);
		this.response = response;
	}


	/**
	 * 公开正在包装的原生{@link HttpServletRequest}.
	 */
	public final HttpServletRequest getRequest() {
		return this.request;
	}

	/**
	 * 公开正在包装的原生{@link HttpServletResponse}.
	 */
	public final HttpServletResponse getResponse() {
		return this.response;
	}

	/**
	 * 公开正在包装的{@link HttpSession}.
	 * 
	 * @param allowCreate 是否允许创建新会话
	 */
	protected final HttpSession getSession(boolean allowCreate) {
		if (isRequestActive()) {
			HttpSession session = this.request.getSession(allowCreate);
			this.session = session;
			return session;
		}
		else {
			// 通过存储的会话引用访问...
			HttpSession session = this.session;
			if (session == null) {
				if (allowCreate) {
					throw new IllegalStateException(
							"No session found and request already completed - cannot create new session!");
				}
				else {
					session = this.request.getSession(false);
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
			HttpSession session = getSession(false);
			if (session != null) {
				try {
					Object value = session.getAttribute(name);
					if (value != null) {
						this.sessionAttributesToUpdate.put(name, value);
					}
					return value;
				}
				catch (IllegalStateException ex) {
					// Session invalidated - shouldn't usually happen.
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
			HttpSession session = getSession(true);
			this.sessionAttributesToUpdate.remove(name);
			session.setAttribute(name, value);
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
			HttpSession session = getSession(false);
			if (session != null) {
				this.sessionAttributesToUpdate.remove(name);
				try {
					session.removeAttribute(name);
					// 删除所有已注册的销毁回调.
					session.removeAttribute(DESTRUCTION_CALLBACK_NAME_PREFIX + name);
				}
				catch (IllegalStateException ex) {
					// 会话无效 - 通常不应该发生.
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
			HttpSession session = getSession(false);
			if (session != null) {
				try {
					return StringUtils.toStringArray(session.getAttributeNames());
				}
				catch (IllegalStateException ex) {
					// 会话无效 - 通常不应该发生.
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
		return WebUtils.getSessionMutex(getSession(true));
	}


	/**
	 * 通过{@code session.setAttribute}调用更新所有访问的会话属性, 向容器明确指示它们可能已被修改.
	 */
	@Override
	protected void updateAccessedSessionAttributes() {
		if (!this.sessionAttributesToUpdate.isEmpty()) {
			// 更新所有受影响的会话属性.
			HttpSession session = getSession(false);
			if (session != null) {
				try {
					for (Map.Entry<String, Object> entry : this.sessionAttributesToUpdate.entrySet()) {
						String name = entry.getKey();
						Object newValue = entry.getValue();
						Object oldValue = session.getAttribute(name);
						if (oldValue == newValue && !isImmutableSessionAttribute(name, newValue)) {
							session.setAttribute(name, newValue);
						}
					}
				}
				catch (IllegalStateException ex) {
					// 会话无效 - 通常不应该发生.
				}
			}
			this.sessionAttributesToUpdate.clear();
		}
	}

	/**
	 * 确定是否将给定值视为不可变会话属性, 即不必通过{@code session.setAttribute}重新设置, 因为其值无法在内部进行有意义的更改.
	 * <p>对于{@code String}, {@code Character}, {@code Boolean}和标准 {@code Number}值, 默认实现返回{@code true}.
	 * 
	 * @param name 属性的名称
	 * @param value 要检查的相应值
	 * 
	 * @return {@code true} 如果为了会话属性管理的目的, 将值视为不可变的; 否则{@code false}
	 */
	protected boolean isImmutableSessionAttribute(String name, Object value) {
		return (value == null || immutableValueTypes.contains(value.getClass()));
	}

	/**
	 * 注册给定的回调, 以便在会话终止后执行.
	 * <p>Note: 回调对象应该是可序列化的, 以便在Web应用程序重新启动后继续运行.
	 * 
	 * @param name 要注册回调的属性的名称
	 * @param callback 要执行的销毁回调
	 */
	protected void registerSessionDestructionCallback(String name, Runnable callback) {
		HttpSession session = getSession(true);
		session.setAttribute(DESTRUCTION_CALLBACK_NAME_PREFIX + name,
				new DestructionCallbackBindingListener(callback));
	}


	@Override
	public String toString() {
		return this.request.toString();
	}

}
