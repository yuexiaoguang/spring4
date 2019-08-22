package org.springframework.web.socket.handler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;

/**
 * {@link WebSocketHandler}, 它为每个WebSocket连接初始化并销毁{@link WebSocketHandler}实例, 并将所有其他方法委托给它.
 *
 * <p>基本上创建此类的一个实例, 提供为每个连接创建的{@link WebSocketHandler}类的类型,
 * 然后将其传递给任何需要{@link WebSocketHandler}的API方法.
 *
 * <p>如果初始化目标{@link WebSocketHandler}类型需要Spring BeanFctory, 则相应地设置{@link #setBeanFactory(BeanFactory)}属性.
 * 简单地将此类声明为Spring bean就可以做到这一点.
 * 否则, 将使用默认构造函数创建目标类型的{@link WebSocketHandler}实例.
 */
public class PerConnectionWebSocketHandler implements WebSocketHandler, BeanFactoryAware {

	private static final Log logger = LogFactory.getLog(PerConnectionWebSocketHandler.class);


	private final BeanCreatingHandlerProvider<WebSocketHandler> provider;

	private final Map<WebSocketSession, WebSocketHandler> handlers =
			new ConcurrentHashMap<WebSocketSession, WebSocketHandler>();

	private final boolean supportsPartialMessages;


	public PerConnectionWebSocketHandler(Class<? extends WebSocketHandler> handlerType) {
		this(handlerType, false);
	}

	public PerConnectionWebSocketHandler(Class<? extends WebSocketHandler> handlerType, boolean supportsPartialMessages) {
		this.provider = new BeanCreatingHandlerProvider<WebSocketHandler>(handlerType);
		this.supportsPartialMessages = supportsPartialMessages;
	}


	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		this.provider.setBeanFactory(beanFactory);
	}


	@Override
	public void afterConnectionEstablished(WebSocketSession session) throws Exception {
		WebSocketHandler handler = this.provider.getHandler();
		this.handlers.put(session, handler);
		handler.afterConnectionEstablished(session);
	}

	@Override
	public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
		getHandler(session).handleMessage(session, message);
	}

	@Override
	public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
		getHandler(session).handleTransportError(session, exception);
	}

	@Override
	public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
		try {
			getHandler(session).afterConnectionClosed(session, closeStatus);
		}
		finally {
			destroyHandler(session);
		}
	}

	@Override
	public boolean supportsPartialMessages() {
		return this.supportsPartialMessages;
	}


	private WebSocketHandler getHandler(WebSocketSession session) {
		WebSocketHandler handler = this.handlers.get(session);
		if (handler == null) {
			throw new IllegalStateException("WebSocketHandler not found for " + session);
		}
		return handler;
	}

	private void destroyHandler(WebSocketSession session) {
		WebSocketHandler handler = this.handlers.remove(session);
		try {
			if (handler != null) {
				this.provider.destroy(handler);
			}
		}
		catch (Throwable ex) {
			if (logger.isWarnEnabled()) {
				logger.warn("Error while destroying " + handler, ex);
			}
		}
	}


	@Override
	public String toString() {
		return "PerConnectionWebSocketHandlerProxy[handlerType=" + this.provider.getHandlerType() + "]";
	}

}
