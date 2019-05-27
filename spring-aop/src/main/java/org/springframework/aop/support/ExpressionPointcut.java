package org.springframework.aop.support;

import org.springframework.aop.Pointcut;

/**
 * 由使用String表达式的切点实现的接口.
 */
public interface ExpressionPointcut extends Pointcut {

	/**
	 * 返回此切点的String表达式.
	 */
	String getExpression();

}
