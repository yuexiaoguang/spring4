package org.springframework.web.socket.server.jetty;

import java.io.IOException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.websocket.api.WebSocketPolicy;
import org.eclipse.jetty.websocket.api.extensions.ExtensionConfig;
import org.eclipse.jetty.websocket.server.HandshakeRFC6455;
import org.eclipse.jetty.websocket.server.WebSocketServerFactory;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeResponse;
import org.eclipse.jetty.websocket.servlet.WebSocketCreator;

import org.springframework.context.Lifecycle;
import org.springframework.core.NamedThreadLocal;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.socket.WebSocketExtension;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.adapter.jetty.JettyWebSocketHandlerAdapter;
import org.springframework.web.socket.adapter.jetty.JettyWebSocketSession;
import org.springframework.web.socket.adapter.jetty.WebSocketToJettyExtensionConfigAdapter;
import org.springframework.web.socket.server.HandshakeFailureException;
import org.springframework.web.socket.server.RequestUpgradeStrategy;

/**
 * 用于Jetty 9.1-9.4的{@link RequestUpgradeStrategy}.
 * 基于Jetty的内部{@code org.eclipse.jetty.websocket.server.WebSocketHandler}类.
 */
public class JettyRequestUpgradeStrategy implements RequestUpgradeStrategy, ServletContextAware, Lifecycle {

	private static final ThreadLocal<WebSocketHandlerContainer> containerHolder =
			new NamedThreadLocal<WebSocketHandlerContainer>("WebSocketHandlerContainer");


	// 由于Jetty 9.3.15+ API差异, 可配置的工厂适配器:
	// 使用 WebSocketServerFactory(ServletContext) 作为版本指示符
	private final WebSocketServerFactoryAdapter factoryAdapter =
			(ClassUtils.hasConstructor(WebSocketServerFactory.class, ServletContext.class) ?
					new ModernJettyWebSocketServerFactoryAdapter() : new LegacyJettyWebSocketServerFactoryAdapter());

	private ServletContext servletContext;

	private volatile boolean running = false;

	private volatile List<WebSocketExtension> supportedExtensions;


	/**
	 * 默认构造函数, 通过其默认构造函数创建{@link WebSocketServerFactory}, 从而使用默认的{@link WebSocketPolicy}.
	 */
	public JettyRequestUpgradeStrategy() {
		this.factoryAdapter.setPolicy(WebSocketPolicy.newServerPolicy());
	}

	/**
	 * 接受在创建{@link WebSocketServerFactory}实例时使用的{@link WebSocketPolicy}的构造函数.
	 * 
	 * @param policy 要使用的策略
	 */
	public JettyRequestUpgradeStrategy(WebSocketPolicy policy) {
		Assert.notNull(policy, "WebSocketPolicy must not be null");
		this.factoryAdapter.setPolicy(policy);
	}

	/**
	 * 接受{@link WebSocketServerFactory}.
	 * 
	 * @param factory 要使用的预配置的工厂
	 */
	public JettyRequestUpgradeStrategy(WebSocketServerFactory factory) {
		Assert.notNull(factory, "WebSocketServerFactory must not be null");
		this.factoryAdapter.setFactory(factory);
	}


	@Override
	public void setServletContext(ServletContext servletContext) {
		this.servletContext = servletContext;
	}

	@Override
	public void start() {
		if (!isRunning()) {
			this.running = true;
			try {
				this.factoryAdapter.start();
			}
			catch (Throwable ex) {
				throw new IllegalStateException("Unable to start Jetty WebSocketServerFactory", ex);
			}
		}
	}

	@Override
	public void stop() {
		if (isRunning()) {
			this.running = false;
			try {
				this.factoryAdapter.stop();
			}
			catch (Throwable ex) {
				throw new IllegalStateException("Unable to stop Jetty WebSocketServerFactory", ex);
			}
		}
	}

	@Override
	public boolean isRunning() {
		return this.running;
	}


	@Override
	public String[] getSupportedVersions() {
		return new String[] { String.valueOf(HandshakeRFC6455.VERSION) };
	}

	@Override
	public List<WebSocketExtension> getSupportedExtensions(ServerHttpRequest request) {
		if (this.supportedExtensions == null) {
			this.supportedExtensions = buildWebSocketExtensions();
		}
		return this.supportedExtensions;
	}

	private List<WebSocketExtension> buildWebSocketExtensions() {
		Set<String> names = this.factoryAdapter.getFactory().getExtensionFactory().getExtensionNames();
		List<WebSocketExtension> result = new ArrayList<WebSocketExtension>(names.size());
		for (String name : names) {
			result.add(new WebSocketExtension(name));
		}
		return result;
	}

