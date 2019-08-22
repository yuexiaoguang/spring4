package org.springframework.web.socket.handler;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;

/**
 * 异常处理{@link WebSocketHandlerDecorator}.
 * 捕获从装饰处理器中逃脱的所有{@link Throwable}实例, 并使用{@link CloseStatus#SERVER_ERROR}关闭会话.
 */
public class ExceptionWebSocketHandlerDecorator extends WebSocketHandlerDecorator {

	private static final Log logger = LogFactory.getLog(ExceptionWebSocketHandlerDecorator.class);


	public ExceptionWebSocketHandlerDecorator(WebSocketHandler delegate) {
		super(delegate);
	}


	@Override
	public void afterConnectionEstablished(WebSocketSession session) {
		try {
			getDelegate().afterConnectionEstablished(session);
		}
		catch (Throwable ex) {
			tryCloseWithError(session, ex, logger);
		}
	}

	@Override
	public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) {
		try {
			getDelegate().handleMessage(session, message);
		}
		catch (Throwable ex) {
			tryCloseWithError(session, ex, logger);
		}
	}

	@Override
	public void handleTransportError(WebSocketSession session, Throwable exception) {
		try {
			getDelegate().handleTransportError(session, exception);
		}
		catch (Throwable ex) {
			tryCloseWithError(session, ex, logger);
		}
	}

	@Override
	public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) {
		try {
			getDelegate().afterConnectionClosed(session, closeStatus);
		}
		catch (Throwable ex) {
			if (logger.isWarnEnabled()) {
				logger.warn("Unhandled exception after connection closed for " + this, ex);
			}
		}
	}


	public static void tryCloseWithError(WebSocketSession session, Throwable exception, Log logger) {
		if (logger.isErrorEnabled()) {
			logger.error("Closing session due to exception for " + session, exception);
		}
		if (session.isOpen()) {
			try {
				session.close(CloseStatus.SERVER_ERROR);
			}
			catch (Throwable ex) {
				// ignore
			}
		}
	}

}
