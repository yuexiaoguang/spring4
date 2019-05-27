package org.springframework.aop.framework.autoproxy;

import org.springframework.aop.TargetSource;

/**
 * 实现可以创建特殊的目标源, 例如使用功能池的目标源, 对于特定的 beans.
 * 例如, 可能会根据目标类的属性做出选择, 例如池的属性.
 *
 * <p>AbstractAutoProxyCreator可以按顺序支持多个 TargetSourceCreator.
 */
public interface TargetSourceCreator {

	/**
	 * 为给定的bean创建一个特殊的TargetSource.
	 * 
	 * @param beanClass 用于创建TargetSource的bean的类
	 * @param beanName bean的名称
	 * 
	 * @return 特殊的TargetSource或{@code null}, 如果此TargetSourceCreator对特定bean不感兴趣
	 */
	TargetSource getTargetSource(Class<?> beanClass, String beanName);

}
