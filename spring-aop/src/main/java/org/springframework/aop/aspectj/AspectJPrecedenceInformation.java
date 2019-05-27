package org.springframework.aop.aspectj;

import org.springframework.core.Ordered;

/**
 * 由类型实现的接口，这些类型可以提供按AspectJ的优先规则对增强/切面进行排序所需的信息.
 */
public interface AspectJPrecedenceInformation extends Ordered {

	// Implementation note:
	// 需要此接口提供的间接级别, 否则AspectJPrecedenceComparator必须在所有情况下向切面询问其增强, 以便对切面进行排序.
	// 这会导致InstantiationModelAwarePointcutAdvisor出现问题，需要延迟为非单例实例化模型创建切面的增强.

	/**
	 * 声明增强的切面（bean）的名称.
	 */
	String getAspectName();

	/**
	 * 切面内的增强成员的声明顺序.
	 */
	int getDeclarationOrder();

	/**
	 * 是否是前置增强.
	 */
	boolean isBeforeAdvice();

	/**
	 * 是否是后置增强.
	 */
	boolean isAfterAdvice();

}
