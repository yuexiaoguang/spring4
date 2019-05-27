package org.springframework.aop.framework.autoproxy;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.aop.Advisor;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanCurrentlyInCreationException;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.util.Assert;

/**
 * 用于从BeanFactory检索标准Spring Advisor, 用于自动代理.
 */
public class BeanFactoryAdvisorRetrievalHelper {

	private static final Log logger = LogFactory.getLog(BeanFactoryAdvisorRetrievalHelper.class);

	private final ConfigurableListableBeanFactory beanFactory;

	private volatile String[] cachedAdvisorBeanNames;


	/**
	 * @param beanFactory 要扫描的ListableBeanFactory
	 */
	public BeanFactoryAdvisorRetrievalHelper(ConfigurableListableBeanFactory beanFactory) {
		Assert.notNull(beanFactory, "ListableBeanFactory must not be null");
		this.beanFactory = beanFactory;
	}


	/**
	 * 在当前bean工厂中查找所有符合条件的Advisor bean, 忽略FactoryBeans并排除当前正在创建的bean.
	 * 
	 * @return {@link org.springframework.aop.Advisor} bean的列表
	 */
	public List<Advisor> findAdvisorBeans() {
		// 确定切面bean名称列表, 如果没有缓存.
		String[] advisorNames = this.cachedAdvisorBeanNames;
		if (advisorNames == null) {
			// 不要在这里初始化FactoryBeans: 需要保留所有未初始化的常规bean, 让自动代理创建者应用于它们!
			advisorNames = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(
					this.beanFactory, Advisor.class, true, false);
			this.cachedAdvisorBeanNames = advisorNames;
		}
		if (advisorNames.length == 0) {
			return new ArrayList<Advisor>();
		}

		List<Advisor> advisors = new ArrayList<Advisor>();
		for (String name : advisorNames) {
			if (isEligibleBean(name)) {
				if (this.beanFactory.isCurrentlyInCreation(name)) {
					if (logger.isDebugEnabled()) {
						logger.debug("Skipping currently created advisor '" + name + "'");
					}
				}
				else {
					try {
						advisors.add(this.beanFactory.getBean(name, Advisor.class));
					}
					catch (BeanCreationException ex) {
						Throwable rootCause = ex.getMostSpecificCause();
						if (rootCause instanceof BeanCurrentlyInCreationException) {
							BeanCreationException bce = (BeanCreationException) rootCause;
							if (this.beanFactory.isCurrentlyInCreation(bce.getBeanName())) {
								if (logger.isDebugEnabled()) {
									logger.debug("Skipping advisor '" + name +
											"' with dependency on currently created bean: " + ex.getMessage());
								}
								// Ignore: 表示对我们试图增强的bean的引用.
								// 想要找到除当前创建的bean本身之外的切面.
								continue;
							}
						}
						throw ex;
					}
				}
			}
		}
		return advisors;
	}

	/**
	 * 确定具有给定名称的切面bean是否符合条件.
	 * <p>默认实现总是返回 {@code true}.
	 * 
	 * @param beanName 切面bean的名称
	 * 
	 * @return bean是否符合条件
	 */
	protected boolean isEligibleBean(String beanName) {
		return true;
	}

}
