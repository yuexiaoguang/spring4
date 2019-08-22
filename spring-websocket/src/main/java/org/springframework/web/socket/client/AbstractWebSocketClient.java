package org.springframework.web.socket.client;

import java.net.URI;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.http.HttpHeaders;
import org.springframework.util.Assert;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.web.socket.WebSocketExtension;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * {@link WebSocketClient}实现的抽象基类.
 */
public abstract class AbstractWebSocketClient implements WebSocketClient {

	private static final Set<String> specialHeaders = new HashSet<String>();

	static {
		specialHeaders.add("cache-control");
		specialHeaders.add("connection");
		specialHeaders.add("host");
		specialHeaders.add("sec-websocket-extensions");
		specialHeaders.add("sec-websocket-key");
		specialHeaders.add("sec-websocket-protocol");
		specialHeaders.add("sec-websocket-version");
		specialHeaders.add("pragma");
		specialHeaders.add("upgrade");
	}


	protected final Log logger = LogFactory.getLog(getClass());


	@Override
	public ListenableFuture<WebSocketSession> doHandshake(WebSocketHandler webSocketHandler,
			String uriTemplate, Object... uriVars) {

		Assert.notNull(uriTemplate, "'uriTemplate' must not be null");
		URI uri = UriComponentsBuilder.fromUriString(uriTemplate).buildAndExpand(uriVars).encode().toUri();
		return doHandshake(webSocketHandler, null, uri);
	}

	@Override
	public final ListenableFuture<WebSocketSession> doHandshake(WebSocketHandler webSocketHandler,
			WebSocketHttpHeaders headers, URI uri) {

		Assert.notNull(webSocketHandler, "WebSocketHandler must not be null");
		assertUri(uri);

		if (logger.isDebugEnabled()) {
			logger.debug("Connecting to " + uri);
		}

		HttpHeaders headersToUse = new HttpHeaders();
		if (headers != null) {
			for (String header : headers.keySet()) {
				if (!specialHeaders.contains(header.toLowerCase())) {
					headersToUse.put(header, headers.get(header));
				}
			}
		}

		List<String> subProtocols = (headers != null && headers.getSecWebSocketProtocol() != null ?
				headers.getSecWebSocketProtocol() : Collections.<String>emptyList());

		List<WebSocketExtension> extensions = (headers != null && headers.getSecWebSocketExtensions() != null ?
				headers.getSecWebSocketExtensions() : Collections.<WebSocketExtension>emptyList());

		return doHandshakeInternal(webSocketHandler, headersToUse, uri, subProtocols, extensions,
				Collections.<String, Object>emptyMap());
	}

	protected void assertUri(URI uri) {
		Assert.notNull(uri, "URI must not be null");
		String scheme = uri.getScheme();
		if (!"ws".equals(scheme) && !"wss".equals(scheme)) {
			throw new IllegalArgumentException("Invalid scheme: " + scheme);
		}
	}

	/**
	 * 执行实际的握手以建立与服务器的连接.
	 * 
	 * @param webSocketHandler WebSocket消息的客户端处理器
	 * @param headers 用于握手的HTTP header, 过滤掉不需要的(禁止的) header, never {@code null}
	 * @param uri 握手的目标URI, never {@code null}
	 * @param subProtocols 请求的子协议, 或空列表
	 * @param extensions 请求的WebSocket扩展, 或空列表
	 * @param attributes 与WebSocketSession关联的属性, i.e. 通过{@link WebSocketSession#getAttributes()}; 目前总是空Map.
	 * 
	 * @return 包装在ListenableFuture中的已建立的 WebSocket会话.
	 */
	protected abstract ListenableFuture<WebSocketSession> doHandshakeInternal(WebSocketHandler webSocketHandler,
			HttpHeaders headers, URI uri, List<String> subProtocols, List<WebSocketExtension> extensions,
			Map<String, Object> attributes);

}
