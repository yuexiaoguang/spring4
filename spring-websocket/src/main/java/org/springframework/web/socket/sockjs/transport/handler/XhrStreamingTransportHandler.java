package org.springframework.web.socket.sockjs.transport.handler;

import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.sockjs.frame.DefaultSockJsFrameFormat;
import org.springframework.web.socket.sockjs.frame.SockJsFrameFormat;
import org.springframework.web.socket.sockjs.transport.SockJsServiceConfig;
import org.springframework.web.socket.sockjs.transport.SockJsSession;
import org.springframework.web.socket.sockjs.transport.TransportHandler;
import org.springframework.web.socket.sockjs.transport.TransportType;
import org.springframework.web.socket.sockjs.transport.session.PollingSockJsSession;
import org.springframework.web.socket.sockjs.transport.session.StreamingSockJsSession;

/**
 * 通过HTTP流请求发送消息的{@link TransportHandler}.
 */
public class XhrStreamingTransportHandler extends AbstractHttpSendingTransportHandler {

	private static final byte[] PRELUDE = new byte[2049];

	static {
		for (int i = 0; i < 2048; i++) {
			PRELUDE[i] = 'h';
		}
		PRELUDE[2048] = '\n';
	}


	@Override
	public TransportType getTransportType() {
		return TransportType.XHR_STREAMING;
	}

	@Override
	protected MediaType getContentType() {
		return new MediaType("application", "javascript", UTF8_CHARSET);
	}

	@Override
	public boolean checkSessionType(SockJsSession session) {
		return session instanceof XhrStreamingSockJsSession;
	}

	@Override
	public StreamingSockJsSession createSession(
			String sessionId, WebSocketHandler handler, Map<String, Object> attributes) {

		return new XhrStreamingSockJsSession(sessionId, getServiceConfig(), handler, attributes);
	}

	@Override
	protected SockJsFrameFormat getFrameFormat(ServerHttpRequest request) {
		return new DefaultSockJsFrameFormat("%s\n");
	}


	private class XhrStreamingSockJsSession extends StreamingSockJsSession {

		public XhrStreamingSockJsSession(String sessionId, SockJsServiceConfig config,
				WebSocketHandler wsHandler, Map<String, Object> attributes) {

			super(sessionId, config, wsHandler, attributes);
		}

		@Override
		protected byte[] getPrelude(ServerHttpRequest request) {
			return PRELUDE;
		}
	}

}
