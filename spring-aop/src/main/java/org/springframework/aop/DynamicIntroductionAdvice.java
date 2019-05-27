package org.springframework.aop;

import org.aopalliance.aop.Advice;

/**
 * AOP Alliance Advice的子接口，允许增强实现其他接口，并通过使用该拦截器的代理提供.
 * 这是一个基本的AOP概念: <b>introduction</b>.
 *
 * <p>引入经常<b>mixins</b>, 支持构建Java中多重继承的多个目标的复合对象.
 *
 * <p>对比 {@link IntroductionInfo}, 该接口允许增强实现一系列不必事先知道的接口.
 * 因此, {@link IntroductionAdvisor} 可用于指定将在增强对象中公开哪些接口.
 */
public interface DynamicIntroductionAdvice extends Advice {

	/**
	 * 此引入增强是否实现给定的接口?
	 * 
	 * @param intf 要检查的接口
	 * 
	 * @return 增强是否实现指定的接口
	 */
	boolean implementsInterface(Class<?> intf);

}
