package org.springframework.jms.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.messaging.handler.annotation.support.DefaultMessageHandlerMethodFactory;
import org.springframework.messaging.handler.annotation.support.MessageHandlerMethodFactory;
import org.springframework.util.Assert;

/**
 * 使用{@link JmsListenerEndpointRegistry}注册{@link JmsListenerEndpoint}的助手bean.
 */
public class JmsListenerEndpointRegistrar implements BeanFactoryAware, InitializingBean {

	private JmsListenerEndpointRegistry endpointRegistry;

	private MessageHandlerMethodFactory messageHandlerMethodFactory;

	private JmsListenerContainerFactory<?> containerFactory;

	private String containerFactoryBeanName;

	private BeanFactory beanFactory;

	private final List<JmsListenerEndpointDescriptor> endpointDescriptors =
			new ArrayList<JmsListenerEndpointDescriptor>();

	private boolean startImmediately;

	private Object mutex = endpointDescriptors;


	/**
	 * 设置要使用的{@link JmsListenerEndpointRegistry}实例.
	 */
	public void setEndpointRegistry(JmsListenerEndpointRegistry endpointRegistry) {
		this.endpointRegistry = endpointRegistry;
	}

	/**
	 * 返回此注册商的{@link JmsListenerEndpointRegistry}实例, 可能是{@code null}.
	 */
	public JmsListenerEndpointRegistry getEndpointRegistry() {
		return this.endpointRegistry;
	}

	/**
	 * 设置{@link MessageHandlerMethodFactory}, 用于配置负责为此处理器检测到的端点提供服务的消息监听器.
	 * <p>默认情况下, 使用{@link DefaultMessageHandlerMethodFactory},
	 * 可以进一步配置它以支持其他方法参数, 或自定义转换和验证支持.
	 * 有关更多详细信息, 请参阅{@link DefaultMessageHandlerMethodFactory} javadoc.
	 */
	public void setMessageHandlerMethodFactory(MessageHandlerMethodFactory messageHandlerMethodFactory) {
		this.messageHandlerMethodFactory = messageHandlerMethodFactory;
	}

	/**
	 * 返回要使用的自定义{@link MessageHandlerMethodFactory}.
	 */
	public MessageHandlerMethodFactory getMessageHandlerMethodFactory() {
		return this.messageHandlerMethodFactory;
	}

	/**
	 * 设置{@link JmsListenerContainerFactory}以便在{@link JmsListenerEndpoint}向{@code null}容器工厂注册的情况下使用.
	 * <p>或者, 可以为延迟查找指定要使用的{@link JmsListenerContainerFactory}的bean名称, see {@link #setContainerFactoryBeanName}.
	 */
	public void setContainerFactory(JmsListenerContainerFactory<?> containerFactory) {
		this.containerFactory = containerFactory;
	}

	/**
	 * 设置{@link JmsListenerContainerFactory}的bean名称,
	 * 以便在{@link JmsListenerEndpoint}在{@code null}容器工厂注册的情况下使用.
	 * 或者, 可以直接注册容器工厂实例:
	 * see {@link #setContainerFactory(JmsListenerContainerFactory)}.
	 */
	public void setContainerFactoryBeanName(String containerFactoryBeanName) {
		this.containerFactoryBeanName = containerFactoryBeanName;
	}

	/**
	 * {@link BeanFactory}只需要与{@link #setContainerFactoryBeanName}一起使用.
	 */
	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
		if (beanFactory instanceof ConfigurableBeanFactory) {
			this.mutex = ((ConfigurableBeanFactory) beanFactory).getSingletonMutex();
		}
	}


	@Override
	public void afterPropertiesSet() {
		registerAllEndpoints();
	}

	protected void registerAllEndpoints() {
		synchronized (this.mutex) {
			for (JmsListenerEndpointDescriptor descriptor : this.endpointDescriptors) {
				this.endpointRegistry.registerListenerContainer(
						descriptor.endpoint, resolveContainerFactory(descriptor));
			}
			this.startImmediately = true;  // 触发立即启动
		}
	}

	private JmsListenerContainerFactory<?> resolveContainerFactory(JmsListenerEndpointDescriptor descriptor) {
		if (descriptor.containerFactory != null) {
			return descriptor.containerFactory;
		}
		else if (this.containerFactory != null) {
			return this.containerFactory;
		}
		else if (this.containerFactoryBeanName != null) {
			Assert.state(this.beanFactory != null, "BeanFactory must be set to obtain container factory by bean name");
			// 如果需要更改工厂, 请考虑更改此项...
			this.containerFactory = this.beanFactory.getBean(
					this.containerFactoryBeanName, JmsListenerContainerFactory.class);
			return this.containerFactory;
		}
		else {
			throw new IllegalStateException("Could not resolve the " +
					JmsListenerContainerFactory.class.getSimpleName() + " to use for [" +
					descriptor.endpoint + "] no factory was given and no default is set.");
		}
	}

	/**
	 * 在{@link JmsListenerContainerFactory}旁注册一个新的{@link JmsListenerEndpoint}以用于创建底层容器.
	 * <p>如果必须为该端点使用默认工厂, 则{@code factory}可能是{@code null}.
	 */
	public void registerEndpoint(JmsListenerEndpoint endpoint, JmsListenerContainerFactory<?> factory) {
		Assert.notNull(endpoint, "Endpoint must be set");
		Assert.hasText(endpoint.getId(), "Endpoint id must be set");

		// 工厂可能为null, 在实际创建容器之前推迟解析
		JmsListenerEndpointDescriptor descriptor = new JmsListenerEndpointDescriptor(endpoint, factory);

		synchronized (this.mutex) {
			if (this.startImmediately) {  // 注册并立即开始
				this.endpointRegistry.registerListenerContainer(descriptor.endpoint,
						resolveContainerFactory(descriptor), true);
			}
			else {
				this.endpointDescriptors.add(descriptor);
			}
		}
	}

	/**
	 * 使用默认的{@link JmsListenerContainerFactory}注册新的{@link JmsListenerEndpoint}以创建底层容器.
	 */
	public void registerEndpoint(JmsListenerEndpoint endpoint) {
		registerEndpoint(endpoint, null);
	}


	private static class JmsListenerEndpointDescriptor {

		public final JmsListenerEndpoint endpoint;

		public final JmsListenerContainerFactory<?> containerFactory;

		public JmsListenerEndpointDescriptor(JmsListenerEndpoint endpoint, JmsListenerContainerFactory<?> containerFactory) {
			this.endpoint = endpoint;
			this.containerFactory = containerFactory;
		}
	}

}
