package org.springframework.web.socket.messaging;

import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ScheduledFuture;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.context.Lifecycle;
import org.springframework.context.SmartLifecycle;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.BufferingStompDecoder;
import org.springframework.messaging.simp.stomp.ConnectionHandlingStompSession;
import org.springframework.messaging.simp.stomp.StompClientSupport;
import org.springframework.messaging.simp.stomp.StompDecoder;
import org.springframework.messaging.simp.stomp.StompEncoder;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandler;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.messaging.tcp.TcpConnection;
import org.springframework.messaging.tcp.TcpConnectionHandler;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.util.Assert;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;
import org.springframework.util.concurrent.SettableListenableFuture;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.sockjs.transport.SockJsSession;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * WebSocket客户端上的STOMP, 它使用包含
 * {@link org.springframework.web.socket.sockjs.client.SockJsClient SockJsClient}
 * 的{@link org.springframework.web.socket.client.WebSocketClient WebSocketClient}实现进行连接.
 */
public class WebSocketStompClient extends StompClientSupport implements SmartLifecycle {

	private static final Log logger = LogFactory.getLog(WebSocketStompClient.class);

	private final WebSocketClient webSocketClient;

	private int inboundMessageSizeLimit = 64 * 1024;

	private boolean autoStartup = true;

	private int phase = Integer.MAX_VALUE;

	private volatile boolean running = false;


	/**
	 * 将{@link #setDefaultHeartbeat}设置为"0,0", 但在配置{@link #setTaskScheduler}时将其重置为首选"10000,10000".
	 * 
	 * @param webSocketClient 要连接的WebSocket客户端
	 */
	public WebSocketStompClient(WebSocketClient webSocketClient) {
		Assert.notNull(webSocketClient, "WebSocketClient is required");
		this.webSocketClient = webSocketClient;
		setDefaultHeartbeat(new long[] {0, 0});
	}


	/**
	 * 返回配置的WebSocketClient.
	 */
	public WebSocketClient getWebSocketClient() {
		return this.webSocketClient;
	}

	/**
	 * {@inheritDoc}
	 * <p>如果当前设置为"0,0", 还会自动将{@link #setDefaultHeartbeat defaultHeartbeat}属性设置为"10000,10000".
	 */
	@Override
	public void setTaskScheduler(TaskScheduler taskScheduler) {
		if (taskScheduler != null && !isDefaultHeartbeatEnabled()) {
			setDefaultHeartbeat(new long[] {10000, 10000});
		}
		super.setTaskScheduler(taskScheduler);
	}

	/**
	 * 配置入站STOMP消息允许的最大大小.
	 * 由于可以在多个WebSocket消息中接收STOMP消息, 因此可能需要缓冲, 并且此属性确定每条消息的最大缓冲区大小.
	 * <p>默认为 64 * 1024 (64K).
	 */
	public void setInboundMessageSizeLimit(int inboundMessageSizeLimit) {
		this.inboundMessageSizeLimit = inboundMessageSizeLimit;
	}

	/**
	 * 获取配置的入站消息缓冲区大小, 以字节为单位.
	 */
	public int getInboundMessageSizeLimit() {
		return this.inboundMessageSizeLimit;
	}

	/**
	 * 设置是否在刷新Spring上下文时自动启动包含的WebSocketClient.
	 * <p>默认为 "true".
	 */
	public void setAutoStartup(boolean autoStartup) {
		this.autoStartup = autoStartup;
	}

	/**
	 * 返回'autoStartup'属性的值.
	 * 如果为"true", 则此客户端将自动启动和停止包含的WebSocketClient.
	 */
	@Override
	public boolean isAutoStartup() {
		return this.autoStartup;
	}

	/**
	 * 指定应启动WebSocket客户端并随后关闭的阶段.
	 * 启动顺序从最低到最高, 关闭顺序与此相反.
	 * <p>默认为 Integer.MAX_VALUE, 这意味着WebSocket客户端尽可能晚启动并尽快停止.
	 */
	public void setPhase(int phase) {
		this.phase = phase;
	}

