package org.springframework.jms.listener.endpoint;

import java.util.Map;
import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.Topic;
import javax.resource.spi.ActivationSpec;
import javax.resource.spi.ResourceAdapter;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.jms.support.destination.DestinationResolutionException;
import org.springframework.jms.support.destination.DestinationResolver;

/**
 * {@link JmsActivationSpecFactory}接口的标准实现.
 * 支持JMS 1.5规范 (附录 B)定义的标准JMS属性; 忽略Spring的"maxConcurrency"和"prefetchSize"设置.
 *
 * <p>'activationSpecClass'属性是必需的, 显式定义提供者的ActivationSpec类的完全限定类名
 * (e.g. "org.apache.activemq.ra.ActiveMQActivationSpec").
 *
 * <p>查看{@link DefaultJmsActivationSpecFactory}以获取此类的扩展变体, 支持除纯JMS 1.5规范之外的一些其他默认约定.
 */
public class StandardJmsActivationSpecFactory implements JmsActivationSpecFactory {

	private Class<?> activationSpecClass;

	private Map<String, String> defaultProperties;

	private DestinationResolver destinationResolver;


	/**
	 * 为目标提供者指定完全限定的ActivationSpec类名
	 * (e.g. "org.apache.activemq.ra.ActiveMQActivationSpec").
	 */
	public void setActivationSpecClass(Class<?> activationSpecClass) {
		this.activationSpecClass = activationSpecClass;
	}

	/**
	 * 使用String键和String值指定自定义默认属性.
	 * <p>在使用特定于侦听器的设置填充之前, 应用于每个ActivationSpec对象.
	 * 允许在{@link JmsActivationSpecConfig}中配置超出Spring定义设置的供应商特定属性.
	 */
	public void setDefaultProperties(Map<String, String> defaultProperties) {
		this.defaultProperties = defaultProperties;
	}

	/**
	 * 设置DestinationResolver, 用于将目标名称解析为JCA 1.5 ActivationSpec "destination"属性.
	 * <p>如果未指定, 则目标名称将仅作为字符串传递.
	 * 如果指定, 则首先将目标名称解析为Destination对象.
	 * <p>请注意, 与此工厂一起使用的DestinationResolver必须能够在没有活动的JMS会话的情况下工作:
	 * e.g.
	 * {@link org.springframework.jms.support.destination.JndiDestinationResolver}
	 * 或{@link org.springframework.jms.support.destination.BeanFactoryDestinationResolver},
	 * 而不是{@link org.springframework.jms.support.destination.DynamicDestinationResolver}.
	 */
	public void setDestinationResolver(DestinationResolver destinationResolver) {
		this.destinationResolver = destinationResolver;
	}

	/**
	 * 返回用于解析目标名称的{@link DestinationResolver}.
	 */
	public DestinationResolver getDestinationResolver() {
		return this.destinationResolver;
	}

	@Override
	public ActivationSpec createActivationSpec(ResourceAdapter adapter, JmsActivationSpecConfig config) {
		Class<?> activationSpecClassToUse = this.activationSpecClass;
		if (activationSpecClassToUse == null) {
			activationSpecClassToUse = determineActivationSpecClass(adapter);
			if (activationSpecClassToUse == null) {
				throw new IllegalStateException("Property 'activationSpecClass' is required");
			}
		}

		ActivationSpec spec = (ActivationSpec) BeanUtils.instantiateClass(activationSpecClassToUse);
		BeanWrapper bw = PropertyAccessorFactory.forBeanPropertyAccess(spec);
		if (this.defaultProperties != null) {
			bw.setPropertyValues(this.defaultProperties);
		}
		populateActivationSpecProperties(bw, config);
		return spec;
	}

	/**
	 * 确定给定ResourceAdapter的ActivationSpec类. 如果没有明确设置'activationSpecClass', 则调用.
	 * 
	 * @param adapter 要检查的ResourceAdapter
	 * 
	 * @return 相应的ActivationSpec类, 或{@code null}
	 */
	protected Class<?> determineActivationSpecClass(ResourceAdapter adapter) {
		return null;
	}

