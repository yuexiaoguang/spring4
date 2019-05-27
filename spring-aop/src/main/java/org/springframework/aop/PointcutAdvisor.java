package org.springframework.aop;

/**
 * 由切点驱动的所有Advisor的父级接口.
 * 除了介绍Advisor之外, 这几乎涵盖了所有Advisor, 方法级别匹配不适用.
 */
public interface PointcutAdvisor extends Advisor {

	/**
	 * 获取驱动此Advisor的Pointcut.
	 */
	Pointcut getPointcut();

}
