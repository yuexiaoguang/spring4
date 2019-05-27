package org.springframework.web.socket.client.standard;

import javax.websocket.ContainerProvider;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.util.Assert;
import org.springframework.web.socket.client.ConnectionManagerSupport;
import org.springframework.web.socket.handler.BeanCreatingHandlerProvider;

/**
 * A WebSocket connection manager that is given a URI, a
 * {@link javax.websocket.ClientEndpoint}-annotated endpoint, connects to a
 * WebSocket server through the {@link #start()} and {@link #stop()} methods.
 * If {@link #setAutoStartup(boolean)} is set to {@code true} this will be
 * done automatically when the Spring ApplicationContext is refreshed.
 */
public class AnnotatedEndpointConnectionManager extends ConnectionManagerSupport implements BeanFactoryAware {

	private final Object endpoint;

	private final BeanCreatingHandlerProvider<Object> endpointProvider;

	private WebSocketContainer webSocketContainer = ContainerProvider.getWebSocketContainer();

	private TaskExecutor taskExecutor = new SimpleAsyncTaskExecutor("AnnotatedEndpointConnectionManager-");

	private volatile Session session;


	public AnnotatedEndpointConnectionManager(Object endpoint, String uriTemplate, Object... uriVariables) {
		super(uriTemplate, uriVariables);
		this.endpointProvider = null;
		this.endpoint = endpoint;
	}

	public AnnotatedEndpointConnectionManager(Class<?> endpointClass, String uriTemplate, Object... uriVariables) {
		super(uriTemplate, uriVariables);
		this.endpointProvider = new BeanCreatingHandlerProvider<Object>(endpointClass);
		this.endpoint = null;
	}


	public void setWebSocketContainer(WebSocketContainer webSocketContainer) {
		this.webSocketContainer = webSocketContainer;
	}

	public WebSocketContainer getWebSocketContainer() {
		return this.webSocketContainer;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		if (this.endpointProvider != null) {
			this.endpointProvider.setBeanFactory(beanFactory);
		}
	}

	/**
	 * Set a {@link TaskExecutor} to use to open the connection.
	 * By default {@link SimpleAsyncTaskExecutor} is used.
	 */
	public void setTaskExecutor(TaskExecutor taskExecutor) {
		Assert.notNull(taskExecutor, "TaskExecutor must not be null");
		this.taskExecutor = taskExecutor;
	}

	/**
	 * Return the configured {@link TaskExecutor}.
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
					Object endpointToUse = (endpoint != null) ? endpoint : endpointProvider.getHandler();
					session = webSocketContainer.connectToServer(endpointToUse, getUri());
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
