package org.springframework.web.socket.sockjs.transport.handler;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.sockjs.SockJsException;
import org.springframework.web.socket.sockjs.SockJsTransportFailureException;
import org.springframework.web.socket.sockjs.frame.DefaultSockJsFrameFormat;
import org.springframework.web.socket.sockjs.frame.SockJsFrameFormat;
import org.springframework.web.socket.sockjs.transport.SockJsSession;
import org.springframework.web.socket.sockjs.transport.TransportType;
import org.springframework.web.socket.sockjs.transport.session.AbstractHttpSockJsSession;
import org.springframework.web.socket.sockjs.transport.session.PollingSockJsSession;
import org.springframework.web.util.JavaScriptUtils;

/**
 * 通过JSONP轮询发送消息的TransportHandler.
 *
 * @deprecated 将从Spring Framework 5.1中删除, 改为使用其他传输.
 */
@Deprecated
public class JsonpPollingTransportHandler extends AbstractHttpSendingTransportHandler {

	@Override
	public TransportType getTransportType() {
		return TransportType.JSONP;
	}

	@Override
	protected MediaType getContentType() {
		return new MediaType("application", "javascript", UTF8_CHARSET);
	}

	@Override
	public boolean checkSessionType(SockJsSession session) {
		return session instanceof PollingSockJsSession;
	}

	@Override
	public PollingSockJsSession createSession(
			String sessionId, WebSocketHandler handler, Map<String, Object> attributes) {

		return new PollingSockJsSession(sessionId, getServiceConfig(), handler, attributes);
	}

	@Override
	public void handleRequestInternal(ServerHttpRequest request, ServerHttpResponse response,
			AbstractHttpSockJsSession sockJsSession) throws SockJsException {

		try {
			String callback = getCallbackParam(request);
			if (!StringUtils.hasText(callback)) {
				response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
				response.getBody().write("\"callback\" parameter required".getBytes(UTF8_CHARSET));
				return;
			}
		}
		catch (Throwable ex) {
			sockJsSession.tryCloseWithSockJsTransportError(ex, CloseStatus.SERVER_ERROR);
			throw new SockJsTransportFailureException("Failed to send error", sockJsSession.getId(), ex);
		}

		super.handleRequestInternal(request, response, sockJsSession);
	}

	@Override
	protected SockJsFrameFormat getFrameFormat(ServerHttpRequest request) {
		// We already validated the parameter above...
		String callback = getCallbackParam(request);

		return new DefaultSockJsFrameFormat("/**/" + callback + "(\"%s\");\r\n") {
			@Override
			protected String preProcessContent(String content) {
				return JavaScriptUtils.javaScriptEscape(content);
			}
		};
	}

}