	@Override
	public void upgrade(ServerHttpRequest request, ServerHttpResponse response,
			String selectedProtocol, List<WebSocketExtension> selectedExtensions, Principal user,
			WebSocketHandler wsHandler, Map<String, Object> attributes) throws HandshakeFailureException {

		Assert.isInstanceOf(ServletServerHttpRequest.class, request, "ServletServerHttpRequest required");
		HttpServletRequest servletRequest = ((ServletServerHttpRequest) request).getServletRequest();

		Assert.isInstanceOf(ServletServerHttpResponse.class, response, "ServletServerHttpResponse required");
		HttpServletResponse servletResponse = ((ServletServerHttpResponse) response).getServletResponse();

		Assert.isTrue(this.factoryAdapter.getFactory().isUpgradeRequest(servletRequest, servletResponse),
				"Not a WebSocket handshake");

		JettyWebSocketSession session = new JettyWebSocketSession(attributes, user);
		JettyWebSocketHandlerAdapter handlerAdapter = new JettyWebSocketHandlerAdapter(wsHandler, session);

		WebSocketHandlerContainer container =
				new WebSocketHandlerContainer(handlerAdapter, selectedProtocol, selectedExtensions);

		try {
			containerHolder.set(container);
			this.factoryAdapter.getFactory().acceptWebSocket(servletRequest, servletResponse);
		}
		catch (IOException ex) {
			throw new HandshakeFailureException(
					"Response update failed during upgrade to WebSocket: " + request.getURI(), ex);
		}
		finally {
			containerHolder.remove();
		}
	}


	private static class WebSocketHandlerContainer {

		private final JettyWebSocketHandlerAdapter handler;

		private final String selectedProtocol;

		private final List<ExtensionConfig> extensionConfigs;

		public WebSocketHandlerContainer(
				JettyWebSocketHandlerAdapter handler, String protocol, List<WebSocketExtension> extensions) {

			this.handler = handler;
			this.selectedProtocol = protocol;
			if (CollectionUtils.isEmpty(extensions)) {
				this.extensionConfigs = new ArrayList<ExtensionConfig>(0);
			}
			else {
				this.extensionConfigs = new ArrayList<ExtensionConfig>(extensions.size());
				for (WebSocketExtension extension : extensions) {
					this.extensionConfigs.add(new WebSocketToJettyExtensionConfigAdapter(extension));
				}
			}
		}

		public JettyWebSocketHandlerAdapter getHandler() {
			return this.handler;
		}

		public String getSelectedProtocol() {
			return this.selectedProtocol;
		}

		public List<ExtensionConfig> getExtensionConfigs() {
			return this.extensionConfigs;
		}
	}


	private static abstract class WebSocketServerFactoryAdapter {

		private WebSocketPolicy policy;

		private WebSocketServerFactory factory;

		public void setPolicy(WebSocketPolicy policy) {
			this.policy = policy;
		}

		public void setFactory(WebSocketServerFactory factory) {
			this.factory = factory;
		}

		public WebSocketServerFactory getFactory() {
			return this.factory;
		}

		public void start() throws Exception {
			if (this.factory == null) {
				this.factory = createFactory(this.policy);
			}
			this.factory.setCreator(new WebSocketCreator() {
				@Override
				public Object createWebSocket(ServletUpgradeRequest request, ServletUpgradeResponse response) {
					WebSocketHandlerContainer container = containerHolder.get();
					Assert.state(container != null, "Expected WebSocketHandlerContainer");
					response.setAcceptedSubProtocol(container.getSelectedProtocol());
					response.setExtensions(container.getExtensionConfigs());
					return container.getHandler();
				}
			});
			startFactory(this.factory);
		}

		public void stop() throws Exception {
			if (this.factory != null) {
				stopFactory(this.factory);
			}
		}

		protected abstract WebSocketServerFactory createFactory(WebSocketPolicy policy) throws Exception;

		protected abstract void startFactory(WebSocketServerFactory factory) throws Exception;

		protected abstract void stopFactory(WebSocketServerFactory factory) throws Exception;
	}


	// Jetty 9.3.15+
	private class ModernJettyWebSocketServerFactoryAdapter extends WebSocketServerFactoryAdapter {

		@Override
		protected WebSocketServerFactory createFactory(WebSocketPolicy policy) throws Exception {
			return new WebSocketServerFactory(servletContext, policy);
		}

		@Override
		protected void startFactory(WebSocketServerFactory factory) throws Exception {
			factory.start();
		}

		@Override
		protected void stopFactory(WebSocketServerFactory factory) throws Exception {
			factory.stop();
		}
	}


	// Jetty <9.3.15
	private class LegacyJettyWebSocketServerFactoryAdapter extends WebSocketServerFactoryAdapter {

		@Override
		protected WebSocketServerFactory createFactory(WebSocketPolicy policy) throws Exception {
			return WebSocketServerFactory.class.getConstructor(WebSocketPolicy.class).newInstance(policy);
		}

		@Override
		protected void startFactory(WebSocketServerFactory factory) throws Exception {
			try {
				WebSocketServerFactory.class.getMethod("init", ServletContext.class).invoke(factory, servletContext);
			}
			catch (NoSuchMethodException ex) {
				// Jetty 9.1/9.2
				WebSocketServerFactory.class.getMethod("init").invoke(factory);
			}
		}

		@Override
		protected void stopFactory(WebSocketServerFactory factory) throws Exception {
			WebSocketServerFactory.class.getMethod("cleanup").invoke(factory);
		}
	}

}
