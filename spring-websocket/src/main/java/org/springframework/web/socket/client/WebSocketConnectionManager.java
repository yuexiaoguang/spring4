package org.springframework.web.socket.client;

import java.util.List;

import org.springframework.context.Lifecycle;
import org.springframework.http.HttpHeaders;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.LoggingWebSocketHandlerDecorator;

/**
 * 给定URI, {@link WebSocketClient}, 和{@link WebSocketHandler}的WebSocket连接管理器,
 * 通过{@link #start()} 和 {@link #stop()}方法连接到WebSocket服务器.
 * 如果{@link #setAutoStartup(boolean)}设置为{@code true}, 那么当Spring ApplicationContext刷新时, 这将自动完成.
 */
public class WebSocketConnectionManager extends ConnectionManagerSupport {

	private final WebSocketClient client;

	private final WebSocketHandler webSocketHandler;

	private WebSocketSession webSocketSession;

	private WebSocketHttpHeaders headers = new WebSocketHttpHeaders();


	public WebSocketConnectionManager(WebSocketClient client,
			WebSocketHandler webSocketHandler, String uriTemplate, Object... uriVariables) {

		super(uriTemplate, uriVariables);
		this.client = client;
		this.webSocketHandler = decorateWebSocketHandler(webSocketHandler);
	}


	/**
	 * 装饰提供给类构造函数的WebSocketHandler.
	 * <p>默认添加{@link LoggingWebSocketHandlerDecorator}.
	 */
	protected WebSocketHandler decorateWebSocketHandler(WebSocketHandler handler) {
		return new LoggingWebSocketHandlerDecorator(handler);
	}

	/**
	 * 设置要使用的子协议.
	 * 如果配置, 将通过{@code Sec-WebSocket-Protocol} header在握手中请求指定的子协议.
	 * 生成的WebSocket会话将包含服务器接受的协议.
	 */
	public void setSubProtocols(List<String> protocols) {
		this.headers.setSecWebSocketProtocol(protocols);
	}

	/**
	 * 返回要使用的配置的子协议.
	 */
	public List<String> getSubProtocols() {
		return this.headers.getSecWebSocketProtocol();
	}

	/**
	 * 设置要使用的原点.
	 */
	public void setOrigin(String origin) {
		this.headers.setOrigin(origin);
	}

	/**
	 * 返回配置的原点.
	 */
	public String getOrigin() {
		return this.headers.getOrigin();
	}

	/**
	 * 提供要添加到WebSocket握手请求的默认header.
	 */
	public void setHeaders(HttpHeaders headers) {
		this.headers.clear();
		this.headers.putAll(headers);
	}

	/**
	 * 返回WebSocket握手请求的默认header.
	 */
	public HttpHeaders getHeaders() {
		return this.headers;
	}


	@Override
	public void startInternal() {
		if (this.client instanceof Lifecycle && !((Lifecycle) client).isRunning()) {
			((Lifecycle) client).start();
		}
		super.startInternal();
	}

	@Override
	public void stopInternal() throws Exception {
		if (this.client instanceof Lifecycle && ((Lifecycle) client).isRunning()) {
			((Lifecycle) client).stop();
		}
		super.stopInternal();
	}

	@Override
	protected void openConnection() {
		if (logger.isInfoEnabled()) {
			logger.info("Connecting to WebSocket at " + getUri());
		}

		ListenableFuture<WebSocketSession> future =
				this.client.doHandshake(this.webSocketHandler, this.headers, getUri());

		future.addCallback(new ListenableFutureCallback<WebSocketSession>() {
			@Override
			public void onSuccess(WebSocketSession result) {
				webSocketSession = result;
				logger.info("Successfully connected");
			}
			@Override
			public void onFailure(Throwable ex) {
				logger.error("Failed to connect", ex);
			}
		});
	}

	@Override
	protected void closeConnection() throws Exception {
		if (this.webSocketSession != null) {
			this.webSocketSession.close();
		}
	}

	@Override
	protected boolean isConnected() {
		return (this.webSocketSession != null && this.webSocketSession.isOpen());
	}

}
