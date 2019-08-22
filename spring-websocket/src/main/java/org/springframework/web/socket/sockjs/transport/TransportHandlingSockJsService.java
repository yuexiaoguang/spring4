package org.springframework.web.socket.sockjs.transport;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

import org.springframework.context.Lifecycle;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeFailureException;
import org.springframework.web.socket.server.HandshakeHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.socket.server.support.HandshakeInterceptorChain;
import org.springframework.web.socket.sockjs.SockJsException;
import org.springframework.web.socket.sockjs.frame.Jackson2SockJsMessageCodec;
import org.springframework.web.socket.sockjs.frame.SockJsMessageCodec;
import org.springframework.web.socket.sockjs.support.AbstractSockJsService;

/**
 * {@link org.springframework.web.socket.sockjs.SockJsService}的基本实现, 支持基于SPI的传输处理和会话管理.
 *
 * <p>基于{@link TransportHandler} SPI.
 * {@link TransportHandler}可以另外实现{@link SockJsSessionFactory}和{@link HandshakeHandler}接口.
 *
 * <p>有关请求映射的重要详细信息, 请参阅{@link AbstractSockJsService}基类.
 */
public class TransportHandlingSockJsService extends AbstractSockJsService implements SockJsServiceConfig, Lifecycle {

	private static final boolean jackson2Present = ClassUtils.isPresent(
			"com.fasterxml.jackson.databind.ObjectMapper", TransportHandlingSockJsService.class.getClassLoader());


	private final Map<TransportType, TransportHandler> handlers =
			new EnumMap<TransportType, TransportHandler>(TransportType.class);

	private SockJsMessageCodec messageCodec;

	private final List<HandshakeInterceptor> interceptors = new ArrayList<HandshakeInterceptor>();

	private final Map<String, SockJsSession> sessions = new ConcurrentHashMap<String, SockJsSession>();

	private ScheduledFuture<?> sessionCleanupTask;

	private volatile boolean running;


	/**
	 * @param scheduler 用于心跳消息和删除超时会话的任务调度器;
	 * 应该将提供的TaskScheduler声明为Spring bean, 以确保它在启动时初始化, 并在应用程序停止时关闭
	 * @param handlers 要使用的一个或多个{@link TransportHandler}实现
	 */
	public TransportHandlingSockJsService(TaskScheduler scheduler, TransportHandler... handlers) {
		this(scheduler, Arrays.asList(handlers));
	}

	/**
	 * @param scheduler 用于心跳消息和删除超时会话的任务调度器;
	 * 应该将提供的TaskScheduler声明为Spring bean, 以确保它在启动时初始化, 并在应用程序停止时关闭
	 * @param handlers 要使用的一个或多个{@link TransportHandler}实现
	 */
	public TransportHandlingSockJsService(TaskScheduler scheduler, Collection<TransportHandler> handlers) {
		super(scheduler);

		if (CollectionUtils.isEmpty(handlers)) {
			logger.warn("No transport handlers specified for TransportHandlingSockJsService");
		}
		else {
			for (TransportHandler handler : handlers) {
				handler.initialize(this);
				this.handlers.put(handler.getTransportType(), handler);
			}
		}

		if (jackson2Present) {
			this.messageCodec = new Jackson2SockJsMessageCodec();
		}
	}


	/**
	 * 返回每种传输类型的注册的处理器.
	 */
	public Map<TransportType, TransportHandler> getTransportHandlers() {
		return Collections.unmodifiableMap(this.handlers);
	}

	/**
	 * 用于编码和解码SockJS消息的编解码器.
	 */
	public void setMessageCodec(SockJsMessageCodec messageCodec) {
		this.messageCodec = messageCodec;
	}

	public SockJsMessageCodec getMessageCodec() {
		Assert.state(this.messageCodec != null, "A SockJsMessageCodec is required but not available: " +
				"Add Jackson to the classpath, or configure a custom SockJsMessageCodec.");
		return this.messageCodec;
	}

