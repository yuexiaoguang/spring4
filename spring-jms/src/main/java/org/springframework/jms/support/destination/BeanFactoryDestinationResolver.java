package org.springframework.jms.support.destination;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Session;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.util.Assert;

/**
 * 基于Spring {@link BeanFactory}的{@link DestinationResolver}实现.
 *
 * <p>将查找由bean名称标识的Spring托管的bean, 期望它们的类型为{@code javax.jms.Destination}.
 */
public class BeanFactoryDestinationResolver implements DestinationResolver, BeanFactoryAware {

	private BeanFactory beanFactory;


	/**
	 * <p>要访问的BeanFactory必须通过{@code setBeanFactory}设置.
	 */
	public BeanFactoryDestinationResolver() {
	}

	/**
	 * <p>如果此对象是由Spring IoC容器创建的, 则使用此构造函数是多余的,
	 * 因为提供的{@link BeanFactory}将由创建它的{@link BeanFactory}替换 (c.f. the {@link BeanFactoryAware}约定).
	 * 因此, 只有在Spring IoC容器的上下文之外使用此类时, 才使用此构造函数.
	 * 
	 * @param beanFactory 用于查找 {@link javax.jms.Destination Destinatiosn}的bean工厂
	 */
	public BeanFactoryDestinationResolver(BeanFactory beanFactory) {
		Assert.notNull(beanFactory, "BeanFactory is required");
		this.beanFactory = beanFactory;
	}


	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}


	@Override
	public Destination resolveDestinationName(Session session, String destinationName, boolean pubSubDomain)
			throws JMSException {

		Assert.state(this.beanFactory != null, "BeanFactory is required");
		try {
			return this.beanFactory.getBean(destinationName, Destination.class);
		}
		catch (BeansException ex) {
			throw new DestinationResolutionException(
					"Failed to look up Destination bean with name '" + destinationName + "'", ex);
		}
	}

}
