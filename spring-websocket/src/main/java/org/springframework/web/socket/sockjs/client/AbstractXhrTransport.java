package org.springframework.web.socket.sockjs.client;

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.SettableListenableFuture;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.sockjs.frame.SockJsFrame;
import org.springframework.web.socket.sockjs.transport.TransportType;

/**
 * 用于扩展的XHR传输实现的抽象基类.
 */
public abstract class AbstractXhrTransport implements XhrTransport {

	protected static final String PRELUDE;

	static {
		byte[] bytes = new byte[2048];
		for (int i = 0; i < bytes.length; i++) {
			bytes[i] = 'h';
		}
		PRELUDE = new String(bytes, SockJsFrame.CHARSET);
	}


	protected final Log logger = LogFactory.getLog(getClass());

	private boolean xhrStreamingDisabled;

	private HttpHeaders requestHeaders = new HttpHeaders();


	@Override
	public List<TransportType> getTransportTypes() {
		return (isXhrStreamingDisabled() ? Collections.singletonList(TransportType.XHR) :
				Arrays.asList(TransportType.XHR_STREAMING, TransportType.XHR));
	}

	/**
	 * {@code XhrTransport}可以支持 "xhr_streaming" 和 "xhr" SockJS服务器传输.
	 * 从客户的角度来看, 没有实现差异.
	 * <p>通常, {@code XhrTransport}首先用作"XHR 流", 然后如果失败则用作"XHR".
	 * 但是, 在某些情况下, 抑制XHR流可能有助于仅尝试XHR.
	 * <p>默认为{@code false}, 意味着"XHR 流" 和 "XHR"都适用.
	 */
	public void setXhrStreamingDisabled(boolean disabled) {
		this.xhrStreamingDisabled = disabled;
	}

	/**
	 * XHR流是否被禁用.
	 */
	public boolean isXhrStreamingDisabled() {
		return this.xhrStreamingDisabled;
	}

	/**
	 * 配置要添加到每个已执行的HTTP请求的header.
	 * 
	 * @param requestHeaders 要添加到请求的header
	 * 
	 * @deprecated as of 4.2 in favor of {@link SockJsClient#setHttpHeaderNames}.
	 */
	@Deprecated
	public void setRequestHeaders(HttpHeaders requestHeaders) {
		this.requestHeaders.clear();
		if (requestHeaders != null) {
			this.requestHeaders.putAll(requestHeaders);
		}
	}

	@Deprecated
	public HttpHeaders getRequestHeaders() {
		return this.requestHeaders;
	}


	// Transport methods

	@Override
	@SuppressWarnings("deprecation")
	public ListenableFuture<WebSocketSession> connect(TransportRequest request, WebSocketHandler handler) {
		SettableListenableFuture<WebSocketSession> connectFuture = new SettableListenableFuture<WebSocketSession>();
		XhrClientSockJsSession session = new XhrClientSockJsSession(request, handler, this, connectFuture);
		request.addTimeoutTask(session.getTimeoutTask());

		URI receiveUrl = request.getTransportUrl();
		if (logger.isDebugEnabled()) {
			logger.debug("Starting XHR " +
					(isXhrStreamingDisabled() ? "Polling" : "Streaming") + "session url=" + receiveUrl);
		}

		HttpHeaders handshakeHeaders = new HttpHeaders();
		handshakeHeaders.putAll(getRequestHeaders());
		handshakeHeaders.putAll(request.getHandshakeHeaders());

		connectInternal(request, handler, receiveUrl, handshakeHeaders, session, connectFuture);
		return connectFuture;
	}

	protected abstract void connectInternal(TransportRequest request, WebSocketHandler handler,
			URI receiveUrl, HttpHeaders handshakeHeaders, XhrClientSockJsSession session,
			SettableListenableFuture<WebSocketSession> connectFuture);


	// InfoReceiver methods

	@Override
	@SuppressWarnings("deprecation")
	public String executeInfoRequest(URI infoUrl, HttpHeaders headers) {
		if (logger.isDebugEnabled()) {
			logger.debug("Executing SockJS Info request, url=" + infoUrl);
		}
		HttpHeaders infoRequestHeaders = new HttpHeaders();
		infoRequestHeaders.putAll(getRequestHeaders());
		if (headers != null) {
			infoRequestHeaders.putAll(headers);
		}
		ResponseEntity<String> response = executeInfoRequestInternal(infoUrl, infoRequestHeaders);
		if (response.getStatusCode() != HttpStatus.OK) {
			if (logger.isErrorEnabled()) {
				logger.error("SockJS Info request (url=" + infoUrl + ") failed: " + response);
			}
			throw new HttpServerErrorException(response.getStatusCode());
		}
		if (logger.isTraceEnabled()) {
			logger.trace("SockJS Info request (url=" + infoUrl + ") response: " + response);
		}
		return response.getBody();
	}

	protected abstract ResponseEntity<String> executeInfoRequestInternal(URI infoUrl, HttpHeaders headers);


	// XhrTransport methods

	@Override
	public void executeSendRequest(URI url, HttpHeaders headers, TextMessage message) {
		if (logger.isTraceEnabled()) {
			logger.trace("Starting XHR send, url=" + url);
		}
		ResponseEntity<String> response = executeSendRequestInternal(url, headers, message);
		if (response.getStatusCode() != HttpStatus.NO_CONTENT) {
			if (logger.isErrorEnabled()) {
				logger.error("XHR send request (url=" + url + ") failed: " + response);
			}
			throw new HttpServerErrorException(response.getStatusCode());
		}
		if (logger.isTraceEnabled()) {
			logger.trace("XHR send request (url=" + url + ") response: " + response);
		}
	}

	protected abstract ResponseEntity<String> executeSendRequestInternal(
			URI url, HttpHeaders headers, TextMessage message);


	@Override
	public String toString() {
		return getClass().getSimpleName();
	}

}
