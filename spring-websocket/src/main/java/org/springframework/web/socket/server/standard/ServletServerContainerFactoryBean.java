package org.springframework.web.socket.server.standard;

import javax.servlet.ServletContext;
import javax.websocket.WebSocketContainer;
import javax.websocket.server.ServerContainer;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;
import org.springframework.web.context.ServletContextAware;

/**
 * 用于配置{@link javax.websocket.server.ServerContainer}的{@link FactoryBean}.
 * 由于通常只有一个{@code ServerContainer}实例可以在众所周知的{@code javax.servlet.ServletContext}属性下访问,
 * 因此只需声明此FactoryBean并使用其setter允许通过Spring配置配置{@code ServerContainer}.
 *
 * <p>即使{@code ServerContainer}没有注入Spring应用程序上下文中的任何其他bean, 这也很有用.
 * 例如, 应用程序可以配置
 * {@link org.springframework.web.socket.server.support.DefaultHandshakeHandler},
 * {@link org.springframework.web.socket.sockjs.SockJsService},
 * 或{@link ServerEndpointExporter}, 并单独声明此FactoryBean以自定义 (唯一的) {@code ServerContainer}实例的属性.
 */
public class ServletServerContainerFactoryBean
		implements FactoryBean<WebSocketContainer>, ServletContextAware, InitializingBean {

	private Long asyncSendTimeout;

	private Long maxSessionIdleTimeout;

	private Integer maxTextMessageBufferSize;

	private Integer maxBinaryMessageBufferSize;

	private ServletContext servletContext;

	private ServerContainer serverContainer;


	public void setAsyncSendTimeout(long timeoutInMillis) {
		this.asyncSendTimeout = timeoutInMillis;
	}

	public Long getAsyncSendTimeout() {
		return this.asyncSendTimeout;
	}

	public void setMaxSessionIdleTimeout(long timeoutInMillis) {
		this.maxSessionIdleTimeout = timeoutInMillis;
	}

	public Long getMaxSessionIdleTimeout() {
		return this.maxSessionIdleTimeout;
	}

	public void setMaxTextMessageBufferSize(int bufferSize) {
		this.maxTextMessageBufferSize = bufferSize;
	}

	public Integer getMaxTextMessageBufferSize() {
		return this.maxTextMessageBufferSize;
	}

	public void setMaxBinaryMessageBufferSize(int bufferSize) {
		this.maxBinaryMessageBufferSize = bufferSize;
	}

	public Integer getMaxBinaryMessageBufferSize() {
		return this.maxBinaryMessageBufferSize;
	}

	@Override
	public void setServletContext(ServletContext servletContext) {
		this.servletContext = servletContext;
	}


	@Override
	public void afterPropertiesSet() {
		Assert.state(this.servletContext != null,
				"A ServletContext is required to access the javax.websocket.server.ServerContainer instance");
		this.serverContainer = (ServerContainer) this.servletContext.getAttribute(
				"javax.websocket.server.ServerContainer");
		Assert.state(this.serverContainer != null,
				"Attribute 'javax.websocket.server.ServerContainer' not found in ServletContext");

		if (this.asyncSendTimeout != null) {
			this.serverContainer.setAsyncSendTimeout(this.asyncSendTimeout);
		}
		if (this.maxSessionIdleTimeout != null) {
			this.serverContainer.setDefaultMaxSessionIdleTimeout(this.maxSessionIdleTimeout);
		}
		if (this.maxTextMessageBufferSize != null) {
			this.serverContainer.setDefaultMaxTextMessageBufferSize(this.maxTextMessageBufferSize);
		}
		if (this.maxBinaryMessageBufferSize != null) {
			this.serverContainer.setDefaultMaxBinaryMessageBufferSize(this.maxBinaryMessageBufferSize);
		}
	}


	@Override
	public ServerContainer getObject() {
		return this.serverContainer;
	}

	@Override
	public Class<?> getObjectType() {
		return (this.serverContainer != null ? this.serverContainer.getClass() : ServerContainer.class);
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

}
