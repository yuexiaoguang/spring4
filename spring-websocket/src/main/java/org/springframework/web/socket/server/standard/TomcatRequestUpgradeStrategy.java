package org.springframework.web.socket.server.standard;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.websocket.Endpoint;
import javax.websocket.Extension;

import org.apache.tomcat.websocket.server.WsServerContainer;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.server.HandshakeFailureException;

/**
 * Apache Tomcat的WebSocket {@code RequestUpgradeStrategy}.
 * 兼容支持JSR-356的所有Tomcat版本, i.e. Tomcat 7.0.47+ 及更高版本.
 *
 * <p>要修改底层{@link javax.websocket.server.ServerContainer}的属性,
 * 可以在XML配置中使用{@link ServletServerContainerFactoryBean}, 或者在使用Java配置时,
 * 通过"javax.websocket.server.ServerContainer" ServletContext属性访问容器实例.
 */
public class TomcatRequestUpgradeStrategy extends AbstractStandardUpgradeStrategy {

	@Override
	public String[] getSupportedVersions() {
		return new String[] {"13"};
	}

	@Override
	public void upgradeInternal(ServerHttpRequest request, ServerHttpResponse response,
			String selectedProtocol, List<Extension> selectedExtensions, Endpoint endpoint)
			throws HandshakeFailureException {

		HttpServletRequest servletRequest = getHttpServletRequest(request);
		HttpServletResponse servletResponse = getHttpServletResponse(response);

		StringBuffer requestUrl = servletRequest.getRequestURL();
		String path = servletRequest.getRequestURI();  // shouldn't matter
		Map<String, String> pathParams = Collections.<String, String> emptyMap();

		ServerEndpointRegistration endpointConfig = new ServerEndpointRegistration(path, endpoint);
		endpointConfig.setSubprotocols(Collections.singletonList(selectedProtocol));
		endpointConfig.setExtensions(selectedExtensions);

		try {
			getContainer(servletRequest).doUpgrade(servletRequest, servletResponse, endpointConfig, pathParams);
		}
		catch (ServletException ex) {
			throw new HandshakeFailureException(
					"Servlet request failed to upgrade to WebSocket: " + requestUrl, ex);
		}
		catch (IOException ex) {
			throw new HandshakeFailureException(
					"Response update failed during upgrade to WebSocket: " + requestUrl, ex);
		}
	}

	@Override
	public WsServerContainer getContainer(HttpServletRequest request) {
		return (WsServerContainer) super.getContainer(request);
	}

}
