package org.springframework.web.socket.config.annotation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.scheduling.TaskScheduler;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;
import org.springframework.web.socket.server.support.OriginHandshakeInterceptor;
import org.springframework.web.socket.sockjs.SockJsService;
import org.springframework.web.socket.sockjs.transport.handler.WebSocketTransportHandler;

/**
 * {@link WebSocketHandlerRegistration}的基类, 它收集所有配置选项, 但允许子类将实际的HTTP请求映射放在一起.
 */
public abstract class AbstractWebSocketHandlerRegistration<M> implements WebSocketHandlerRegistration {

	private final TaskScheduler sockJsTaskScheduler;

	private final MultiValueMap<WebSocketHandler, String> handlerMap =
			new LinkedMultiValueMap<WebSocketHandler, String>();

	private HandshakeHandler handshakeHandler;

	private final List<HandshakeInterceptor> interceptors = new ArrayList<HandshakeInterceptor>();

	private final List<String> allowedOrigins = new ArrayList<String>();

	private SockJsServiceRegistration sockJsServiceRegistration;


	public AbstractWebSocketHandlerRegistration(TaskScheduler defaultTaskScheduler) {
		this.sockJsTaskScheduler = defaultTaskScheduler;
	}


	@Override
	public WebSocketHandlerRegistration addHandler(WebSocketHandler handler, String... paths) {
		Assert.notNull(handler, "WebSocketHandler must not be null");
		Assert.notEmpty(paths, "Paths must not be empty");
		this.handlerMap.put(handler, Arrays.asList(paths));
		return this;
	}

	@Override
	public WebSocketHandlerRegistration setHandshakeHandler(HandshakeHandler handshakeHandler) {
		this.handshakeHandler = handshakeHandler;
		return this;
	}

	protected HandshakeHandler getHandshakeHandler() {
		return this.handshakeHandler;
	}

	@Override
	public WebSocketHandlerRegistration addInterceptors(HandshakeInterceptor... interceptors) {
		if (!ObjectUtils.isEmpty(interceptors)) {
			this.interceptors.addAll(Arrays.asList(interceptors));
		}
		return this;
	}

	@Override
	public WebSocketHandlerRegistration setAllowedOrigins(String... allowedOrigins) {
		this.allowedOrigins.clear();
		if (!ObjectUtils.isEmpty(allowedOrigins)) {
			this.allowedOrigins.addAll(Arrays.asList(allowedOrigins));
		}
		return this;
	}

	@Override
	public SockJsServiceRegistration withSockJS() {
		this.sockJsServiceRegistration = new SockJsServiceRegistration(this.sockJsTaskScheduler);
		HandshakeInterceptor[] interceptors = getInterceptors();
		if (interceptors.length > 0) {
			this.sockJsServiceRegistration.setInterceptors(interceptors);
		}
		if (this.handshakeHandler != null) {
			WebSocketTransportHandler transportHandler = new WebSocketTransportHandler(this.handshakeHandler);
			this.sockJsServiceRegistration.setTransportHandlerOverrides(transportHandler);
		}
		if (!this.allowedOrigins.isEmpty()) {
			this.sockJsServiceRegistration.setAllowedOrigins(StringUtils.toStringArray(this.allowedOrigins));
		}
		return this.sockJsServiceRegistration;
	}

	protected HandshakeInterceptor[] getInterceptors() {
		List<HandshakeInterceptor> interceptors =
				new ArrayList<HandshakeInterceptor>(this.interceptors.size() + 1);
		interceptors.addAll(this.interceptors);
		interceptors.add(new OriginHandshakeInterceptor(this.allowedOrigins));
		return interceptors.toArray(new HandshakeInterceptor[interceptors.size()]);
	}

	protected final M getMappings() {
		M mappings = createMappings();
		if (this.sockJsServiceRegistration != null) {
			SockJsService sockJsService = this.sockJsServiceRegistration.getSockJsService();
			for (WebSocketHandler wsHandler : this.handlerMap.keySet()) {
				for (String path : this.handlerMap.get(wsHandler)) {
					String pathPattern = (path.endsWith("/") ? path + "**" : path + "/**");
					addSockJsServiceMapping(mappings, sockJsService, wsHandler, pathPattern);
				}
			}
		}
		else {
			HandshakeHandler handshakeHandler = getOrCreateHandshakeHandler();
			HandshakeInterceptor[] interceptors = getInterceptors();
			for (WebSocketHandler wsHandler : this.handlerMap.keySet()) {
				for (String path : this.handlerMap.get(wsHandler)) {
					addWebSocketHandlerMapping(mappings, wsHandler, handshakeHandler, interceptors, path);
				}
			}
		}

		return mappings;
	}

	private HandshakeHandler getOrCreateHandshakeHandler() {
		return (this.handshakeHandler != null ? this.handshakeHandler : new DefaultHandshakeHandler());
	}


	protected abstract M createMappings();

	protected abstract void addSockJsServiceMapping(M mappings, SockJsService sockJsService,
			WebSocketHandler handler, String pathPattern);

	protected abstract void addWebSocketHandlerMapping(M mappings, WebSocketHandler wsHandler,
			HandshakeHandler handshakeHandler, HandshakeInterceptor[] interceptors, String path);

}
