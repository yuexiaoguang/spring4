package org.springframework.web.socket.sockjs.client;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Collections;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.util.concurrent.SettableListenableFuture;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketExtension;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.sockjs.transport.TransportType;

/**
 * An extension of {@link AbstractClientSockJsSession} for use with HTTP
 * transports simulating a WebSocket session.
 */
public class XhrClientSockJsSession extends AbstractClientSockJsSession {

	private final XhrTransport transport;

	private HttpHeaders headers;

	private HttpHeaders sendHeaders;

	private final URI sendUrl;

	private int textMessageSizeLimit = -1;

	private int binaryMessageSizeLimit = -1;


	public XhrClientSockJsSession(TransportRequest request, WebSocketHandler handler,
			XhrTransport transport, SettableListenableFuture<WebSocketSession> connectFuture) {

		super(request, handler, connectFuture);
		Assert.notNull(transport, "'transport' is required");
		this.transport = transport;
		this.headers = request.getHttpRequestHeaders();
		this.sendHeaders = new HttpHeaders();
		if (this.headers != null) {
			this.sendHeaders.putAll(this.headers);
		}
		this.sendHeaders.setContentType(MediaType.APPLICATION_JSON);
		this.sendUrl = request.getSockJsUrlInfo().getTransportUrl(TransportType.XHR_SEND);
	}


	public HttpHeaders getHeaders() {
		return this.headers;
	}

	@Override
	public InetSocketAddress getLocalAddress() {
		return null;
	}

	@Override
	public InetSocketAddress getRemoteAddress() {
		return new InetSocketAddress(getUri().getHost(), getUri().getPort());
	}

	@Override
	public String getAcceptedProtocol() {
		return null;
	}

	@Override
	public void setTextMessageSizeLimit(int messageSizeLimit) {
		this.textMessageSizeLimit = messageSizeLimit;
	}

	@Override
	public int getTextMessageSizeLimit() {
		return this.textMessageSizeLimit;
	}

	@Override
	public void setBinaryMessageSizeLimit(int messageSizeLimit) {
		this.binaryMessageSizeLimit = -1;
	}

	@Override
	public int getBinaryMessageSizeLimit() {
		return this.binaryMessageSizeLimit;
	}

	@Override
	public List<WebSocketExtension> getExtensions() {
		return Collections.emptyList();
	}

	@Override
	protected void sendInternal(TextMessage message) {
		this.transport.executeSendRequest(this.sendUrl, this.sendHeaders, message);
	}

	@Override
	protected void disconnect(CloseStatus status) {
		// Nothing to do: XHR transports check if session is disconnected.
	}

}