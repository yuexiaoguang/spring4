package org.springframework.jms.config;

import javax.jms.ConnectionFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.jms.listener.AbstractMessageListenerContainer;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.destination.DestinationResolver;
import org.springframework.util.ErrorHandler;

/**
 * 用于Spring的基本容器实现的基础{@link JmsListenerContainerFactory}.
 */
public abstract class AbstractJmsListenerContainerFactory<C extends AbstractMessageListenerContainer>
		implements JmsListenerContainerFactory<C> {

	protected final Log logger = LogFactory.getLog(getClass());

	private ConnectionFactory connectionFactory;

	private DestinationResolver destinationResolver;

	private ErrorHandler errorHandler;

	private MessageConverter messageConverter;

	private Boolean sessionTransacted;

	private Integer sessionAcknowledgeMode;

	private Boolean pubSubDomain;

	private Boolean replyPubSubDomain;

	private Boolean subscriptionDurable;

	private Boolean subscriptionShared;

	private String clientId;

	private Integer phase;

	private Boolean autoStartup;


	public void setConnectionFactory(ConnectionFactory connectionFactory) {
		this.connectionFactory = connectionFactory;
	}

	public void setDestinationResolver(DestinationResolver destinationResolver) {
		this.destinationResolver = destinationResolver;
	}

	public void setErrorHandler(ErrorHandler errorHandler) {
		this.errorHandler = errorHandler;
	}

	public void setMessageConverter(MessageConverter messageConverter) {
		this.messageConverter = messageConverter;
	}

	public void setSessionTransacted(Boolean sessionTransacted) {
		this.sessionTransacted = sessionTransacted;
	}

	public void setSessionAcknowledgeMode(Integer sessionAcknowledgeMode) {
		this.sessionAcknowledgeMode = sessionAcknowledgeMode;
	}

	public void setPubSubDomain(Boolean pubSubDomain) {
		this.pubSubDomain = pubSubDomain;
	}

	public void setReplyPubSubDomain(Boolean replyPubSubDomain) {
		this.replyPubSubDomain = replyPubSubDomain;
	}

	public void setSubscriptionDurable(Boolean subscriptionDurable) {
		this.subscriptionDurable = subscriptionDurable;
	}

	public void setSubscriptionShared(Boolean subscriptionShared) {
		this.subscriptionShared = subscriptionShared;
	}

	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	public void setPhase(int phase) {
		this.phase = phase;
	}

	public void setAutoStartup(boolean autoStartup) {
		this.autoStartup = autoStartup;
	}

	@Override
	public C createListenerContainer(JmsListenerEndpoint endpoint) {
		C instance = createContainerInstance();

		if (this.connectionFactory != null) {
			instance.setConnectionFactory(this.connectionFactory);
		}
		if (this.destinationResolver != null) {
			instance.setDestinationResolver(this.destinationResolver);
		}
		if (this.errorHandler != null) {
			instance.setErrorHandler(this.errorHandler);
		}
		if (this.messageConverter != null) {
			instance.setMessageConverter(this.messageConverter);
		}
		if (this.sessionTransacted != null) {
			instance.setSessionTransacted(this.sessionTransacted);
		}
		if (this.sessionAcknowledgeMode != null) {
			instance.setSessionAcknowledgeMode(this.sessionAcknowledgeMode);
		}
		if (this.pubSubDomain != null) {
			instance.setPubSubDomain(this.pubSubDomain);
		}
		if (this.replyPubSubDomain != null) {
			instance.setReplyPubSubDomain(this.replyPubSubDomain);
		}
		if (this.subscriptionDurable != null) {
			instance.setSubscriptionDurable(this.subscriptionDurable);
		}
		if (this.subscriptionShared != null) {
			instance.setSubscriptionShared(this.subscriptionShared);
		}
		if (this.clientId != null) {
			instance.setClientId(this.clientId);
		}
		if (this.phase != null) {
			instance.setPhase(this.phase);
		}
		if (this.autoStartup != null) {
			instance.setAutoStartup(this.autoStartup);
		}

		initializeContainer(instance);
		endpoint.setupListenerContainer(instance);

		return instance;
	}

	/**
	 * 创建一个空的容器实例.
	 */
	protected abstract C createContainerInstance();

	/**
	 * 进一步初始化指定的容器.
	 * <p>如有必要, 子类可以继承此方法以应用额外配置.
	 */
	protected void initializeContainer(C instance) {
	}

}