	/**
	 * 返回配置的阶段.
	 */
	@Override
	public int getPhase() {
		return this.phase;
	}


	@Override
	public void start() {
		if (!isRunning()) {
			this.running = true;
			if (getWebSocketClient() instanceof Lifecycle) {
				((Lifecycle) getWebSocketClient()).start();
			}
		}

	}

	@Override
	public void stop() {
		if (isRunning()) {
			this.running = false;
			if (getWebSocketClient() instanceof Lifecycle) {
				((Lifecycle) getWebSocketClient()).stop();
			}
		}
	}

	@Override
	public void stop(Runnable callback) {
		stop();
		callback.run();
	}

	@Override
	public boolean isRunning() {
		return this.running;
	}


	/**
	 * 连接到给定的WebSocket URL, 并在STOMP级别连接且收到CONNECTED帧后通知给定的
	 * {@link org.springframework.messaging.simp.stomp.StompSessionHandler}.
	 * 
	 * @param url 要连接的url
	 * @param handler 会话处理器
	 * @param uriVars 要扩展到URL的URI变量
	 * 
	 * @return 在准备使用时用于访问会话的ListenableFuture
	 */
	public ListenableFuture<StompSession> connect(String url, StompSessionHandler handler, Object... uriVars) {
		return connect(url, null, handler, uriVars);
	}

	/**
	 * {@link #connect(String, StompSessionHandler, Object...)}的重载版本, 也接受{@link WebSocketHttpHeaders}用于WebSocket握手.
	 * 
	 * @param url 要连接的url
	 * @param handshakeHeaders WebSocket握手的header
	 * @param handler 会话处理器
	 * @param uriVariables 要扩展到URL的URI变量
	 * 
	 * @return 在准备使用时用于访问会话的ListenableFuture
	 */
	public ListenableFuture<StompSession> connect(String url, WebSocketHttpHeaders handshakeHeaders,
			StompSessionHandler handler, Object... uriVariables) {

		return connect(url, handshakeHeaders, null, handler, uriVariables);
	}

	/**
	 * {@link #connect(String, StompSessionHandler, Object...)}的重载版本,
	 * 也接受用于WebSocket握手的{@link WebSocketHttpHeaders}, 以及用于STOMP CONNECT帧的{@link StompHeaders}.
	 * 
	 * @param url 要连接的url
	 * @param handshakeHeaders WebSocket握手的header
	 * @param connectHeaders STOMP CONNECT帧的header
	 * @param handler 会话处理器
	 * @param uriVariables 要扩展到URL的URI变量
	 * 
	 * @return 在准备使用时用于访问会话的ListenableFuture
	 */
	public ListenableFuture<StompSession> connect(String url, WebSocketHttpHeaders handshakeHeaders,
			StompHeaders connectHeaders, StompSessionHandler handler, Object... uriVariables) {

		Assert.notNull(url, "'url' must not be null");
		URI uri = UriComponentsBuilder.fromUriString(url).buildAndExpand(uriVariables).encode().toUri();
		return connect(uri, handshakeHeaders, connectHeaders, handler);
	}

	/**
	 * {@link #connect(String, WebSocketHttpHeaders, StompSessionHandler, Object...)}的重载版本,
	 * 接受完全准备的{@link java.net.URI}.
	 * 
	 * @param url 要连接的url
	 * @param handshakeHeaders WebSocket握手的header
	 * @param connectHeaders STOMP CONNECT帧的header
	 * @param sessionHandler STOMP会话处理器
	 * 
	 * @return 在准备使用时用于访问会话的ListenableFuture
	 */
	public ListenableFuture<StompSession> connect(URI url, WebSocketHttpHeaders handshakeHeaders,
			StompHeaders connectHeaders, StompSessionHandler sessionHandler) {

		Assert.notNull(url, "'url' must not be null");
		ConnectionHandlingStompSession session = createSession(connectHeaders, sessionHandler);
		WebSocketTcpConnectionHandlerAdapter adapter = new WebSocketTcpConnectionHandlerAdapter(session);
		getWebSocketClient().doHandshake(adapter, handshakeHeaders, url).addCallback(adapter);
		return session.getSessionFuture();
	}

