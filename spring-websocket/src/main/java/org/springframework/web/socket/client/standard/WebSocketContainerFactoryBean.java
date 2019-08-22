package org.springframework.web.socket.client.standard;

import javax.websocket.ContainerProvider;
import javax.websocket.WebSocketContainer;

import org.springframework.beans.factory.FactoryBean;

/**
 * 通过Spring XML配置创建和配置{@link javax.websocket.WebSocketContainer}的FactoryBean.
 * 在Java配置中, 忽略此类并使用{@code ContainerProvider.getWebSocketContainer()}代替.
 */
public class WebSocketContainerFactoryBean implements FactoryBean<WebSocketContainer> {

	private final WebSocketContainer webSocketContainer = ContainerProvider.getWebSocketContainer();


	public void setAsyncSendTimeout(long timeoutInMillis) {
		this.webSocketContainer.setAsyncSendTimeout(timeoutInMillis);
	}

	public long getAsyncSendTimeout() {
		return this.webSocketContainer.getDefaultAsyncSendTimeout();
	}

	public void setMaxSessionIdleTimeout(long timeoutInMillis) {
		this.webSocketContainer.setDefaultMaxSessionIdleTimeout(timeoutInMillis);
	}

	public long getMaxSessionIdleTimeout() {
		return this.webSocketContainer.getDefaultMaxSessionIdleTimeout();
	}

	public void setMaxTextMessageBufferSize(int bufferSize) {
		this.webSocketContainer.setDefaultMaxTextMessageBufferSize(bufferSize);
	}

	public int getMaxTextMessageBufferSize() {
		return this.webSocketContainer.getDefaultMaxTextMessageBufferSize();
	}

	public void setMaxBinaryMessageBufferSize(int bufferSize) {
		this.webSocketContainer.setDefaultMaxBinaryMessageBufferSize(bufferSize);
	}

	public int getMaxBinaryMessageBufferSize() {
		return this.webSocketContainer.getDefaultMaxBinaryMessageBufferSize();
	}

	@Override
	public WebSocketContainer getObject() throws Exception {
		return this.webSocketContainer;
	}

	@Override
	public Class<?> getObjectType() {
		return WebSocketContainer.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

}
