package org.springframework.aop.aspectj;

import org.springframework.aop.PointcutAdvisor;

/**
 * 由Spring AOP Advisor实现的接口, 封装可能具有延迟初始化策略的AspectJ切面.
 * 例如,  perThis实例化模型意味着增强的延迟初始化.
 */
public interface InstantiationModelAwarePointcutAdvisor extends PointcutAdvisor {

	/**
	 * 返回此切面是否延迟初始化其底层增强.
	 */
	boolean isLazy();

	/**
	 * 返回此切面是否已经实例化了其增强.
	 */
	boolean isAdviceInstantiated();

}