	@Override
	protected StompHeaders processConnectHeaders(StompHeaders connectHeaders) {
		connectHeaders = super.processConnectHeaders(connectHeaders);
		if (connectHeaders.isHeartbeatEnabled()) {
			Assert.state(getTaskScheduler() != null, "TaskScheduler must be set if heartbeats are enabled");
		}
		return connectHeaders;
	}


	/**
	 * 使WebSocket适配TcpConnectionHandler和TcpConnection约定.
	 */
	private class WebSocketTcpConnectionHandlerAdapter implements ListenableFutureCallback<WebSocketSession>,
			WebSocketHandler, TcpConnection<byte[]> {

		private final TcpConnectionHandler<byte[]> connectionHandler;

		private final StompWebSocketMessageCodec codec = new StompWebSocketMessageCodec(getInboundMessageSizeLimit());

		private volatile WebSocketSession session;

		private volatile long lastReadTime = -1;

		private volatile long lastWriteTime = -1;

		private final List<ScheduledFuture<?>> inactivityTasks = new ArrayList<ScheduledFuture<?>>(2);

		public WebSocketTcpConnectionHandlerAdapter(TcpConnectionHandler<byte[]> connectionHandler) {
			Assert.notNull(connectionHandler, "TcpConnectionHandler must not be null");
			this.connectionHandler = connectionHandler;
		}

		// ListenableFutureCallback implementation: handshake outcome

		@Override
		public void onSuccess(WebSocketSession webSocketSession) {
		}

		@Override
		public void onFailure(Throwable ex) {
			this.connectionHandler.afterConnectFailure(ex);
		}

		// WebSocketHandler implementation

		@Override
		public void afterConnectionEstablished(WebSocketSession session) {
			this.session = session;
			this.connectionHandler.afterConnected(this);
		}

		@Override
		public void handleMessage(WebSocketSession session, WebSocketMessage<?> webSocketMessage) {
			this.lastReadTime = (this.lastReadTime != -1 ? System.currentTimeMillis() : -1);
			List<Message<byte[]>> messages;
			try {
				messages = this.codec.decode(webSocketMessage);
			}
			catch (Throwable ex) {
				this.connectionHandler.handleFailure(ex);
				return;
			}
			for (Message<byte[]> message : messages) {
				this.connectionHandler.handleMessage(message);
			}
		}

		@Override
		public void handleTransportError(WebSocketSession session, Throwable ex) throws Exception {
			this.connectionHandler.handleFailure(ex);
		}

		@Override
		public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
			cancelInactivityTasks();
			this.connectionHandler.afterConnectionClosed();
		}

		private void cancelInactivityTasks() {
			for (ScheduledFuture<?> task : this.inactivityTasks) {
				try {
					task.cancel(true);
				}
				catch (Throwable ex) {
					// Ignore
				}
			}
			this.lastReadTime = -1;
			this.lastWriteTime = -1;
			this.inactivityTasks.clear();
		}

		@Override
		public boolean supportsPartialMessages() {
			return false;
		}

		// TcpConnection implementation

		@Override
		public ListenableFuture<Void> send(Message<byte[]> message) {
			updateLastWriteTime();
			SettableListenableFuture<Void> future = new SettableListenableFuture<Void>();
			try {
				this.session.sendMessage(this.codec.encode(message, this.session.getClass()));
				future.set(null);
			}
			catch (Throwable ex) {
				future.setException(ex);
			}
			finally {
				updateLastWriteTime();
			}
			return future;
		}

