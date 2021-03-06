package org.springframework.web.socket.client.standard;

import java.util.Arrays;
import java.util.List;
import javax.websocket.ClientEndpointConfig;
import javax.websocket.ClientEndpointConfig.Configurator;
import javax.websocket.ContainerProvider;
import javax.websocket.Decoder;
import javax.websocket.Encoder;
import javax.websocket.Endpoint;
import javax.websocket.Extension;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.util.Assert;
import org.springframework.web.socket.client.ConnectionManagerSupport;
import org.springframework.web.socket.handler.BeanCreatingHandlerProvider;

/**
 * 给定URI, {@link Endpoint}的WebSocket连接管理器, 通过{@link #start()}和{@link #stop()}方法连接到WebSocket服务器.
 * 如果{@link #setAutoStartup(boolean)}设置为{@code true}, 那么当Spring ApplicationContext刷新时, 这将自动完成.
 */
public class EndpointConnectionManager extends ConnectionManagerSupport implements BeanFactoryAware {

	private final Endpoint endpoint;

	private final BeanCreatingHandlerProvider<Endpoint> endpointProvider;

	private final ClientEndpointConfig.Builder configBuilder = ClientEndpointConfig.Builder.create();

	private WebSocketContainer webSocketContainer = ContainerProvider.getWebSocketContainer();

	private TaskExecutor taskExecutor = new SimpleAsyncTaskExecutor("EndpointConnectionManager-");

	private volatile Session session;


	public EndpointConnectionManager(Endpoint endpoint, String uriTemplate, Object... uriVariables) {
		super(uriTemplate, uriVariables);
		Assert.notNull(endpoint, "endpoint must not be null");
		this.endpointProvider = null;
		this.endpoint = endpoint;
	}

	public EndpointConnectionManager(Class<? extends Endpoint> endpointClass, String uriTemplate, Object... uriVars) {
		super(uriTemplate, uriVars);
		Assert.notNull(endpointClass, "endpointClass must not be null");
		this.endpointProvider = new BeanCreatingHandlerProvider<Endpoint>(endpointClass);
		this.endpoint = null;
	}


	public void setSupportedProtocols(String... protocols) {
		this.configBuilder.preferredSubprotocols(Arrays.asList(protocols));
	}

	public void setExtensions(Extension... extensions) {
		this.configBuilder.extensions(Arrays.asList(extensions));
	}

	public void setEncoders(List<Class<? extends Encoder>> encoders) {
		this.configBuilder.encoders(encoders);
	}

	public void setDecoders(List<Class<? extends Decoder>> decoders) {
		this.configBuilder.decoders(decoders);
	}

	public void setConfigurator(Configurator configurator) {
		this.configBuilder.configurator(configurator);
	}

	public void setWebSocketContainer(WebSocketContainer webSocketContainer) {
		this.webSocketContainer = webSocketContainer;
	}

	public WebSocketContainer getWebSocketContainer() {
		return this.webSocketContainer;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		if (this.endpointProvider != null) {
			this.endpointProvider.setBeanFactory(beanFactory);
		}
	}

	/**
	 * 设置{@link TaskExecutor}以用于打开连接.
	 * 默认使用{@link SimpleAsyncTaskExecutor}.
	 */
	public void setTaskExecutor(TaskExecutor taskExecutor) {
		Assert.notNull(taskExecutor, "TaskExecutor must not be null");
		this.taskExecutor = taskExecutor;
	}

	/**
	 * 返回配置的{@link TaskExecutor}.
	 */
	public TaskExecutor getTaskExecutor() {
		return this.taskExecutor;
	}


	@Override
	protected void openConnection() {
		this.taskExecutor.execute(new Runnable() {
			@Override
			public void run() {
				try {
					if (logger.isInfoEnabled()) {
						logger.info("Connecting to WebSocket at " + getUri());
					}
					Endpoint endpointToUse = (endpoint != null) ? endpoint : endpointProvider.getHandler();
					ClientEndpointConfig endpointConfig = configBuilder.build();
					session = getWebSocketContainer().connectToServer(endpointToUse, endpointConfig, getUri());
					logger.info("Successfully connected to WebSocket");
				}
				catch (Throwable ex) {
					logger.error("Failed to connect to WebSocket", ex);
				}
			}
		});
	}

	@Override
	protected void closeConnection() throws Exception {
		try {
			if (isConnected()) {
				this.session.close();
			}
		}
		finally {
			this.session = null;
		}
	}

	@Override
	protected boolean isConnected() {
		return (this.session != null && this.session.isOpen());
	}

}
