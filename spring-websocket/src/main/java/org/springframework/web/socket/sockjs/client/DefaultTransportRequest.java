package org.springframework.web.socket.sockjs.client;

import java.net.URI;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.http.HttpHeaders;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.util.Assert;
import org.springframework.util.concurrent.ListenableFutureCallback;
import org.springframework.util.concurrent.SettableListenableFuture;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.sockjs.SockJsTransportFailureException;
import org.springframework.web.socket.sockjs.frame.SockJsMessageCodec;
import org.springframework.web.socket.sockjs.transport.TransportType;

/**
 * {@link TransportRequest}的默认实现.
 */
class DefaultTransportRequest implements TransportRequest {

	private static final Log logger = LogFactory.getLog(DefaultTransportRequest.class);


	private final SockJsUrlInfo sockJsUrlInfo;

	private final HttpHeaders handshakeHeaders;

	private final HttpHeaders httpRequestHeaders;

	private final Transport transport;

	private final TransportType serverTransportType;

	private SockJsMessageCodec codec;

	private Principal user;

	private long timeoutValue;

	private TaskScheduler timeoutScheduler;

	private final List<Runnable> timeoutTasks = new ArrayList<Runnable>();

	private DefaultTransportRequest fallbackRequest;


	public DefaultTransportRequest(SockJsUrlInfo sockJsUrlInfo,
			HttpHeaders handshakeHeaders, HttpHeaders httpRequestHeaders,
			Transport transport, TransportType serverTransportType, SockJsMessageCodec codec) {

		Assert.notNull(sockJsUrlInfo, "SockJsUrlInfo is required");
		Assert.notNull(transport, "Transport is required");
		Assert.notNull(serverTransportType, "TransportType is required");
		Assert.notNull(codec, "SockJsMessageCodec is required");
		this.sockJsUrlInfo = sockJsUrlInfo;
		this.handshakeHeaders = (handshakeHeaders != null ? handshakeHeaders : new HttpHeaders());
		this.httpRequestHeaders = (httpRequestHeaders != null ? httpRequestHeaders : new HttpHeaders());
		this.transport = transport;
		this.serverTransportType = serverTransportType;
		this.codec = codec;
	}


	@Override
	public SockJsUrlInfo getSockJsUrlInfo() {
		return this.sockJsUrlInfo;
	}

	@Override
	public HttpHeaders getHandshakeHeaders() {
		return this.handshakeHeaders;
	}

	@Override
	public HttpHeaders getHttpRequestHeaders() {
		return this.httpRequestHeaders;
	}

	@Override
	public URI getTransportUrl() {
		return this.sockJsUrlInfo.getTransportUrl(this.serverTransportType);
	}

	public void setUser(Principal user) {
		this.user = user;
	}

	@Override
	public Principal getUser() {
		return this.user;
	}

	@Override
	public SockJsMessageCodec getMessageCodec() {
		return this.codec;
	}

	public void setTimeoutValue(long timeoutValue) {
		this.timeoutValue = timeoutValue;
	}

	public void setTimeoutScheduler(TaskScheduler scheduler) {
		this.timeoutScheduler = scheduler;
	}

	@Override
	public void addTimeoutTask(Runnable runnable) {
		this.timeoutTasks.add(runnable);
	}

	public void setFallbackRequest(DefaultTransportRequest fallbackRequest) {
		this.fallbackRequest = fallbackRequest;
	}


	public void connect(WebSocketHandler handler, SettableListenableFuture<WebSocketSession> future) {
		if (logger.isTraceEnabled()) {
			logger.trace("Starting " + this);
		}
		ConnectCallback connectCallback = new ConnectCallback(handler, future);
		scheduleConnectTimeoutTask(connectCallback);
		this.transport.connect(this, handler).addCallback(connectCallback);
	}


	private void scheduleConnectTimeoutTask(ConnectCallback connectHandler) {
		if (this.timeoutScheduler != null) {
			if (logger.isTraceEnabled()) {
				logger.trace("Scheduling connect to time out after " + this.timeoutValue + " ms.");
			}
			Date timeoutDate = new Date(System.currentTimeMillis() + this.timeoutValue);
			this.timeoutScheduler.schedule(connectHandler, timeoutDate);
		}
		else if (logger.isTraceEnabled()) {
			logger.trace("Connect timeout task not scheduled (no TaskScheduler configured).");
		}
	}


	@Override
	public String toString() {
		return "TransportRequest[url=" + getTransportUrl() + "]";
	}


	/**
	 * 更新给定的(全局) Future, 基于整个SockJS请求的连接的成功或失败, 无论实际设置连接哪个传输.
	 * 还实现{@code Runnable}来处理定时的超时回调.
	 */
	private class ConnectCallback implements ListenableFutureCallback<WebSocketSession>, Runnable {

		private final WebSocketHandler handler;

		private final SettableListenableFuture<WebSocketSession> future;

		private final AtomicBoolean handled = new AtomicBoolean(false);

		public ConnectCallback(WebSocketHandler handler, SettableListenableFuture<WebSocketSession> future) {
			this.handler = handler;
			this.future = future;
		}

		@Override
		public void onSuccess(WebSocketSession session) {
			if (this.handled.compareAndSet(false, true)) {
				this.future.set(session);
			}
			else if (logger.isErrorEnabled()) {
				logger.error("Connect success/failure already handled for " + DefaultTransportRequest.this);
			}
		}

		@Override
		public void onFailure(Throwable ex) {
			handleFailure(ex, false);
		}

		@Override
		public void run() {
			handleFailure(null, true);
		}

		private void handleFailure(Throwable ex, boolean isTimeoutFailure) {
			if (this.handled.compareAndSet(false, true)) {
				if (isTimeoutFailure) {
					String message = "Connect timed out for " + DefaultTransportRequest.this;
					logger.error(message);
					ex = new SockJsTransportFailureException(message, getSockJsUrlInfo().getSessionId(), ex);
				}
				if (fallbackRequest != null) {
					logger.error(DefaultTransportRequest.this + " failed. Falling back on next transport.", ex);
					fallbackRequest.connect(this.handler, this.future);
				}
				else {
					logger.error("No more fallback transports after " + DefaultTransportRequest.this, ex);
					this.future.setException(ex);
				}
				if (isTimeoutFailure) {
					try {
						for (Runnable runnable : timeoutTasks) {
							runnable.run();
						}
					}
					catch (Throwable ex2) {
						logger.error("Transport failed to run timeout tasks for " + DefaultTransportRequest.this, ex2);
					}
				}
			}
			else {
				logger.error("Connect success/failure events already took place for " +
						DefaultTransportRequest.this + ". Ignoring this additional failure event.", ex);
			}
		}
	}

}