	/**
	 * 配置一个或多个WebSocket握手请求拦截器.
	 */
	public void setHandshakeInterceptors(List<HandshakeInterceptor> interceptors) {
		this.interceptors.clear();
		if (interceptors != null) {
			this.interceptors.addAll(interceptors);
		}
	}

	/**
	 * 返回配置的WebSocket握手请求拦截器.
	 */
	public List<HandshakeInterceptor> getHandshakeInterceptors() {
		return this.interceptors;
	}


	@Override
	public void start() {
		if (!isRunning()) {
			this.running = true;
			for (TransportHandler handler : this.handlers.values()) {
				if (handler instanceof Lifecycle) {
					((Lifecycle) handler).start();
				}
			}
		}
	}

	@Override
	public void stop() {
		if (isRunning()) {
			this.running = false;
			for (TransportHandler handler : this.handlers.values()) {
				if (handler instanceof Lifecycle) {
					((Lifecycle) handler).stop();
				}
			}
		}
	}

	@Override
	public boolean isRunning() {
		return this.running;
	}


	@Override
	protected void handleRawWebSocketRequest(ServerHttpRequest request, ServerHttpResponse response,
			WebSocketHandler handler) throws IOException {

		TransportHandler transportHandler = this.handlers.get(TransportType.WEBSOCKET);
		if (!(transportHandler instanceof HandshakeHandler)) {
			logger.error("No handler configured for raw WebSocket messages");
			response.setStatusCode(HttpStatus.NOT_FOUND);
			return;
		}

		HandshakeInterceptorChain chain = new HandshakeInterceptorChain(this.interceptors, handler);
		HandshakeFailureException failure = null;

		try {
			Map<String, Object> attributes = new HashMap<String, Object>();
			if (!chain.applyBeforeHandshake(request, response, attributes)) {
				return;
			}
			((HandshakeHandler) transportHandler).doHandshake(request, response, handler, attributes);
			chain.applyAfterHandshake(request, response, null);
		}
		catch (HandshakeFailureException ex) {
			failure = ex;
		}
		catch (Throwable ex) {
			failure = new HandshakeFailureException("Uncaught failure for request " + request.getURI(), ex);
		}
			finally {
			if (failure != null) {
				chain.applyAfterHandshake(request, response, failure);
				throw failure;
			}
		}
	}

