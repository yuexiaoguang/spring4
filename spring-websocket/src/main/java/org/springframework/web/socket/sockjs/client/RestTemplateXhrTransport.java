package org.springframework.web.socket.sockjs.client;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.Assert;
import org.springframework.util.StreamUtils;
import org.springframework.util.concurrent.SettableListenableFuture;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.ResponseExtractor;
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.UnknownHttpStatusCodeException;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.sockjs.frame.SockJsFrame;

/**
 * {@code XhrTransport}实现, 使用{@link org.springframework.web.client.RestTemplate RestTemplate}.
 */
public class RestTemplateXhrTransport extends AbstractXhrTransport {

	private final RestOperations restTemplate;

	private TaskExecutor taskExecutor = new SimpleAsyncTaskExecutor();


	public RestTemplateXhrTransport() {
		this(new RestTemplate());
	}

	public RestTemplateXhrTransport(RestOperations restTemplate) {
		Assert.notNull(restTemplate, "'restTemplate' is required");
		this.restTemplate = restTemplate;
	}


	/**
	 * 返回配置的{@code RestTemplate}.
	 */
	public RestOperations getRestTemplate() {
		return this.restTemplate;
	}

	/**
	 * 配置用于执行XHR接收请求的{@code TaskExecutor}.
	 * <p>默认配置{@link org.springframework.core.task.SimpleAsyncTaskExecutor SimpleAsyncTaskExecutor},
	 * 每次传输连接时都会创建一个新线程.
	 */
	public void setTaskExecutor(TaskExecutor taskExecutor) {
		Assert.notNull(taskExecutor, "TaskExecutor must not be null");
		this.taskExecutor = taskExecutor;
	}

	/**
	 * 返回配置的{@code TaskExecutor}.
	 */
	public TaskExecutor getTaskExecutor() {
		return this.taskExecutor;
	}


	@Override
	protected void connectInternal(final TransportRequest transportRequest, final WebSocketHandler handler,
			final URI receiveUrl, final HttpHeaders handshakeHeaders, final XhrClientSockJsSession session,
			final SettableListenableFuture<WebSocketSession> connectFuture) {

		getTaskExecutor().execute(new Runnable() {
			@Override
			public void run() {
				HttpHeaders httpHeaders = transportRequest.getHttpRequestHeaders();
				XhrRequestCallback requestCallback = new XhrRequestCallback(handshakeHeaders);
				XhrRequestCallback requestCallbackAfterHandshake = new XhrRequestCallback(httpHeaders);
				XhrReceiveExtractor responseExtractor = new XhrReceiveExtractor(session);
				while (true) {
					if (session.isDisconnected()) {
						session.afterTransportClosed(null);
						break;
					}
					try {
						if (logger.isTraceEnabled()) {
							logger.trace("Starting XHR receive request, url=" + receiveUrl);
						}
						getRestTemplate().execute(receiveUrl, HttpMethod.POST, requestCallback, responseExtractor);
						requestCallback = requestCallbackAfterHandshake;
					}
					catch (Throwable ex) {
						if (!connectFuture.isDone()) {
							connectFuture.setException(ex);
						}
						else {
							session.handleTransportError(ex);
							session.afterTransportClosed(new CloseStatus(1006, ex.getMessage()));
						}
						break;
					}
				}
			}
		});
	}

	@Override
	protected ResponseEntity<String> executeInfoRequestInternal(URI infoUrl, HttpHeaders headers) {
		RequestCallback requestCallback = new XhrRequestCallback(headers);
		return this.restTemplate.execute(infoUrl, HttpMethod.GET, requestCallback, textResponseExtractor);
	}

	@Override
	public ResponseEntity<String> executeSendRequestInternal(URI url, HttpHeaders headers, TextMessage message) {
		RequestCallback requestCallback = new XhrRequestCallback(headers, message.getPayload());
		return this.restTemplate.execute(url, HttpMethod.POST, requestCallback, textResponseExtractor);
	}


	/**
	 * 一个简单的ResponseExtractor, 它将主体读入String.
	 */
	private final static ResponseExtractor<ResponseEntity<String>> textResponseExtractor =
			new ResponseExtractor<ResponseEntity<String>>() {
				@Override
				public ResponseEntity<String> extractData(ClientHttpResponse response) throws IOException {
					if (response.getBody() == null) {
						return new ResponseEntity<String>(response.getHeaders(), response.getStatusCode());
					}
					else {
						String body = StreamUtils.copyToString(response.getBody(), SockJsFrame.CHARSET);
						return new ResponseEntity<String>(body, response.getHeaders(), response.getStatusCode());
					}
				}
			};

	/**
	 * RequestCallback, 用于添加header和 (可选) String内容.
	 */
	private static class XhrRequestCallback implements RequestCallback {

		private final HttpHeaders headers;

		private final String body;

		public XhrRequestCallback(HttpHeaders headers) {
			this(headers, null);
		}

		public XhrRequestCallback(HttpHeaders headers, String body) {
			this.headers = headers;
			this.body = body;
		}

		@Override
		public void doWithRequest(ClientHttpRequest request) throws IOException {
			if (this.headers != null) {
				request.getHeaders().putAll(this.headers);
			}
			if (this.body != null) {
				StreamUtils.copy(this.body, SockJsFrame.CHARSET, request.getBody());
			}
		}
	}

	/**
	 * 将HTTP响应的主体拆分为SockJS帧, 并将其委托给{@link XhrClientSockJsSession}.
	 */
	private class XhrReceiveExtractor implements ResponseExtractor<Object> {

		private final XhrClientSockJsSession sockJsSession;

		public XhrReceiveExtractor(XhrClientSockJsSession sockJsSession) {
			this.sockJsSession = sockJsSession;
		}

		@Override
		public Object extractData(ClientHttpResponse response) throws IOException {
			try {
				if (!HttpStatus.OK.equals(response.getStatusCode())) {
					throw new HttpServerErrorException(response.getStatusCode());
				}
			}
			catch (IllegalArgumentException ex) {
				throw new UnknownHttpStatusCodeException(
						response.getRawStatusCode(), response.getStatusText(), response.getHeaders(), null, null);
			}

			if (logger.isTraceEnabled()) {
				logger.trace("XHR receive headers: " + response.getHeaders());
			}
			InputStream is = response.getBody();
			ByteArrayOutputStream os = new ByteArrayOutputStream();

			while (true) {
				if (this.sockJsSession.isDisconnected()) {
					if (logger.isDebugEnabled()) {
						logger.debug("SockJS sockJsSession closed, closing response.");
					}
					response.close();
					break;
				}
				int b = is.read();
				if (b == -1) {
					if (os.size() > 0) {
						handleFrame(os);
					}
					if (logger.isTraceEnabled()) {
						logger.trace("XHR receive completed");
					}
					break;
				}
				if (b == '\n') {
					handleFrame(os);
				}
				else {
					os.write(b);
				}
			}
			return null;
		}

		private void handleFrame(ByteArrayOutputStream os) {
			byte[] bytes = os.toByteArray();
			os.reset();
			String content = new String(bytes, SockJsFrame.CHARSET);
			if (logger.isTraceEnabled()) {
				logger.trace("XHR receive content: " + content);
			}
			if (!PRELUDE.equals(content)) {
				this.sockJsSession.handleFrame(new String(bytes, SockJsFrame.CHARSET));
			}
		}
	}

}
