package org.springframework.web.socket.sockjs.transport.session;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.NestedExceptionUtils;
import org.springframework.util.Assert;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.sockjs.SockJsMessageDeliveryException;
import org.springframework.web.socket.sockjs.SockJsTransportFailureException;
import org.springframework.web.socket.sockjs.frame.SockJsFrame;
import org.springframework.web.socket.sockjs.frame.SockJsMessageCodec;
import org.springframework.web.socket.sockjs.transport.SockJsServiceConfig;
import org.springframework.web.socket.sockjs.transport.SockJsSession;

/**
 * 实现{@link SockJsSession}的SockJS会话的抽象基类.
 */
public abstract class AbstractSockJsSession implements SockJsSession {

	private enum State {NEW, OPEN, CLOSED}


	/**
	 * 客户端离开后用于网络IO异常的日志类别.
	 * <p>当客户端断开连接时, Servlet API不会提供通知;
	 * see <a href="https://java.net/jira/browse/SERVLET_SPEC-44">SERVLET_SPEC-44</a>.
	 * 因此, 网络IO故障可能仅仅因为客户端已经消失, 并且可以用不必要的堆栈跟踪填充日志.
	 * <p>尽最大努力在每个服务器的基础上识别此类网络故障, 并将它们记录在单独的日志类别下.
	 * 在DEBUG级别记录一个简单的单行消息, 而在TRACE级别显示完整的堆栈跟踪.
	 */
	public static final String DISCONNECTED_CLIENT_LOG_CATEGORY =
			"org.springframework.web.socket.sockjs.DisconnectedClient";

	/**
	 * Tomcat: ClientAbortException 或 EOFException
	 * Jetty: EofException
	 * WildFly, GlassFish: java.io.IOException "Broken pipe" (already covered)
	 */
	private static final Set<String> DISCONNECTED_CLIENT_EXCEPTIONS =
			new HashSet<String>(Arrays.asList("ClientAbortException", "EOFException", "EofException"));


	/**
	 * 在客户端离开后, 用于网络IO故障的单独的记录器.
	 */
	protected static final Log disconnectedClientLogger = LogFactory.getLog(DISCONNECTED_CLIENT_LOG_CATEGORY);

	protected final Log logger = LogFactory.getLog(getClass());

	protected final Object responseLock = new Object();

	private final String id;

	private final SockJsServiceConfig config;

	private final WebSocketHandler handler;

	private final Map<String, Object> attributes = new ConcurrentHashMap<String, Object>();

	private volatile State state = State.NEW;

	private final long timeCreated = System.currentTimeMillis();

	private volatile long timeLastActive = this.timeCreated;

	private ScheduledFuture<?> heartbeatFuture;

	private HeartbeatTask heartbeatTask;

	private volatile boolean heartbeatDisabled;


	/**
	 * @param id 会话 ID
	 * @param config SockJS服务配置选项
	 * @param handler SockJS消息的接收者
	 * @param attributes 来自HTTP握手的属性, 要与WebSocket会话关联; 复制提供的属性, 不使用原始Map.
	 */
	public AbstractSockJsSession(String id, SockJsServiceConfig config, WebSocketHandler handler,
			Map<String, Object> attributes) {

		Assert.notNull(id, "Session id must not be null");
		Assert.notNull(config, "SockJsServiceConfig must not be null");
		Assert.notNull(handler, "WebSocketHandler must not be null");

		this.id = id;
		this.config = config;
		this.handler = handler;

		if (attributes != null) {
			this.attributes.putAll(attributes);
		}
	}


	@Override
	public String getId() {
		return this.id;
	}

	protected SockJsMessageCodec getMessageCodec() {
		return this.config.getMessageCodec();
	}

	public SockJsServiceConfig getSockJsServiceConfig() {
		return this.config;
	}

	@Override
	public Map<String, Object> getAttributes() {
		return this.attributes;
	}


	// Message sending

	public final void sendMessage(WebSocketMessage<?> message) throws IOException {
		Assert.state(!isClosed(), "Cannot send a message when session is closed");
		Assert.isInstanceOf(TextMessage.class, message, "SockJS supports text messages only");
		sendMessageInternal(((TextMessage) message).getPayload());
	}

	protected abstract void sendMessageInternal(String message) throws IOException;


