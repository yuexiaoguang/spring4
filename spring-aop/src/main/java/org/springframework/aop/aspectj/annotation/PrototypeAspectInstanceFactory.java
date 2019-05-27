package org.springframework.aop.aspectj.annotation;

import java.io.Serializable;

import org.springframework.beans.factory.BeanFactory;

/**
 * 由{@link BeanFactory}提供的原型支持的{@link org.springframework.aop.aspectj.AspectInstanceFactory}, 强制执行原型语义.
 *
 * <p>请注意, 这可能会多次实例化, 这可能不会给你期望的语义.
 * 使用{@link LazySingletonAspectInstanceFactoryDecorator}封装这个, 确保只返回一个切面.
 */
@SuppressWarnings("serial")
public class PrototypeAspectInstanceFactory extends BeanFactoryAspectInstanceFactory implements Serializable {

	/**
	 * 将调用AspectJ, 以使用从BeanFactory为给定bean名称返回的类型, 来反射创建AJType元数据.
	 * 
	 * @param beanFactory 从中获取实例的BeanFactory
	 * @param name bean的名字
	 */
	public PrototypeAspectInstanceFactory(BeanFactory beanFactory, String name) {
		super(beanFactory, name);
		if (!beanFactory.isPrototype(name)) {
			throw new IllegalArgumentException(
					"Cannot use PrototypeAspectInstanceFactory with bean named '" + name + "': not a prototype");
		}
	}

}
