package org.springframework.web.socket.sockjs.transport.session;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.security.Principal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.servlet.ServletRequest;

import org.springframework.http.HttpHeaders;
import org.springframework.http.server.ServerHttpAsyncRequestControl;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.filter.ShallowEtagHeaderFilter;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketExtension;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.sockjs.SockJsException;
import org.springframework.web.socket.sockjs.SockJsTransportFailureException;
import org.springframework.web.socket.sockjs.frame.SockJsFrame;
import org.springframework.web.socket.sockjs.frame.SockJsFrameFormat;
import org.springframework.web.socket.sockjs.transport.SockJsServiceConfig;

/**
 * 用于HTTP传输SockJS会话的抽象基类.
 */
public abstract class AbstractHttpSockJsSession extends AbstractSockJsSession {

	private final Queue<String> messageCache;

	private volatile URI uri;

	private volatile HttpHeaders handshakeHeaders;

	private volatile Principal principal;

	private volatile InetSocketAddress localAddress;

	private volatile InetSocketAddress remoteAddress;

	private volatile String acceptedProtocol;

	private volatile ServerHttpResponse response;

	private volatile SockJsFrameFormat frameFormat;

	private volatile ServerHttpAsyncRequestControl asyncRequestControl;

	private boolean readyToSend;


	public AbstractHttpSockJsSession(String id, SockJsServiceConfig config,
			WebSocketHandler wsHandler, Map<String, Object> attributes) {

		super(id, config, wsHandler, attributes);
		this.messageCache = new LinkedBlockingQueue<String>(config.getHttpMessageCacheSize());
	}


	@Override
	public URI getUri() {
		return this.uri;
	}

	@Override
	public HttpHeaders getHandshakeHeaders() {
		return this.handshakeHeaders;
	}

	@Override
	public Principal getPrincipal() {
		return this.principal;
	}

	@Override
	public InetSocketAddress getLocalAddress() {
		return this.localAddress;
	}

	@Override
	public InetSocketAddress getRemoteAddress() {
		return this.remoteAddress;
	}

	/**
	 * 与子协议协商是初始握手的一部分的WebSocket不同,
	 * 在HTTP传输中, 必须模拟相同的协商并通过此setter设置所选协议.
	 * 
	 * @param protocol 要设置的子协议
	 */
	public void setAcceptedProtocol(String protocol) {
		this.acceptedProtocol = protocol;
	}

	/**
	 * 返回要使用的选择的子协议.
	 */
	public String getAcceptedProtocol() {
		return this.acceptedProtocol;
	}

	/**
	 * 返回SockJS缓冲区, 以便在轮询请求之间透明地存储消息.
	 * 如果轮询请求的时间超过5秒, 则会话将关闭.
	 */
	protected Queue<String> getMessageCache() {
		return this.messageCache;
	}

	@Override
	public boolean isActive() {
		ServerHttpAsyncRequestControl control = this.asyncRequestControl;
		return (control != null && !control.isCompleted());
	}

	@Override
	public void setTextMessageSizeLimit(int messageSizeLimit) {
		// ignore
	}

	@Override
	public int getTextMessageSizeLimit() {
		return -1;
	}

	@Override
	public void setBinaryMessageSizeLimit(int messageSizeLimit) {
		// ignore
	}

	@Override
	public int getBinaryMessageSizeLimit() {
		return -1;
	}

	@Override
	public List<WebSocketExtension> getExtensions() {
		return Collections.emptyList();
	}

	/**
	 * @deprecated as of 4.2, since this method is no longer used.
	 */
	@Deprecated
	protected abstract boolean isStreaming();


	/**
	 * 处理在基于SockJS HTTP传输的会话上接收消息的第一个请求.
	 * <p>基于长轮询的传输 (e.g. "xhr", "jsonp") 在写入open帧之后完成请求.
	 * 基于流的传输 ("xhr_streaming", "eventsource", 和"htmlfile")使响应打开的时间更长,
	 * 以便进一步传输消息帧, 但最终也会在发送一些数据后关闭它.
	 * 
	 * @param request 当前的请求
	 * @param response 当前的响应
	 * @param frameFormat 要使用的特定于传输的SocksJS帧格式
	 */
	public void handleInitialRequest(ServerHttpRequest request, ServerHttpResponse response,
			SockJsFrameFormat frameFormat) throws SockJsException {

		this.uri = request.getURI();
		this.handshakeHeaders = request.getHeaders();
		this.principal = request.getPrincipal();
		try {
			this.localAddress = request.getLocalAddress();
		}
		catch (Exception ex) {
			// Ignore
		}
		try {
			this.remoteAddress = request.getRemoteAddress();
		}
		catch (Exception ex) {
			// Ignore
		}

		synchronized (this.responseLock) {
			try {
				this.response = response;
				this.frameFormat = frameFormat;
				this.asyncRequestControl = request.getAsyncRequestControl(response);
				this.asyncRequestControl.start(-1);
				disableShallowEtagHeaderFilter(request);
				// 在将open帧发送到远程处理器之前, 让“我们的”处理器知道
				delegateConnectionEstablished();
				handleRequestInternal(request, response, true);
				// 请求可能已被重置 (e.g. 写入后的轮询会话)
				this.readyToSend = isActive();
			}
			catch (Throwable ex) {
				tryCloseWithSockJsTransportError(ex, CloseStatus.SERVER_ERROR);
				throw new SockJsTransportFailureException("Failed to open session", getId(), ex);
			}
		}
	}

