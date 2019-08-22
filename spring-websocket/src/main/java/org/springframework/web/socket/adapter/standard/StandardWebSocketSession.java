package org.springframework.web.socket.adapter.standard;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.websocket.CloseReason;
import javax.websocket.CloseReason.CloseCodes;
import javax.websocket.Extension;
import javax.websocket.Session;

import org.springframework.http.HttpHeaders;
import org.springframework.util.CollectionUtils;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.PingMessage;
import org.springframework.web.socket.PongMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketExtension;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.adapter.AbstractWebSocketSession;

/**
 * {@link WebSocketSession}, 用于Java API标准的WebSocket.
 */
public class StandardWebSocketSession extends AbstractWebSocketSession<Session> {

	private String id;

	private URI uri;

	private final HttpHeaders handshakeHeaders;

	private String acceptedProtocol;

	private List<WebSocketExtension> extensions;

	private Principal user;

	private final InetSocketAddress localAddress;

	private final InetSocketAddress remoteAddress;


	/**
	 * @param headers 握手请求的header
	 * @param attributes 来自HTTP握手的属性, 将与WebSocket会话关联; 复制提供的属性, 不使用原始Map.
	 * @param localAddress 接收请求的地址
	 * @param remoteAddress 远程客户端的地址
	 */
	public StandardWebSocketSession(HttpHeaders headers, Map<String, Object> attributes,
			InetSocketAddress localAddress, InetSocketAddress remoteAddress) {

		this(headers, attributes, localAddress, remoteAddress, null);
	}

	/**
	 * @param headers 握手请求的header
	 * @param attributes 来自HTTP握手的属性, 将与WebSocket会话关联
	 * @param localAddress 接收请求的地址
	 * @param remoteAddress 远程客户端的地址
	 * @param user 与会话关联的用户; 如果是{@code null}, 将回退到底层WebSocket会话中的可用用户
	 */
	public StandardWebSocketSession(HttpHeaders headers, Map<String, Object> attributes,
			InetSocketAddress localAddress, InetSocketAddress remoteAddress, Principal user) {

		super(attributes);
		headers = (headers != null) ? headers : new HttpHeaders();
		this.handshakeHeaders = HttpHeaders.readOnlyHttpHeaders(headers);
		this.user = user;
		this.localAddress = localAddress;
		this.remoteAddress = remoteAddress;
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
		return this.handshakeHeaders;
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

	public Principal getPrincipal() {
		return this.user;
	}

	@Override
	public InetSocketAddress getLocalAddress() {
		return this.localAddress;
	}

	@Override
	public InetSocketAddress getRemoteAddress() {
		return this.remoteAddress;
	}

	@Override
	public void setTextMessageSizeLimit(int messageSizeLimit) {
		checkNativeSessionInitialized();
		getNativeSession().setMaxTextMessageBufferSize(messageSizeLimit);
	}

	@Override
	public int getTextMessageSizeLimit() {
		checkNativeSessionInitialized();
		return getNativeSession().getMaxTextMessageBufferSize();
	}

	@Override
	public void setBinaryMessageSizeLimit(int messageSizeLimit) {
		checkNativeSessionInitialized();
		getNativeSession().setMaxBinaryMessageBufferSize(messageSizeLimit);
	}

	@Override
	public int getBinaryMessageSizeLimit() {
		checkNativeSessionInitialized();
		return getNativeSession().getMaxBinaryMessageBufferSize();
	}

	@Override
	public boolean isOpen() {
		return (getNativeSession() != null && getNativeSession().isOpen());
	}

	@Override
	public void initializeNativeSession(Session session) {
		super.initializeNativeSession(session);

		this.id = session.getId();
		this.uri = session.getRequestURI();

		this.acceptedProtocol = session.getNegotiatedSubprotocol();

		List<Extension> standardExtensions = getNativeSession().getNegotiatedExtensions();
		if (!CollectionUtils.isEmpty(standardExtensions)) {
			this.extensions = new ArrayList<WebSocketExtension>(standardExtensions.size());
			for (Extension standardExtension : standardExtensions) {
				this.extensions.add(new StandardToWebSocketExtensionAdapter(standardExtension));
			}
			this.extensions = Collections.unmodifiableList(this.extensions);
		}
		else {
			this.extensions = Collections.emptyList();
		}

		if (this.user == null) {
			this.user = session.getUserPrincipal();
		}
	}

	@Override
	protected void sendTextMessage(TextMessage message) throws IOException {
		getNativeSession().getBasicRemote().sendText(message.getPayload(), message.isLast());
	}

	@Override
	protected void sendBinaryMessage(BinaryMessage message) throws IOException {
		getNativeSession().getBasicRemote().sendBinary(message.getPayload(), message.isLast());
	}

	@Override
	protected void sendPingMessage(PingMessage message) throws IOException {
		getNativeSession().getBasicRemote().sendPing(message.getPayload());
	}

	@Override
	protected void sendPongMessage(PongMessage message) throws IOException {
		getNativeSession().getBasicRemote().sendPong(message.getPayload());
	}

	@Override
	protected void closeInternal(CloseStatus status) throws IOException {
		getNativeSession().close(new CloseReason(CloseCodes.getCloseCode(status.getCode()), status.getReason()));
	}

}
