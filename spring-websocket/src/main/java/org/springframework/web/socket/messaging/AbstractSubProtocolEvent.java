package org.springframework.web.socket.messaging;

import java.security.Principal;

import org.springframework.context.ApplicationEvent;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

/**
 * 从WebSocket客户端接收并解析为更高级别子协议的消息的事件的基类 (e.g. STOMP).
 */
@SuppressWarnings("serial")
public abstract class AbstractSubProtocolEvent extends ApplicationEvent {

	private final Message<byte[]> message;

	private final Principal user;


	/**
	 * @param source 发布事件的组件 (never {@code null})
	 * @param message 传入的消息
	 */
	protected AbstractSubProtocolEvent(Object source, Message<byte[]> message) {
		super(source);
		Assert.notNull(message, "Message must not be null");
		this.message = message;
		this.user = null;
	}

	/**
	 * @param source 发布事件的组件 (never {@code null})
	 * @param message 传入的消息
	 */
	protected AbstractSubProtocolEvent(Object source, Message<byte[]> message, Principal user) {
		super(source);
		Assert.notNull(message, "Message must not be null");
		this.message = message;
		this.user = user;
	}


	/**
	 * 返回与事件关联的消息.
	 * 以下是获取有关会话ID或消息中任何header的信息的示例:
	 * <pre class="code">
	 * StompHeaderAccessor headers = StompHeaderAccessor.wrap(message);
	 * headers.getSessionId();
	 * headers.getSessionAttributes();
	 * headers.getPrincipal();
	 * </pre>
	 */
	public Message<byte[]> getMessage() {
		return this.message;
	}

	/**
	 * 返回与该事件关联的会话的用户.
	 */
	public Principal getUser() {
		return this.user;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + this.message + "]";
	}
}
