package org.springframework.web.socket.server.support;

import java.io.IOException;
import java.nio.charset.Charset;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.context.Lifecycle;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.SubProtocolCapable;
import org.springframework.web.socket.WebSocketExtension;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.handler.WebSocketHandlerDecorator;
import org.springframework.web.socket.server.HandshakeFailureException;
import org.springframework.web.socket.server.HandshakeHandler;
import org.springframework.web.socket.server.RequestUpgradeStrategy;

/**
 * {@link HandshakeHandler}实现的基类, 独立于Servlet API.
 *
 * <p>执行WebSocket握手请求的初始验证 - 可能通过适当的HTTP状态码拒绝它
 *  - 同时还允许其子类覆盖协商过程的各个部分 (e.g. 来源验证, 子协议协商, 扩展协商等).
 *
 * <p>如果协商成功, 则将实际升级委托给特定于服务器的{@link org.springframework.web.socket.server.RequestUpgradeStrategy},
 * 它将根据需要更新响应并初始化WebSocket.
 * 目前支持的服务器是 Jetty 9.0-9.3, Tomcat 7.0.47+ 和 8.x, Undertow 1.0-1.3, GlassFish 4.1+, WebLogic 12.1.3+.
 */
public abstract class AbstractHandshakeHandler implements HandshakeHandler, Lifecycle {

	private static final Charset UTF8_CHARSET = Charset.forName("UTF-8");


	private static final boolean jettyWsPresent = ClassUtils.isPresent(
			"org.eclipse.jetty.websocket.server.WebSocketServerFactory", AbstractHandshakeHandler.class.getClassLoader());

	private static final boolean tomcatWsPresent = ClassUtils.isPresent(
			"org.apache.tomcat.websocket.server.WsHttpUpgradeHandler", AbstractHandshakeHandler.class.getClassLoader());

	private static final boolean undertowWsPresent = ClassUtils.isPresent(
			"io.undertow.websockets.jsr.ServerWebSocketContainer", AbstractHandshakeHandler.class.getClassLoader());

	private static final boolean glassfishWsPresent = ClassUtils.isPresent(
			"org.glassfish.tyrus.servlet.TyrusHttpUpgradeHandler", AbstractHandshakeHandler.class.getClassLoader());

	private static final boolean weblogicWsPresent = ClassUtils.isPresent(
			"weblogic.websocket.tyrus.TyrusServletWriter", AbstractHandshakeHandler.class.getClassLoader());

	private static final boolean websphereWsPresent = ClassUtils.isPresent(
			"com.ibm.websphere.wsoc.WsWsocServerContainer", AbstractHandshakeHandler.class.getClassLoader());


	protected final Log logger = LogFactory.getLog(getClass());

	private final RequestUpgradeStrategy requestUpgradeStrategy;

	private final List<String> supportedProtocols = new ArrayList<String>();

	private volatile boolean running = false;


	/**
	 * 默认构造函数, 用于自动检测并实例化适用于运行时容器的{@link RequestUpgradeStrategy}.
	 * 
	 * @throws IllegalStateException 如果找不到{@link RequestUpgradeStrategy}
	 */
	protected AbstractHandshakeHandler() {
		this(initRequestUpgradeStrategy());
	}

	/**
	 * 接受特定于运行时的{@link RequestUpgradeStrategy}的构造函数.
	 * 
	 * @param requestUpgradeStrategy 要使用的升级策略
	 */
	protected AbstractHandshakeHandler(RequestUpgradeStrategy requestUpgradeStrategy) {
		Assert.notNull(requestUpgradeStrategy, "RequestUpgradeStrategy must not be null");
		this.requestUpgradeStrategy = requestUpgradeStrategy;
	}


	private static RequestUpgradeStrategy initRequestUpgradeStrategy() {
		String className;
		if (tomcatWsPresent) {
			className = "org.springframework.web.socket.server.standard.TomcatRequestUpgradeStrategy";
		}
		else if (jettyWsPresent) {
			className = "org.springframework.web.socket.server.jetty.JettyRequestUpgradeStrategy";
		}
		else if (undertowWsPresent) {
			className = "org.springframework.web.socket.server.standard.UndertowRequestUpgradeStrategy";
		}
		else if (glassfishWsPresent) {
			className = "org.springframework.web.socket.server.standard.GlassFishRequestUpgradeStrategy";
		}
		else if (weblogicWsPresent) {
			className = "org.springframework.web.socket.server.standard.WebLogicRequestUpgradeStrategy";
		}
		else if (websphereWsPresent) {
			className = "org.springframework.web.socket.server.standard.WebSphereRequestUpgradeStrategy";
		}
		else {
			throw new IllegalStateException("No suitable default RequestUpgradeStrategy found");
		}

		try {
			Class<?> clazz = ClassUtils.forName(className, AbstractHandshakeHandler.class.getClassLoader());
			return (RequestUpgradeStrategy) clazz.newInstance();
		}
		catch (Throwable ex) {
			throw new IllegalStateException(
					"Failed to instantiate RequestUpgradeStrategy: " + className, ex);
		}
	}


