package org.springframework.web.socket.client.standard;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Callable;
import javax.websocket.ClientEndpointConfig;
import javax.websocket.ClientEndpointConfig.Configurator;
import javax.websocket.ContainerProvider;
import javax.websocket.Endpoint;
import javax.websocket.Extension;
import javax.websocket.HandshakeResponse;
import javax.websocket.WebSocketContainer;

import org.springframework.core.task.AsyncListenableTaskExecutor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.UsesJava7;
import org.springframework.util.Assert;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureTask;
import org.springframework.web.socket.WebSocketExtension;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.adapter.standard.StandardWebSocketHandlerAdapter;
import org.springframework.web.socket.adapter.standard.StandardWebSocketSession;
import org.springframework.web.socket.adapter.standard.WebSocketToStandardExtensionAdapter;
import org.springframework.web.socket.client.AbstractWebSocketClient;

/**
 * 基于标准Java WebSocket API的WebSocketClient.
 */
public class StandardWebSocketClient extends AbstractWebSocketClient {

	private final WebSocketContainer webSocketContainer;

	private final Map<String,Object> userProperties = new HashMap<String, Object>();

	private AsyncListenableTaskExecutor taskExecutor = new SimpleAsyncTaskExecutor();


	/**
	 * 调用{@code ContainerProvider.getWebSocketContainer()}以获取{@link WebSocketContainer}实例的默认构造函数.
	 */
	public StandardWebSocketClient() {
		this.webSocketContainer = ContainerProvider.getWebSocketContainer();
	}

	/**
	 * 接受现有{@link WebSocketContainer}实例.
	 * <p>For XML configuration, see {@link WebSocketContainerFactoryBean}.
	 * 对于Java配置, 使用{@code ContainerProvider.getWebSocketContainer()}来获取{@code WebSocketContainer}实例.
	 */
	public StandardWebSocketClient(WebSocketContainer webSocketContainer) {
		Assert.notNull(webSocketContainer, "WebSocketContainer must not be null");
		this.webSocketContainer = webSocketContainer;
	}


	/**
	 * 标准的Java WebSocket API允许通过{@link ClientEndpointConfig#getUserProperties() userProperties}将"用户属性"传递给服务器.
	 * 使用此属性可配置每次握手时传递的一个或多个属性.
	 */
	public void setUserProperties(Map<String, Object> userProperties) {
		if (userProperties != null) {
			this.userProperties.putAll(userProperties);
		}
	}

	/**
	 * 配置的用户属性, 或{@code null}.
	 */
	public Map<String, Object> getUserProperties() {
		return this.userProperties;
	}

	/**
	 * 设置打开连接时使用的{@link AsyncListenableTaskExecutor}.
	 * 如果此属性设置为{@code null}, 则对任何{@code doHandshake}方法的调用将阻塞, 直到建立连接为止.
	 * <p>默认使用{@code SimpleAsyncTaskExecutor}的实例.
	 */
	public void setTaskExecutor(AsyncListenableTaskExecutor taskExecutor) {
		this.taskExecutor = taskExecutor;
	}

	/**
	 * 返回配置的{@link TaskExecutor}.
	 */
	public AsyncListenableTaskExecutor getTaskExecutor() {
		return this.taskExecutor;
	}


	@Override
	protected ListenableFuture<WebSocketSession> doHandshakeInternal(WebSocketHandler webSocketHandler,
			HttpHeaders headers, final URI uri, List<String> protocols,
			List<WebSocketExtension> extensions, Map<String, Object> attributes) {

		int port = getPort(uri);
		InetSocketAddress localAddress = new InetSocketAddress(getLocalHost(), port);
		InetSocketAddress remoteAddress = new InetSocketAddress(uri.getHost(), port);

		final StandardWebSocketSession session = new StandardWebSocketSession(headers,
				attributes, localAddress, remoteAddress);

		final ClientEndpointConfig endpointConfig = ClientEndpointConfig.Builder.create()
				.configurator(new StandardWebSocketClientConfigurator(headers))
				.preferredSubprotocols(protocols)
				.extensions(adaptExtensions(extensions)).build();

		endpointConfig.getUserProperties().putAll(getUserProperties());

		final Endpoint endpoint = new StandardWebSocketHandlerAdapter(webSocketHandler, session);

		Callable<WebSocketSession> connectTask = new Callable<WebSocketSession>() {
			@Override
			public WebSocketSession call() throws Exception {
				webSocketContainer.connectToServer(endpoint, endpointConfig, uri);
				return session;
			}
		};

		if (this.taskExecutor != null) {
			return this.taskExecutor.submitListenable(connectTask);
		}
		else {
			ListenableFutureTask<WebSocketSession> task = new ListenableFutureTask<WebSocketSession>(connectTask);
			task.run();
			return task;
		}
	}

	private static List<Extension> adaptExtensions(List<WebSocketExtension> extensions) {
		List<Extension> result = new ArrayList<Extension>();
		for (WebSocketExtension extension : extensions) {
			result.add(new WebSocketToStandardExtensionAdapter(extension));
		}
		return result;
	}

	@UsesJava7  // fallback to InetAddress.getLoopbackAddress()
	private InetAddress getLocalHost() {
		try {
			return InetAddress.getLocalHost();
		}
		catch (UnknownHostException ex) {
			return InetAddress.getLoopbackAddress();
		}
	}

	private int getPort(URI uri) {
		if (uri.getPort() == -1) {
	        String scheme = uri.getScheme().toLowerCase(Locale.ENGLISH);
	        return ("wss".equals(scheme) ? 443 : 80);
		}
		return uri.getPort();
	}


	private class StandardWebSocketClientConfigurator extends Configurator {

		private final HttpHeaders headers;

		public StandardWebSocketClientConfigurator(HttpHeaders headers) {
			this.headers = headers;
		}

		@Override
		public void beforeRequest(Map<String, List<String>> requestHeaders) {
			requestHeaders.putAll(this.headers);
			if (logger.isTraceEnabled()) {
				logger.trace("Handshake request headers: " + requestHeaders);
			}
		}
		@Override
		public void afterResponse(HandshakeResponse response) {
			if (logger.isTraceEnabled()) {
				logger.trace("Handshake response headers: " + response.getHeaders());
			}
		}
	}
}