	// Lifecycle related methods

	public boolean isNew() {
		return State.NEW.equals(this.state);
	}

	@Override
	public boolean isOpen() {
		return State.OPEN.equals(this.state);
	}

	public boolean isClosed() {
		return State.CLOSED.equals(this.state);
	}

	/**
	 * 执行清理并通知{@link WebSocketHandler}.
	 */
	@Override
	public final void close() throws IOException {
		close(new CloseStatus(3000, "Go away!"));
	}

	/**
	 * 执行清理并通知{@link WebSocketHandler}.
	 */
	@Override
	public final void close(CloseStatus status) throws IOException {
		if (isOpen()) {
			if (logger.isDebugEnabled()) {
				logger.debug("Closing SockJS session " + getId() + " with " + status);
			}
			this.state = State.CLOSED;
			try {
				if (isActive() && !CloseStatus.SESSION_NOT_RELIABLE.equals(status)) {
					try {
						writeFrameInternal(SockJsFrame.closeFrame(status.getCode(), status.getReason()));
					}
					catch (Throwable ex) {
						logger.debug("Failure while sending SockJS close frame", ex);
					}
				}
				updateLastActiveTime();
				cancelHeartbeat();
				disconnect(status);
			}
			finally {
				try {
					this.handler.afterConnectionClosed(this, status);
				}
				catch (Throwable ex) {
					logger.debug("Error from WebSocketHandler.afterConnectionClosed in " + this, ex);
				}
			}
		}
	}

	@Override
	public long getTimeSinceLastActive() {
		if (isNew()) {
			return (System.currentTimeMillis() - this.timeCreated);
		}
		else {
			return (isActive() ? 0 : System.currentTimeMillis() - this.timeLastActive);
		}
	}

	/**
	 * 只要会话变为非活动状态, 就应该调用它.
	 */
	protected void updateLastActiveTime() {
		this.timeLastActive = System.currentTimeMillis();
	}

	@Override
	public void disableHeartbeat() {
		this.heartbeatDisabled = true;
		cancelHeartbeat();
	}

	public void sendHeartbeat() throws SockJsTransportFailureException {
		synchronized (this.responseLock) {
			if (isActive() && !this.heartbeatDisabled) {
				writeFrame(SockJsFrame.heartbeatFrame());
				scheduleHeartbeat();
			}
		}
	}

	protected void scheduleHeartbeat() {
		if (this.heartbeatDisabled) {
			return;
		}
		synchronized (this.responseLock) {
			cancelHeartbeat();
			if (!isActive()) {
				return;
			}
			Date time = new Date(System.currentTimeMillis() + this.config.getHeartbeatTime());
			this.heartbeatTask = new HeartbeatTask();
			this.heartbeatFuture = this.config.getTaskScheduler().schedule(this.heartbeatTask, time);
			if (logger.isTraceEnabled()) {
				logger.trace("Scheduled heartbeat in session " + getId());
			}
		}
	}

	protected void cancelHeartbeat() {
		synchronized (this.responseLock) {
			if (this.heartbeatFuture != null) {
				if (logger.isTraceEnabled()) {
					logger.trace("Cancelling heartbeat in session " + getId());
				}
				this.heartbeatFuture.cancel(false);
				this.heartbeatFuture = null;
			}
			if (this.heartbeatTask != null) {
				this.heartbeatTask.cancel();
				this.heartbeatTask = null;
			}
		}
	}

	/**
	 * 轮询和流式会话定期关闭当前的HTTP请求, 并等待下一个请求到来.
	 * 在此"downtime"期间, 会话仍处于打开状态, 但处于非活动状态, 且无法发送消息, 因此必须暂时缓冲它们.
	 * 相比之下, WebSocket会话是有状态的, 并且在关闭之前保持活动状态.
	 */
	public abstract boolean isActive();

	/**
	 * 实际关闭底层WebSocket会话, 或在HTTP传输的情况下完成底层请求.
	 */
	protected abstract void disconnect(CloseStatus status) throws IOException;


	// Frame writing

