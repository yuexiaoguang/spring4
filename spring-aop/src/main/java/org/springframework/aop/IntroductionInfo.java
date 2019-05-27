package org.springframework.aop;

/**
 * 提供描述引入所需信息的接口.
 *
 * <p>{@link IntroductionAdvisor IntroductionAdvisors} 必须实现这个接口.
 * 如果{@link org.aopalliance.aop.Advice}实现了这个接口, 它将作为一个引入使用, 而不是一个 {@link IntroductionAdvisor}.
 * 在这种情况下, 增强是自我描述的, 不仅提供必要的行为, 也描述它引入的接口.
 */
public interface IntroductionInfo {

	/**
	 * 返回此Advisor或Advice引入的其他接口.
	 * 
	 * @return 引入的接口
	 */
	Class<?>[] getInterfaces();

}
