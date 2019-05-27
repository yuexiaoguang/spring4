package org.springframework.aop.support;

import java.io.Serializable;

import org.aopalliance.aop.Advice;

import org.springframework.aop.Pointcut;

/**
 * 方便的Pointcut驱动的Advisor实现.
 *
 * <p>这是最常用的Advisor实现. 它可以与任何切点和增强类型一起使用, 除了引用.
 * 通常不需要为此类创建子类, 或实现自定义Advisor.
 */
@SuppressWarnings("serial")
public class DefaultPointcutAdvisor extends AbstractGenericPointcutAdvisor implements Serializable {

	private Pointcut pointcut = Pointcut.TRUE;


	/**
	 * <p>必须在使用setter方法之前设置增强. 通常也会设置切点, 但默认是 {@code Pointcut.TRUE}.
	 */
	public DefaultPointcutAdvisor() {
	}

	/**
	 * 创建一个匹配所有方法的DefaultPointcutAdvisor.
	 * <p>{@code Pointcut.TRUE}将用作 Pointcut.
	 * 
	 * @param advice 要使用的Advice
	 */
	public DefaultPointcutAdvisor(Advice advice) {
		this(Pointcut.TRUE, advice);
	}

	/**
	 * @param pointcut 针对增强的切点
	 * @param advice Pointcut匹配时运行的增强
	 */
	public DefaultPointcutAdvisor(Pointcut pointcut, Advice advice) {
		this.pointcut = pointcut;
		setAdvice(advice);
	}


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
		return getClass().getName() + ": pointcut [" + getPointcut() + "]; advice [" + getAdvice() + "]";
	}

}
