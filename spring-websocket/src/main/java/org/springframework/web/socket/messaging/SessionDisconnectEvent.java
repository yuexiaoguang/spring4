package org.springframework.web.socket.messaging;

import java.security.Principal;

import org.springframework.messaging.Message;
import org.springframework.util.Assert;
import org.springframework.web.socket.CloseStatus;

/**
 * 使用简单消息传递协议 (e.g. STOMP)作为WebSocket子协议的WebSocket客户端会话关闭时引发的事件.
 *
 * <p>请注意, 对于单个会话, 此事件可能会多次引发, 因此事件使用者应该是幂等的并忽略重复事件.
 */
@SuppressWarnings("serial")
public class SessionDisconnectEvent extends AbstractSubProtocolEvent {

	private final String sessionId;

	private final CloseStatus status;


	/**
	 * @param source 发布事件的组件 (never {@code null})
	 * @param message 消息
	 * @param sessionId 断开连接的消息
	 * @param closeStatus 状态对象
	 */
	public SessionDisconnectEvent(Object source, Message<byte[]> message, String sessionId,
			CloseStatus closeStatus) {

		this(source, message, sessionId, closeStatus, null);
	}

	/**
	 * @param source 发布事件的组件 (never {@code null})
	 * @param message 消息
	 * @param sessionId 断开连接的消息
	 * @param closeStatus 状态对象
	 * @param user 当前会话用户
	 */
	public SessionDisconnectEvent(Object source, Message<byte[]> message, String sessionId,
			CloseStatus closeStatus, Principal user) {

		super(source, message, user);
		Assert.notNull(sessionId, "Session id must not be null");
		this.sessionId = sessionId;
		this.status = closeStatus;
	}


	/**
	 * 返回会话id.
	 */
	public String getSessionId() {
		return this.sessionId;
	}

	/**
	 * 返回会话关闭的状态.
	 */
	public CloseStatus getCloseStatus() {
		return this.status;
	}


	@Override
	public String toString() {
		return "SessionDisconnectEvent[sessionId=" + this.sessionId + ", " +
				(this.status != null ? this.status.toString() : "closeStatus=null") + "]";
	}
}
