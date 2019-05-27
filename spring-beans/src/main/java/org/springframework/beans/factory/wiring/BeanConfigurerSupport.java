package org.springframework.beans.factory.wiring;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanCurrentlyInCreationException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * bean配置器的便捷基类, 可以对对象执行依赖注入 (但是它们可能会被创建).
 * 通常由AspectJ切面子类化.
 *
 * <p>子类还可能需要在{@link BeanWiringInfoResolver}接口中使用自定义元数据解析策略.
 * 默认实现查找与完全限定类名同名的bean.
 * (如果未使用'{@code id}'属性, 这是Spring XML文件中bean的默认名称.)
 */
public class BeanConfigurerSupport implements BeanFactoryAware, InitializingBean, DisposableBean {

	/** Logger available to subclasses */
	protected final Log logger = LogFactory.getLog(getClass());

	private volatile BeanWiringInfoResolver beanWiringInfoResolver;

	private volatile ConfigurableListableBeanFactory beanFactory;


	/**
	 * 设置要使用的{@link BeanWiringInfoResolver}.
	 * <p>默认行为是查找与该类同名的bean.
	 * 作为替代方案, 请考虑使用注解驱动的bean装配.
	 */
	public void setBeanWiringInfoResolver(BeanWiringInfoResolver beanWiringInfoResolver) {
		Assert.notNull(beanWiringInfoResolver, "BeanWiringInfoResolver must not be null");
		this.beanWiringInfoResolver = beanWiringInfoResolver;
	}

	/**
	 * 设置此切面必须配置bean的{@link BeanFactory}.
	 */
	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		if (!(beanFactory instanceof ConfigurableListableBeanFactory)) {
			throw new IllegalArgumentException(
				 "Bean configurer aspect needs to run in a ConfigurableListableBeanFactory: " + beanFactory);
		}
		this.beanFactory = (ConfigurableListableBeanFactory) beanFactory;
		if (this.beanWiringInfoResolver == null) {
			this.beanWiringInfoResolver = createDefaultBeanWiringInfoResolver();
		}
	}

	/**
	 * 如果没有明确指定, 则创建要使用的默认BeanWiringInfoResolver.
	 * <p>默认实现构建了一个 {@link ClassNameBeanWiringInfoResolver}.
	 * 
	 * @return 默认的BeanWiringInfoResolver (never {@code null})
	 */
	protected BeanWiringInfoResolver createDefaultBeanWiringInfoResolver() {
		return new ClassNameBeanWiringInfoResolver();
	}

	/**
	 * 检查是否已设置{@link BeanFactory}.
	 */
	@Override
	public void afterPropertiesSet() {
		Assert.notNull(this.beanFactory, "BeanFactory must be set");
	}

	/**
	 * 当容器被销毁时, 释放对{@link BeanFactory}和{@link BeanWiringInfoResolver}的引用.
	 */
	@Override
	public void destroy() {
		this.beanFactory = null;
		this.beanWiringInfoResolver = null;
	}


	/**
	 * 配置bean实例.
	 * <p>子类可以覆盖它以提供自定义配置逻辑.
	 * 通常由切面调用, 用于由切点匹配的所有bean实例.
	 * 
	 * @param beanInstance 要配置的bean实例 (must <b>not</b> be {@code null})
	 */
	public void configureBean(Object beanInstance) {
		if (this.beanFactory == null) {
			if (logger.isDebugEnabled()) {
				logger.debug("BeanFactory has not been set on " + ClassUtils.getShortName(getClass()) + ": " +
						"Make sure this configurer runs in a Spring container. Unable to configure bean of type [" +
						ClassUtils.getDescriptiveType(beanInstance) + "]. Proceeding without injection.");
			}
			return;
		}

		BeanWiringInfo bwi = this.beanWiringInfoResolver.resolveWiringInfo(beanInstance);
		if (bwi == null) {
			// 如果没有给出装配信息, 请跳过bean.
			return;
		}

		try {
			if (bwi.indicatesAutowiring() ||
					(bwi.isDefaultBeanName() && !this.beanFactory.containsBean(bwi.getBeanName()))) {
				// 执行自动装配 (还应用标准的工厂/后处理器回调).
				this.beanFactory.autowireBeanProperties(beanInstance, bwi.getAutowireMode(), bwi.getDependencyCheck());
				Object result = this.beanFactory.initializeBean(beanInstance, bwi.getBeanName());
				checkExposedObject(result, beanInstance);
			}
			else {
				// 根据指定的bean定义执行显式装配.
				Object result = this.beanFactory.configureBean(beanInstance, bwi.getBeanName());
				checkExposedObject(result, beanInstance);
			}
		}
		catch (BeanCreationException ex) {
			Throwable rootCause = ex.getMostSpecificCause();
			if (rootCause instanceof BeanCurrentlyInCreationException) {
				BeanCreationException bce = (BeanCreationException) rootCause;
				if (this.beanFactory.isCurrentlyInCreation(bce.getBeanName())) {
					if (logger.isDebugEnabled()) {
						logger.debug("Failed to create target bean '" + bce.getBeanName() +
								"' while configuring object of type [" + beanInstance.getClass().getName() +
								"] - probably due to a circular reference. This is a common startup situation " +
								"and usually not fatal. Proceeding without injection. Original exception: " + ex);
					}
					return;
				}
			}
			throw ex;
		}
	}

	private void checkExposedObject(Object exposedObject, Object originalBeanInstance) {
		if (exposedObject != originalBeanInstance) {
			throw new IllegalStateException("Post-processor tried to replace bean instance of type [" +
					originalBeanInstance.getClass().getName() + "] with (proxy) object of type [" +
					exposedObject.getClass().getName() + "] - not supported for aspect-configured classes!");
		}
	}

}
