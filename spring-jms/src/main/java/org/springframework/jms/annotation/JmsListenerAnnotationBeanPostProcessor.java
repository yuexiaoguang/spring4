package org.springframework.jms.annotation;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.EmbeddedValueResolver;
import org.springframework.beans.factory.support.MergedBeanDefinitionPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.jms.config.JmsListenerConfigUtils;
import org.springframework.jms.config.JmsListenerContainerFactory;
import org.springframework.jms.config.JmsListenerEndpointRegistrar;
import org.springframework.jms.config.JmsListenerEndpointRegistry;
import org.springframework.jms.config.MethodJmsListenerEndpoint;
import org.springframework.messaging.handler.annotation.support.DefaultMessageHandlerMethodFactory;
import org.springframework.messaging.handler.annotation.support.MessageHandlerMethodFactory;
import org.springframework.messaging.handler.invocation.InvocableHandlerMethod;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.util.StringValueResolver;

/**
 * Bean后处理器, 它注册带{@link JmsListener}注解的方法,
 * 根据注解属性, 由{@link org.springframework.jms.config.JmsListenerContainerFactory}创建的JMS消息监听器容器调用.
 *
 * <p>带注解的方法可以使用{@link JmsListener}定义的灵活参数.
 *
 * <p>这个后处理器由Spring的{@code <jms:annotation-driven>} XML元素, 以及{@link EnableJms}注解自动注册.
 *
 * <p>自动检测容器中的任何{@link JmsListenerConfigurer}实例,
 * 允许自定义要使用的注册表, 默认容器工厂, 或对端点注册进行细粒度控制.
 * See the {@link EnableJms} javadocs for complete usage details.
 */
