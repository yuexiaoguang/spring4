package org.springframework.web.socket.server.support;

import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;
import javax.servlet.http.HttpSession;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.server.HandshakeInterceptor;

/**
 * 一个拦截器, 用于将信息从HTTP会话复制到"手属性" map, 以通过{@link WebSocketSession#getAttributes()}提供.
 *
 * <p>在键{@link #HTTP_SESSION_ID_ATTR_NAME}下复制所有HTTP会话属性或其子集和/或HTTP会话ID.
 */
public class HttpSessionHandshakeInterceptor implements HandshakeInterceptor {

	/**
	 * 当{@link #setCopyHttpSessionId(boolean) copyHttpSessionId}为"true"时, HTTP会话ID所在的属性的名称.
	 */
	public static final String HTTP_SESSION_ID_ATTR_NAME = "HTTP.SESSION.ID";


	private final Collection<String> attributeNames;

	private boolean copyAllAttributes;

	private boolean copyHttpSessionId = true;

	private boolean createSession;


	/**
	 * 用于复制所有HTTP会话属性和HTTP会话ID的默认构造函数.
	 */
	public HttpSessionHandshakeInterceptor() {
		this.attributeNames = Collections.emptyList();
		this.copyAllAttributes = true;
	}

	/**
	 * 用于复制特定HTTP会话属性和HTTP会话ID的构造方法.
	 * 
	 * @param attributeNames 要复制的会话属性
	 */
	public HttpSessionHandshakeInterceptor(Collection<String> attributeNames) {
		this.attributeNames = Collections.unmodifiableCollection(attributeNames);
		this.copyAllAttributes = false;
	}


	/**
	 * 返回要复制的配置的属性名称 (只读).
	 */
	public Collection<String> getAttributeNames() {
		return this.attributeNames;
	}

	/**
	 * 是否从HTTP会话复制所有属性.
	 * 如果设置为"true", 则忽略任何显式配置的属性名称.
	 * <p>默认情况下, 根据使用的构造函数(默认或分别使用属性名称)将其设置为 "true"或"false".
	 * 
	 * @param copyAllAttributes 是否复制所有属性
	 */
	public void setCopyAllAttributes(boolean copyAllAttributes) {
		this.copyAllAttributes = copyAllAttributes;
	}

	/**
	 * 是否复制所有HTTP会话属性.
	 */
	public boolean isCopyAllAttributes() {
		return this.copyAllAttributes;
	}

	/**
	 * 是否应将HTTP会话ID复制到名称为 {@link #HTTP_SESSION_ID_ATTR_NAME}的握手属性.
	 * <p>默认为 "true".
	 * 
	 * @param copyHttpSessionId 是否复制HTTP会话ID
	 */
	public void setCopyHttpSessionId(boolean copyHttpSessionId) {
		this.copyHttpSessionId = copyHttpSessionId;
	}

	/**
	 * 是否将HTTP会话ID复制到握手属性.
	 */
	public boolean isCopyHttpSessionId() {
		return this.copyHttpSessionId;
	}

	/**
	 * 是否允许在访问HTTP会话时创建HTTP会话.
	 * <p>默认为{@code false}.
	 */
	public void setCreateSession(boolean createSession) {
		this.createSession = createSession;
	}

	/**
	 * 是否允许创建HTTP会话.
	 */
	public boolean isCreateSession() {
		return this.createSession;
	}


	@Override
	public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
			WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {

		HttpSession session = getSession(request);
		if (session != null) {
			if (isCopyHttpSessionId()) {
				attributes.put(HTTP_SESSION_ID_ATTR_NAME, session.getId());
			}
			Enumeration<String> names = session.getAttributeNames();
			while (names.hasMoreElements()) {
				String name = names.nextElement();
				if (isCopyAllAttributes() || getAttributeNames().contains(name)) {
					attributes.put(name, session.getAttribute(name));
				}
			}
		}
		return true;
	}

	private HttpSession getSession(ServerHttpRequest request) {
		if (request instanceof ServletServerHttpRequest) {
			ServletServerHttpRequest serverRequest = (ServletServerHttpRequest) request;
			return serverRequest.getServletRequest().getSession(isCreateSession());
		}
		return null;
	}

	@Override
	public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
			WebSocketHandler wsHandler, Exception ex) {
	}

}