	/**
	 * 用于TransportHandler和 (特定于TransportHandler)会话类中的内部使用.
	 */
	protected void writeFrame(SockJsFrame frame) throws SockJsTransportFailureException {
		if (logger.isTraceEnabled()) {
			logger.trace("Preparing to write " + frame);
		}
		try {
			writeFrameInternal(frame);
		}
		catch (Throwable ex) {
			logWriteFrameFailure(ex);
			try {
				// 强制断开连接 (所以不会尝试发送close帧)
				disconnect(CloseStatus.SERVER_ERROR);
			}
			catch (Throwable disconnectFailure) {
				// Ignore
			}
			try {
				close(CloseStatus.SERVER_ERROR);
			}
			catch (Throwable closeFailure) {
				// Nothing of consequence, already forced disconnect
			}
			throw new SockJsTransportFailureException("Failed to write " + frame, getId(), ex);
		}
	}

	protected abstract void writeFrameInternal(SockJsFrame frame) throws IOException;

	private void logWriteFrameFailure(Throwable ex) {
		if (indicatesDisconnectedClient(ex)) {
			if (disconnectedClientLogger.isTraceEnabled()) {
				disconnectedClientLogger.trace("Looks like the client has gone away", ex);
			}
			else if (disconnectedClientLogger.isDebugEnabled()) {
				disconnectedClientLogger.debug("Looks like the client has gone away: " + ex +
						" (For a full stack trace, set the log category '" + DISCONNECTED_CLIENT_LOG_CATEGORY +
						"' to TRACE level.)");
			}
		}
		else {
			logger.debug("Terminating connection after failure to send message to client", ex);
		}
	}

	private boolean indicatesDisconnectedClient(Throwable ex)  {
		String message = NestedExceptionUtils.getMostSpecificCause(ex).getMessage();
		message = (message != null ? message.toLowerCase() : "");
		String className = ex.getClass().getSimpleName();
		return (message.contains("broken pipe") || DISCONNECTED_CLIENT_EXCEPTIONS.contains(className));
	}


	// Delegation methods

	public void delegateConnectionEstablished() throws Exception {
		this.state = State.OPEN;
		this.handler.afterConnectionEstablished(this);
	}

	public void delegateMessages(String... messages) throws SockJsMessageDeliveryException {
		List<String> undelivered = new ArrayList<String>(Arrays.asList(messages));
		for (String message : messages) {
			try {
				if (isClosed()) {
					throw new SockJsMessageDeliveryException(this.id, undelivered, "Session closed");
				}
				else {
					this.handler.handleMessage(this, new TextMessage(message));
					undelivered.remove(0);
				}
			}
			catch (Throwable ex) {
				throw new SockJsMessageDeliveryException(this.id, undelivered, ex);
			}
		}
	}

	/**
	 * 关闭底层连接时调用.
	 */
	public final void delegateConnectionClosed(CloseStatus status) throws Exception {
		if (!isClosed()) {
			try {
				updateLastActiveTime();
				// 避免服务器"close"回调中的 cancelHeartbeat() 和responseLock
				ScheduledFuture<?> future = this.heartbeatFuture;
				if (future != null) {
					this.heartbeatFuture = null;
					future.cancel(false);
				}
			}
			finally {
				this.state = State.CLOSED;
				this.handler.afterConnectionClosed(this, status);
			}
		}
	}

	/**
	 * 由于SockJS运输处理引起的错误而关闭.
	 */
	public void tryCloseWithSockJsTransportError(Throwable error, CloseStatus closeStatus) {
		if (logger.isDebugEnabled()) {
			logger.debug("Closing due to transport error for " + this);
		}
		try {
			delegateError(error);
		}
		catch (Throwable delegateException) {
			// Ignore
			logger.debug("Exception from error handling delegate", delegateException);
		}
		try {
			close(closeStatus);
		}
		catch (Throwable closeException) {
			logger.debug("Failure while closing " + this, closeException);
		}
	}

	public void delegateError(Throwable ex) throws Exception {
		this.handler.handleTransportError(this, ex);
	}


	// Self description

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[id=" + getId() + "]";
	}


	private class HeartbeatTask implements Runnable {

		private boolean expired;

		@Override
		public void run() {
			synchronized (responseLock) {
				if (!this.expired && !isClosed()) {
					try {
						sendHeartbeat();
					}
					catch (Throwable ex) {
						// Ignore: already handled in writeFrame...
					}
					finally {
						this.expired = true;
					}
				}
			}
		}

		void cancel() {
			this.expired = true;
		}
	}

}
