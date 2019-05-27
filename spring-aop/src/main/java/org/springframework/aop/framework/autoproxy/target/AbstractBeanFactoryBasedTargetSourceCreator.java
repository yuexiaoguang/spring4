package org.springframework.aop.framework.autoproxy.target;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.aop.TargetSource;
import org.springframework.aop.framework.AopInfrastructureBean;
import org.springframework.aop.framework.autoproxy.TargetSourceCreator;
import org.springframework.aop.target.AbstractBeanFactoryBasedTargetSource;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.GenericBeanDefinition;

/**
 * {@link org.springframework.aop.framework.autoproxy.TargetSourceCreator}实现的父类,
 * 需要创建原型bean的多个实例.
 *
 * <p>使用内部BeanFactory来管理目标实例, 将原始bean定义复制到此内部工厂.
 * 这是必要的，因为原始的BeanFactory将只包含通过自动代理创建的代理实例.
 *
 * <p>需要在一个{@link org.springframework.beans.factory.support.AbstractBeanFactory}中运行.
 */
public abstract class AbstractBeanFactoryBasedTargetSourceCreator
		implements TargetSourceCreator, BeanFactoryAware, DisposableBean {

	protected final Log logger = LogFactory.getLog(getClass());

	private ConfigurableBeanFactory beanFactory;

	/** 内部使用的DefaultListableBeanFactory实例, 使用bean 名称作为Key */
	private final Map<String, DefaultListableBeanFactory> internalBeanFactories =
			new HashMap<String, DefaultListableBeanFactory>();


	@Override
	public final void setBeanFactory(BeanFactory beanFactory) {
		if (!(beanFactory instanceof ConfigurableBeanFactory)) {
			throw new IllegalStateException("Cannot do auto-TargetSource creation with a BeanFactory " +
					"that doesn't implement ConfigurableBeanFactory: " + beanFactory.getClass());
		}
		this.beanFactory = (ConfigurableBeanFactory) beanFactory;
	}

	/**
	 * 返回此TargetSourceCreators运行的BeanFactory.
	 */
	protected final BeanFactory getBeanFactory() {
		return this.beanFactory;
	}


	//---------------------------------------------------------------------
	// Implementation of the TargetSourceCreator interface
	//---------------------------------------------------------------------

	@Override
	public final TargetSource getTargetSource(Class<?> beanClass, String beanName) {
		AbstractBeanFactoryBasedTargetSource targetSource =
				createBeanFactoryBasedTargetSource(beanClass, beanName);
		if (targetSource == null) {
			return null;
		}

		if (logger.isDebugEnabled()) {
			logger.debug("Configuring AbstractBeanFactoryBasedTargetSource: " + targetSource);
		}

		DefaultListableBeanFactory internalBeanFactory = getInternalBeanFactoryForBean(beanName);

		// 我们需要覆盖这个bean定义, 因为它可能引用其他bean, 我们很乐意采用父级的定义.
		// 如果需要，始终使用原型范围.
		BeanDefinition bd = this.beanFactory.getMergedBeanDefinition(beanName);
		GenericBeanDefinition bdCopy = new GenericBeanDefinition(bd);
		if (isPrototypeBased()) {
			bdCopy.setScope(BeanDefinition.SCOPE_PROTOTYPE);
		}
		internalBeanFactory.registerBeanDefinition(beanName, bdCopy);

		// 完成PrototypeTargetSource的配置.
		targetSource.setTargetBeanName(beanName);
		targetSource.setBeanFactory(internalBeanFactory);

		return targetSource;
	}

	/**
	 * 返回用于指定bean的内部BeanFactory.
	 * 
	 * @param beanName 目标bean的名称
	 * 
	 * @return 要使用的内部BeanFactory
	 */
	protected DefaultListableBeanFactory getInternalBeanFactoryForBean(String beanName) {
		synchronized (this.internalBeanFactories) {
			DefaultListableBeanFactory internalBeanFactory = this.internalBeanFactories.get(beanName);
			if (internalBeanFactory == null) {
				internalBeanFactory = buildInternalBeanFactory(this.beanFactory);
				this.internalBeanFactories.put(beanName, internalBeanFactory);
			}
			return internalBeanFactory;
		}
	}

	/**
	 * 构建一个内部BeanFactory来解析目标bean.
	 * 
	 * @param containingFactory 包含最初定义bean的BeanFactory
	 * 
	 * @return 一个独立的内部BeanFactory来保存一些目标bean的副本
	 */
	protected DefaultListableBeanFactory buildInternalBeanFactory(ConfigurableBeanFactory containingFactory) {
		// 设置父级以便正确解析引用（向上容器层次结构）.
		DefaultListableBeanFactory internalBeanFactory = new DefaultListableBeanFactory(containingFactory);

		// 必需，以便所有BeanPostProcessor，Scope等可用.
		internalBeanFactory.copyConfigurationFrom(containingFactory);

		// 过滤掉属于AOP基础架构的BeanPostProcessor, 因为这些仅适用于原始工厂中定义的bean.
		for (Iterator<BeanPostProcessor> it = internalBeanFactory.getBeanPostProcessors().iterator(); it.hasNext();) {
			if (it.next() instanceof AopInfrastructureBean) {
				it.remove();
			}
		}

		return internalBeanFactory;
	}

	/**
	 * 在关闭TargetSourceCreator时销毁内部BeanFactory.
	 */
	@Override
	public void destroy() {
		synchronized (this.internalBeanFactories) {
			for (DefaultListableBeanFactory bf : this.internalBeanFactories.values()) {
				bf.destroySingletons();
			}
		}
	}


	//---------------------------------------------------------------------
	// Template methods to be implemented by subclasses
	//---------------------------------------------------------------------

	/**
	 * 返回此TargetSourceCreator是否基于原型.
	 * 将相应地设置目标bean定义的范围.
	 * <p>默认是 "true".
	 */
	protected boolean isPrototypeBased() {
		return true;
	}

	/**
	 * 如果子类希望为此bean创建自定义TargetSource, 则必须实现此方法以返回新的AbstractPrototypeBasedTargetSource,
	 * 或{@code null}如果他们不感兴趣的话, 在这种情况下, 不会创建特殊目标源.
	 * 子类不应该在AbstractPrototypeBasedTargetSource上调用{@code setTargetBeanName}或{@code setBeanFactory}:
	 * 这个类的{@code getTargetSource()}实现将会这样做.
	 * 
	 * @param beanClass 用于创建TargetSource的bean的类
	 * @param beanName bean的名称
	 * 
	 * @return AbstractPrototypeBasedTargetSource, 或{@code null}如果不匹配
	 */
	protected abstract AbstractBeanFactoryBasedTargetSource createBeanFactoryBasedTargetSource(
			Class<?> beanClass, String beanName);

}
