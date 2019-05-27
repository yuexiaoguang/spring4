package org.springframework.beans.factory.config;

import java.io.Serializable;
import javax.inject.Provider;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.util.Assert;

/**
 * {@link org.springframework.beans.factory.FactoryBean}实现,
 * 返回一个JSR-330 {@link javax.inject.Provider}的值, 该值又返回一个来自{@link org.springframework.beans.factory.BeanFactory}的bean.
 *
 * <p>这基本上是一个符合JSR-330的Spring变体的旧版本{@link ObjectFactoryCreatingFactoryBean}.
 * 它可用于传统外部依赖项注入配置，该配置以{@code javax.inject.Provider}类型的属性或构造函数参数为目标,
 * 作为JSR-330的{@code @Inject}注解驱动方法的替代方案.
 */
public class ProviderCreatingFactoryBean extends AbstractFactoryBean<Provider<Object>> {

	private String targetBeanName;


	/**
	 * 设置目标bean的名称.
	 * <p>目标不必是非单例bean, 但实际上总是如此
	 * (因为如果目标bean是单例, 那么所说的单例bean可以简单地直接注入到依赖对象中, 从而避免了对这种工厂方法提供额外的间接级别的需求).
	 */
	public void setTargetBeanName(String targetBeanName) {
		this.targetBeanName = targetBeanName;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		Assert.hasText(this.targetBeanName, "Property 'targetBeanName' is required");
		super.afterPropertiesSet();
	}


	@Override
	public Class<?> getObjectType() {
		return Provider.class;
	}

	@Override
	protected Provider<Object> createInstance() {
		return new TargetBeanProvider(getBeanFactory(), this.targetBeanName);
	}


	/**
	 * 独立的内部类 - 用于序列化.
	 */
	@SuppressWarnings("serial")
	private static class TargetBeanProvider implements Provider<Object>, Serializable {

		private final BeanFactory beanFactory;

		private final String targetBeanName;

		public TargetBeanProvider(BeanFactory beanFactory, String targetBeanName) {
			this.beanFactory = beanFactory;
			this.targetBeanName = targetBeanName;
		}

		@Override
		public Object get() throws BeansException {
			return this.beanFactory.getBean(this.targetBeanName);
		}
	}

}
