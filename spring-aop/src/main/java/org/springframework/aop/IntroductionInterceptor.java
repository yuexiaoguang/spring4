package org.springframework.aop;

import org.aopalliance.intercept.MethodInterceptor;

/**
 * AOP Alliance MethodInterceptor的子接口，允许拦截器实现其他接口，并通过使用该拦截器的代理提供.
 * 这是一个基本的AOP概念: <b>introduction</b>.
 *
 * <p>介绍通常是<b>mixins</b>, 支持构建Java中多重继承的多个目标的复合对象.
 */
public interface IntroductionInterceptor extends MethodInterceptor, DynamicIntroductionAdvice {

}
