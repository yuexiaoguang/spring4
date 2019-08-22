package org.springframework.web.socket.config.annotation;

import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

/**
 * 提供配置WebSocket处理器的方法.
 */
public interface WebSocketHandlerRegistration {

	/**
	 * 添加更多将共享相同配置的处理器 (拦截器, SockJS配置等)
	 */
	WebSocketHandlerRegistration addHandler(WebSocketHandler handler, String... paths);

	/**
	 * 配置要使用的HandshakeHandler.
	 */
	WebSocketHandlerRegistration setHandshakeHandler(HandshakeHandler handshakeHandler);

	/**
	 * 配置握手请求的拦截器.
	 */
	WebSocketHandlerRegistration addInterceptors(HandshakeInterceptor... interceptors);

	/**
	 * 配置允许的{@code Origin} header值.
	 * 此检查主要是为浏览器客户端设计的.
	 * 没有什么可以阻止其他类型的客户端修改{@code Origin} header值.
	 *
	 * <p>启用S​​ockJS并限制来源时, 将禁用不允许检查请求源(基于JSONP和Iframe的传输)的传输类型.
	 * 因此, 当来源受到限制时, 不支持IE 6到9.
	 *
	 * <p>每个提供的允许来源必须以 "http://", "https://" 或"*"(表示允许所有来源)开头.
	 * 默认只允许相同来源的请求 (空列表).
	 *
	 * @see <a href="https://tools.ietf.org/html/rfc6454">RFC 6454: The Web Origin Concept</a>
	 * @see <a href="https://github.com/sockjs/sockjs-client#supported-transports-by-browser-html-served-from-http-or-https">SockJS supported transports by browser</a>
	 */
	WebSocketHandlerRegistration setAllowedOrigins(String... origins);

	/**
	 * 启用S​​ockJS后备选项.
	 */
	SockJsServiceRegistration withSockJS();

}