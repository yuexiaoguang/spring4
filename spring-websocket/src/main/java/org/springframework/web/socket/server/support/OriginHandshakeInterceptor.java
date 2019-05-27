package org.springframework.web.socket.server.support;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.util.Assert;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.WebUtils;

/**
 * An interceptor to check request {@code Origin} header value against a
 * collection of allowed origins.
 */
public class OriginHandshakeInterceptor implements HandshakeInterceptor {

	protected final Log logger = LogFactory.getLog(getClass());

	private final Set<String> allowedOrigins = new LinkedHashSet<String>();


	/**
	 * Default constructor with only same origin requests allowed.
	 */
	public OriginHandshakeInterceptor() {
	}

	/**
	 * Constructor using the specified allowed origin values.
	 */
	public OriginHandshakeInterceptor(Collection<String> allowedOrigins) {
		setAllowedOrigins(allowedOrigins);
	}


	/**
	 * Configure allowed {@code Origin} header values. This check is mostly
	 * designed for browsers. There is nothing preventing other types of client
	 * to modify the {@code Origin} header value.
	 * <p>Each provided allowed origin must have a scheme, and optionally a port
	 * (e.g. "http://example.org", "http://example.org:9090"). An allowed origin
	 * string may also be "*" in which case all origins are allowed.
	 */
	public void setAllowedOrigins(Collection<String> allowedOrigins) {
		Assert.notNull(allowedOrigins, "Allowed origins Collection must not be null");
		this.allowedOrigins.clear();
		this.allowedOrigins.addAll(allowedOrigins);
	}

	public Collection<String> getAllowedOrigins() {
		return Collections.unmodifiableSet(this.allowedOrigins);
	}


	@Override
	public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
			WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {

		if (!WebUtils.isSameOrigin(request) && !WebUtils.isValidOrigin(request, this.allowedOrigins)) {
			response.setStatusCode(HttpStatus.FORBIDDEN);
			if (logger.isDebugEnabled()) {
				logger.debug("Handshake request rejected, Origin header value " +
						request.getHeaders().getOrigin() + " not allowed");
			}
			return false;
		}
		return true;
	}

	@Override
	public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
			WebSocketHandler wsHandler, Exception exception) {
	}

}