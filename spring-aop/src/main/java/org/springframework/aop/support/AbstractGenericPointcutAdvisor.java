package org.springframework.aop.support;

import org.aopalliance.aop.Advice;

/**
 * 抽象通用PointcutAdvisor，允许配置任何增强.
 */
@SuppressWarnings("serial")
public abstract class AbstractGenericPointcutAdvisor extends AbstractPointcutAdvisor {

	private Advice advice;


	/**
	 * 指定此切面应该应用的增强.
	 */
	public void setAdvice(Advice advice) {
		this.advice = advice;
	}

	@Override
	public Advice getAdvice() {
		return this.advice;
	}


	@Override
	public String toString() {
		return getClass().getName() + ": advice [" + getAdvice() + "]";
	}

}