	/**
	 * 返回用于WebSocket请求的{@link RequestUpgradeStrategy}.
	 */
	public RequestUpgradeStrategy getRequestUpgradeStrategy() {
		return this.requestUpgradeStrategy;
	}

	/**
	 * 使用此属性配置支持的子协议列表.
	 * 接受与客户端请求的子协议匹配的第一个配置的子协议.
	 * 如果没有匹配项, 则响应将不包含{@literal Sec-WebSocket-Protocol} header.
	 * <p>请注意, 如果在运行时传入的WebSocketHandler是{@link SubProtocolCapable}的实例, 则无需显式配置此属性.
	 * 对于内置的WebSocket的STOMP支持, 情况确实如此.
	 * 因此, 只有在WebSocketHandler未实现{@code SubProtocolCapable}时才应显式配置此属性.
	 */
	public void setSupportedProtocols(String... protocols) {
		this.supportedProtocols.clear();
		for (String protocol : protocols) {
			this.supportedProtocols.add(protocol.toLowerCase());
		}
	}

	/**
	 * 返回支持的子协议列表.
	 */
	public String[] getSupportedProtocols() {
		return StringUtils.toStringArray(this.supportedProtocols);
	}


	@Override
	public void start() {
		if (!isRunning()) {
			this.running = true;
			doStart();
		}
	}

	protected void doStart() {
		if (this.requestUpgradeStrategy instanceof Lifecycle) {
			((Lifecycle) this.requestUpgradeStrategy).start();
		}
	}

	@Override
	public void stop() {
		if (isRunning()) {
			this.running = false;
			doStop();
		}
	}

	protected void doStop() {
		if (this.requestUpgradeStrategy instanceof Lifecycle) {
			((Lifecycle) this.requestUpgradeStrategy).stop();
		}
	}

	@Override
	public boolean isRunning() {
		return this.running;
	}


	@Override
	public final boolean doHandshake(ServerHttpRequest request, ServerHttpResponse response,
			WebSocketHandler wsHandler, Map<String, Object> attributes) throws HandshakeFailureException {

		WebSocketHttpHeaders headers = new WebSocketHttpHeaders(request.getHeaders());
		if (logger.isTraceEnabled()) {
			logger.trace("Processing request " + request.getURI() + " with headers=" + headers);
		}
		try {
			if (HttpMethod.GET != request.getMethod()) {
				response.setStatusCode(HttpStatus.METHOD_NOT_ALLOWED);
				response.getHeaders().setAllow(Collections.singleton(HttpMethod.GET));
				if (logger.isErrorEnabled()) {
					logger.error("Handshake failed due to unexpected HTTP method: " + request.getMethod());
				}
				return false;
			}
			if (!"WebSocket".equalsIgnoreCase(headers.getUpgrade())) {
				handleInvalidUpgradeHeader(request, response);
				return false;
			}
			if (!headers.getConnection().contains("Upgrade") && !headers.getConnection().contains("upgrade")) {
				handleInvalidConnectHeader(request, response);
				return false;
			}
			if (!isWebSocketVersionSupported(headers)) {
				handleWebSocketVersionNotSupported(request, response);
				return false;
			}
			if (!isValidOrigin(request)) {
				response.setStatusCode(HttpStatus.FORBIDDEN);
				return false;
			}
			String wsKey = headers.getSecWebSocketKey();
			if (wsKey == null) {
				if (logger.isErrorEnabled()) {
					logger.error("Missing \"Sec-WebSocket-Key\" header");
				}
				response.setStatusCode(HttpStatus.BAD_REQUEST);
				return false;
			}
		}
		catch (IOException ex) {
			throw new HandshakeFailureException(
					"Response update failed during upgrade to WebSocket: " + request.getURI(), ex);
		}

		String subProtocol = selectProtocol(headers.getSecWebSocketProtocol(), wsHandler);
		List<WebSocketExtension> requested = headers.getSecWebSocketExtensions();
		List<WebSocketExtension> supported = this.requestUpgradeStrategy.getSupportedExtensions(request);
		List<WebSocketExtension> extensions = filterRequestedExtensions(request, requested, supported);
		Principal user = determineUser(request, wsHandler, attributes);

		if (logger.isTraceEnabled()) {
			logger.trace("Upgrading to WebSocket, subProtocol=" + subProtocol + ", extensions=" + extensions);
		}
		this.requestUpgradeStrategy.upgrade(request, response, subProtocol, extensions, user, wsHandler, attributes);
		return true;
	}

	protected void handleInvalidUpgradeHeader(ServerHttpRequest request, ServerHttpResponse response) throws IOException {
		if (logger.isErrorEnabled()) {
			logger.error("Handshake failed due to invalid Upgrade header: " + request.getHeaders().getUpgrade());
		}
		response.setStatusCode(HttpStatus.BAD_REQUEST);
		response.getBody().write("Can \"Upgrade\" only to \"WebSocket\".".getBytes(UTF8_CHARSET));
	}

