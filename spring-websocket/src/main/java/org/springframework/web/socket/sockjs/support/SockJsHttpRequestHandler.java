package org.springframework.web.socket.sockjs.support;

import java.io.IOException;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.context.Lifecycle;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.util.Assert;
import org.springframework.web.HttpRequestHandler;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.handler.ExceptionWebSocketHandlerDecorator;
import org.springframework.web.socket.handler.LoggingWebSocketHandlerDecorator;
import org.springframework.web.socket.sockjs.SockJsException;
import org.springframework.web.socket.sockjs.SockJsService;

/**
 * 一个{@link HttpRequestHandler}, 它允许将{@link SockJsService}映射到Servlet容器中的请求.
 */
public class SockJsHttpRequestHandler
		implements HttpRequestHandler, CorsConfigurationSource, Lifecycle, ServletContextAware {

	// No logging: HTTP传输过于冗长, 不知道记录哪些有价值的内容

	private final SockJsService sockJsService;

	private final WebSocketHandler webSocketHandler;

	private volatile boolean running = false;


	/**
	 * @param sockJsService SockJS服务
	 * @param webSocketHandler websocket处理器
	 */
	public SockJsHttpRequestHandler(SockJsService sockJsService, WebSocketHandler webSocketHandler) {
		Assert.notNull(sockJsService, "SockJsService must not be null");
		Assert.notNull(webSocketHandler, "WebSocketHandler must not be null");
		this.sockJsService = sockJsService;
		this.webSocketHandler =
				new ExceptionWebSocketHandlerDecorator(new LoggingWebSocketHandlerDecorator(webSocketHandler));
	}


	public SockJsService getSockJsService() {
		return this.sockJsService;
	}

	public WebSocketHandler getWebSocketHandler() {
		return this.webSocketHandler;
	}

	@Override
	public void setServletContext(ServletContext servletContext) {
		if (this.sockJsService instanceof ServletContextAware) {
			((ServletContextAware) this.sockJsService).setServletContext(servletContext);
		}
	}


	@Override
	public void start() {
		if (!isRunning()) {
			this.running = true;
			if (this.sockJsService instanceof Lifecycle) {
				((Lifecycle) this.sockJsService).start();
			}
		}
	}

	@Override
	public void stop() {
		if (isRunning()) {
			this.running = false;
			if (this.sockJsService instanceof Lifecycle) {
				((Lifecycle) this.sockJsService).stop();
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

		try {
			this.sockJsService.handleRequest(request, response, getSockJsPath(servletRequest), this.webSocketHandler);
		}
		catch (Throwable ex) {
			throw new SockJsException("Uncaught failure in SockJS request, uri=" + request.getURI(), ex);
		}
	}

	private String getSockJsPath(HttpServletRequest servletRequest) {
		String attribute = HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE;
		String path = (String) servletRequest.getAttribute(attribute);
		return (path.length() > 0 && path.charAt(0) != '/' ? "/" + path : path);
	}

	@Override
	public CorsConfiguration getCorsConfiguration(HttpServletRequest request) {
		if (this.sockJsService instanceof CorsConfigurationSource) {
			return ((CorsConfigurationSource) this.sockJsService).getCorsConfiguration(request);
		}
		return null;
	}

}
