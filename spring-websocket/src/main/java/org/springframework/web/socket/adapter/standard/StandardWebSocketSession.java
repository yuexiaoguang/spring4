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
 * A {@link WebSocketSession} for use with the standard WebSocket for Java API.
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
	 * Constructor for a standard WebSocket session.
	 * @param headers the headers of the handshake request
	 * @param attributes attributes from the HTTP handshake to associate with the WebSocket
	 * session; the provided attributes are copied, the original map is not used.
	 * @param localAddress the address on which the request was received
	 * @param remoteAddress the address of the remote client
	 */
	public StandardWebSocketSession(HttpHeaders headers, Map<String, Object> attributes,
			InetSocketAddress localAddress, InetSocketAddress remoteAddress) {

		this(headers, attributes, localAddress, remoteAddress, null);
	}

	/**
	 * Constructor that associates a user with the WebSocket session.
	 * @param headers the headers of the handshake request
	 * @param attributes attributes from the HTTP handshake to associate with the WebSocket session
	 * @param localAddress the address on which the request was received
	 * @param remoteAddress the address of the remote client
	 * @param user the user associated with the session; if {@code null} we'll
	 * 	fallback on the user available in the underlying WebSocket session
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
