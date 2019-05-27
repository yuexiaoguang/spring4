package org.springframework.aop.framework.autoproxy;

import java.util.List;

import org.springframework.aop.Advisor;
import org.springframework.aop.TargetSource;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;

/**
 * 通用自动代理创建程序，它根据检测到的每个bean的Advisor为特定bean构建AOP代理.
 *
 * <p>子类必须实现抽象 {@link #findCandidateAdvisors()}方法，以返回应用于任何对象的Advisor列表.
 * 子类还可以覆盖继承的{@link #shouldSkip}方法，以从自动代理中排除某些对象.
 *
 * <p>需要排序的Advisor或增强应该实现{@link org.springframework.core.Ordered}接口.
 * 这个类通过Ordered的排序值排序 Advisor. 未实现Ordered接口的Advisor将被认为不排序; 它们将以未定义的顺序出现在切面链的末尾.
 */
@SuppressWarnings("serial")
public abstract class AbstractAdvisorAutoProxyCreator extends AbstractAutoProxyCreator {

	private BeanFactoryAdvisorRetrievalHelper advisorRetrievalHelper;


	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		super.setBeanFactory(beanFactory);
		if (!(beanFactory instanceof ConfigurableListableBeanFactory)) {
			throw new IllegalArgumentException(
					"AdvisorAutoProxyCreator requires a ConfigurableListableBeanFactory: " + beanFactory);
		}
		initBeanFactory((ConfigurableListableBeanFactory) beanFactory);
	}

	protected void initBeanFactory(ConfigurableListableBeanFactory beanFactory) {
		this.advisorRetrievalHelper = new BeanFactoryAdvisorRetrievalHelperAdapter(beanFactory);
	}


	@Override
	protected Object[] getAdvicesAndAdvisorsForBean(Class<?> beanClass, String beanName, TargetSource targetSource) {
		List<Advisor> advisors = findEligibleAdvisors(beanClass, beanName);
		if (advisors.isEmpty()) {
			return DO_NOT_PROXY;
		}
		return advisors.toArray();
	}

	/**
	 * 找到所有符合条件的自动代理此类的切面.
	 * 
	 * @param beanClass 要查找其切面的类
	 * @param beanName 当前代理的bean的名称
	 * 
	 * @return 空列表, 不是 {@code null}, 如果没有切点或拦截器
	 */
	protected List<Advisor> findEligibleAdvisors(Class<?> beanClass, String beanName) {
		List<Advisor> candidateAdvisors = findCandidateAdvisors();
		List<Advisor> eligibleAdvisors = findAdvisorsThatCanApply(candidateAdvisors, beanClass, beanName);
		extendAdvisors(eligibleAdvisors);
		if (!eligibleAdvisors.isEmpty()) {
			eligibleAdvisors = sortAdvisors(eligibleAdvisors);
		}
		return eligibleAdvisors;
	}

	/**
	 * 找到要在自动代理中使用的所有候选 Advisor.
	 * 
	 * @return 候选 Advisor的列表
	 */
	protected List<Advisor> findCandidateAdvisors() {
		return this.advisorRetrievalHelper.findAdvisorBeans();
	}

	/**
	 * 搜索给定的候选Advisor，找到可以应用于指定bean的所有Advisor.
	 * 
	 * @param candidateAdvisors 候选Advisor
	 * @param beanClass 目标的bean类
	 * @param beanName 目标的bean名称
	 * 
	 * @return 应用的Advisor列表
	 */
	protected List<Advisor> findAdvisorsThatCanApply(
			List<Advisor> candidateAdvisors, Class<?> beanClass, String beanName) {

		ProxyCreationContext.setCurrentProxiedBeanName(beanName);
		try {
			return AopUtils.findAdvisorsThatCanApply(candidateAdvisors, beanClass);
		}
		finally {
			ProxyCreationContext.setCurrentProxiedBeanName(null);
		}
	}

	/**
	 * 返回具有给定名称的Advisor bean是否有资格首先进行代理.
	 * 
	 * @param beanName Advisor bean的名称
	 * 
	 * @return bean是否符合条件
	 */
	protected boolean isEligibleAdvisorBean(String beanName) {
		return true;
	}

	/**
	 * 根据顺序对切面进行排序. 子类可以选择覆盖此方法以自定义排序策略.
	 * 
	 * @param advisors 源Advisor列表
	 * 
	 * @return 有序的Advisor列表
	 */
	protected List<Advisor> sortAdvisors(List<Advisor> advisors) {
		AnnotationAwareOrderComparator.sort(advisors);
		return advisors;
	}

	/**
	 * 子类可以覆盖的扩展钩子以注册其他Advisor, 给出迄今为止获得的有序的Advisor.
	 * <p>默认实现为空.
	 * <p>通常用于添加Advisor，公开某些后来的切面所需的上下文信息.
	 * 
	 * @param candidateAdvisors 已被确定为适用于给定bean的Advisor
	 */
	protected void extendAdvisors(List<Advisor> candidateAdvisors) {
	}

	/**
	 * 此自动代理创建者始终返回预过滤的 Advisor.
	 */
	@Override
	protected boolean advisorsPreFiltered() {
		return true;
	}


	/**
	 * 委托给周围的AbstractAdvisorAutoProxyCreator工具的BeanFactoryAdvisorRetrievalHelper的子类.
	 */
	private class BeanFactoryAdvisorRetrievalHelperAdapter extends BeanFactoryAdvisorRetrievalHelper {

		public BeanFactoryAdvisorRetrievalHelperAdapter(ConfigurableListableBeanFactory beanFactory) {
			super(beanFactory);
		}

		@Override
		protected boolean isEligibleBean(String beanName) {
			return AbstractAdvisorAutoProxyCreator.this.isEligibleAdvisorBean(beanName);
		}
	}
}
