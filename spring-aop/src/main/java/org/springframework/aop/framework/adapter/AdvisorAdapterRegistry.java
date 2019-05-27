package org.springframework.aop.framework.adapter;

import org.aopalliance.intercept.MethodInterceptor;

import org.springframework.aop.Advisor;

/**
 * 用于Advisor适配器的注册表的接口.
 *
 * <p><i>这是一个SPI接口, 任何Spring用户都不能实现.</i>
 */
public interface AdvisorAdapterRegistry {

	/**
	 * 返回包含给定增强的{@link Advisor}.
	 * <p>默认至少支持
	 * {@link org.aopalliance.intercept.MethodInterceptor},
	 * {@link org.springframework.aop.MethodBeforeAdvice},
	 * {@link org.springframework.aop.AfterReturningAdvice},
	 * {@link org.springframework.aop.ThrowsAdvice}.
	 * 
	 * @param advice 应该是增强对象
	 * 
	 * @return 包含给定增强的{@link Advisor} (never {@code null}; 如果 advice 参数是 Advisor, 它按原样返回)
	 * @throws UnknownAdviceTypeException 如果没有注册的切面适配器可以包装增强
	 */
	Advisor wrap(Object advice) throws UnknownAdviceTypeException;

	/**
	 * 返回一组AOP Alliance MethodInterceptors，以允许在基于拦截的框架中使用给定的Advisor.
	 * <p>不要担心与{@link Advisor}相关的切点, 如果它是一个{@link org.springframework.aop.PointcutAdvisor}: 返回一个拦截器.
	 * 
	 * @param advisor 要寻找拦截器的Advisor
	 * 
	 * @return 一组MethodInterceptor，用于公开此Advisor的行为
	 * @throws UnknownAdviceTypeException 如果所有已注册的AdvisorAdapter都不理解Advisor的类型
	 */
	MethodInterceptor[] getInterceptors(Advisor advisor) throws UnknownAdviceTypeException;

	/**
	 * 注册指定的{@link AdvisorAdapter}.
	 * 请注意，没有必要为AOP联盟拦截器或Spring增强注册适配器: 这些必须由{@code AdvisorAdapterRegistry}实现自动识别.
	 * 
	 * @param adapter 理解特定的Advisor或Advice类型的AdvisorAdapter
	 */
	void registerAdvisorAdapter(AdvisorAdapter adapter);

}
