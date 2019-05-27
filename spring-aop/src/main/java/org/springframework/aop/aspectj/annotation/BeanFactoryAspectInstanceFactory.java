package org.springframework.aop.aspectj.annotation;

import java.io.Serializable;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.OrderUtils;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * 通过{@link org.springframework.beans.factory.BeanFactory}返回的{@link org.springframework.aop.aspectj.AspectInstanceFactory}实现.
 *
 * <p>请注意，如果使用原型，这可能会多次实例化, 这可能不会给出你期望的语义.
 * 使用{@link LazySingletonAspectInstanceFactoryDecorator}封装这个, 确保只会返回一个新的切面.
 */
@SuppressWarnings("serial")
public class BeanFactoryAspectInstanceFactory implements MetadataAwareAspectInstanceFactory, Serializable {

	private final BeanFactory beanFactory;

	private final String name;

	private final AspectMetadata aspectMetadata;


	/**
	 * 将调用AspectJ以使用从BeanFactory为给定bean名称返回的类型, 来反射创建AJType元数据.
	 * 
	 * @param beanFactory 要从中获取实例的BeanFactory
	 * @param name bean的名称
	 */
	public BeanFactoryAspectInstanceFactory(BeanFactory beanFactory, String name) {
		this(beanFactory, name, beanFactory.getType(name));
	}

	/**
	 * 提供AspectJ应该反射创建AJType元数据的类型.
	 * 如果BeanFactory可能认为类型是子类 (使用CGLIB时), 请使用, 并且信息应该与超类相关.
	 * 
	 * @param beanFactory 要从中获取实例的BeanFactory
	 * @param name bean的名称
	 * @param type AspectJ应该反射的类型
	 */
	public BeanFactoryAspectInstanceFactory(BeanFactory beanFactory, String name, Class<?> type) {
		Assert.notNull(beanFactory, "BeanFactory must not be null");
		Assert.notNull(name, "Bean name must not be null");
		this.beanFactory = beanFactory;
		this.name = name;
		this.aspectMetadata = new AspectMetadata(type, name);
	}


	@Override
	public Object getAspectInstance() {
		return this.beanFactory.getBean(this.name);
	}

	@Override
	public ClassLoader getAspectClassLoader() {
		return (this.beanFactory instanceof ConfigurableBeanFactory ?
				((ConfigurableBeanFactory) this.beanFactory).getBeanClassLoader() :
				ClassUtils.getDefaultClassLoader());
	}

	@Override
	public AspectMetadata getAspectMetadata() {
		return this.aspectMetadata;
	}

	@Override
	public Object getAspectCreationMutex() {
		if (this.beanFactory != null) {
			if (this.beanFactory.isSingleton(name)) {
				// 依靠工厂提供的单例语义 -> 不是本地锁.
				return null;
			}
			else if (this.beanFactory instanceof ConfigurableBeanFactory) {
				// 工厂不保证单例 -> 在本地锁定，但重用工厂的单例锁, 以防增强bean的延迟依赖关系发生, 隐式的触发单例锁定...
				return ((ConfigurableBeanFactory) this.beanFactory).getSingletonMutex();
			}
		}
		return this;
	}

	/**
	 * 确定此工厂目标切面的顺序,
	 * 要么通过实现{@link org.springframework.core.Ordered}接口表示特定于实例的顺序 (仅检查单例bean),
	 * 或者通过类级别的{@link org.springframework.core.annotation.Order}注解表示顺序.
	 */
	@Override
	public int getOrder() {
		Class<?> type = this.beanFactory.getType(this.name);
		if (type != null) {
			if (Ordered.class.isAssignableFrom(type) && this.beanFactory.isSingleton(this.name)) {
				return ((Ordered) this.beanFactory.getBean(this.name)).getOrder();
			}
			return OrderUtils.getOrder(type, Ordered.LOWEST_PRECEDENCE);
		}
		return Ordered.LOWEST_PRECEDENCE;
	}


	@Override
	public String toString() {
		return getClass().getSimpleName() + ": bean name '" + this.name + "'";
	}

}