	@Override
	protected void handleTransportRequest(ServerHttpRequest request, ServerHttpResponse response,
			WebSocketHandler handler, String sessionId, String transport) throws SockJsException {

		TransportType transportType = TransportType.fromValue(transport);
		if (transportType == null) {
			if (logger.isWarnEnabled()) {
				logger.warn("Unknown transport type for " + request.getURI());
			}
			response.setStatusCode(HttpStatus.NOT_FOUND);
			return;
		}

		TransportHandler transportHandler = this.handlers.get(transportType);
		if (transportHandler == null) {
			if (logger.isWarnEnabled()) {
				logger.warn("No TransportHandler for " + request.getURI());
			}
			response.setStatusCode(HttpStatus.NOT_FOUND);
			return;
		}

		SockJsException failure = null;
		HandshakeInterceptorChain chain = new HandshakeInterceptorChain(this.interceptors, handler);

		try {
			HttpMethod supportedMethod = transportType.getHttpMethod();
			if (supportedMethod != request.getMethod()) {
				if (request.getMethod() == HttpMethod.OPTIONS && transportType.supportsCors()) {
					if (checkOrigin(request, response, HttpMethod.OPTIONS, supportedMethod)) {
						response.setStatusCode(HttpStatus.NO_CONTENT);
						addCacheHeaders(response);
					}
				}
				else if (transportType.supportsCors()) {
					sendMethodNotAllowed(response, supportedMethod, HttpMethod.OPTIONS);
				}
				else {
					sendMethodNotAllowed(response, supportedMethod);
				}
				return;
			}

			SockJsSession session = this.sessions.get(sessionId);
			if (session == null) {
				if (transportHandler instanceof SockJsSessionFactory) {
					Map<String, Object> attributes = new HashMap<String, Object>();
					if (!chain.applyBeforeHandshake(request, response, attributes)) {
						return;
					}
					SockJsSessionFactory sessionFactory = (SockJsSessionFactory) transportHandler;
					session = createSockJsSession(sessionId, sessionFactory, handler, attributes);
				}
				else {
					response.setStatusCode(HttpStatus.NOT_FOUND);
					if (logger.isDebugEnabled()) {
						logger.debug("Session not found, sessionId=" + sessionId +
								". The session may have been closed " +
								"(e.g. missed heart-beat) while a message was coming in.");
					}
					return;
				}
			}
			else {
				if (session.getPrincipal() != null) {
					if (!session.getPrincipal().equals(request.getPrincipal())) {
						logger.debug("The user for the session does not match the user for the request.");
						response.setStatusCode(HttpStatus.NOT_FOUND);
						return;
					}
				}
				if (!transportHandler.checkSessionType(session)) {
					logger.debug("Session type does not match the transport type for the request.");
					response.setStatusCode(HttpStatus.NOT_FOUND);
					return;
				}
			}

			if (transportType.sendsNoCacheInstruction()) {
				addNoCacheHeaders(response);
			}

			if (transportType.supportsCors()) {
				if (!checkOrigin(request, response)) {
					return;
				}
			}


			transportHandler.handleRequest(request, response, handler, session);


			chain.applyAfterHandshake(request, response, null);
		}
		catch (SockJsException ex) {
			failure = ex;
		}
		catch (Throwable ex) {
			failure = new SockJsException("Uncaught failure for request " + request.getURI(), sessionId, ex);
		}
		finally {
			if (failure != null) {
				chain.applyAfterHandshake(request, response, failure);
				throw failure;
			}
		}
	}

	@Override
	protected boolean validateRequest(String serverId, String sessionId, String transport) {
		if (!super.validateRequest(serverId, sessionId, transport)) {
			return false;
		}

		if (!this.allowedOrigins.contains("*")) {
			TransportType transportType = TransportType.fromValue(transport);
			if (transportType == null || !transportType.supportsOrigin()) {
				if (logger.isWarnEnabled()) {
					logger.warn("Origin check enabled but transport '" + transport + "' does not support it.");
				}
				return false;
			}
		}

		return true;
	}

	private SockJsSession createSockJsSession(String sessionId, SockJsSessionFactory sessionFactory,
			WebSocketHandler handler, Map<String, Object> attributes) {

		SockJsSession session = this.sessions.get(sessionId);
		if (session != null) {
			return session;
		}
		if (this.sessionCleanupTask == null) {
			scheduleSessionTask();
		}
		session = sessionFactory.createSession(sessionId, handler, attributes);
		this.sessions.put(sessionId, session);
		return session;
	}

	private void scheduleSessionTask() {
		synchronized (this.sessions) {
			if (this.sessionCleanupTask != null) {
				return;
			}
			this.sessionCleanupTask = getTaskScheduler().scheduleAtFixedRate(new Runnable() {
				@Override
				public void run() {
					List<String> removedIds = new ArrayList<String>();
					for (SockJsSession session : sessions.values()) {
						try {
							if (session.getTimeSinceLastActive() > getDisconnectDelay()) {
								sessions.remove(session.getId());
								removedIds.add(session.getId());
								session.close();
							}
						}
						catch (Throwable ex) {
							// 可以成为正常工作流程的一部分 (e.g. 浏览器标签页已关闭)
							logger.debug("Failed to close " + session, ex);
						}
					}
					if (logger.isDebugEnabled() && !removedIds.isEmpty()) {
						logger.debug("Closed " + removedIds.size() + " sessions: " + removedIds);
					}
				}
			}, getDisconnectDelay());
		}
	}

}
