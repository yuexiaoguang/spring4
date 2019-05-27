package org.springframework.aop.framework.adapter;

import org.aopalliance.aop.Advice;
import org.aopalliance.intercept.MethodInterceptor;

import org.springframework.aop.Advisor;

/**
 * 允许扩展Spring AOP框架，以处理新的Advisors和Advice类型的接口.
 *
 * <p>实现对象可以从自定义增强类型创建AOP联盟拦截器, 使这些增强类型能够在Spring AOP框架中使用, 使用拦截.
 *
 * <p>大多数Spring用户不需要实现此接口; 仅在您需要向Spring引入更多Advisor或Advice类型时才这样做.
 */
public interface AdvisorAdapter {

	/**
	 * 此适配器是否理解此增强对象?
	 * 使用包含此增强作为参数的Advisor调用{@code getInterceptors}方法是否有效?
	 * 
	 * @param advice 一个Advice, 例如 BeforeAdvice
	 * 
	 * @return 此适配器是否理解给定的建议对象
	 */
	boolean supportsAdvice(Advice advice);

	/**
	 * 返回AOP Alliance MethodInterceptor，将给定增强的行为公开给基于拦截的AOP框架.
	 * <p>不要担心切面中包含的任何Pointcut;  AOP框架将负责检查切点.
	 * 
	 * @param advisor Advisor. supportsAdvice()方法必须在此对象上返回true
	 * 
	 * @return 这个Advisor的AOP联盟拦截器. 无需缓存实例以提高效率, 因为AOP框架缓存了增强链.
	 */
	MethodInterceptor getInterceptor(Advisor advisor);

}
