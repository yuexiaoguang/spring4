package org.springframework.web.socket.server.standard;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.websocket.Endpoint;
import javax.websocket.Extension;
import javax.websocket.server.ServerContainer;
import javax.websocket.server.ServerEndpointConfig;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.server.HandshakeFailureException;

/**
 * WebSphere支持在WebSocket握手期间升级{@link HttpServletRequest}.
 * 要修改底层{@link javax.websocket.server.ServerContainer}的属性,
 * 可以在XML配置中使用{@link ServletServerContainerFactoryBean}, 或者在使用Java配置时,
 * 通过"javax.websocket.server.ServerContainer" ServletContext属性访问容器实例.
 *
 * <p>为即将发布的8.5.5.7版本测试了WAS Liberty beta (August 2015).
 */
public class WebSphereRequestUpgradeStrategy extends AbstractStandardUpgradeStrategy {

	private final static Method upgradeMethod;

	static {
		ClassLoader loader = WebSphereRequestUpgradeStrategy.class.getClassLoader();
		try {
			Class<?> type = loader.loadClass("com.ibm.websphere.wsoc.WsWsocServerContainer");
			upgradeMethod = type.getMethod("doUpgrade", HttpServletRequest.class,
					HttpServletResponse.class, ServerEndpointConfig.class, Map.class);
		}
		catch (Exception ex) {
			throw new IllegalStateException("No compatible WebSphere version found", ex);
		}
	}


	@Override
	public String[] getSupportedVersions() {
		return new String[] {"13"};
	}

	@Override
	public void upgradeInternal(ServerHttpRequest httpRequest, ServerHttpResponse httpResponse,
			String selectedProtocol, List<Extension> selectedExtensions, Endpoint endpoint)
			throws HandshakeFailureException {

		HttpServletRequest request = getHttpServletRequest(httpRequest);
		HttpServletResponse response = getHttpServletResponse(httpResponse);

		StringBuffer requestUrl = request.getRequestURL();
		String path = request.getRequestURI();  // shouldn't matter
		Map<String, String> pathParams = Collections.<String, String> emptyMap();

		ServerEndpointRegistration endpointConfig = new ServerEndpointRegistration(path, endpoint);
		endpointConfig.setSubprotocols(Collections.singletonList(selectedProtocol));
		endpointConfig.setExtensions(selectedExtensions);

		try {
			ServerContainer container = getContainer(request);
			upgradeMethod.invoke(container, request, response, endpointConfig, pathParams);
		}
		catch (Exception ex) {
			throw new HandshakeFailureException(
					"Servlet request failed to upgrade to WebSocket for " + requestUrl, ex);
		}
	}

}