public class JmsListenerAnnotationBeanPostProcessor
		implements MergedBeanDefinitionPostProcessor, Ordered, BeanFactoryAware, SmartInitializingSingleton {

	/**
	 * 默认{@link JmsListenerContainerFactory}的bean名称.
	 */
	static final String DEFAULT_JMS_LISTENER_CONTAINER_FACTORY_BEAN_NAME = "jmsListenerContainerFactory";


	protected final Log logger = LogFactory.getLog(getClass());

	private JmsListenerEndpointRegistry endpointRegistry;

	private String containerFactoryBeanName = DEFAULT_JMS_LISTENER_CONTAINER_FACTORY_BEAN_NAME;

	private final MessageHandlerMethodFactoryAdapter messageHandlerMethodFactory =
			new MessageHandlerMethodFactoryAdapter();

	private BeanFactory beanFactory;

	private StringValueResolver embeddedValueResolver;

	private final JmsListenerEndpointRegistrar registrar = new JmsListenerEndpointRegistrar();

	private final AtomicInteger counter = new AtomicInteger();

	private final Set<Class<?>> nonAnnotatedClasses =
			Collections.newSetFromMap(new ConcurrentHashMap<Class<?>, Boolean>(64));


	@Override
	public int getOrder() {
		return LOWEST_PRECEDENCE;
	}

	/**
	 * 设置将保存已创建的端点并管理相关监听器容器的生命周期的{@link JmsListenerEndpointRegistry}.
	 */
	public void setEndpointRegistry(JmsListenerEndpointRegistry endpointRegistry) {
		this.endpointRegistry = endpointRegistry;
	}

	/**
	 * 设置默认使用的{@link JmsListenerContainerFactory}的名称.
	 * <p>如果未指定, 则假定定义"jmsListenerContainerFactory".
	 */
	public void setContainerFactoryBeanName(String containerFactoryBeanName) {
		this.containerFactoryBeanName = containerFactoryBeanName;
	}

	/**
	 * 设置{@link MessageHandlerMethodFactory}, 用于配置负责为此处理器检测到的端点提供服务的消息监听器.
	 * <p>默认情况下, 使用{@link DefaultMessageHandlerMethodFactory}, 可以进一步配置它以支持其他方法参数, 或自定义转换和验证支持.
	 * 有关更多详细信息, 请参阅{@link DefaultMessageHandlerMethodFactory} Javadoc.
	 */
	public void setMessageHandlerMethodFactory(MessageHandlerMethodFactory messageHandlerMethodFactory) {
		this.messageHandlerMethodFactory.setMessageHandlerMethodFactory(messageHandlerMethodFactory);
	}

	/**
	 * 使{@link BeanFactory}可用是可选的;
	 * 如果没有设置, {@link JmsListenerConfigurer} bean将不会被自动检测, 并且必须显式配置{@link #setEndpointRegistry 端点注册表}.
	 */
	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
		if (beanFactory instanceof ConfigurableBeanFactory) {
			this.embeddedValueResolver = new EmbeddedValueResolver((ConfigurableBeanFactory) beanFactory);
		}
		this.registrar.setBeanFactory(beanFactory);
	}


	@Override
	public void afterSingletonsInstantiated() {
		// 从缓存中删除已解析的单例类
		this.nonAnnotatedClasses.clear();

		if (this.beanFactory instanceof ListableBeanFactory) {
			// 从BeanFactory应用JmsListenerConfigurer bean
			Map<String, JmsListenerConfigurer> beans =
					((ListableBeanFactory) this.beanFactory).getBeansOfType(JmsListenerConfigurer.class);
			List<JmsListenerConfigurer> configurers = new ArrayList<JmsListenerConfigurer>(beans.values());
			AnnotationAwareOrderComparator.sort(configurers);
			for (JmsListenerConfigurer configurer : configurers) {
				configurer.configureJmsListeners(this.registrar);
			}
		}

		if (this.registrar.getEndpointRegistry() == null) {
			// 从BeanFactory确定JmsListenerEndpointRegistry bean
			if (this.endpointRegistry == null) {
				Assert.state(this.beanFactory != null, "BeanFactory must be set to find endpoint registry by bean name");
				this.endpointRegistry = this.beanFactory.getBean(
						JmsListenerConfigUtils.JMS_LISTENER_ENDPOINT_REGISTRY_BEAN_NAME, JmsListenerEndpointRegistry.class);
			}
			this.registrar.setEndpointRegistry(this.endpointRegistry);
		}

		if (this.containerFactoryBeanName != null) {
			this.registrar.setContainerFactoryBeanName(this.containerFactoryBeanName);
		}

		// 配置器解析完成后, 设置自定义处理器方法工厂
		MessageHandlerMethodFactory handlerMethodFactory = this.registrar.getMessageHandlerMethodFactory();
		if (handlerMethodFactory != null) {
			this.messageHandlerMethodFactory.setMessageHandlerMethodFactory(handlerMethodFactory);
		}

		// 实际注册所有监听器
		this.registrar.afterPropertiesSet();
	}


	@Override
	public void postProcessMergedBeanDefinition(RootBeanDefinition beanDefinition, Class<?> beanType, String beanName) {
	}

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		return bean;
	}

	@Override
	public Object postProcessAfterInitialization(final Object bean, String beanName) throws BeansException {
		if (!this.nonAnnotatedClasses.contains(bean.getClass())) {
			Class<?> targetClass = AopProxyUtils.ultimateTargetClass(bean);
			Map<Method, Set<JmsListener>> annotatedMethods = MethodIntrospector.selectMethods(targetClass,
					new MethodIntrospector.MetadataLookup<Set<JmsListener>>() {
						@Override
						public Set<JmsListener> inspect(Method method) {
							Set<JmsListener> listenerMethods = AnnotatedElementUtils.getMergedRepeatableAnnotations(
									method, JmsListener.class, JmsListeners.class);
							return (!listenerMethods.isEmpty() ? listenerMethods : null);
						}
					});
			if (annotatedMethods.isEmpty()) {
				this.nonAnnotatedClasses.add(bean.getClass());
				if (logger.isTraceEnabled()) {
					logger.trace("No @JmsListener annotations found on bean type: " + bean.getClass());
				}
			}
			else {
				// Non-empty set of methods
				for (Map.Entry<Method, Set<JmsListener>> entry : annotatedMethods.entrySet()) {
					Method method = entry.getKey();
					for (JmsListener listener : entry.getValue()) {
						processJmsListener(listener, method, bean);
					}
				}
				if (logger.isDebugEnabled()) {
					logger.debug(annotatedMethods.size() + " @JmsListener methods processed on bean '" + beanName +
							"': " + annotatedMethods);
				}
			}
		}
		return bean;
	}

	/**
	 * 处理给定方法上给定的{@link JmsListener}注解, 为给定的bean实例注册相应的端点.
	 * 
	 * @param jmsListener 要处理的注解
	 * @param mostSpecificMethod 带注解的方法
	 * @param bean 要调用方法的实例
	 */
	protected void processJmsListener(JmsListener jmsListener, Method mostSpecificMethod, Object bean) {
		Method invocableMethod = AopUtils.selectInvocableMethod(mostSpecificMethod, bean.getClass());

		MethodJmsListenerEndpoint endpoint = createMethodJmsListenerEndpoint();
		endpoint.setBean(bean);
		endpoint.setMethod(invocableMethod);
		endpoint.setMostSpecificMethod(mostSpecificMethod);
		endpoint.setMessageHandlerMethodFactory(this.messageHandlerMethodFactory);
		endpoint.setEmbeddedValueResolver(this.embeddedValueResolver);
		endpoint.setBeanFactory(this.beanFactory);
		endpoint.setId(getEndpointId(jmsListener));
		endpoint.setDestination(resolve(jmsListener.destination()));
		if (StringUtils.hasText(jmsListener.selector())) {
			endpoint.setSelector(resolve(jmsListener.selector()));
		}
		if (StringUtils.hasText(jmsListener.subscription())) {
			endpoint.setSubscription(resolve(jmsListener.subscription()));
		}
		if (StringUtils.hasText(jmsListener.concurrency())) {
			endpoint.setConcurrency(resolve(jmsListener.concurrency()));
		}

		JmsListenerContainerFactory<?> factory = null;
		String containerFactoryBeanName = resolve(jmsListener.containerFactory());
		if (StringUtils.hasText(containerFactoryBeanName)) {
			Assert.state(this.beanFactory != null, "BeanFactory must be set to obtain container factory by bean name");
			try {
				factory = this.beanFactory.getBean(containerFactoryBeanName, JmsListenerContainerFactory.class);
			}
			catch (NoSuchBeanDefinitionException ex) {
				throw new BeanInitializationException("Could not register JMS listener endpoint on [" +
						mostSpecificMethod + "], no " + JmsListenerContainerFactory.class.getSimpleName() +
						" with id '" + containerFactoryBeanName + "' was found in the application context", ex);
			}
		}

		this.registrar.registerEndpoint(endpoint, factory);
	}

	/**
	 * 使用{@link #processJmsListener}中提供的参数实例化一个空的{@link MethodJmsListenerEndpoint}以进行进一步配置.
	 * 
	 * @return 新的{@code MethodJmsListenerEndpoint}或其子类
	 */
	protected MethodJmsListenerEndpoint createMethodJmsListenerEndpoint() {
		return new MethodJmsListenerEndpoint();
	}

	private String getEndpointId(JmsListener jmsListener) {
		if (StringUtils.hasText(jmsListener.id())) {
			return resolve(jmsListener.id());
		}
		else {
			return "org.springframework.jms.JmsListenerEndpointContainer#" + this.counter.getAndIncrement();
		}
	}

	private String resolve(String value) {
		return (this.embeddedValueResolver != null ? this.embeddedValueResolver.resolveStringValue(value) : value);
	}


	/**
	 * {@link MessageHandlerMethodFactory}适配器, 它提供了一个可配置的底层实例.
	 * 如果在端点已注册但尚未创建的情况下确定要使用的工厂, 则非常有用.
	 */
	private class MessageHandlerMethodFactoryAdapter implements MessageHandlerMethodFactory {

		private MessageHandlerMethodFactory messageHandlerMethodFactory;

		public void setMessageHandlerMethodFactory(MessageHandlerMethodFactory messageHandlerMethodFactory) {
			this.messageHandlerMethodFactory = messageHandlerMethodFactory;
		}

		@Override
		public InvocableHandlerMethod createInvocableHandlerMethod(Object bean, Method method) {
			return getMessageHandlerMethodFactory().createInvocableHandlerMethod(bean, method);
		}

		private MessageHandlerMethodFactory getMessageHandlerMethodFactory() {
			if (this.messageHandlerMethodFactory == null) {
				this.messageHandlerMethodFactory = createDefaultJmsHandlerMethodFactory();
			}
			return this.messageHandlerMethodFactory;
		}

		private MessageHandlerMethodFactory createDefaultJmsHandlerMethodFactory() {
			DefaultMessageHandlerMethodFactory defaultFactory = new DefaultMessageHandlerMethodFactory();
			defaultFactory.setBeanFactory(beanFactory);
			defaultFactory.afterPropertiesSet();
			return defaultFactory;
		}
	}
}