		private void updateLastWriteTime() {
			this.lastWriteTime = (this.lastWriteTime != -1 ? System.currentTimeMillis() : -1);
		}

		@Override
		public void onReadInactivity(final Runnable runnable, final long duration) {
			Assert.state(getTaskScheduler() != null, "No TaskScheduler configured");
			this.lastReadTime = System.currentTimeMillis();
			this.inactivityTasks.add(getTaskScheduler().scheduleWithFixedDelay(new Runnable() {
				@Override
				public void run() {
					if (System.currentTimeMillis() - lastReadTime > duration) {
						try {
							runnable.run();
						}
						catch (Throwable ex) {
							if (logger.isDebugEnabled()) {
								logger.debug("ReadInactivityTask failure", ex);
							}
						}
					}
				}
			}, duration / 2));
		}

		@Override
		public void onWriteInactivity(final Runnable runnable, final long duration) {
			Assert.state(getTaskScheduler() != null, "No TaskScheduler configured");
			this.lastWriteTime = System.currentTimeMillis();
			this.inactivityTasks.add(getTaskScheduler().scheduleWithFixedDelay(new Runnable() {
				@Override
				public void run() {
					if (System.currentTimeMillis() - lastWriteTime > duration) {
						try {
							runnable.run();
						}
						catch (Throwable ex) {
							if (logger.isDebugEnabled()) {
								logger.debug("WriteInactivityTask failure", ex);
							}
						}
					}
				}
			}, duration / 2));
		}

		@Override
		public void close() {
			try {
				this.session.close();
			}
			catch (IOException ex) {
				if (logger.isDebugEnabled()) {
					logger.debug("Failed to close session: " + this.session.getId(), ex);
				}
			}
		}
	}


	/**
	 * 编码和解码STOMP WebSocket消息.
	 */
	private static class StompWebSocketMessageCodec {

		private static final StompEncoder ENCODER = new StompEncoder();

		private static final StompDecoder DECODER = new StompDecoder();

		private final BufferingStompDecoder bufferingDecoder;

		public StompWebSocketMessageCodec(int messageSizeLimit) {
			this.bufferingDecoder = new BufferingStompDecoder(DECODER, messageSizeLimit);
		}

		public List<Message<byte[]>> decode(WebSocketMessage<?> webSocketMessage) {
			List<Message<byte[]>> result = Collections.<Message<byte[]>>emptyList();
			ByteBuffer byteBuffer;
			if (webSocketMessage instanceof TextMessage) {
				byteBuffer = ByteBuffer.wrap(((TextMessage) webSocketMessage).asBytes());
			}
			else if (webSocketMessage instanceof BinaryMessage) {
				byteBuffer = ((BinaryMessage) webSocketMessage).getPayload();
			}
			else {
				return result;
			}
			result = this.bufferingDecoder.decode(byteBuffer);
			if (result.isEmpty()) {
				if (logger.isTraceEnabled()) {
					logger.trace("Incomplete STOMP frame content received, bufferSize=" +
							this.bufferingDecoder.getBufferSize() + ", bufferSizeLimit=" +
							this.bufferingDecoder.getBufferSizeLimit() + ".");
				}
			}
			return result;
		}

		public WebSocketMessage<?> encode(Message<byte[]> message, Class<? extends WebSocketSession> sessionType) {
			StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
			Assert.notNull(accessor, "No StompHeaderAccessor available");
			byte[] payload = message.getPayload();
			byte[] bytes = ENCODER.encode(accessor.getMessageHeaders(), payload);

			boolean useBinary = (payload.length > 0  &&
					!(SockJsSession.class.isAssignableFrom(sessionType)) &&
					MimeTypeUtils.APPLICATION_OCTET_STREAM.isCompatibleWith(accessor.getContentType()));

			return (useBinary ? new BinaryMessage(bytes) : new TextMessage(bytes));
		}
	}

}
