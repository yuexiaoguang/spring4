package org.springframework.web.socket.adapter.jetty;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.URI;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.eclipse.jetty.websocket.api.RemoteEndpoint;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.UpgradeRequest;
import org.eclipse.jetty.websocket.api.UpgradeResponse;
import org.eclipse.jetty.websocket.api.WebSocketException;
import org.eclipse.jetty.websocket.api.extensions.ExtensionConfig;

import org.springframework.http.HttpHeaders;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.PingMessage;
import org.springframework.web.socket.PongMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketExtension;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.adapter.AbstractWebSocketSession;

/**
 * 用于Jetty 9.3/9.4 WebSocket API的{@link WebSocketSession}.
 */
public class JettyWebSocketSession extends AbstractWebSocketSession<Session> {

	// 从Jetty 9.4开始, UpgradeRequest和UpgradeResponse是接口而不是类
	private static final boolean directInterfaceCalls;

	private static Method getUpgradeRequest;
	private static Method getUpgradeResponse;
	private static Method getRequestURI;
	private static Method getHeaders;
	private static Method getUserPrincipal;
	private static Method getAcceptedSubProtocol;
	private static Method getExtensions;

	static {
		directInterfaceCalls = UpgradeRequest.class.isInterface();
		if (!directInterfaceCalls) {
			try {
				getUpgradeRequest = Session.class.getMethod("getUpgradeRequest");
				getUpgradeResponse = Session.class.getMethod("getUpgradeResponse");
				getRequestURI = UpgradeRequest.class.getMethod("getRequestURI");
				getHeaders = UpgradeRequest.class.getMethod("getHeaders");
				getUserPrincipal = UpgradeRequest.class.getMethod("getUserPrincipal");
				getAcceptedSubProtocol = UpgradeResponse.class.getMethod("getAcceptedSubProtocol");
				getExtensions = UpgradeResponse.class.getMethod("getExtensions");
			}
			catch (NoSuchMethodException ex) {
				throw new IllegalStateException("Incompatible Jetty API", ex);
			}
		}
	}


	private String id;

	private URI uri;

	private HttpHeaders headers;

	private String acceptedProtocol;

	private List<WebSocketExtension> extensions;

	private Principal user;


	/**
	 * @param attributes 来自HTTP握手的属性, 将与WebSocket会话关联
	 */
	public JettyWebSocketSession(Map<String, Object> attributes) {
		this(attributes, null);
	}

	/**
	 * @param attributes 来自HTTP握手的属性, 将与WebSocket会话关联; 复制提供的属性, 不使用原始Map.
	 * @param user 与会话关联的用户; 如果是{@code null},
	 * 将通过{@link org.eclipse.jetty.websocket.api.Session#getUpgradeRequest()}回退用户
	 */
	public JettyWebSocketSession(Map<String, Object> attributes, Principal user) {
		super(attributes);
		this.user = user;
	}


	@Override
	public String getId() {
		checkNativeSessionInitialized();
		return this.id;
	}

	@Override
	public URI getUri() {
		checkNativeSessionInitialized();
		return this.uri;
	}

	@Override
	public HttpHeaders getHandshakeHeaders() {
		checkNativeSessionInitialized();
		return this.headers;
	}

	@Override
	public String getAcceptedProtocol() {
		checkNativeSessionInitialized();
		return this.acceptedProtocol;
	}

	@Override
	public List<WebSocketExtension> getExtensions() {
		checkNativeSessionInitialized();
		return this.extensions;
	}

	@Override
	public Principal getPrincipal() {
		return this.user;
	}

	@Override
	public InetSocketAddress getLocalAddress() {
		checkNativeSessionInitialized();
		return getNativeSession().getLocalAddress();
	}

	@Override
	public InetSocketAddress getRemoteAddress() {
		checkNativeSessionInitialized();
		return getNativeSession().getRemoteAddress();
	}

	@Override
	public void setTextMessageSizeLimit(int messageSizeLimit) {
		checkNativeSessionInitialized();
		getNativeSession().getPolicy().setMaxTextMessageSize(messageSizeLimit);
	}

	@Override
	public int getTextMessageSizeLimit() {
		checkNativeSessionInitialized();
		return getNativeSession().getPolicy().getMaxTextMessageSize();
	}

	@Override
	public void setBinaryMessageSizeLimit(int messageSizeLimit) {
		checkNativeSessionInitialized();
		getNativeSession().getPolicy().setMaxBinaryMessageSize(messageSizeLimit);
	}

