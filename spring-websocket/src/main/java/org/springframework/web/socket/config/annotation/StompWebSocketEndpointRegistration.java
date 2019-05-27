package org.springframework.web.socket.config.annotation;

import org.springframework.web.socket.server.HandshakeHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

/**
 * A contract for configuring a STOMP over WebSocket endpoint.
 */
public interface StompWebSocketEndpointRegistration {

	/**
	 * Enable SockJS fallback options.
	 */
	SockJsServiceRegistration withSockJS();

	/**
	 * Configure the HandshakeHandler to use.
	 */
	StompWebSocketEndpointRegistration setHandshakeHandler(HandshakeHandler handshakeHandler);

	/**
	 * Configure the HandshakeInterceptor's to use.
	 */
	StompWebSocketEndpointRegistration addInterceptors(HandshakeInterceptor... interceptors);

	/**
	 * Configure allowed {@code Origin} header values. This check is mostly designed for
	 * browser clients. There is nothing preventing other types of client to modify the
	 * {@code Origin} header value.
	 *
	 * <p>When SockJS is enabled and origins are restricted, transport types that do not
	 * allow to check request origin (JSONP and Iframe based transports) are disabled.
	 * As a consequence, IE 6 to 9 are not supported when origins are restricted.
	 *
	 * <p>Each provided allowed origin must start by "http://", "https://" or be "*"
	 * (means that all origins are allowed). By default, only same origin requests are
	 * allowed (empty list).
	 *
	 * @since 4.1.2
	 * @see <a href="https://tools.ietf.org/html/rfc6454">RFC 6454: The Web Origin Concept</a>
	 * @see <a href="https://github.com/sockjs/sockjs-client#supported-transports-by-browser-html-served-from-http-or-https">SockJS supported transports by browser</a>
	 */
	StompWebSocketEndpointRegistration setAllowedOrigins(String... origins);

}