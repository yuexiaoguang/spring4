package org.springframework.web.socket.handler;

import org.springframework.util.Assert;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;

/**
 * 包装另一个{@link org.springframework.web.socket.WebSocketHandler}实例并委托给它.
 *
 * <p>还提供了一个{@link #getDelegate()}方法来返回修饰的处理器,
 * 以及一个{@link #getLastHandler()}方法来遍历所有嵌套的委托并返回"最后"的处理器.
 */
public class WebSocketHandlerDecorator implements WebSocketHandler {

	private final WebSocketHandler delegate;


	public WebSocketHandlerDecorator(WebSocketHandler delegate) {
		Assert.notNull(delegate, "Delegate must not be null");
		this.delegate = delegate;
	}


	public WebSocketHandler getDelegate() {
		return this.delegate;
	}

	public WebSocketHandler getLastHandler() {
		WebSocketHandler result = this.delegate;
		while (result instanceof WebSocketHandlerDecorator) {
			result = ((WebSocketHandlerDecorator) result).getDelegate();
		}
		return result;
	}

	public static WebSocketHandler unwrap(WebSocketHandler handler) {
		if (handler instanceof WebSocketHandlerDecorator) {
			return ((WebSocketHandlerDecorator) handler).getLastHandler();
		}
		else {
			return handler;
		}
	}

	@Override
	public void afterConnectionEstablished(WebSocketSession session) throws Exception {
		this.delegate.afterConnectionEstablished(session);
	}

	@Override
	public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
		this.delegate.handleMessage(session, message);
	}

	@Override
	public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
		this.delegate.handleTransportError(session, exception);
	}

	@Override
	public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
		this.delegate.afterConnectionClosed(session, closeStatus);
	}

	@Override
	public boolean supportsPartialMessages() {
		return this.delegate.supportsPartialMessages();
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + " [delegate=" + this.delegate + "]";
	}

}
