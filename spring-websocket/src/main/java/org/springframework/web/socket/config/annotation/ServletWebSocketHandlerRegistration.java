package org.springframework.web.socket.config.annotation;

import java.util.Arrays;

import org.springframework.scheduling.TaskScheduler;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ObjectUtils;
import org.springframework.web.HttpRequestHandler;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.socket.server.support.WebSocketHttpRequestHandler;
import org.springframework.web.socket.sockjs.SockJsService;
import org.springframework.web.socket.sockjs.support.SockJsHttpRequestHandler;

/**
 * 用于配置{@link WebSocketHandler}请求处理的辅助类, 包括SockJS回退选项.
 */
public class ServletWebSocketHandlerRegistration
		extends AbstractWebSocketHandlerRegistration<MultiValueMap<HttpRequestHandler, String>> {

	public ServletWebSocketHandlerRegistration(TaskScheduler sockJsTaskScheduler) {
		super(sockJsTaskScheduler);
	}


	@Override
	protected MultiValueMap<HttpRequestHandler, String> createMappings() {
		return new LinkedMultiValueMap<HttpRequestHandler, String>();
	}

	@Override
	protected void addSockJsServiceMapping(MultiValueMap<HttpRequestHandler, String> mappings,
			SockJsService sockJsService, WebSocketHandler handler, String pathPattern) {

		SockJsHttpRequestHandler httpHandler = new SockJsHttpRequestHandler(sockJsService, handler);
		mappings.add(httpHandler, pathPattern);
	}

	@Override
	protected void addWebSocketHandlerMapping(MultiValueMap<HttpRequestHandler, String> mappings,
			WebSocketHandler wsHandler, HandshakeHandler handshakeHandler,
			HandshakeInterceptor[] interceptors, String path) {

		WebSocketHttpRequestHandler httpHandler = new WebSocketHttpRequestHandler(wsHandler, handshakeHandler);
		if (!ObjectUtils.isEmpty(interceptors)) {
			httpHandler.setHandshakeInterceptors(Arrays.asList(interceptors));
		}
		mappings.add(httpHandler, path);
	}

}
