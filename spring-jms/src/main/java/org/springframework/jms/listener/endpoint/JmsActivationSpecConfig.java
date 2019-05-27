package org.springframework.jms.listener.endpoint;

import javax.jms.Session;

import org.springframework.core.Constants;
import org.springframework.jms.support.converter.MessageConverter;

/**
 * 用于激活JMS消息端点的公共配置对象.
 * 获取转换为特定于提供者的JCA 1.5 ActivationSpec对象以激活端点.
 *
 * <p>通常与{@link JmsMessageEndpointManager}结合使用, 但不与它绑定.
 */
public class JmsActivationSpecConfig {

	/** Constants instance for javax.jms.Session */
	private static final Constants sessionConstants = new Constants(Session.class);


	private String destinationName;

	private boolean pubSubDomain = false;

	private Boolean replyPubSubDomain;

	private boolean subscriptionDurable = false;

	private boolean subscriptionShared = false;

	private String subscriptionName;

	private String clientId;

	private String messageSelector;

	private int acknowledgeMode = Session.AUTO_ACKNOWLEDGE;

	private int maxConcurrency = -1;

	private int prefetchSize = -1;

	private MessageConverter messageConverter;


	public void setDestinationName(String destinationName) {
		this.destinationName = destinationName;
	}

	public String getDestinationName() {
		return this.destinationName;
	}

	public void setPubSubDomain(boolean pubSubDomain) {
		this.pubSubDomain = pubSubDomain;
	}

	public boolean isPubSubDomain() {
		return this.pubSubDomain;
	}

	public void setReplyPubSubDomain(boolean replyPubSubDomain) {
		this.replyPubSubDomain = replyPubSubDomain;
	}

	public boolean isReplyPubSubDomain() {
		if (this.replyPubSubDomain != null) {
			return this.replyPubSubDomain;
		}
		else {
			return isPubSubDomain();
		}
	}

	public void setSubscriptionDurable(boolean subscriptionDurable) {
		this.subscriptionDurable = subscriptionDurable;
		if (subscriptionDurable) {
			this.pubSubDomain = true;
		}
	}

	public boolean isSubscriptionDurable() {
		return this.subscriptionDurable;
	}

	public void setSubscriptionShared(boolean subscriptionShared) {
		this.subscriptionShared = subscriptionShared;
		if (subscriptionShared) {
			this.pubSubDomain = true;
		}
	}

	public boolean isSubscriptionShared() {
		return this.subscriptionShared;
	}

	public void setSubscriptionName(String subscriptionName) {
		this.subscriptionName = subscriptionName;
	}

	public String getSubscriptionName() {
		return this.subscriptionName;
	}

	public void setDurableSubscriptionName(String durableSubscriptionName) {
		this.subscriptionName = durableSubscriptionName;
		this.subscriptionDurable = true;
	}

	public String getDurableSubscriptionName() {
		return (this.subscriptionDurable ? this.subscriptionName : null);
	}

	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	public String getClientId() {
		return this.clientId;
	}

	public void setMessageSelector(String messageSelector) {
		this.messageSelector = messageSelector;
	}

	public String getMessageSelector() {
		return this.messageSelector;
	}

	/**
	 * 通过JMS {@link Session}接口中相应常量的名称设置JMS确认模式, e.g. "CLIENT_ACKNOWLEDGE".
	 * <p>请注意, JCA资源适配器通常只支持auto和dups-ok (see Spring's {@link StandardJmsActivationSpecFactory}).
	 * ActiveMQ还以RA管理的事务形式支持"SESSION_TRANSACTED" (由Spring的{@link DefaultJmsActivationSpecFactory}自动转换).
	 * 
	 * @param constantName {@link Session}确认模式常量的名称
	 */
	public void setAcknowledgeModeName(String constantName) {
		setAcknowledgeMode(sessionConstants.asNumber(constantName).intValue());
	}

	/**
	 * 设置要使用的JMS确认模式.
	 */
	public void setAcknowledgeMode(int acknowledgeMode) {
		this.acknowledgeMode = acknowledgeMode;
	}

	/**
	 * 返回要使用的JMS确认模式.
	 */
	public int getAcknowledgeMode() {
		return this.acknowledgeMode;
	}

	/**
	 * 通过"下限-上限"字符串指定并发限制, e.g. "5-10", 或简单的上限字符串, e.g. "10".
	 * <p>JCA监听器容器将始终从零扩展到给定的上限. 有效地忽略指定的下限.
	 * <p>此属性主要支持与
	 * {@link org.springframework.jms.listener.DefaultMessageListenerContainer}的配置兼容性.
	 * 对于此激活配置, 通常使用{@link #setMaxConcurrency}.
	 */
	public void setConcurrency(String concurrency) {
		try {
			int separatorIndex = concurrency.indexOf('-');
			if (separatorIndex != -1) {
				setMaxConcurrency(Integer.parseInt(concurrency.substring(separatorIndex + 1, concurrency.length())));
			}
			else {
				setMaxConcurrency(Integer.parseInt(concurrency));
			}
		}
		catch (NumberFormatException ex) {
			throw new IllegalArgumentException("Invalid concurrency value [" + concurrency + "]: only " +
					"single maximum integer (e.g. \"5\") and minimum-maximum combo (e.g. \"3-5\") supported. " +
					"Note that JmsActivationSpecConfig will effectively ignore the minimum value and " +
					"scale from zero up to the number of consumers according to the maximum value.");
		}
	}

	/**
	 * 指定要使用的最大消费者/会话数, 从而有效地控制目标监听器上的并发调用数.
	 */
	public void setMaxConcurrency(int maxConcurrency) {
		this.maxConcurrency = maxConcurrency;
	}

	/**
	 * 返回要使用的最大消费者/会话数.
	 */
	public int getMaxConcurrency() {
		return this.maxConcurrency;
	}

	/**
	 * 指定要加载到会话中的最大消息数 (一种批处理大小).
	 */
	public void setPrefetchSize(int prefetchSize) {
		this.prefetchSize = prefetchSize;
	}

	/**
	 * 返回要加载到会话中的最大消息数.
	 */
	public int getPrefetchSize() {
		return this.prefetchSize;
	}

	/**
	 * 设置用于转换JMS消息的{@link MessageConverter}策略.
	 * 
	 * @param messageConverter 要使用的消息转换器
	 */
	public void setMessageConverter(MessageConverter messageConverter) {
		this.messageConverter = messageConverter;
	}

	/**
	 * 返回要使用的{@link MessageConverter}.
	 */
	public MessageConverter getMessageConverter() {
		return this.messageConverter;
	}

}