	@Override
	public int getBinaryMessageSizeLimit() {
		checkNativeSessionInitialized();
		return getNativeSession().getPolicy().getMaxBinaryMessageSize();
	}

	@Override
	public boolean isOpen() {
		return (getNativeSession() != null && getNativeSession().isOpen());
	}


	@Override
	public void initializeNativeSession(Session session) {
		super.initializeNativeSession(session);
		if (directInterfaceCalls) {
			initializeJettySessionDirectly(session);
		}
		else {
			initializeJettySessionReflectively(session);
		}
	}

	private void initializeJettySessionDirectly(Session session) {
		this.id = ObjectUtils.getIdentityHexString(getNativeSession());
		this.uri = session.getUpgradeRequest().getRequestURI();

		HttpHeaders headers = new HttpHeaders();
		headers.putAll(session.getUpgradeRequest().getHeaders());
		this.headers = HttpHeaders.readOnlyHttpHeaders(headers);

		this.acceptedProtocol = session.getUpgradeResponse().getAcceptedSubProtocol();

		List<ExtensionConfig> jettyExtensions = session.getUpgradeResponse().getExtensions();
		if (!CollectionUtils.isEmpty(jettyExtensions)) {
			List<WebSocketExtension> extensions = new ArrayList<WebSocketExtension>(jettyExtensions.size());
			for (ExtensionConfig jettyExtension : jettyExtensions) {
				extensions.add(new WebSocketExtension(jettyExtension.getName(), jettyExtension.getParameters()));
			}
			this.extensions = Collections.unmodifiableList(extensions);
		}
		else {
			this.extensions = Collections.emptyList();
		}

		if (this.user == null) {
			this.user = session.getUpgradeRequest().getUserPrincipal();
		}
	}

	@SuppressWarnings("unchecked")
	private void initializeJettySessionReflectively(Session session) {
		Object request = ReflectionUtils.invokeMethod(getUpgradeRequest, session);
		Object response = ReflectionUtils.invokeMethod(getUpgradeResponse, session);

		this.id = ObjectUtils.getIdentityHexString(getNativeSession());
		this.uri = (URI) ReflectionUtils.invokeMethod(getRequestURI, request);

		HttpHeaders headers = new HttpHeaders();
		headers.putAll((Map<String, List<String>>) ReflectionUtils.invokeMethod(getHeaders, request));
		this.headers = HttpHeaders.readOnlyHttpHeaders(headers);

		this.acceptedProtocol = (String) ReflectionUtils.invokeMethod(getAcceptedSubProtocol, response);

		List<ExtensionConfig> jettyExtensions = (List<ExtensionConfig>) ReflectionUtils.invokeMethod(getExtensions, response);
		if (!CollectionUtils.isEmpty(jettyExtensions)) {
			List<WebSocketExtension> extensions = new ArrayList<WebSocketExtension>(jettyExtensions.size());
			for (ExtensionConfig jettyExtension : jettyExtensions) {
				extensions.add(new WebSocketExtension(jettyExtension.getName(), jettyExtension.getParameters()));
			}
			this.extensions = Collections.unmodifiableList(extensions);
		}
		else {
			this.extensions = Collections.emptyList();
		}

		if (this.user == null) {
			this.user = (Principal) ReflectionUtils.invokeMethod(getUserPrincipal, request);
		}
	}


	@Override
	protected void sendTextMessage(TextMessage message) throws IOException {
		getRemoteEndpoint().sendString(message.getPayload());
	}

	@Override
	protected void sendBinaryMessage(BinaryMessage message) throws IOException {
		getRemoteEndpoint().sendBytes(message.getPayload());
	}

	@Override
	protected void sendPingMessage(PingMessage message) throws IOException {
		getRemoteEndpoint().sendPing(message.getPayload());
	}

	@Override
	protected void sendPongMessage(PongMessage message) throws IOException {
		getRemoteEndpoint().sendPong(message.getPayload());
	}

	private RemoteEndpoint getRemoteEndpoint() throws IOException {
		try {
			return getNativeSession().getRemote();
		}
		catch (WebSocketException ex) {
			throw new IOException("Unable to obtain RemoteEndpoint in session " + getId(), ex);
		}
	}

	@Override
	protected void closeInternal(CloseStatus status) throws IOException {
		getNativeSession().close(status.getCode(), status.getReason());
	}

}
