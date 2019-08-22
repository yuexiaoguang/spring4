package org.springframework.web.socket.sockjs.transport.handler;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.sockjs.SockJsException;
import org.springframework.web.socket.sockjs.frame.SockJsMessageCodec;
import org.springframework.web.socket.sockjs.transport.TransportHandler;
import org.springframework.web.socket.sockjs.transport.TransportType;
import org.springframework.web.socket.sockjs.transport.session.AbstractHttpSockJsSession;

/**
 * 通过HTTP接收消息的{@link TransportHandler}.
 *
 * @deprecated 将从Spring Framework 5.1中删除, 改为使用其他传输.
 */
@Deprecated
public class JsonpReceivingTransportHandler extends AbstractHttpReceivingTransportHandler {

	private final FormHttpMessageConverter formConverter = new FormHttpMessageConverter();


	@Override
	public TransportType getTransportType() {
		return TransportType.JSONP_SEND;
	}

	@Override
	public void handleRequestInternal(ServerHttpRequest request, ServerHttpResponse response,
			WebSocketHandler wsHandler, AbstractHttpSockJsSession sockJsSession) throws SockJsException {

		super.handleRequestInternal(request, response, wsHandler, sockJsSession);
		try {
			response.getBody().write("ok".getBytes(UTF8_CHARSET));
		}
		catch (IOException ex) {
			throw new SockJsException("Failed to write to the response body", sockJsSession.getId(), ex);
		}
	}

	@Override
	protected String[] readMessages(ServerHttpRequest request) throws IOException {
		SockJsMessageCodec messageCodec = getServiceConfig().getMessageCodec();
		MediaType contentType = request.getHeaders().getContentType();
		if (contentType != null && MediaType.APPLICATION_FORM_URLENCODED.isCompatibleWith(contentType)) {
			MultiValueMap<String, String> map = this.formConverter.read(null, request);
			String d = map.getFirst("d");
			return (StringUtils.hasText(d) ? messageCodec.decode(d) : null);
		}
		else {
			return messageCodec.decodeInputStream(request.getBody());
		}
	}

	@Override
	protected HttpStatus getResponseStatus() {
		return HttpStatus.OK;
	}

}
