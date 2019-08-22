package org.springframework.web.socket.server.standard;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.websocket.Decoder;
import javax.websocket.Encoder;
import javax.websocket.Endpoint;
import javax.websocket.Extension;
import javax.websocket.HandshakeResponse;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerEndpointConfig;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.util.Assert;
import org.springframework.web.socket.handler.BeanCreatingHandlerProvider;

/**
 * 用于基于Spring的应用程序的{@link javax.websocket.server.ServerEndpointConfig}的实现.
 * {@link ServerEndpointExporter}检测{@link ServerEndpointRegistration} bean,
 * 并在启动时使用Java WebSocket运行时注册.
 *
 * <p>类构造函数接受单个{@link javax.websocket.Endpoint}实例或类型{@link Class}指定的端点.
 * 当按类型指定时, 端点将在每个客户端WebSocket连接之前通过Spring ApplicationContext进行实例化和初始化.
 *
 * <p>此类还扩展了{@link javax.websocket.server.ServerEndpointConfig.Configurator},
 * 以便更轻松地覆盖自定义握手过程的方法.
 */
public class ServerEndpointRegistration extends ServerEndpointConfig.Configurator
		implements ServerEndpointConfig, BeanFactoryAware {

	private final String path;

	private final Endpoint endpoint;

	private final BeanCreatingHandlerProvider<Endpoint> endpointProvider;

	private List<String> subprotocols = new ArrayList<String>(0);

	private List<Extension> extensions = new ArrayList<Extension>(0);

	private List<Class<? extends Encoder>> encoders = new ArrayList<Class<? extends Encoder>>(0);

	private List<Class<? extends Decoder>> decoders = new ArrayList<Class<? extends Decoder>>(0);

	private final Map<String, Object> userProperties = new HashMap<String, Object>(4);


	/**
	 * @param path 端点路径
	 * @param endpoint 端点实例
	 */
	public ServerEndpointRegistration(String path, Endpoint endpoint) {
		Assert.hasText(path, "Path must not be empty");
		Assert.notNull(endpoint, "Endpoint must not be null");
		this.path = path;
		this.endpoint = endpoint;
		this.endpointProvider = null;
	}

	/**
	 * @param path 端点路径
	 * @param endpointClass 端点类
	 */
	public ServerEndpointRegistration(String path, Class<? extends Endpoint> endpointClass) {
		Assert.hasText(path, "Path must not be empty");
		Assert.notNull(endpointClass, "Endpoint Class must not be null");
		this.path = path;
		this.endpoint = null;
		this.endpointProvider = new BeanCreatingHandlerProvider<Endpoint>(endpointClass);
	}


	// ServerEndpointConfig implementation

	@Override
	public String getPath() {
		return this.path;
	}

	@Override
	public Class<? extends Endpoint> getEndpointClass() {
		return (this.endpoint != null ? this.endpoint.getClass() : this.endpointProvider.getHandlerType());
	}

	public Endpoint getEndpoint() {
		return (this.endpoint != null) ? this.endpoint : this.endpointProvider.getHandler();
	}

	public void setSubprotocols(List<String> subprotocols) {
		this.subprotocols = subprotocols;
	}

	@Override
	public List<String> getSubprotocols() {
		return this.subprotocols;
	}

	public void setExtensions(List<Extension> extensions) {
		this.extensions = extensions;
	}

	@Override
	public List<Extension> getExtensions() {
		return this.extensions;
	}

	public void setEncoders(List<Class<? extends Encoder>> encoders) {
		this.encoders = encoders;
	}

	@Override
	public List<Class<? extends Encoder>> getEncoders() {
		return this.encoders;
	}

	public void setDecoders(List<Class<? extends Decoder>> decoders) {
		this.decoders = decoders;
	}

	@Override
	public List<Class<? extends Decoder>> getDecoders() {
		return this.decoders;
	}

	public void setUserProperties(Map<String, Object> userProperties) {
		this.userProperties.clear();
		this.userProperties.putAll(userProperties);
	}

	@Override
	public Map<String, Object> getUserProperties() {
		return this.userProperties;
	}

	@Override
	public Configurator getConfigurator() {
		return this;
	}


	// ServerEndpointConfig.Configurator implementation

	@SuppressWarnings("unchecked")
	@Override
	public final <T> T getEndpointInstance(Class<T> clazz) throws InstantiationException {
		return (T) getEndpoint();
	}

	@Override
	public void modifyHandshake(ServerEndpointConfig sec, HandshakeRequest request, HandshakeResponse response) {
		super.modifyHandshake(this, request, response);
	}


	// Remaining methods

	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		if (this.endpointProvider != null) {
			this.endpointProvider.setBeanFactory(beanFactory);
		}
	}

	@Override
	public String toString() {
		return "ServerEndpointRegistration for path '" + getPath() + "': " + getEndpointClass();
	}

}
