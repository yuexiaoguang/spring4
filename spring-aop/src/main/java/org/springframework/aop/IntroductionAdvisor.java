package org.springframework.aop;

/**
 * 执行一个或多个AOP <b>introductions</b>的切面的父级接口.
 *
 * <p>此接口无法直接实现; 子接口必须提供实现引入的增强类型.
 *
 * <p>引入是附加接口通过AOP增强的实现(不是由目标实现).
 */
public interface IntroductionAdvisor extends Advisor, IntroductionInfo {

	/**
	 * 返回过滤器，确定此引入应适用于哪些目标类.
	 * <p>表示切点的类部分. 请注意，方法匹配对引入没有意义.
	 * 
	 * @return 类过滤器
	 */
	ClassFilter getClassFilter();

	/**
	 * 被增强的接口是否可以通过引入增强实现? 在添加IntroductionAdvisor之前调用.
	 * 
	 * @throws IllegalArgumentException 如果被增强的接口接口不能通过引入增强实现
	 */
	void validateInterfaces() throws IllegalArgumentException;

}
