package org.springframework.aop.support;

import org.springframework.aop.Pointcut;

/**
 * 基于Concrete BeanFactory的PointcutAdvisor, 允许将任何增强配置为对BeanFactory中的Advice bean以及通过bean属性配置的切点的引用.
 *
 * <p>指定增强bean的名称而不是增强对象本身（如果在BeanFactory中运行）会在初始化时增加松散耦合,
 * 为了在切点实际匹配之前不初始化增强对象.
 */
@SuppressWarnings("serial")
public class DefaultBeanFactoryPointcutAdvisor extends AbstractBeanFactoryPointcutAdvisor {

	private Pointcut pointcut = Pointcut.TRUE;


	/**
	 * 指定针对增强的切点.
	 * <p>默认是 {@code Pointcut.TRUE}.
	 */
	public void setPointcut(Pointcut pointcut) {
		this.pointcut = (pointcut != null ? pointcut : Pointcut.TRUE);
	}

	@Override
	public Pointcut getPointcut() {
		return this.pointcut;
	}


	@Override
	public String toString() {
		return getClass().getName() + ": pointcut [" + getPointcut() + "]; advice bean '" + getAdviceBeanName() + "'";
	}

}
