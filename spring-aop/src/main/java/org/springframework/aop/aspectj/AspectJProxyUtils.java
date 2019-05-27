package org.springframework.aop.aspectj;

import java.util.List;

import org.springframework.aop.Advisor;
import org.springframework.aop.PointcutAdvisor;
import org.springframework.aop.interceptor.ExposeInvocationInterceptor;

/**
 * 使用AspectJ代理的实用方法.
 */
public abstract class AspectJProxyUtils {

	/**
	 * 如有必要，添加特殊切面以使用包含AspectJ切面的代理链.
	 * 这将公开当前的Spring AOP调用 (某些AspectJ切点匹配所必需的), 并提供当前的AspectJ JoinPoint.
	 * 如果切面链中没有AspectJ切面, 则调用将不起作用.
	 * 
	 * @param advisors 可用的切面
	 * 
	 * @return {@code true} 如果添加了特殊的{@link Advisor Advisors}, 否则{@code false}.
	 */
	public static boolean makeAdvisorChainAspectJCapableIfNecessary(List<Advisor> advisors) {
		// 不要将切面添加到空列表中; 可能表示不需要代理
		if (!advisors.isEmpty()) {
			boolean foundAspectJAdvice = false;
			for (Advisor advisor : advisors) {
				// 小心不要在没有验证的情况下获得增强, 因为这可能会实时地实例化非单例AspectJ切面
				if (isAspectJAdvice(advisor)) {
					foundAspectJAdvice = true;
				}
			}
			if (foundAspectJAdvice && !advisors.contains(ExposeInvocationInterceptor.ADVISOR)) {
				advisors.add(0, ExposeInvocationInterceptor.ADVISOR);
				return true;
			}
		}
		return false;
	}

	/**
	 * 确定指定的Advisor是否包含一个AspectJ 增强.
	 * 
	 * @param advisor 要检查的Advisor
	 */
	private static boolean isAspectJAdvice(Advisor advisor) {
		return (advisor instanceof InstantiationModelAwarePointcutAdvisor ||
				advisor.getAdvice() instanceof AbstractAspectJAdvice ||
				(advisor instanceof PointcutAdvisor &&
						 ((PointcutAdvisor) advisor).getPointcut() instanceof AspectJExpressionPointcut));
	}

}
