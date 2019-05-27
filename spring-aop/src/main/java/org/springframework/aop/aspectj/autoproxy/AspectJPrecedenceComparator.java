package org.springframework.aop.aspectj.autoproxy;

import java.util.Comparator;

import org.springframework.aop.Advisor;
import org.springframework.aop.aspectj.AspectJAopUtils;
import org.springframework.aop.aspectj.AspectJPrecedenceInformation;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.util.Assert;

/**
 * 按优先顺序排序AspectJ增强/切面 (不是调用顺序).
 *
 * <p>给定两个增强, {@code a} 和 {@code b}:
 * <ul>
 * <li>如果{@code a} 和 {@code b}定义在不同的切面, 那么具有最低排序值的切面的增强拥有最高的优先级</li>
 * <li>如果{@code a} 和 {@code b}定义在同一个的切面, 那么如果{@code a} 或 {@code b} 是一个后置增强,
 * 那么切面中最后声明的增强拥有最高的优先级.
 * 如果{@code a} 和 {@code b}都不是后置增强, 那么切面中第一个声明的增强拥有最高的优先级.</li>
 * </ul>
 *
 * <p>Important: 请注意，与普通比较器不同，返回0意味着我们不关心排序，而不是必须对两个元素进行相同的排序.
 * 与AspectJ PartialOrder类一起使用.
 */
class AspectJPrecedenceComparator implements Comparator<Advisor> {

	private static final int HIGHER_PRECEDENCE = -1;

	private static final int SAME_PRECEDENCE = 0;

	private static final int LOWER_PRECEDENCE = 1;


	private final Comparator<? super Advisor> advisorComparator;


	public AspectJPrecedenceComparator() {
		this.advisorComparator = AnnotationAwareOrderComparator.INSTANCE;
	}

	/**
	 * @param advisorComparator 用于比较Advisor的Comparator
	 */
	public AspectJPrecedenceComparator(Comparator<? super Advisor> advisorComparator) {
		Assert.notNull(advisorComparator, "Advisor comparator must not be null");
		this.advisorComparator = advisorComparator;
	}


	@Override
	public int compare(Advisor o1, Advisor o2) {
		int advisorPrecedence = this.advisorComparator.compare(o1, o2);
		if (advisorPrecedence == SAME_PRECEDENCE && declaredInSameAspect(o1, o2)) {
			advisorPrecedence = comparePrecedenceWithinAspect(o1, o2);
		}
		return advisorPrecedence;
	}

	private int comparePrecedenceWithinAspect(Advisor advisor1, Advisor advisor2) {
		boolean oneOrOtherIsAfterAdvice =
				(AspectJAopUtils.isAfterAdvice(advisor1) || AspectJAopUtils.isAfterAdvice(advisor2));
		int adviceDeclarationOrderDelta = getAspectDeclarationOrder(advisor1) - getAspectDeclarationOrder(advisor2);

		if (oneOrOtherIsAfterAdvice) {
			// 最后声明的增强具有更高的优先级
			if (adviceDeclarationOrderDelta < 0) {
				// advice1 在 advice2之前声明, 因此advice1具有较低的优先级
				return LOWER_PRECEDENCE;
			}
			else if (adviceDeclarationOrderDelta == 0) {
				return SAME_PRECEDENCE;
			}
			else {
				return HIGHER_PRECEDENCE;
			}
		}
		else {
			// 第一个声明的增强具有更高的优先级
			if (adviceDeclarationOrderDelta < 0) {
				// advice1 在 advice2之前声明, 因此advice1具有较高的优先级
				return HIGHER_PRECEDENCE;
			}
			else if (adviceDeclarationOrderDelta == 0) {
				return SAME_PRECEDENCE;
			}
			else {
				return LOWER_PRECEDENCE;
			}
		}
	}

	private boolean declaredInSameAspect(Advisor advisor1, Advisor advisor2) {
		return (hasAspectName(advisor1) && hasAspectName(advisor2) &&
				getAspectName(advisor1).equals(getAspectName(advisor2)));
	}

	private boolean hasAspectName(Advisor anAdvisor) {
		return (anAdvisor instanceof AspectJPrecedenceInformation ||
				anAdvisor.getAdvice() instanceof AspectJPrecedenceInformation);
	}

	// 前置条件是hasAspectName返回true
	private String getAspectName(Advisor anAdvisor) {
		return AspectJAopUtils.getAspectJPrecedenceInformationFor(anAdvisor).getAspectName();
	}

	private int getAspectDeclarationOrder(Advisor anAdvisor) {
		AspectJPrecedenceInformation precedenceInfo =
			AspectJAopUtils.getAspectJPrecedenceInformationFor(anAdvisor);
		if (precedenceInfo != null) {
			return precedenceInfo.getDeclarationOrder();
		}
		else {
			return 0;
		}
	}
}
