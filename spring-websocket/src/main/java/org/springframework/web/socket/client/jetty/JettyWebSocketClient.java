package org.springframework.web.socket.client.jetty;

import java.net.URI;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;

import org.springframework.context.Lifecycle;
import org.springframework.core.task.AsyncListenableTaskExecutor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.HttpHeaders;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureTask;
import org.springframework.web.socket.WebSocketExtension;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.adapter.jetty.JettyWebSocketHandlerAdapter;
import org.springframework.web.socket.adapter.jetty.JettyWebSocketSession;
import org.springframework.web.socket.adapter.jetty.WebSocketToJettyExtensionConfigAdapter;
import org.springframework.web.socket.client.AbstractWebSocketClient;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * 通过Jetty WebSocket API以编程方式启动对WebSocket服务器的WebSocket请求.
 *
 * <p>从4.1开始, 这个类实现了{@link Lifecycle}而不是{@link org.springframework.context.SmartLifecycle}.
 * 使用{@link org.springframework.web.socket.client.WebSocketConnectionManager WebSocketConnectionManager}
 * 来自动启动WebSocket连接.
 */
public class JettyWebSocketClient extends AbstractWebSocketClient implements Lifecycle {

	private final org.eclipse.jetty.websocket.client.WebSocketClient client;

	private AsyncListenableTaskExecutor taskExecutor = new SimpleAsyncTaskExecutor();


	/**
	 * 创建{@link org.eclipse.jetty.websocket.client.WebSocketClient}实例的默认构造函数.
	 */
	public JettyWebSocketClient() {
		this.client = new org.eclipse.jetty.websocket.client.WebSocketClient();
	}

	/**
	 * 接收现有{@link org.eclipse.jetty.websocket.client.WebSocketClient}实例的构造函数.
	 */
	public JettyWebSocketClient(WebSocketClient client) {
		this.client = client;
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
	 * 返回配置的 {@link TaskExecutor}.
	 */
	public AsyncListenableTaskExecutor getTaskExecutor() {
		return this.taskExecutor;
	}


	@Override
	public void start() {
		try {
			this.client.start();
		}
		catch (Exception ex) {
			throw new IllegalStateException("Failed to start Jetty WebSocketClient", ex);
		}
	}

	@Override
	public void stop() {
		try {
			this.client.stop();
		}
		catch (Exception ex) {
			logger.error("Failed to stop Jetty WebSocketClient", ex);
		}
	}

	@Override
	public boolean isRunning() {
		return this.client.isStarted();
	}


	@Override
	public ListenableFuture<WebSocketSession> doHandshake(WebSocketHandler webSocketHandler,
			String uriTemplate, Object... uriVars) {

		UriComponents uriComponents = UriComponentsBuilder.fromUriString(uriTemplate).buildAndExpand(uriVars).encode();
		return doHandshake(webSocketHandler, null, uriComponents.toUri());
	}

	@Override
	public ListenableFuture<WebSocketSession> doHandshakeInternal(WebSocketHandler wsHandler,
			HttpHeaders headers, final URI uri, List<String> protocols,
			List<WebSocketExtension> extensions,  Map<String, Object> attributes) {

		final ClientUpgradeRequest request = new ClientUpgradeRequest();
		request.setSubProtocols(protocols);

		for (WebSocketExtension e : extensions) {
			request.addExtensions(new WebSocketToJettyExtensionConfigAdapter(e));
		}

		for (String header : headers.keySet()) {
			request.setHeader(header, headers.get(header));
		}

		Principal user = getUser();
		final JettyWebSocketSession wsSession = new JettyWebSocketSession(attributes, user);
		final JettyWebSocketHandlerAdapter listener = new JettyWebSocketHandlerAdapter(wsHandler, wsSession);

		Callable<WebSocketSession> connectTask = new Callable<WebSocketSession>() {
			@Override
			public WebSocketSession call() throws Exception {
				Future<Session> future = client.connect(listener, uri, request);
				future.get();
				return wsSession;
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

	/**
	 * @return 通过{@link WebSocketSession#getPrincipal()}提供的用户; 此方法默认返回{@code null}
	 */
	protected Principal getUser() {
		return null;
	}

}