	protected void handleInvalidConnectHeader(ServerHttpRequest request, ServerHttpResponse response) throws IOException {
		if (logger.isErrorEnabled()) {
			logger.error("Handshake failed due to invalid Connection header " + request.getHeaders().getConnection());
		}
		response.setStatusCode(HttpStatus.BAD_REQUEST);
		response.getBody().write("\"Connection\" must be \"upgrade\".".getBytes(UTF8_CHARSET));
	}

	protected boolean isWebSocketVersionSupported(WebSocketHttpHeaders httpHeaders) {
		String version = httpHeaders.getSecWebSocketVersion();
		String[] supportedVersions = getSupportedVersions();
		for (String supportedVersion : supportedVersions) {
			if (supportedVersion.trim().equals(version)) {
				return true;
			}
		}
		return false;
	}

	protected String[] getSupportedVersions() {
		return this.requestUpgradeStrategy.getSupportedVersions();
	}

	protected void handleWebSocketVersionNotSupported(ServerHttpRequest request, ServerHttpResponse response) {
		if (logger.isErrorEnabled()) {
			String version = request.getHeaders().getFirst("Sec-WebSocket-Version");
			logger.error("Handshake failed due to unsupported WebSocket version: " + version +
					". Supported versions: " + Arrays.toString(getSupportedVersions()));
		}
		response.setStatusCode(HttpStatus.UPGRADE_REQUIRED);
		response.getHeaders().set(WebSocketHttpHeaders.SEC_WEBSOCKET_VERSION,
				StringUtils.arrayToCommaDelimitedString(getSupportedVersions()));
	}

	/**
	 * 返回请求{@code Origin} header值是否有效.
	 * 默认情况下, 所有来源都被视为有效. 如果需要, 考虑使用{@link OriginHandshakeInterceptor}来过滤来源.
	 */
	protected boolean isValidOrigin(ServerHttpRequest request) {
		return true;
	}

	/**
	 * 根据请求和支持的子协议执行子协议协商.
	 * 对于支持的子协议列表, 此方法首先检查目标WebSocketHandler是否为{@link SubProtocolCapable},
	 * 然后还检查是否已使用{@link #setSupportedProtocols(String...)}显式配置了任何子协议.
	 * 
	 * @param requestedProtocols 请求的子协议
	 * @param webSocketHandler 将使用的WebSocketHandler
	 * 
	 * @return 选择的协议或{@code null}
	 */
	protected String selectProtocol(List<String> requestedProtocols, WebSocketHandler webSocketHandler) {
		if (requestedProtocols != null) {
			List<String> handlerProtocols = determineHandlerSupportedProtocols(webSocketHandler);
			for (String protocol : requestedProtocols) {
				if (handlerProtocols.contains(protocol.toLowerCase())) {
					return protocol;
				}
				if (this.supportedProtocols.contains(protocol.toLowerCase())) {
					return protocol;
				}
			}
		}
		return null;
	}

	/**
	 * 通过检查它是否是{@link SubProtocolCapable}的实例来确定给定WebSocketHandler支持的子协议.
	 * 
	 * @param handler 要检查的处理器
	 * 
	 * @return 支持的协议列表, 或空列表
	 */
	protected final List<String> determineHandlerSupportedProtocols(WebSocketHandler handler) {
		WebSocketHandler handlerToCheck = WebSocketHandlerDecorator.unwrap(handler);
		List<String> subProtocols = null;
		if (handlerToCheck instanceof SubProtocolCapable) {
			subProtocols = ((SubProtocolCapable) handlerToCheck).getSubProtocols();
		}
		return (subProtocols != null ? subProtocols : Collections.<String>emptyList());
	}

	/**
	 * 过滤请求的WebSocket扩展列表.
	 * <p>从4.1开始, 此方法的默认实现过滤列表, 仅保留请求的和支持的扩展.
	 * 
	 * @param request 当前的请求
	 * @param requestedExtensions 客户端请求的扩展列表
	 * @param supportedExtensions 服务器支持的扩展列表
	 * 
	 * @return 选择的扩展或空列表
	 */
	protected List<WebSocketExtension> filterRequestedExtensions(ServerHttpRequest request,
			List<WebSocketExtension> requestedExtensions, List<WebSocketExtension> supportedExtensions) {

		List<WebSocketExtension> result = new ArrayList<WebSocketExtension>(requestedExtensions.size());
		for (WebSocketExtension extension : requestedExtensions) {
			if (supportedExtensions.contains(extension)) {
				result.add(extension);
			}
		}
		return result;
	}

	/**
	 * 用于在建立过程中将用户与WebSocket会话相关联.
	 * 默认实现调用{@link ServerHttpRequest#getPrincipal()}
	 * <p>子类可以提供用于将用户与会话相关联的自定义逻辑, 例如, 为匿名用户分配名称 (i.e. 未完全验证).
	 * 
	 * @param request 握手请求
	 * @param wsHandler 将处理消息的WebSocket处理器
	 * @param attributes 传递给WebSocket会话的握手属性
	 * 
	 * @return WebSocket会话的用户, 或{@code null}
	 */
	protected Principal determineUser(
			ServerHttpRequest request, WebSocketHandler wsHandler, Map<String, Object> attributes) {

		return request.getPrincipal();
	}
}