	/**
	 * 处理除第一个请求之外的所有请求, 以在基于SockJS HTTP传输的会话上接收消息.
	 * <p>基于长轮询的传输 (e.g. "xhr", "jsonp") 在写入任何缓冲的消息帧 (或下一个)之后完成请求.
	 * 基于流的传输 ("xhr_streaming", "eventsource", 和"htmlfile") 使响应打开的时间更长,
	 * 以便进一步传输消息帧, 但最终也会在发送一些数据后关闭它.
	 * 
	 * @param request 当前的请求
	 * @param response 当前的响应
	 * @param frameFormat 要使用的特定于传输的SocksJS帧格式
	 */
	public void handleSuccessiveRequest(ServerHttpRequest request, ServerHttpResponse response,
			SockJsFrameFormat frameFormat) throws SockJsException {

		synchronized (this.responseLock) {
			try {
				if (isClosed()) {
					response.getBody().write(SockJsFrame.closeFrameGoAway().getContentBytes());
					return;
				}
				this.response = response;
				this.frameFormat = frameFormat;
				this.asyncRequestControl = request.getAsyncRequestControl(response);
				this.asyncRequestControl.start(-1);
				disableShallowEtagHeaderFilter(request);
				handleRequestInternal(request, response, false);
				this.readyToSend = isActive();
			}
			catch (Throwable ex) {
				tryCloseWithSockJsTransportError(ex, CloseStatus.SERVER_ERROR);
				throw new SockJsTransportFailureException("Failed to handle SockJS receive request", getId(), ex);
			}
		}
	}

	private void disableShallowEtagHeaderFilter(ServerHttpRequest request) {
		if (request instanceof ServletServerHttpRequest) {
			ServletRequest servletRequest = ((ServletServerHttpRequest) request).getServletRequest();
			ShallowEtagHeaderFilter.disableContentCaching(servletRequest);
		}
	}

	/**
	 * 收到SockJS传输请求时调用.
	 * 
	 * @param request 当前的请求
	 * @param response 当前的响应
	 * @param initialRequest 是否是会话的第一个请求
	 */
	protected abstract void handleRequestInternal(ServerHttpRequest request, ServerHttpResponse response,
			boolean initialRequest) throws IOException;

	@Override
	protected final void sendMessageInternal(String message) throws SockJsTransportFailureException {
		synchronized (this.responseLock) {
			this.messageCache.add(message);
			if (logger.isTraceEnabled()) {
				logger.trace(this.messageCache.size() + " message(s) to flush in session " + this.getId());
			}
			if (isActive() && this.readyToSend) {
				if (logger.isTraceEnabled()) {
					logger.trace("Session is active, ready to flush.");
				}
				cancelHeartbeat();
				flushCache();
			}
			else {
				if (logger.isTraceEnabled()) {
					logger.trace("Session is not active, not ready to flush.");
				}
			}
		}
	}

	/**
	 * 在连接处于活动状态并准备写入响应时调用.
	 * 子类只应从获取"responseLock"的方法中调用此方法.
	 */
	protected abstract void flushCache() throws SockJsTransportFailureException;


	/**
	 * @deprecated 从4.2开始, 不推荐使用此方法,
	 * 因为前缀是在StreamingSockJsSession子类的{@link #handleRequestInternal}中写入的.
	 */
	@Deprecated
	protected void writePrelude(ServerHttpRequest request, ServerHttpResponse response) throws IOException {
	}

	@Override
	protected void disconnect(CloseStatus status) {
		resetRequest();
	}

	protected void resetRequest() {
		synchronized (this.responseLock) {
			ServerHttpAsyncRequestControl control = this.asyncRequestControl;
			this.asyncRequestControl = null;
			this.readyToSend = false;
			this.response = null;
			updateLastActiveTime();
			if (control != null && !control.isCompleted()) {
				if (control.isStarted()) {
					try {
						control.complete();
					}
					catch (Throwable ex) {
						// 可以成为正常工作流程的一部分 (e.g. 浏览器标签页已关闭)
						logger.debug("Failed to complete request: " + ex.getMessage());
					}
				}
			}
		}
	}

	@Override
	protected void writeFrameInternal(SockJsFrame frame) throws IOException {
		if (isActive()) {
			String formattedFrame = this.frameFormat.format(frame);
			if (logger.isTraceEnabled()) {
				logger.trace("Writing to HTTP response: " + formattedFrame);
			}
			this.response.getBody().write(formattedFrame.getBytes(SockJsFrame.CHARSET));
			this.response.flush();
		}
	}
}