	/**
	 * 使用给定配置对象中定义的设置填充给定的ApplicationSpec对象.
	 * <p>此实现应用所有标准JMS设置, 但忽略"maxConcurrency" 和 "prefetchSize" - 标准JCA 1.5不支持.
	 * 
	 * @param bw 包装ActivationSpec对象的BeanWrapper
	 * @param config 配置的对象保存常见的JMS设置
	 */
	protected void populateActivationSpecProperties(BeanWrapper bw, JmsActivationSpecConfig config) {
		String destinationName = config.getDestinationName();
		boolean pubSubDomain = config.isPubSubDomain();
		Object destination = destinationName;
		if (this.destinationResolver != null) {
			try {
				destination = this.destinationResolver.resolveDestinationName(null, destinationName, pubSubDomain);
			}
			catch (JMSException ex) {
				throw new DestinationResolutionException("Cannot resolve destination name [" + destinationName + "]", ex);
			}
		}
		bw.setPropertyValue("destination", destination);
		bw.setPropertyValue("destinationType", pubSubDomain ? Topic.class.getName() : Queue.class.getName());

		if (bw.isWritableProperty("subscriptionDurability")) {
			bw.setPropertyValue("subscriptionDurability", config.isSubscriptionDurable() ? "Durable" : "NonDurable");
		}
		else if (config.isSubscriptionDurable()) {
			// 标准JCA 1.5 "subscriptionDurability"显然不受支持...
			throw new IllegalArgumentException(
					"Durable subscriptions not supported by underlying provider: " + this.activationSpecClass.getName());
		}
		if (config.isSubscriptionShared()) {
			throw new IllegalArgumentException("Shared subscriptions not supported for JCA-driven endpoints");
		}

		if (config.getSubscriptionName() != null) {
			bw.setPropertyValue("subscriptionName", config.getSubscriptionName());
		}
		if (config.getClientId() != null) {
			bw.setPropertyValue("clientId", config.getClientId());
		}
		if (config.getMessageSelector() != null) {
			bw.setPropertyValue("messageSelector", config.getMessageSelector());
		}
		applyAcknowledgeMode(bw, config.getAcknowledgeMode());
	}

	/**
	 * 将指定的确认模式应用于ActivationSpec对象.
	 * <p>此实现应用标准JCA 1.5确认模式 "Auto-acknowledge" 和 "Dups-ok-acknowledge".
	 * 如果已请求{@code CLIENT_ACKNOWLEDGE}或{@code SESSION_TRANSACTED}, 则会引发异常.
	 * 
	 * @param bw 包装ActivationSpec对象的BeanWrapper
	 * @param ackMode 配置的确认模式 (根据{@link javax.jms.Session}中的常量)
	 */
	protected void applyAcknowledgeMode(BeanWrapper bw, int ackMode) {
		if (ackMode == Session.SESSION_TRANSACTED) {
			throw new IllegalArgumentException("No support for SESSION_TRANSACTED: Only \"Auto-acknowledge\" " +
					"and \"Dups-ok-acknowledge\" supported in standard JCA 1.5");
		}
		else if (ackMode == Session.CLIENT_ACKNOWLEDGE) {
			throw new IllegalArgumentException("No support for CLIENT_ACKNOWLEDGE: Only \"Auto-acknowledge\" " +
					"and \"Dups-ok-acknowledge\" supported in standard JCA 1.5");
		}
		else if (bw.isWritableProperty("acknowledgeMode")) {
			bw.setPropertyValue("acknowledgeMode",
					ackMode == Session.DUPS_OK_ACKNOWLEDGE ? "Dups-ok-acknowledge" : "Auto-acknowledge");
		}
		else if (ackMode == Session.DUPS_OK_ACKNOWLEDGE) {
			// 标准JCA 1.5 "acknowledgeMode"显然不受支持 (e.g. WebSphere MQ 6.0.2.1)
			throw new IllegalArgumentException(
					"Dups-ok-acknowledge not supported by underlying provider: " + this.activationSpecClass.getName());
		}
	}

}
