package org.springframework.web.socket.server.support;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.context.Lifecycle;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.util.Assert;
import org.springframework.web.HttpRequestHandler;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.handler.ExceptionWebSocketHandlerDecorator;
import org.springframework.web.socket.handler.LoggingWebSocketHandlerDecorator;
import org.springframework.web.socket.server.HandshakeFailureException;
import org.springframework.web.socket.server.HandshakeHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

/**
 * 用于处理WebSocket握手请求的{@link HttpRequestHandler}.
 *
 * <p>这是在特定URL上配置服务器WebSocket时使用的主类.
 * 它是{@link WebSocketHandler} 和 {@link HandshakeHandler}的一个非常轻量级的包装器,
 * 也分别将{@link HttpServletRequest}和{@link HttpServletResponse}
 * 适配为{@link ServerHttpRequest}和{@link ServerHttpResponse}.
 */
public class WebSocketHttpRequestHandler implements HttpRequestHandler, Lifecycle, ServletContextAware {

	private final Log logger = LogFactory.getLog(WebSocketHttpRequestHandler.class);

	private final WebSocketHandler wsHandler;

	private final HandshakeHandler handshakeHandler;

	private final List<HandshakeInterceptor> interceptors = new ArrayList<HandshakeInterceptor>();

	private volatile boolean running = false;


	public WebSocketHttpRequestHandler(WebSocketHandler wsHandler) {
		this(wsHandler, new DefaultHandshakeHandler());
	}

	public WebSocketHttpRequestHandler(WebSocketHandler wsHandler, HandshakeHandler handshakeHandler) {
		Assert.notNull(wsHandler, "wsHandler must not be null");
		Assert.notNull(handshakeHandler, "handshakeHandler must not be null");
		this.wsHandler = new ExceptionWebSocketHandlerDecorator(new LoggingWebSocketHandlerDecorator(wsHandler));
		this.handshakeHandler = handshakeHandler;
	}


	public WebSocketHandler getWebSocketHandler() {
		return this.wsHandler;
	}

	public HandshakeHandler getHandshakeHandler() {
		return this.handshakeHandler;
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
	public void setServletContext(ServletContext servletContext) {
		if (this.handshakeHandler instanceof ServletContextAware) {
			((ServletContextAware) this.handshakeHandler).setServletContext(servletContext);
		}
	}


	@Override
	public void start() {
		if (!isRunning()) {
			this.running = true;
			if (this.handshakeHandler instanceof Lifecycle) {
				((Lifecycle) this.handshakeHandler).start();
			}
		}
	}

	@Override
	public void stop() {
		if (isRunning()) {
			this.running = false;
			if (this.handshakeHandler instanceof Lifecycle) {
				((Lifecycle) this.handshakeHandler).stop();
			}
		}
	}

	@Override
	public boolean isRunning() {
		return this.running;
	}


	@Override
	public void handleRequest(HttpServletRequest servletRequest, HttpServletResponse servletResponse)
			throws ServletException, IOException {

		ServerHttpRequest request = new ServletServerHttpRequest(servletRequest);
		ServerHttpResponse response = new ServletServerHttpResponse(servletResponse);

		HandshakeInterceptorChain chain = new HandshakeInterceptorChain(this.interceptors, this.wsHandler);
		HandshakeFailureException failure = null;

		try {
			if (logger.isDebugEnabled()) {
				logger.debug(servletRequest.getMethod() + " " + servletRequest.getRequestURI());
			}
			Map<String, Object> attributes = new HashMap<String, Object>();
			if (!chain.applyBeforeHandshake(request, response, attributes)) {
				return;
			}
			this.handshakeHandler.doHandshake(request, response, this.wsHandler, attributes);
			chain.applyAfterHandshake(request, response, null);
			response.close();
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

}
