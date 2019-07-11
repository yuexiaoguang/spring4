package org.springframework.dao.support;

import java.util.Map;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

/**
 * AOP Alliance MethodInterceptor, 基于给定的PersistenceExceptionTranslator提供持久化异常转换.
 *
 * <p>委托给给定的{@link PersistenceExceptionTranslator}来转换抛出的RuntimeException
 * 为Spring的 DataAccessException层次结构.
 * 如果在目标方法上声明了有问题的RuntimeException, 则它始终按原样传播 (不应用任何转换).
 */
public class PersistenceExceptionTranslationInterceptor
		implements MethodInterceptor, BeanFactoryAware, InitializingBean {

	private volatile PersistenceExceptionTranslator persistenceExceptionTranslator;

	private boolean alwaysTranslate = false;

	private ListableBeanFactory beanFactory;


	/**
	 * 之后需要使用PersistenceExceptionTranslator进行配置.
	 */
	public PersistenceExceptionTranslationInterceptor() {
	}

	/**
	 * @param pet 要使用的PersistenceExceptionTranslator
	 */
	public PersistenceExceptionTranslationInterceptor(PersistenceExceptionTranslator pet) {
		Assert.notNull(pet, "PersistenceExceptionTranslator must not be null");
		this.persistenceExceptionTranslator = pet;
	}

	/**
	 * 在给定的BeanFactory中自动检测PersistenceExceptionTranslators.
	 * 
	 * @param beanFactory 从中获取所有PersistenceExceptionTranslators的ListableBeanFactory
	 */
	public PersistenceExceptionTranslationInterceptor(ListableBeanFactory beanFactory) {
		Assert.notNull(beanFactory, "ListableBeanFactory must not be null");
		this.beanFactory = beanFactory;
	}


	/**
	 * 指定要使用的PersistenceExceptionTranslator.
	 * <p>默认是自动检测BeanFactory中的所有PersistenceExceptionTranslators, 在链中使用它们.
	 */
	public void setPersistenceExceptionTranslator(PersistenceExceptionTranslator pet) {
		this.persistenceExceptionTranslator = pet;
	}

	/**
	 * 指定是否始终转换异常 ("true"), 或者是否在声明时抛出原始异常, i.e. 当原始方法签名的异常声明允许抛出原始异常时 ("false").
	 * <p>默认 "false". 将此标志切换为"true", 以便始终转换适用的异常, 而不依赖于原始方法签名.
	 * <p>请注意, 原始方法不必声明特定的异常.
	 * 任何基类都可以, 甚至{@code throws Exception}: 只要原始方法显式声明兼容的异常, 原始异常将被重新抛出.
	 * 如果想避免在任何情况下抛出原始异常, 请将此标志切换为"true".
	 */
	public void setAlwaysTranslate(boolean alwaysTranslate) {
		this.alwaysTranslate = alwaysTranslate;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		if (this.persistenceExceptionTranslator == null) {
			// 未指定显式异常转换器 - 执行自动检测.
			if (!(beanFactory instanceof ListableBeanFactory)) {
				throw new IllegalArgumentException(
						"Cannot use PersistenceExceptionTranslator autodetection without ListableBeanFactory");
			}
			this.beanFactory = (ListableBeanFactory) beanFactory;
		}
	}

	@Override
	public void afterPropertiesSet() {
		if (this.persistenceExceptionTranslator == null && this.beanFactory == null) {
			throw new IllegalArgumentException("Property 'persistenceExceptionTranslator' is required");
		}
	}


	@Override
	public Object invoke(MethodInvocation mi) throws Throwable {
		try {
			return mi.proceed();
		}
		catch (RuntimeException ex) {
			// 如果异常的类型在方法的throws子句上, 则抛出原始异常.
			if (!this.alwaysTranslate && ReflectionUtils.declaresException(mi.getMethod(), ex.getClass())) {
				throw ex;
			}
			else {
				if (this.persistenceExceptionTranslator == null) {
					this.persistenceExceptionTranslator = detectPersistenceExceptionTranslators(this.beanFactory);
				}
				throw DataAccessUtils.translateIfNecessary(ex, this.persistenceExceptionTranslator);
			}
		}
	}

	/**
	 * 检测给定BeanFactory中的所有PersistenceExceptionTranslators.
	 * 
	 * @param beanFactory 从中获取所有PersistenceExceptionTranslators的ListableBeanFactory
	 * 
	 * @return 一个链式PersistenceExceptionTranslator, 它结合了工厂中的所有PersistenceExceptionTranslators
	 */
	protected PersistenceExceptionTranslator detectPersistenceExceptionTranslators(ListableBeanFactory beanFactory) {
		// 查找所有转换器, 注意不要激活FactoryBeans.
		Map<String, PersistenceExceptionTranslator> pets = BeanFactoryUtils.beansOfTypeIncludingAncestors(
				beanFactory, PersistenceExceptionTranslator.class, false, false);
		ChainedPersistenceExceptionTranslator cpet = new ChainedPersistenceExceptionTranslator();
		for (PersistenceExceptionTranslator pet : pets.values()) {
			cpet.addDelegate(pet);
		}
		return cpet;
	}

}
