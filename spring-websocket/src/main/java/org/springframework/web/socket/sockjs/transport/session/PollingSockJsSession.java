package org.springframework.web.socket.sockjs.transport.session;

import java.io.IOException;
import java.util.Map;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.sockjs.SockJsTransportFailureException;
import org.springframework.web.socket.sockjs.frame.SockJsFrame;
import org.springframework.web.socket.sockjs.frame.SockJsMessageCodec;
import org.springframework.web.socket.sockjs.transport.SockJsServiceConfig;

/**
 * 用于轮询HTTP传输的SockJS会话.
 */
public class PollingSockJsSession extends AbstractHttpSockJsSession {

	public PollingSockJsSession(String sessionId, SockJsServiceConfig config,
			WebSocketHandler wsHandler, Map<String, Object> attributes) {

		super(sessionId, config, wsHandler, attributes);
	}


	/**
	 * @deprecated as of 4.2 this method is no longer used.
	 */
	@Override
	@Deprecated
	protected boolean isStreaming() {
		return false;
	}

	@Override
	protected void handleRequestInternal(ServerHttpRequest request, ServerHttpResponse response,
			boolean initialRequest) throws IOException {

		if (initialRequest) {
			writeFrame(SockJsFrame.openFrame());
		}
		else if (!getMessageCache().isEmpty()) {
			flushCache();
		}
		else {
			scheduleHeartbeat();
		}
	}

	@Override
	protected void flushCache() throws SockJsTransportFailureException {
		String[] messages = new String[getMessageCache().size()];
		for (int i = 0; i < messages.length; i++) {
			messages[i] = getMessageCache().poll();
		}
		SockJsMessageCodec messageCodec = getSockJsServiceConfig().getMessageCodec();
		SockJsFrame frame = SockJsFrame.messageFrame(messageCodec, messages);
		writeFrame(frame);
	}

	@Override
	protected void writeFrame(SockJsFrame frame) throws SockJsTransportFailureException {
		super.writeFrame(frame);
		resetRequest();
	}

}

