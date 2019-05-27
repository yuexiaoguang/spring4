package org.springframework.web.socket.adapter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.util.Assert;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.PingMessage;
import org.springframework.web.socket.PongMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;

/**
 * An abstract base class for implementations of {@link WebSocketSession}.
 */
public abstract class AbstractWebSocketSession<T> implements NativeWebSocketSession {

	protected static final Log logger = LogFactory.getLog(NativeWebSocketSession.class);

	private final Map<String, Object> attributes = new ConcurrentHashMap<String, Object>();

	private T nativeSession;


	/**
	 * Create a new instance and associate the given attributes with it.
	 * @param attributes attributes from the HTTP handshake to associate with the WebSocket
	 * session; the provided attributes are copied, the original map is not used.
	 */
	public AbstractWebSocketSession(Map<String, Object> attributes) {
		if (attributes != null) {
			this.attributes.putAll(attributes);
		}
	}


	@Override
	public Map<String, Object> getAttributes() {
		return this.attributes;
	}

	@Override
	public T getNativeSession() {
		return this.nativeSession;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <R> R getNativeSession(Class<R> requiredType) {
		if (requiredType != null) {
			if (requiredType.isInstance(this.nativeSession)) {
				return (R) this.nativeSession;
			}
		}
		return null;
	}

	public void initializeNativeSession(T session) {
		Assert.notNull(session, "WebSocket session must not be null");
		this.nativeSession = session;
	}

	protected final void checkNativeSessionInitialized() {
		Assert.state(this.nativeSession != null, "WebSocket session is not yet initialized");
	}

	@Override
	public final void sendMessage(WebSocketMessage<?> message) throws IOException {
		checkNativeSessionInitialized();

		if (logger.isTraceEnabled()) {
			logger.trace("Sending " + message + ", " + this);
		}

		if (message instanceof TextMessage) {
			sendTextMessage((TextMessage) message);
		}
		else if (message instanceof BinaryMessage) {
			sendBinaryMessage((BinaryMessage) message);
		}
		else if (message instanceof PingMessage) {
			sendPingMessage((PingMessage) message);
		}
		else if (message instanceof PongMessage) {
			sendPongMessage((PongMessage) message);
		}
		else {
			throw new IllegalStateException("Unexpected WebSocketMessage type: " + message);
		}
	}

	protected abstract void sendTextMessage(TextMessage message) throws IOException;

	protected abstract void sendBinaryMessage(BinaryMessage message) throws IOException;

	protected abstract void sendPingMessage(PingMessage message) throws IOException;

	protected abstract void sendPongMessage(PongMessage message) throws IOException;


	@Override
	public final void close() throws IOException {
		close(CloseStatus.NORMAL);
	}

	@Override
	public final void close(CloseStatus status) throws IOException {
		checkNativeSessionInitialized();
		if (logger.isDebugEnabled()) {
			logger.debug("Closing " + this);
		}
		closeInternal(status);
	}

	protected abstract void closeInternal(CloseStatus status) throws IOException;


	@Override
	public String toString() {
		if (this.nativeSession != null) {
			return getClass().getSimpleName() + "[id=" + getId() + ", uri=" + getUri() + "]";
		}
		else {
			return getClass().getSimpleName() + "[nativeSession=null]";
		}
	}

}
