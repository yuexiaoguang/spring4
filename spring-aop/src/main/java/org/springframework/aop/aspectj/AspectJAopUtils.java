package org.springframework.aop.aspectj;

import org.aopalliance.aop.Advice;

import org.springframework.aop.Advisor;
import org.springframework.aop.AfterAdvice;
import org.springframework.aop.BeforeAdvice;

/**
 * 处理AspectJ切面的实用方法.
 */
public abstract class AspectJAopUtils {

	/**
	 * 如果切面是前置增强, 返回 {@code true}.
	 */
	public static boolean isBeforeAdvice(Advisor anAdvisor) {
		AspectJPrecedenceInformation precedenceInfo = getAspectJPrecedenceInformationFor(anAdvisor);
		if (precedenceInfo != null) {
			return precedenceInfo.isBeforeAdvice();
		}
		return (anAdvisor.getAdvice() instanceof BeforeAdvice);
	}

	/**
	 * 如果切面是后置增强, 返回 {@code true}.
	 */
	public static boolean isAfterAdvice(Advisor anAdvisor) {
		AspectJPrecedenceInformation precedenceInfo = getAspectJPrecedenceInformationFor(anAdvisor);
		if (precedenceInfo != null) {
			return precedenceInfo.isAfterAdvice();
		}
		return (anAdvisor.getAdvice() instanceof AfterAdvice);
	}

	/**
	 * 返回切面或它的增强提供的 AspectJPrecedenceInformation.
	 * 如果切面和增强都没有优先信息, 这个方法返回 {@code null}.
	 */
	public static AspectJPrecedenceInformation getAspectJPrecedenceInformationFor(Advisor anAdvisor) {
		if (anAdvisor instanceof AspectJPrecedenceInformation) {
			return (AspectJPrecedenceInformation) anAdvisor;
		}
		Advice advice = anAdvisor.getAdvice();
		if (advice instanceof AspectJPrecedenceInformation) {
			return (AspectJPrecedenceInformation) advice;
		}
		return null;
	}

}
