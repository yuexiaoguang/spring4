package org.springframework.jms.config;

import java.lang.reflect.Method;
import java.util.Arrays;

import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.EmbeddedValueResolver;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.jms.listener.MessageListenerContainer;
import org.springframework.jms.listener.adapter.MessagingMessageListenerAdapter;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.destination.DestinationResolver;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.handler.annotation.support.MessageHandlerMethodFactory;
import org.springframework.messaging.handler.invocation.InvocableHandlerMethod;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.util.StringValueResolver;

/**
 * 一个{@link JmsListenerEndpoint}, 提供处理该端点的传入消息的方法.
 */
public class MethodJmsListenerEndpoint extends AbstractJmsListenerEndpoint implements BeanFactoryAware {

	private Object bean;

	private Method method;

	private Method mostSpecificMethod;

	private MessageHandlerMethodFactory messageHandlerMethodFactory;

	private StringValueResolver embeddedValueResolver;


	/**
	 * 设置调用此端点方法的实际的bean实例.
	 */
	public void setBean(Object bean) {
		this.bean = bean;
	}

	public Object getBean() {
		return this.bean;
	}

	/**
	 * 设置处理此端点管理的消息的方法.
	 */
	public void setMethod(Method method) {
		this.method = method;
	}

	public Method getMethod() {
		return this.method;
	}

	/**
	 * 设置此端点声明的最具体方法.
	 * <p>如果是代理, 这将是目标类的方法 (如果注解了自己, 也就是说, 如果不是在接口中注解的话).
	 */
	public void setMostSpecificMethod(Method mostSpecificMethod) {
		this.mostSpecificMethod = mostSpecificMethod;
	}

	public Method getMostSpecificMethod() {
		if (this.mostSpecificMethod != null) {
			return this.mostSpecificMethod;
		}
		Method method = getMethod();
		if (method != null) {
			Object bean = getBean();
			if (AopUtils.isAopProxy(bean)) {
				Class<?> targetClass = AopProxyUtils.ultimateTargetClass(bean);
				method = AopUtils.getMostSpecificMethod(method, targetClass);
			}
		}
		return method;
	}

	/**
	 * 设置{@link MessageHandlerMethodFactory}, 用于构建负责管理此端点的调用的{@link InvocableHandlerMethod}.
	 */
	public void setMessageHandlerMethodFactory(MessageHandlerMethodFactory messageHandlerMethodFactory) {
		this.messageHandlerMethodFactory = messageHandlerMethodFactory;
	}

	/**
	 * 设置嵌入的占位符和表达式的值解析器.
	 */
	public void setEmbeddedValueResolver(StringValueResolver embeddedValueResolver) {
		this.embeddedValueResolver = embeddedValueResolver;
	}

	/**
	 * 设置用于解析表达式的{@link BeanFactory} (may be {@code null}).
	 */
	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		if (this.embeddedValueResolver == null && beanFactory instanceof ConfigurableBeanFactory) {
			this.embeddedValueResolver = new EmbeddedValueResolver((ConfigurableBeanFactory) beanFactory);
		}
	}


	@Override
	protected MessagingMessageListenerAdapter createMessageListener(MessageListenerContainer container) {
		Assert.state(this.messageHandlerMethodFactory != null,
				"Could not create message listener - MessageHandlerMethodFactory not set");
		MessagingMessageListenerAdapter messageListener = createMessageListenerInstance();
		InvocableHandlerMethod invocableHandlerMethod =
				this.messageHandlerMethodFactory.createInvocableHandlerMethod(getBean(), getMethod());
		messageListener.setHandlerMethod(invocableHandlerMethod);
		String responseDestination = getDefaultResponseDestination();
		if (StringUtils.hasText(responseDestination)) {
			if (container.isReplyPubSubDomain()) {
				messageListener.setDefaultResponseTopicName(responseDestination);
			}
			else {
				messageListener.setDefaultResponseQueueName(responseDestination);
			}
		}
		MessageConverter messageConverter = container.getMessageConverter();
		if (messageConverter != null) {
			messageListener.setMessageConverter(messageConverter);
		}
		DestinationResolver destinationResolver = container.getDestinationResolver();
		if (destinationResolver != null) {
			messageListener.setDestinationResolver(destinationResolver);
		}
		return messageListener;
	}

	/**
	 * 创建一个空的{@link MessagingMessageListenerAdapter}实例.
	 * 
	 * @return 新的{@code MessagingMessageListenerAdapter}或子类
	 */
	protected MessagingMessageListenerAdapter createMessageListenerInstance() {
		return new MessagingMessageListenerAdapter();
	}

	/**
	 * 返回默认响应目标.
	 */
	protected String getDefaultResponseDestination() {
		Method specificMethod = getMostSpecificMethod();
		SendTo ann = getSendTo(specificMethod);
		if (ann != null) {
			Object[] destinations = ann.value();
			if (destinations.length != 1) {
				throw new IllegalStateException("Invalid @" + SendTo.class.getSimpleName() + " annotation on '" +
						specificMethod + "' one destination must be set (got " + Arrays.toString(destinations) + ")");
			}
			return resolve((String) destinations[0]);
		}
		return null;
	}

	private SendTo getSendTo(Method specificMethod) {
		SendTo ann = AnnotatedElementUtils.findMergedAnnotation(specificMethod, SendTo.class);
		if (ann == null) {
			ann = AnnotatedElementUtils.findMergedAnnotation(specificMethod.getDeclaringClass(), SendTo.class);
		}
		return ann;
	}

	private String resolve(String value) {
		return (this.embeddedValueResolver != null ? this.embeddedValueResolver.resolveStringValue(value) : value);
	}


	@Override
	protected StringBuilder getEndpointDescription() {
		return super.getEndpointDescription()
				.append(" | bean='").append(this.bean).append("'")
				.append(" | method='").append(this.method).append("'");
	}

}
