package org.springframework.messaging.simp;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * 用于访问与SiMP会话 (e.g. WebSocket会话)关联的属性的包装类.
 */
public class SimpAttributes {

	/** 互斥锁会话属性的键 */
	public static final String SESSION_MUTEX_NAME = SimpAttributes.class.getName() + ".MUTEX";

	/** 会话完成后的键集 */
	public static final String SESSION_COMPLETED_NAME = SimpAttributes.class.getName() + ".COMPLETED";

	/** 用于存储销毁回调的会话属性名称的前缀. */
	public static final String DESTRUCTION_CALLBACK_NAME_PREFIX = SimpAttributes.class.getName() + ".DESTRUCTION_CALLBACK.";

	private static final Log logger = LogFactory.getLog(SimpAttributes.class);


	private final String sessionId;

	private final Map<String, Object> attributes;


	/**
	 * @param sessionId 相关会话的ID
	 * @param attributes 属性
	 */
	public SimpAttributes(String sessionId, Map<String, Object> attributes) {
		Assert.notNull(sessionId, "'sessionId' is required");
		Assert.notNull(attributes, "'attributes' is required");
		this.sessionId = sessionId;
		this.attributes = attributes;
	}


	/**
	 * 返回给定名称的属性值.
	 * 
	 * @param name 属性名称
	 * 
	 * @return 当前属性值, 或{@code null}
	 */
	public Object getAttribute(String name) {
		return this.attributes.get(name);
	}

	/**
	 * 设置给定名称的值, 替换现有值.
	 * 
	 * @param name 属性名称
	 * @param value 属性值
	 */
	public void setAttribute(String name, Object value) {
		this.attributes.put(name, value);
	}

	/**
	 * 删除给定名称的属性.
	 * <p>还删除指定属性的已注册的销毁回调. 但是它<i>不</i> 执行</i>回调.
	 * 假设移除的对象将在适当的时间独立地继续使用和销毁.
	 * 
	 * @param name 属性名称
	 */
	public void removeAttribute(String name) {
		this.attributes.remove(name);
		removeDestructionCallback(name);
	}

	/**
	 * 检索所有属性的名称.
	 * 
	 * @return 属性名称, never {@code null}
	 */
	public String[] getAttributeNames() {
		return StringUtils.toStringArray(this.attributes.keySet());
	}

	/**
	 * 注册一个回调以在销毁指定属性时执行.
	 * 会话关闭时执行回调.
	 * 
	 * @param name 注册回调的属性的名称
	 * @param callback 要执行的销毁回调
	 */
	public void registerDestructionCallback(String name, Runnable callback) {
		synchronized (getSessionMutex()) {
			if (isSessionCompleted()) {
				throw new IllegalStateException("Session id=" + getSessionId() + " already completed");
			}
			this.attributes.put(DESTRUCTION_CALLBACK_NAME_PREFIX + name, callback);
		}
	}

	private void removeDestructionCallback(String name) {
		synchronized (getSessionMutex()) {
			this.attributes.remove(DESTRUCTION_CALLBACK_NAME_PREFIX + name);
		}
	}

	/**
	 * 返回关联会话的ID.
	 * 
	 * @return 会话ID (never {@code null})
	 */
	public String getSessionId() {
		return this.sessionId;
	}

	/**
	 * 公开对象以同步底层会话.
	 * 
	 * @return 要使用的会话互斥锁 (never {@code null})
	 */
	public Object getSessionMutex() {
		Object mutex = this.attributes.get(SESSION_MUTEX_NAME);
		if (mutex == null) {
			mutex = this.attributes;
		}
		return mutex;
	}

	/**
	 * 是否已调用{@link #sessionCompleted()}.
	 */
	public boolean isSessionCompleted() {
		return (this.attributes.get(SESSION_COMPLETED_NAME) != null);
	}

	/**
	 * 会话完成时调用. 执行完成回调.
	 */
	public void sessionCompleted() {
		synchronized (getSessionMutex()) {
			if (!isSessionCompleted()) {
				executeDestructionCallbacks();
				this.attributes.put(SESSION_COMPLETED_NAME, Boolean.TRUE);
			}
		}
	}

	private void executeDestructionCallbacks() {
		for (Map.Entry<String, Object> entry : this.attributes.entrySet()) {
			if (entry.getKey().startsWith(DESTRUCTION_CALLBACK_NAME_PREFIX)) {
				try {
					((Runnable) entry.getValue()).run();
				}
				catch (Throwable ex) {
					logger.error("Uncaught error in session attribute destruction callback", ex);
				}
			}
		}
	}


	/**
	 * 从给定消息中提取SiMP会话属性, 并将它们包装在{@link SimpAttributes}实例中.
	 * 
	 * @param message 从中提取会话属性的消息
	 */
	public static SimpAttributes fromMessage(Message<?> message) {
		Assert.notNull(message, "Message must not be null");
		MessageHeaders headers = message.getHeaders();
		String sessionId = SimpMessageHeaderAccessor.getSessionId(headers);
		Map<String, Object> sessionAttributes = SimpMessageHeaderAccessor.getSessionAttributes(headers);
		if (sessionId == null) {
			throw new IllegalStateException("No session id in " + message);
		}
		if (sessionAttributes == null) {
			throw new IllegalStateException("No session attributes in " + message);
		}
		return new SimpAttributes(sessionId, sessionAttributes);
	}

}
