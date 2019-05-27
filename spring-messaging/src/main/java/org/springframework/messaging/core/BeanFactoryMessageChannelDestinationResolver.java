package org.springframework.messaging.core;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.messaging.MessageChannel;
import org.springframework.util.Assert;

/**
 * {@link DestinationResolver}的实现, 它将目标名称解释为{@link MessageChannel}的bean名称, 并在配置的{@link BeanFactory}中查找bean.
 */
public class BeanFactoryMessageChannelDestinationResolver
		implements DestinationResolver<MessageChannel>, BeanFactoryAware {

	private BeanFactory beanFactory;


	/**
	 * 一个默认构造函数, 可以在解析器本身配置为Spring bean时使用,
	 * 并且由于已经实现{@link BeanFactoryAware}而获取注入{@code BeanFactory}.
	 */
	public BeanFactoryMessageChannelDestinationResolver() {
	}

	/**
	 * 如果手动实例化此解析器, 而不是将其定义为Spring管理的bean, 则接受{@link BeanFactory}的构造函数很有用.
	 * 
	 * @param beanFactory 执行查找的bean工厂
	 */
	public BeanFactoryMessageChannelDestinationResolver(BeanFactory beanFactory) {
		Assert.notNull(beanFactory, "beanFactory must not be null");
		this.beanFactory = beanFactory;
	}


	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}


	@Override
	public MessageChannel resolveDestination(String name) {
		Assert.state(this.beanFactory != null, "No BeanFactory configured");
		try {
			return this.beanFactory.getBean(name, MessageChannel.class);
		}
		catch (BeansException ex) {
			throw new DestinationResolutionException(
					"Failed to find MessageChannel bean with name '" + name + "'", ex);
		}
	}

}
